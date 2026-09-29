package dev.aincrad;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlayerAccessIntegrationTest {
    static final String PASSWORD="Cuenta-pruebas-456",ORIGIN="http://localhost:8080";
    static Path root;
    static {try{root=Files.createTempDirectory("aincrad-player-auth-");Files.copy(Path.of("../world/world.json"),root.resolve("world.json"));}catch(Exception e){throw new RuntimeException(e);}}
    @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("aincrad.world",()->root.resolve("world.json").toString());r.add("aincrad.data",()->root.resolve("data").toString());r.add("aincrad.root-password",()->"Root-isolated-987");r.add("aincrad.player-cookie-secure",()->true);}
    @LocalServerPort int port;
    @Autowired Application.GameServer game;
    ObjectMapper json=new ObjectMapper();HttpClient client=HttpClient.newHttpClient();
    static String aliceCharacter,rootCookie,alicePassword=PASSWORD;
    String base(){return "http://127.0.0.1:"+port;}
    HttpResponse<String> request(String path,String method,Map<String,?> body,String cookie,String origin,boolean header)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create(base()+path)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json");
        if(header){builder.header("X-Aincrad-Player","1");builder.header("X-Aincrad-Admin","1");}
        if(cookie!=null)builder.header("Cookie",cookie);if(origin!=null)builder.header("Origin",origin);
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> post(String path,Map<String,?> body,String cookie)throws Exception{return request(path,"POST",body,cookie,ORIGIN,true);}
    JsonNode session(String cookie)throws Exception{return json.readTree(request("/api/player/session","GET",null,cookie,null,true).body());}
    String cookie(HttpResponse<String> response){return response.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];}
    String login(String name,String password)throws Exception{var response=post("/api/player/login",Map.of("username",name,"password",password),null);assertEquals(200,response.statusCode(),response.body());return cookie(response);}
    String admin()throws Exception{if(rootCookie==null){var response=post("/api/admin/login",Map.of("username","root","password","Root-isolated-987"),null);assertEquals(200,response.statusCode());rootCookie=cookie(response);}return rootCookie;}
    static class Listener implements WebSocket.Listener {
        BlockingQueue<String> messages=new LinkedBlockingQueue<>();CompletableFuture<Integer> closed=new CompletableFuture<>();StringBuilder partial=new StringBuilder();
        public void onOpen(WebSocket socket){socket.request(1);}
        public CompletionStage<?> onText(WebSocket socket,CharSequence text,boolean last){partial.append(text);if(last){messages.add(partial.toString());partial.setLength(0);}socket.request(1);return null;}
        public CompletionStage<?> onClose(WebSocket socket,int code,String reason){closed.complete(code);return null;}
    }
    WebSocket socket(String cookie,String origin,Listener listener)throws Exception{
        var builder=client.newWebSocketBuilder();if(cookie!=null)builder.header("Cookie",cookie);if(origin!=null)builder.header("Origin",origin);
        return builder.buildAsync(URI.create("ws://127.0.0.1:"+port+"/ws"),listener).get(5,TimeUnit.SECONDS);
    }
    void rejectedSocket(String cookie,String origin,int status){var failure=assertThrows(ExecutionException.class,()->socket(cookie,origin,new Listener()));assertInstanceOf(WebSocketHandshakeException.class,failure.getCause());assertEquals(status,((WebSocketHandshakeException)failure.getCause()).getResponse().statusCode());}
    void send(WebSocket socket,Map<String,?> payload)throws Exception{socket.sendText(json.writeValueAsString(payload),true).join();}
    JsonNode await(Listener listener,String type)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<end){String message=listener.messages.poll(100,TimeUnit.MILLISECONDS);if(message!=null){var n=json.readTree(message);if(type.equals(n.path("type").asText()))return n;}}throw new AssertionError("No llegó "+type);}

    @Test @Order(1) void registrationIsNotLoginAndOriginAndSessionAreRequired()throws Exception{
        var payload=Map.of("username","alicia","email","alicia@example.test","password",PASSWORD,"confirmPassword",PASSWORD);
        assertEquals(403,request("/api/player/register","POST",payload,null,"http://evil.invalid",true).statusCode());
        assertEquals(403,request("/api/player/register","POST",payload,null,ORIGIN,false).statusCode());
        assertEquals(403,request("/api/player/register","POST",payload,null,null,true).statusCode());
        var registered=post("/api/player/register",payload,null);assertEquals(201,registered.statusCode());assertTrue(registered.headers().allValues("Set-Cookie").isEmpty());
        var status=request("/api/player/session","GET",null,null,null,true);assertEquals("no-store",status.headers().firstValue("Cache-Control").orElseThrow());assertFalse(json.readTree(status.body()).path("authenticated").asBoolean());
        rejectedSocket(null,ORIGIN,401);rejectedSocket("aincrad_player=inventado",ORIGIN,401);rejectedSocket(admin(),ORIGIN,401);
        assertEquals(401,post("/api/player/claim-character",Map.of("legacyToken","a".repeat(43)),null).statusCode());
        assertEquals(401,post("/api/player/password",Map.of("currentPassword",PASSWORD,"password",PASSWORD,"confirmPassword",PASSWORD),null).statusCode());
    }
    @Test @Order(2) void loginProtectsCharacterOwnershipAndLogoutClosesLiveSocket()throws Exception{
        var wrong=post("/api/player/login",Map.of("username","alicia","email","alicia@example.test","password","Equivocada-123"),null);
        var unknown=post("/api/player/login",Map.of("username","inexistente","password","Equivocada-123"),null);
        assertEquals(401,wrong.statusCode());assertEquals(wrong.body(),unknown.body());
        var login=post("/api/player/login",Map.of("username","ALICIA","password",PASSWORD),null);assertEquals(200,login.statusCode());
        String full=login.headers().firstValue("Set-Cookie").orElseThrow();assertTrue(full.contains("HttpOnly"));assertTrue(full.contains("Secure"));assertTrue(full.contains("SameSite=Strict"));assertTrue(full.contains("Path=/"));String alice=cookie(login);
        rejectedSocket(alice,"http://evil.invalid",403);rejectedSocket(alice,null,403);
        assertEquals(403,request("/api/admin/accounts","GET",null,alice,null,true).statusCode());
        var a=new Listener();var one=socket(alice,ORIGIN,a);send(one,Map.of("type","join","name","Propietaria"));var welcome=await(a,"welcome");assertFalse(welcome.has("token"));aliceCharacter=welcome.path("id").asText();
        assertEquals(1,session(alice).path("characters").size());
        assertEquals(201,post("/api/player/register",Map.of("username","beatriz","email","beatriz@example.test","password",PASSWORD,"confirmPassword",PASSWORD),null).statusCode());String bob=login("beatriz",PASSWORD);assertEquals(0,session(bob).path("characters").size());
        var b=new Listener();var two=socket(bob,ORIGIN,b);
        send(two,Map.of("type","join","characterId",aliceCharacter,"accountId",game.players.require(alice.substring(alice.indexOf('=')+1)).accountId()));assertEquals("error",await(b,"error").path("type").asText());
        send(two,Map.of("type","join","name","Intrusa","token","a".repeat(43)));assertEquals("error",await(b,"error").path("type").asText());assertEquals(0,session(bob).path("characters").size());
        two.sendClose(1000,"done").join();
        var logout=post("/api/player/logout",Map.of(),alice);assertEquals(200,logout.statusCode());assertTrue(logout.headers().firstValue("Set-Cookie").orElseThrow().contains("Max-Age=0"));
        assertEquals(4401,a.closed.get(5,TimeUnit.SECONDS));assertFalse(session(alice).path("authenticated").asBoolean());rejectedSocket(alice,ORIGIN,401);
        String again=login("alicia",PASSWORD);var resumed=new Listener();var third=socket(again,ORIGIN,resumed);send(third,Map.of("type","join","characterId",aliceCharacter));assertEquals(aliceCharacter,await(resumed,"welcome").path("id").asText());post("/api/player/logout",Map.of(),again);assertEquals(4401,resumed.closed.get(5,TimeUnit.SECONDS));
    }
    @Test @Order(3) void passwordChangeAndRootResetInvalidateExistingConnections()throws Exception{
        String first=login("alicia",PASSWORD),second=login("alicia",PASSWORD);var listener=new Listener();var socket=socket(first,ORIGIN,listener);send(socket,Map.of("type","join","characterId",aliceCharacter));await(listener,"welcome");
        String next="Cambio-personal-123";
        assertEquals(401,post("/api/player/password",Map.of("currentPassword","Incorrecta-987","password",next,"confirmPassword",next),second).statusCode());assertTrue(session(first).path("authenticated").asBoolean());
        assertEquals(200,post("/api/player/password",Map.of("currentPassword",PASSWORD,"password",next,"confirmPassword",next),second).statusCode());assertEquals(4401,listener.closed.get(5,TimeUnit.SECONDS));assertFalse(session(first).path("authenticated").asBoolean());assertFalse(session(second).path("authenticated").asBoolean());
        String third=login("alicia",next);var active=new Listener();var connection=socket(third,ORIGIN,active);send(connection,Map.of("type","join","characterId",aliceCharacter));await(active,"welcome");
        String id=game.players.require(third.substring(third.indexOf('=')+1)).accountId();String path="/api/admin/accounts/"+id+"/password";
        assertEquals(403,post(path,Map.of("password","Restablecida-987"),third).statusCode());
        assertEquals(200,post(path,Map.of("password","Restablecida-987"),admin()).statusCode());assertEquals(4401,active.closed.get(5,TimeUnit.SECONDS));rejectedSocket(third,ORIGIN,401);alicePassword="Restablecida-987";
        assertFalse(Files.readString(root.resolve("data/accounts.json")).contains(alicePassword));
    }
    @Test @Order(4) void legacyClaimRequiresLoginAndCannotStealAnAlreadyLinkedCharacter()throws Exception{
        // This fixture simulates a profile made by the old release; there is no anonymous HTTP/WS route.
        String id,token; synchronized(game){var old=game.world.join("Heredado","");id=(String)old.get("id");token=(String)old.get("token");game.world.online.get(id).xp=700;game.world.online.get(id).col=237;game.world.leave(id);}
        String alice=login("alicia",alicePassword),bob=login("beatriz",PASSWORD);
        assertEquals(401,post("/api/player/claim-character",Map.of("legacyToken",token),null).statusCode());
        assertEquals(200,post("/api/player/claim-character",Map.of("legacyToken",token),alice).statusCode());assertEquals(400,post("/api/player/claim-character",Map.of("legacyToken",token),bob).statusCode());
        var l=new Listener();var socket=socket(alice,ORIGIN,l);send(socket,Map.of("type","join","characterId",id));var welcome=await(l,"welcome");assertEquals(700,welcome.path("player").path("xp").asInt());assertEquals(237,welcome.path("player").path("col").asInt());post("/api/player/logout",Map.of(),alice);assertEquals(4401,l.closed.get(5,TimeUnit.SECONDS));
        String bobId=game.players.require(bob.substring(bob.indexOf('=')+1)).accountId();String path="/api/admin/characters/"+id+"/account";
        assertEquals(403,request(path,"PATCH",Map.of("accountId",bobId),bob,ORIGIN,true).statusCode());assertEquals(200,request(path,"PATCH",Map.of("accountId",bobId),admin(),ORIGIN,true).statusCode());assertEquals(1,session(bob).path("characters").size());
        String accounts=request("/api/admin/accounts","GET",null,admin(),null,true).body();assertFalse(accounts.contains("hash"));assertFalse(accounts.contains("salt"));assertFalse(accounts.contains("password"));
    }
}
