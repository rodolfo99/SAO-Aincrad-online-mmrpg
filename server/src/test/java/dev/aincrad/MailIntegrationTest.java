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
class MailIntegrationTest {
    static Path dir;static SmtpInbox smtp;static final String ORIGIN="http://localhost:8080",PASSWORD="Correo-original-987",NEXT="Correo-restablecido-123";
    static{try{dir=Files.createTempDirectory("aincrad-mail-");Files.copy(Path.of("../world/world.json"),dir.resolve("world.json"));smtp=new SmtpInbox();}catch(Exception e){throw new RuntimeException(e);}}
    @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("aincrad.data",()->dir.resolve("data").toString());r.add("aincrad.world",()->dir.resolve("world.json").toString());r.add("aincrad.root-password",()->"Root-test-mail-987");r.add("aincrad.mail.enabled",()->true);r.add("aincrad.mail.from",()->"noreply@[203.0.113.10]");r.add("aincrad.public-url",()->ORIGIN);r.add("spring.mail.host",()->"127.0.0.1");r.add("spring.mail.port",smtp::port);}
    @LocalServerPort int port;@Autowired Application.GameServer game;
    ObjectMapper json=new ObjectMapper();HttpClient client=HttpClient.newHttpClient();
    HttpResponse<String> request(String path,Map<String,?> payload,String cookie,String origin)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("X-Aincrad-Player","1").header("X-Aincrad-Admin","1");
        if(cookie!=null)b.header("Cookie",cookie);if(origin!=null)b.header("Origin",origin);
        if(payload!=null)b.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> post(String path,Map<String,?> body,String cookie)throws Exception{return request(path,body,cookie,ORIGIN);}
    String cookie(HttpResponse<String> r){assertEquals(200,r.statusCode(),r.body());return r.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];}
    @AfterAll static void close()throws Exception{smtp.close();}
    @Test void fullEmailRecoveryUsesActualSmtpAndInvalidatesLiveSession()throws Exception{
        assertEquals(201,post("/api/player/register",Map.of("username","recupera","email","recipient@example.test","password",PASSWORD,"confirmPassword",PASSWORD),null).statusCode());
        String session=cookie(post("/api/player/login",Map.of("username","recupera","password",PASSWORD),null));
        var listener=new PlayerAccessIntegrationTest.Listener();var ws=client.newWebSocketBuilder().header("Origin",ORIGIN).header("Cookie",session).buildAsync(URI.create("ws://127.0.0.1:"+port+"/ws"),listener).get(5,TimeUnit.SECONDS);
        ws.sendText("{\"type\":\"join\",\"name\":\"Correo\"}",true).join();assertNotNull(listener.messages.poll(5,TimeUnit.SECONDS));
        var known=post("/api/player/forgot-password",Map.of("email","recipient@example.test"),null);var unknown=post("/api/player/forgot-password",Map.of("email","unknown@example.test"),null);
        assertEquals(202,known.statusCode());assertEquals(known.body(),unknown.body());assertFalse(known.body().contains("token"));assertEquals("no-store",known.headers().firstValue("Cache-Control").orElseThrow());
        var email=smtp.await("Restablecer");assertEquals("noreply@[203.0.113.10]",email.from());assertEquals("recipient@example.test",email.to());assertFalse(email.text().contains(PASSWORD));
        String link=Arrays.stream(email.text().split("\\s+")).filter(x->x.startsWith("http")).findFirst().orElseThrow();assertTrue(link.startsWith(ORIGIN+"/#reset-password="));String token=URI.create(link).getFragment().split("=",2)[1];
        assertFalse(Files.readString(dir.resolve("data/accounts.json")).contains(token));assertTrue(json.readTree(request("/api/player/session",null,session,null).body()).path("authenticated").asBoolean());
        var reset=post("/api/player/reset-password",Map.of("token",token,"password",NEXT,"confirmPassword",NEXT),null);assertEquals(200,reset.statusCode(),reset.body());assertEquals(4401,listener.closed.get(5,TimeUnit.SECONDS));
        assertFalse(json.readTree(request("/api/player/session",null,session,null).body()).path("authenticated").asBoolean());assertEquals(400,post("/api/player/reset-password",Map.of("token",token,"password",PASSWORD,"confirmPassword",PASSWORD),null).statusCode());
        assertEquals(401,post("/api/player/login",Map.of("username","recupera","password",PASSWORD),null).statusCode());String restored=cookie(post("/api/player/login",Map.of("username","recupera","password",NEXT),null));assertEquals(1,json.readTree(request("/api/player/session",null,restored,null).body()).path("characters").size());smtp.await("actualizada");
    }
    @Test void mailStatusAndTestAreRootOnlyAndRejectCrossOrigin()throws Exception{
        assertEquals(403,request("/api/admin/mail",null,null,null).statusCode());assertEquals(403,post("/api/admin/mail/test",Map.of("email","root@example.test"),null).statusCode());
        String root=cookie(post("/api/admin/login",Map.of("username","root","password","Root-test-mail-987"),null));var status=request("/api/admin/mail",null,root,null);assertEquals(200,status.statusCode());assertFalse(status.body().contains("password"));
        assertEquals(403,request("/api/admin/mail/test",Map.of("email","root@example.test"),root,"https://evil.invalid").statusCode());assertEquals(200,post("/api/admin/mail/test",Map.of("email","root@example.test"),root).statusCode());assertEquals("root@example.test",smtp.await("Prueba").to());
    }
    @Test void forgotAndEmailChangeRejectUnauthorizedOriginsAndHeaders()throws Exception{
        assertEquals(403,request("/api/player/forgot-password",Map.of("email","recipient@example.test"),null,"http://evil.invalid").statusCode());
        assertEquals(400,post("/api/player/forgot-password",Map.of("email","a@example.test\r\nBcc:b@example.test"),null).statusCode());
        assertEquals(401,post("/api/player/email",Map.of("email","new@example.test","currentPassword",PASSWORD),null).statusCode());
    }
}
