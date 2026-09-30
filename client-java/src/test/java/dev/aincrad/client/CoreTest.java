package dev.aincrad.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @Test void oneOriginOwnsHttpAndWebSocket() {
        var e=Endpoint.parse("https://example.org:8443/");assertEquals("https://example.org:8443",e.origin());assertEquals("wss://example.org:8443/ws",e.socket().toString());assertEquals("https://example.org:8443/api/player/login",e.api("api/player/login").toString());
    }
    @Test void browserDefaultPortsAndIpv6AreSerializedCorrectly() {
        assertEquals("http://localhost",Endpoint.parse("HTTP://LOCALHOST:80").origin());assertEquals("https://example.org",Endpoint.parse("https://example.org:443").origin());assertEquals("ws://[::1]:8081/ws",Endpoint.parse("http://[::1]:8081").socket().toString());
    }
    @ParameterizedTest @ValueSource(strings={"ftp://host","http://user:secret@host","http://host/path","http://host?token=secret","http://host#foo","host:8081","http://host:0","http://host:70000"})
    void invalidServerCannotReceiveCredentials(String value) {assertThrows(IllegalArgumentException.class,()->Endpoint.parse(value));}
    @Test void diagonalInputUsesSameSpeedAsSingleAxis() {
        double[] diagonal=InputController.movement(Set.of("W","D"),0);assertEquals(1,Math.hypot(diagonal[0],diagonal[1]),1e-9);assertTrue(diagonal[0]>0);assertTrue(diagonal[1]<0);
    }
    @Test void cameraRotationPreservesAngularDirections() {
        assertArrayEquals(new double[]{-1,0},InputController.movement(Set.of("W"),Math.PI/2),1e-9);assertArrayEquals(new double[]{0,-1},InputController.movement(Set.of("D"),Math.PI/2),1e-9);assertArrayEquals(new double[]{0,0},InputController.movement(Set.of("W","S","A","D"),0),1e-9);
    }
    @Test void repeatKeysDoNotRepeatActionsAndStopClearsMotion() {
        var messages=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();InputController input=new InputController(messages::add);assertTrue(input.press("W"));assertFalse(input.press("W"));input.tick(0);input.stop();input.tick(0);assertEquals("stop",messages.get(1).path("type").asText());assertEquals(0,messages.get(2).path("dz").asDouble());assertTrue(messages.get(2).path("seq").asLong()>messages.get(0).path("seq").asLong());input.reset();input.tick(0);assertEquals(1,messages.get(3).path("seq").asInt());
    }
    @Test void zeroInputPreservesClickMovementProtocolAndMoveHasNoSpeed() {
        assertEquals(Set.of("type","x","z"),keys(Commands.move(2,-3)));assertEquals(Set.of("type","seq","dx","dz"),keys(Commands.input(7,0,0)));assertFalse(Commands.join("Rodo","existing",Json.object()).has("token"));assertThrows(IllegalArgumentException.class,()->Commands.move(Double.NaN,0));assertThrows(IllegalArgumentException.class,()->Commands.input(1,2,0));
    }
    @Test void snapshotsRemainUntouchedAndCrossFloorChatIsIgnored() {
        var model=new GameModel();var snapshot=Json.object("type","state","floor",1,"players",List.of(Json.object("id","me","x",4,"hp",90,"target","orc","targetKind","monster")),"monsters",List.of(Json.object("id","orc","hp",50)));model.receive(Json.object("type","welcome","id","me"));model.receive(snapshot);model.receive(Json.object("type","chat","floor",2,"text","other"));assertTrue(model.chat.isEmpty());model.receive(Json.object("type","chat","floor",1,"text","hello"));assertEquals(1,model.chat.size());assertEquals(50,model.target().path("hp").asInt());assertEquals(4,snapshot.path("players").path(0).path("x").asInt());
    }
    @Test void delayedNpcReplyDoesNotReplaceAnotherDialogue() {
        var model=new GameModel();model.receive(Json.object("type","dialogue","npcId","lyra"));model.receive(Json.object("type","npcReply","npcId","brann","text","wrong"));assertFalse(model.dialogue.has("reply"));model.receive(Json.object("type","npcReply","npcId","lyra","text","hola"));assertEquals("hola",model.dialogue.path("reply").asText());
    }
    @Test void catalogFiltersHonorClassSpecialtyGenderAndWeaponKinds() throws Exception {
        var world=Json.MAPPER.readTree(getClass().getResourceAsStream("/world.json"));var look=Json.object("classId","warrior","gender","male","specializationId","swordsman");var player=Json.object("appearance",look);var catalog=world.path("characterOptions");assertFalse(Catalog.fits(catalog,Json.find(catalog.path("weaponSets"),"warrior-guardia-1"),player,false));assertTrue(Json.list(catalog.path("weaponSets")).stream().anyMatch(w->Catalog.fits(catalog,w,player,false)));assertFalse(Catalog.fits(catalog,Json.object("classId","mage","gender","male"),player,true));
    }
    @Test void angleInterpolationCrossesTheShortestArc() {assertEquals(Math.PI,CameraMath.approachAngle(Math.toRadians(179),Math.toRadians(-179),.5),1e-8);}
    private Set<String> keys(com.fasterxml.jackson.databind.JsonNode node) {Set<String> fields=new HashSet<>();node.fieldNames().forEachRemaining(fields::add);return fields;}
}
