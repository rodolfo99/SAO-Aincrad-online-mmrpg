package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

/** Actual unmodified Spring Boot release, isolated accounts/world/data, no mock game protocol. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerContractTest {
    @TempDir Path temp;
    private Process server;
    private Endpoint endpoint;
    private final String secret="JavaContract"+UUID.randomUUID();
    private JsonNode world;
    private static final class Peer implements GameClient.Listener,AutoCloseable {
        final BlockingQueue<JsonNode> messages=new LinkedBlockingQueue<>();
        final BlockingQueue<String> statuses=new LinkedBlockingQueue<>();
        volatile JsonNode snapshot=Json.EMPTY;
        final GameClient client;
        Peer(Endpoint endpoint){client=new GameClient(endpoint,Runnable::run,this);}
        public void world(JsonNode world){}
        public void status(String state,String detail){statuses.add(state);}
        public void message(JsonNode message){if(message.path("type").asText().equals("state"))snapshot=message;messages.add(message);}
        JsonNode until(Predicate<JsonNode> predicate)throws Exception {long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(System.nanoTime()<end){JsonNode m=messages.poll(100,TimeUnit.MILLISECONDS);if(m!=null&&predicate.test(m))return m;}throw new AssertionError("Mensaje esperado no recibido");}
        JsonNode type(String type)throws Exception{return until(m->m.path("type").asText().equals(type));}
        @Override public void close(){client.close();}
    }
    @BeforeEach void startServer() throws Exception {
        Path jar=Path.of(System.getProperty("sao.server.jar"));assertTrue(Files.isRegularFile(jar),"Necesitas release/aincrad-server-0.2.0.jar");Files.copy(Path.of(System.getProperty("sao.repository"),"world/world.json"),temp.resolve("world.json"));
        int port;try(var reserved=new java.net.ServerSocket(0)){port=reserved.getLocalPort();}endpoint=Endpoint.parse("http://127.0.0.1:"+port);
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-jar",jar.toAbsolutePath().toString());process.directory(Path.of(System.getProperty("sao.repository")).toFile());process.environment().putAll(Map.of("PORT",Integer.toString(port),"ALLOWED_ORIGINS",endpoint.origin(),"ROOT_PASSWORD",secret,"WORLD_FILE",temp.resolve("world.json").toString(),"DATA_DIR",temp.resolve("data").toString()));process.redirectErrorStream(true).redirectOutput(temp.resolve("server.log").toFile());server=process.start();
        HttpClient probe=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<end){try{var result=probe.send(HttpRequest.newBuilder(endpoint.api("api/world")).timeout(Duration.ofSeconds(1)).GET().build(),HttpResponse.BodyHandlers.ofString());if(result.statusCode()==200){world=Json.MAPPER.readTree(result.body());return;}}catch(Exception ignored){}if(!server.isAlive())fail(Files.readString(temp.resolve("server.log")));Thread.sleep(100);}fail("Servidor no disponible");
    }
    @AfterEach void stopServer() throws Exception {if(server!=null){server.destroy();if(!server.waitFor(6,TimeUnit.SECONDS))server.destroyForcibly();}}
    private Peer login(String suffix)throws Exception {
        Peer peer=new Peer(endpoint);String username="java_"+suffix+UUID.randomUUID().toString().substring(0,8);JsonNode created=peer.client.player("register",Json.object("username",username,"email",username+"@example.org","password",secret,"confirmPassword",secret)).get(8,TimeUnit.SECONDS);assertEquals(username,created.path("username").asText());assertFalse(peer.client.player("session",null).get().path("authenticated").asBoolean());assertTrue(peer.client.player("login",Json.object("username",username,"password",secret)).get().path("authenticated").asBoolean());return peer;
    }
    private JsonNode join(Peer peer,String name)throws Exception {peer.client.connect(name,"",CharacterEditor.defaults(world.path("characterOptions")));return peer.type("welcome");}
    @Test void authenticationRegistrationAndOwnershipMatchAngular() throws Exception {
        try(Peer one=login("auth");Peer two=login("other")){
            JsonNode welcome=join(one,"JavaOwns");assertFalse(welcome.has("token"));JsonNode status=one.client.player("session",null).get();assertEquals(welcome.path("id").asText(),status.path("characters").path(0).path("id").asText());
            two.client.connect("Stolen",welcome.path("id").asText(),CharacterEditor.defaults(world.path("characterOptions")));assertTrue(two.type("error").path("text").asText().contains("Personaje no disponible"));assertFalse(two.client.online());
        }
    }
    @Test void nativePeersSharePlayersAndUnicodeChat() throws Exception {
        try(Peer one=login("share");Peer two=login("share")){
            JsonNode a=join(one,"JavaOne"),b=join(two,"JavaTwo");one.until(m->m.path("type").asText().equals("state")&&m.path("players").size()==2);one.client.send(Commands.action("chat","text","¡Hola desde Java! áéíóú ⚔"));assertEquals("¡Hola desde Java! áéíóú ⚔",two.type("chat").path("text").asText());assertNotEquals(a.path("id"),b.path("id"));
        }
    }
    @Test void movementSpeedStopBlockedGoalAndSequenceRemainAuthoritative() throws Exception {
        try(Peer peer=login("move")){
            String id=join(peer,"JavaMover").path("id").asText();JsonNode first=peer.type("state");JsonNode origin=Json.find(first.path("players"),id);double x=Json.num(origin,"x"),z=Json.num(origin,"z");
            peer.client.send(Commands.input(1,0,-1));JsonNode moved=peer.until(m->m.path("type").asText().equals("state")&&Json.num(Json.find(m.path("players"),id),"z")<z-.3);JsonNode player=Json.find(moved.path("players"),id);assertTrue(z-Json.num(player,"z")<player.path("speed").asDouble()*.5);peer.client.send(Commands.action("stop"));JsonNode stopped=peer.until(m->m.path("type").asText().equals("state")&&Json.find(m.path("players"),id).path("sequence").asLong()==1);double stoppedZ=Json.num(Json.find(stopped.path("players"),id),"z");Thread.sleep(350);assertEquals(stoppedZ,Json.num(Json.find(peer.snapshot.path("players"),id),"z"),.08);
            JsonNode blocker=world.path("floors").path(0).path("props").path(0);peer.client.send(Commands.move(Json.num(blocker,"x"),Json.num(blocker,"z")));assertEquals("Destino bloqueado",peer.type("error").path("text").asText());peer.client.send(Commands.input(0,1,0));Thread.sleep(200);assertEquals(x,Json.num(Json.find(peer.snapshot.path("players"),id),"x"),.05);
            peer.client.send(Commands.move(x,z+1));peer.until(m->m.path("type").asText().equals("state")&&Json.num(Json.find(m.path("players"),id),"z")>stoppedZ+.2);peer.client.send(Commands.action("stop"));
        }
    }
    @Test void everyGamePanelUsesRecognizedOriginalCommands() throws Exception {
        try(Peer peer=login("commands")){
            join(peer,"JavaCommands");peer.type("state");String dummy=world.path("floors").path(0).path("training").path("dummies").path(0).path("id").asText();
            List<JsonNode> commands=List.of(Commands.target(dummy,"monster"),Commands.action("attack"),Commands.action("attack","skill",true),Commands.action("racial"),Commands.action("potion"),Commands.action("interact"),Commands.action("portal"),Commands.action("pvpMode","enabled",true),Commands.action("redeem"),Commands.action("resetTraining"),Commands.action("equip","weaponSetId",""),Commands.action("gather","nodeId","absent"),Commands.action("castSkill","skillId","absent"),Commands.action("bindSkill","slot",0,"skillId","absent"),Commands.action("craft","recipeId","absent"),Commands.action("craftUpgrade","itemId","absent"),Commands.action("craftEquip","itemId","absent","slot","mainHand"),Commands.action("craftUnequip","slot","offHand"),Commands.action("shopBuy","materialId","potion","quantity",1),Commands.action("shopBuyEquipment","productId","absent"),Commands.action("shopSell","itemId","absent"),Commands.action("shopSellMaterial","materialId","wood","quantity",1),Commands.action("forgeUpgrade"),Commands.action("progression","attributes",Map.of(),"talents",Map.of()),Commands.action("profile","name","JavaUpdated","appearance",CharacterEditor.defaults(world.path("characterOptions"))),Commands.action("npcChat","npcId","lyra","text","hola"),Commands.action("ping"));
            for(JsonNode command:commands){peer.client.send(command);Thread.sleep(35);}Thread.sleep(500);assertTrue(peer.client.online());List<JsonNode> received=new ArrayList<>();peer.messages.drainTo(received);assertTrue(received.stream().noneMatch(m->m.path("type").asText().equals("error")&&m.path("text").asText().toLowerCase(Locale.ROOT).contains("desconocid")),received.toString());assertEquals("JavaUpdated",Json.find(peer.snapshot.path("players"),peer.snapshot.path("you").asText()).path("name").asText());
        }
    }
    @Test void logoutRevokesNativeWebsocketAndSession() throws Exception {
        try(Peer peer=login("logout")){join(peer,"JavaLogout");peer.type("state");peer.client.player("logout",Json.object()).get();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(peer.client.online()&&System.nanoTime()<end)Thread.sleep(50);assertFalse(peer.client.online());assertFalse(peer.client.player("session",null).get().path("authenticated").asBoolean());assertTrue(peer.statuses.contains("expired"));}
    }
    @Test void untrustedOriginAndUnauthenticatedSocketAreRejected() throws Exception {
        var http=HttpClient.newHttpClient();var login=HttpRequest.newBuilder(endpoint.api("api/player/login")).header("Origin","http://untrusted.example").header("X-Aincrad-Player","1").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(Json.object("username","java_missing","password",secret).toString())).build();assertEquals(403,http.send(login,HttpResponse.BodyHandlers.ofString()).statusCode());
        var upgrade=http.newWebSocketBuilder().header("Origin",endpoint.origin()).buildAsync(endpoint.socket(),new WebSocket.Listener(){});ExecutionException denied=assertThrows(ExecutionException.class,()->upgrade.get(5,TimeUnit.SECONDS));assertInstanceOf(WebSocketHandshakeException.class,denied.getCause());assertEquals(401,((WebSocketHandshakeException)denied.getCause()).getResponse().statusCode());
    }
    @Test void nativeAdminCanInspectValidateReloadAndBackUpWithoutPlayerPrivileges() throws Exception {
        try(Peer peer=login("admin")){
            assertThrows(ExecutionException.class,()->peer.client.admin("world","GET",null).get());peer.client.admin("login","POST",Json.object("username","root","password",secret)).get();JsonNode edit=peer.client.admin("world","GET",null).get();peer.client.admin("world","PUT",edit).get();assertTrue(Files.list(temp.resolve("data/backups")).findAny().isPresent());assertTrue(peer.client.admin("accounts","GET",null).get().isArray());assertTrue(peer.client.admin("characters","GET",null).get().isArray());assertTrue(peer.client.admin("ai","GET",null).get().isObject());peer.client.admin("reload","POST",null).get();peer.client.admin("logout","POST",null).get();assertTrue(peer.client.player("session",null).get().path("authenticated").asBoolean());
        }
    }
}
