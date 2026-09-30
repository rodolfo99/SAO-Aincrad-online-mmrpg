package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClickMovementTest {
    @TempDir Path dir;
    final ObjectMapper json=new ObjectMapper();
    final List<Map<String,Object>> events=new ArrayList<>();
    PlayerStore store;GameWorld world;GameWorld.Player player;GameWorld.Monster boar;String token;

    @BeforeEach void setup()throws Exception{
        store=new PlayerStore(dir,json);world=new GameWorld(WorldData.load(Path.of("../world/world.json"),json),store,(id,event)->events.add(event));
        boar=world.monsters.get("boar-1");world.monsters.clear();
        var welcome=world.join("Caminante","");token=(String)welcome.get("token");player=world.online.get(welcome.get("id"));
    }
    @AfterEach void close()throws Exception{store.close();}
    void command(String text)throws Exception{world.command(player.id,json.readTree(text));}
    void move(double x,double z)throws Exception{world.command(player.id,json.valueToTree(Map.of("type","move","x",x,"z",z)));}
    void tick(int count){for(int i=0;i<count;i++)world.tick(.05);}
    void emptyFloor(){world.data.floor(1).props.clear();player.x=player.z=0;}
    void assertStopped(){double x=player.x,z=player.z;assertNull(player.goalX);assertNull(player.goalZ);tick(20);assertEquals(x,player.x);assertEquals(z,player.z);}

    @Test void reachesAcrossARealBuildingWithoutTeleportingOrCrossingIt()throws Exception{
        player.x=4;player.z=19;assertTrue(world.data.walkable(1,player.x,player.z));move(13.6,19);
        assertEquals(4,player.x);assertEquals(19,player.z);double travelled=0;boolean detoured=false;
        for(int i=0;i<160&&player.goalX!=null;i++){
            double x=player.x,z=player.z;world.tick(.05);double step=Math.hypot(player.x-x,player.z-z);travelled+=step;
            assertTrue(step<=player.speed()*.05+1e-9);assertTrue(world.data.walkable(1,player.x,player.z));
            assertTrue(Math.hypot(player.x-10,player.z-19)>=3.5-1e-9);
            detoured|=Math.abs(player.z-19)>3;
        }
        assertTrue(detoured);assertTrue(travelled>10.1);assertEquals(13.6,player.x,1e-9);assertEquals(19,player.z,1e-9);assertStopped();
    }

    @Test void enclosedDestinationIsRejectedWithoutReplacingAnExistingRoute()throws Exception{
        emptyFloor();world.data.floor(1).radius=35;WorldPathfinderTest.enclose(world.data,10,0);
        move(0,-8);assertTrue(world.data.walkable(1,10,0));
        assertTrue(assertThrows(IllegalArgumentException.class,()->move(10,0)).getMessage().contains("inaccesible"));
        assertEquals(0.0,player.goalX);assertEquals(-8.0,player.goalZ);tick(40);assertEquals(-8,player.z,1e-9);assertStopped();
    }

    @Test void malformedBlockedAndOutOfBoundsMovesPreserveValidationAndAuthority()throws Exception{
        emptyFloor();world.data.floor(1).props.add(WorldPathfinderTest.prop("house",5,0,3.1));move(0,-8);
        for(String payload:List.of("{\"type\":\"move\",\"z\":0}","{\"type\":\"move\",\"x\":null,\"z\":0}","{\"type\":\"move\",\"x\":\"1\",\"z\":0}","{\"type\":\"move\",\"x\":1e999,\"z\":0}","{\"type\":\"move\",\"x\":9999,\"z\":0}","{\"type\":\"move\",\"x\":5,\"z\":0}")){
            assertThrows(IllegalArgumentException.class,()->command(payload));assertEquals(-8.0,player.goalZ);
        }
        command("{\"type\":\"move\",\"x\":0,\"z\":-6,\"hp\":999,\"col\":9999,\"speed\":999,\"floor\":2,\"route\":[{\"x\":5,\"z\":0}]}");
        assertEquals(0,player.x);assertEquals(0,player.z);assertEquals(100,player.hp);assertEquals(0,player.col);assertEquals(1,player.floor);
        tick(1);assertEquals(-GameWorld.SPEED*.05,player.z,1e-9);
        assertThrows(IllegalArgumentException.class,()->command("{\"type\":\"path\",\"points\":[]}"));
        var view=json.valueToTree(world.snapshot(player.id));assertFalse(view.toString().contains("goalX"));assertFalse(view.toString().contains("\"route\""));
    }

    @Test void idleStaleAndInvalidInputsDoNotCancelButFreshWasdDoes()throws Exception{
        emptyFloor();move(0,-10);command("{\"type\":\"input\",\"seq\":1,\"dx\":0,\"dz\":0}");tick(2);assertNotNull(player.goalX);
        command("{\"type\":\"input\",\"seq\":0,\"dx\":1,\"dz\":0}");assertNotNull(player.goalX);
        assertThrows(IllegalArgumentException.class,()->command("{\"type\":\"input\",\"seq\":2,\"dx\":2,\"dz\":0}"));assertNotNull(player.goalX);
        assertThrows(IllegalArgumentException.class,()->command("{\"type\":\"input\",\"seq\":1.5,\"dx\":1,\"dz\":0}"));
        double x=player.x,z=player.z;command("{\"type\":\"input\",\"seq\":2,\"dx\":1,\"dz\":1}");tick(1);
        assertNull(player.goalX);assertEquals(player.speed()*.05,Math.hypot(player.x-x,player.z-z),1e-9);
        tick(20);assertStopped();
    }

    @Test void stopClearsDirectionAndAllWaypoints()throws Exception{
        player.x=4;player.z=19;move(13.6,19);tick(3);command("{\"type\":\"stop\"}");assertEquals(0,player.dx);assertEquals(0,player.dz);assertStopped();
    }

    @Test void crossingAWaypointDoesNotGrantASecondSpeedBudget()throws Exception{
        player.x=4;player.z=19;var route=new WorldPathfinder(world.data).find(1,player.x,player.z,13.6,19);
        assertTrue(route.size()>1);var corner=route.get(0);move(13.6,19);
        double distance=Math.hypot(player.x-corner.x(),player.z-corner.z());
        for(int i=0;i<80&&distance>.45;i++){world.tick(.05);distance=Math.hypot(player.x-corner.x(),player.z-corner.z());}
        assertTrue(distance>0&&distance<=.45);world.tick(.1);
        double aroundCorner=distance+Math.hypot(player.x-corner.x(),player.z-corner.z());
        assertEquals(player.speed()*.1,aroundCorner,1e-9);
    }

    @Test void newClickReplacesTheWholePreviousRoute()throws Exception{
        player.x=4;player.z=19;move(13.6,19);tick(3);move(0,16);tick(80);assertEquals(0,player.x,1e-9);assertEquals(16,player.z,1e-9);assertStopped();
    }

    @Test void shortClickArrivesExactlyAndDoesNotResumePreviousKeyboardInput()throws Exception{
        emptyFloor();command("{\"type\":\"input\",\"seq\":1,\"dx\":1,\"dz\":0}");move(0,-.13);tick(1);
        assertEquals(0,player.x);assertEquals(-.13,player.z);assertStopped();move(0,-.13);tick(1);assertStopped();
    }

    @Test void speedModifiersAndTickClampAlsoApplyToRoutes()throws Exception{
        emptyFloor();player.racialEffect="sprint";player.racialBonus=3;player.racialUntil=100;
        move(10,0);double speed=player.speed();world.tick(.1);assertEquals(speed*.1,player.x,1e-9);
        world.tick(10);assertEquals(speed*.2,player.x,1e-9);world.tick(0);assertEquals(speed*.2,player.x,1e-9);
    }

    @Test void collisionChangesStopInsteadOfSlidingThroughOrResumingTheRoute()throws Exception{
        move(0,8);world.data.floor(1).props.add(WorldPathfinderTest.prop("tree",0,14,.65));world.tick(.05);
        assertEquals(0,player.x);assertEquals(16,player.z);assertStopped();
        assertTrue(events.stream().anyMatch(e->"notice".equals(e.get("type"))&&e.get("text").toString().contains("ruta")));
        world.data.floor(1).props.remove(world.data.floor(1).props.size()-1);assertStopped();
    }

    @Test void wasdStillSlidesAxesAndExpiresAfterAQuarterSecond()throws Exception{
        emptyFloor();world.data.floor(1).props.add(WorldPathfinderTest.prop("resource",1,0,.3));
        command("{\"type\":\"input\",\"seq\":1,\"dx\":1,\"dz\":1}");world.tick(.1);
        assertEquals(0,player.x);assertEquals(GameWorld.SPEED*.1/Math.sqrt(2),player.z,1e-9);tick(10);assertStopped();
    }

    @Test void portalDisconnectAndReconnectCancelRoutesWithoutPersistingWaypoints()throws Exception{
        var portal=world.data.floor(1).portal;player.x=portal.x();player.z=portal.z();player.unlocked=true;
        move(player.x+1,player.z);command("{\"type\":\"portal\"}");assertEquals(2,player.floor);assertStopped();
        move(player.x,player.z-3);tick(2);world.leave(player.id);assertNull(player.goalX);
        var saved=json.valueToTree(store.load().get(0));assertFalse(saved.has("route"));assertFalse(saved.has("goalX"));
        world.join("Ignorado",token);assertStopped();
        var reloaded=new GameWorld(world.data,store,(id,e)->{});reloaded.join("Ignorado",token);assertNull(reloaded.online.get(player.id).goalX);
    }

    @Test void pveDeathAndRespawnCancelAllWaypoints()throws Exception{
        emptyFloor();player.x=boar.x;player.z=boar.z;player.hp=1;move(player.x,player.z-6);world.monsters.put(boar.id,boar);
        world.tick(0);assertEquals(0,player.hp);assertNull(player.goalX);world.monsters.clear();tick(61);
        assertEquals(world.data.floor(1).spawn.x(),player.x);assertEquals(world.data.floor(1).spawn.z(),player.z);assertStopped();
    }

    @Test void pvpDeathAlsoClearsTheRoute()throws Exception{
        emptyFloor();var joined=world.join("Atacante","");var attacker=world.online.get(joined.get("id"));
        player.z=-6;attacker.x=0;attacker.z=-7;player.xp=attacker.xp=1000;player.hp=1;
        player.pvp.spawnProtectedUntil=attacker.pvp.spawnProtectedUntil=0;attacker.pvpMode=true;
        move(8,-6);world.command(attacker.id,json.valueToTree(Map.of("type","target","kind","player","id",player.id)));
        world.command(attacker.id,json.readTree("{\"type\":\"attack\"}"));
        assertEquals(0,player.hp);assertNull(player.goalX);tick(61);assertStopped();
    }
}
