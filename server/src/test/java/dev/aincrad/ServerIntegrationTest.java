package dev.aincrad;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerIntegrationTest {
 static Path root;static {try{root=Files.createTempDirectory("aincrad-api-test-");Files.copy(Path.of("../world/world.json"),root.resolve("world.json"));}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("aincrad.world",()->root.resolve("world.json").toString());r.add("aincrad.data",()->root.resolve("data").toString());r.add("aincrad.root-password",()->"IntegrationTestOnly-123");}
 @LocalServerPort int port;ObjectMapper json=new ObjectMapper();HttpClient client=HttpClient.newHttpClient();
 String base(){return "http://127.0.0.1:"+port;}
 HttpResponse<String> request(String path,String method,String body,String cookie,String origin)throws Exception {var b=HttpRequest.newBuilder(URI.create(base()+path)).timeout(Duration.ofSeconds(8)).header("Content-Type","application/json").header("X-Aincrad-Admin","1").header("X-Aincrad-Player","1");if(cookie!=null)b.header("Cookie",cookie);if(origin!=null)b.header("Origin",origin);b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));return client.send(b.build(),HttpResponse.BodyHandlers.ofString());}
 String login()throws Exception{var r=request("/api/admin/login","POST","{\"username\":\"root\",\"password\":\"IntegrationTestOnly-123\"}",null,"http://localhost:8080");assertEquals(200,r.statusCode(),r.body());String c=r.headers().firstValue("set-cookie").orElseThrow();assertTrue(c.contains("HttpOnly"));assertTrue(c.contains("SameSite=Strict"));return c.split(";",2)[0];}
 @Test @Order(1) void adminAuthValidationBackupAndReload()throws Exception {
  assertEquals(403,request("/api/admin/world","GET",null,null,null).statusCode());
  assertEquals(403,request("/api/admin/login","POST","{\"username\":\"root\",\"password\":\"IntegrationTestOnly-123\"}",null,"http://malicious.invalid").statusCode());
  String cookie=login();ObjectNode config=(ObjectNode)json.readTree(request("/api/admin/world","GET",null,cookie,null).body());
  ((ObjectNode)config.path("floors").get(0).get("portal")).put("to",999);
  assertEquals(400,request("/api/admin/world","PUT",config.toString(),cookie,null).statusCode());
  assertFalse(Files.exists(root.resolve("data/backups")));
  ((ObjectNode)config.path("floors").get(0).get("portal")).put("to",2);config.put("seed",123456);
  assertEquals(200,request("/api/admin/world","PUT",config.toString(),cookie,null).statusCode());
  assertTrue(Files.list(root.resolve("data/backups")).anyMatch(p->p.getFileName().toString().startsWith("world-")));
  assertEquals(200,request("/api/admin/reload","POST","{}",cookie,null).statusCode());
  assertEquals(123456,json.readTree(request("/api/world","GET",null,null,null).body()).path("seed").asInt());
  assertEquals(403,request("/api/admin/ai","GET",null,null,null).statusCode());
  ObjectNode ai=(ObjectNode)json.readTree(request("/api/admin/ai","GET",null,cookie,null).body());ai.put("maxConcurrent",1000);
  assertEquals(400,request("/api/admin/ai","PUT",ai.toString(),cookie,null).statusCode());
  assertEquals(200,request("/api/admin/logout","POST","{}",cookie,null).statusCode());assertEquals(403,request("/api/admin/world","GET",null,cookie,null).statusCode());
 }
 static class Listener implements WebSocket.Listener {final BlockingQueue<String> queue=new LinkedBlockingQueue<>();StringBuilder partial=new StringBuilder();public void onOpen(WebSocket socket){socket.request(1);}public CompletionStage<?> onText(WebSocket socket,CharSequence text,boolean last){partial.append(text);if(last){queue.add(partial.toString());partial.setLength(0);}socket.request(1);return null;}}
 JsonNode await(Listener l,String type)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<end){String s=l.queue.poll(250,TimeUnit.MILLISECONDS);if(s!=null){JsonNode n=json.readTree(s);if(n.path("type").asText().equals(type))return n;}}throw new AssertionError("No llegó "+type);}
 WebSocket connect(Listener l)throws Exception{String username="player"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,12);String payload=json.writeValueAsString(java.util.Map.of("username",username,"email",username+"@example.test","password","Player-test-only-987","confirmPassword","Player-test-only-987"));assertEquals(201,request("/api/player/register","POST",payload,null,"http://localhost:8080").statusCode());var response=request("/api/player/login","POST",payload,null,"http://localhost:8080");assertEquals(200,response.statusCode());String cookie=response.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];return client.newWebSocketBuilder().header("Cookie",cookie).header("Origin","http://localhost:8080").buildAsync(URI.create("ws://127.0.0.1:"+port+"/ws"),l).get(5,TimeUnit.SECONDS);}
 @Test @Order(2) void realWebSocketTwoPlayersShareAuthoritativeMovement()throws Exception{
  Listener a=new Listener(),b=new Listener();WebSocket one=connect(a),two=connect(b);
  try{one.sendText("{\"type\":\"join\",\"name\":\"Uno\"}",true).join();String id=await(a,"welcome").path("id").asText();two.sendText("{\"type\":\"join\",\"name\":\"Dos\"}",true).join();await(b,"welcome");JsonNode state=await(b,"state");assertEquals(2,state.path("players").size());one.sendText("{\"type\":\"input\",\"seq\":1,\"dx\":0,\"dz\":-1,\"x\":9000}",true).join();Thread.sleep(300);b.queue.clear();state=await(b,"state");JsonNode player=null;for(JsonNode p:state.path("players"))if(p.path("id").asText().equals(id))player=p;assertNotNull(player);assertTrue(player.path("z").asDouble()<15.5);assertEquals(0,player.path("x").asDouble());one.sendText("{\"type\":\"chat\",\"text\":\"Hola compañeros\"}",true).join();assertEquals("Hola compañeros",await(b,"chat").path("text").asText());one.sendText("{\"type\":\"npcChat\",\"npcId\":\"fake\",\"text\":\"hola\"}",true).join();assertEquals("error",await(a,"error").path("type").asText());}finally{one.sendClose(1000,"test").join();two.sendClose(1000,"test").join();}
 }
 @Test @Order(3) void wrongSocketOriginIsRejected(){assertThrows(ExecutionException.class,()->client.newWebSocketBuilder().header("Origin","http://malicious.invalid").buildAsync(URI.create("ws://127.0.0.1:"+port+"/ws"),new Listener()).get(5,TimeUnit.SECONDS));}
 @Test @Order(4) void rootCharacterApiDoesNotExposePlayerSecrets()throws Exception{assertEquals(403,request("/api/admin/characters","GET",null,null,null).statusCode());String cookie=login();var response=request("/api/admin/characters","GET",null,cookie,null);assertEquals(200,response.statusCode());assertFalse(response.body().contains("tokenHash"));assertFalse(response.body().contains("token"));JsonNode p=json.readTree(response.body()).get(0);ObjectNode edit=json.createObjectNode();edit.put("name","Configurado");edit.set("appearance",p.get("appearance"));edit.put("xp",150);edit.put("col",77);edit.put("potions",9);String path="/api/admin/characters/"+p.path("id").asText();assertEquals(403,request(path,"PATCH",edit.toString(),null,null).statusCode());var updated=request(path,"PATCH",edit.toString(),cookie,null);assertEquals(200,updated.statusCode(),updated.body());assertEquals(77,json.readTree(updated.body()).path("col").asInt());assertEquals(2,json.readTree(updated.body()).path("level").asInt());}
 @Test @Order(5) void newCharacterMutationsRequireRootAndValidatePayloads()throws Exception{String cookie=login();var p=json.readTree(request("/api/admin/characters","GET",null,cookie,null).body()).get(0);String path="/api/admin/characters/"+p.path("id").asText();for(String endpoint:new String[]{"weapons","citizenship","crafting"})assertEquals(403,request(path+"/"+endpoint,"PATCH","{}",null,null).statusCode());assertEquals(200,request(path+"/weapons","PATCH","{\"weaponSetId\":\"\"}",cookie,null).statusCode());assertEquals(400,request(path+"/citizenship","PATCH","{\"citizenship\":1.5}",cookie,null).statusCode());assertEquals(200,request(path+"/citizenship","PATCH","{\"citizenship\":100,\"pardon\":true}",cookie,null).statusCode());assertEquals(200,request(path+"/crafting","PATCH","{\"professionXp\":{\"mining\":100},\"materials\":{\"iron\":50}}",cookie,null).statusCode());assertEquals(400,request(path+"/crafting","PATCH","{\"professionXp\":{},\"materials\":{\"iron\":-1}}",cookie,null).statusCode());}

 JsonNode ownPlayer(JsonNode state,String id){for(JsonNode p:state.path("players"))if(p.path("id").asText().equals(id))return p;throw new AssertionError("Jugador ausente");}
 JsonNode awaitPlayer(Listener listener,String id,java.util.function.Predicate<JsonNode> condition)throws Exception{
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
  while(System.nanoTime()<end){JsonNode p=ownPlayer(await(listener,"state"),id);if(condition.test(p))return p;}
  throw new AssertionError("No llegó el estado esperado del jugador");
 }
 JsonNode awaitPosition(Listener listener,String id,double x,double z)throws Exception{
  return awaitPlayer(listener,id,p->Math.hypot(p.path("x").asDouble()-x,p.path("z").asDouble()-z)<1e-6);
 }
 @Test @Order(6) void realWebSocketClickDetoursAndKeepsMoveInputStopAndErrorProtocol()throws Exception{
  Listener listener=new Listener();WebSocket socket=connect(listener);
  try{
   socket.sendText("{\"type\":\"join\",\"name\":\"ClicRuta\"}",true).join();String id=await(listener,"welcome").path("id").asText();
   socket.sendText("{\"type\":\"move\",\"x\":4,\"z\":19}",true).join();awaitPosition(listener,id,4,19);
   socket.sendText("{\"type\":\"move\",\"x\":13.6,\"z\":19,\"hp\":999,\"col\":9999,\"speed\":999,\"route\":[{\"x\":10,\"z\":19}]}",true).join();
   // Normal idle input from the existing browser must not cancel a click route.
   socket.sendText("{\"type\":\"input\",\"seq\":10,\"dx\":0,\"dz\":0}",true).join();
   double lastX=4,lastZ=19,lastTime=-1;boolean detoured=false,arrived=false;
   long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
   while(System.nanoTime()<deadline){
    JsonNode state=await(listener,"state"),p=ownPlayer(state,id);
    if(p.path("sequence").asLong()<10)continue;
    double x=p.path("x").asDouble(),z=p.path("z").asDouble(),time=state.path("time").asDouble();
    assertTrue(Math.hypot(x-10,z-19)>=3.5-1e-8,"Entered the authoritative building collision");
    if(lastTime>=0)assertTrue(Math.hypot(x-lastX,z-lastZ)<=p.path("speed").asDouble()*(time-lastTime)+1e-8,"Movement exceeded server speed");
    assertEquals(100,p.path("hp").asInt());assertEquals(0,p.path("col").asInt());assertFalse(p.has("route"));
    detoured|=Math.abs(z-19)>3;lastX=x;lastZ=z;lastTime=time;
    if(Math.hypot(x-13.6,z-19)<1e-6){arrived=true;break;}
   }
   assertTrue(detoured);assertTrue(arrived);
   for(String invalid:new String[]{"{\"type\":\"move\",\"x\":10,\"z\":19}","{\"type\":\"move\",\"x\":9999,\"z\":0}","{\"type\":\"move\",\"x\":\"NaN\",\"z\":0}","{\"type\":\"path\",\"points\":[]}"}){
    socket.sendText(invalid,true).join();assertFalse(await(listener,"error").path("text").asText().isEmpty());
   }
   socket.sendText("{\"type\":\"move\",\"x\":0,\"z\":16}",true).join();
   awaitPlayer(listener,id,p->p.path("x").asDouble()<13.2);
   socket.sendText("{\"type\":\"stop\"}",true).join();socket.sendText("{\"type\":\"input\",\"seq\":11,\"dx\":0,\"dz\":0}",true).join();
   JsonNode stopped=awaitPlayer(listener,id,p->p.path("sequence").asLong()>=11);
   socket.sendText("{\"type\":\"input\",\"seq\":10,\"dx\":1,\"dz\":0}",true).join();
   for(int i=0;i<5;i++){
    var p=ownPlayer(await(listener,"state"),id);assertEquals(stopped.path("x").asDouble(),p.path("x").asDouble());assertEquals(stopped.path("z").asDouble(),p.path("z").asDouble());
   }
  }finally{socket.sendClose(1000,"click-test").join();}
 }

}
