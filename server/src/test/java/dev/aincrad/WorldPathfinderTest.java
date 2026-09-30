package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorldPathfinderTest {
    static WorldData emptyWorld(double radius){
        WorldData world=new WorldData();WorldData.Floor floor=new WorldData.Floor();
        floor.id=1;floor.radius=radius;world.floors=List.of(floor);return world;
    }
    static WorldData.Prop prop(String kind,double x,double z,double radius){return new WorldData.Prop(kind,x,z,1,0,radius);}
    static void enclose(WorldData world,double x,double z){
        for(int i=0;i<12;i++){double angle=i*Math.PI/6;world.floor(1).props.add(prop("tree",x+5*Math.cos(angle),z+5*Math.sin(angle),1.2));}
    }
    static void assertClear(WorldData world,int floor,double x,double z,List<WorldData.Point> route){
        assertFalse(route.isEmpty());
        for(var point:route){
            // Sample the returned polyline independently of the planner's segment predicate.
            int samples=Math.max(1,(int)Math.ceil(Math.hypot(point.x()-x,point.z()-z)/.025));
            for(int i=0;i<=samples;i++)assertTrue(world.walkable(floor,x+(point.x()-x)*i/samples,z+(point.z()-z)*i/samples),"Blocked route segment");
            x=point.x();z=point.z();
        }
    }

    @Test void clearLinePreservesFractionalDestinationAndZeroLength(){
        var world=emptyWorld(35);var finder=new WorldPathfinder(world);var target=new WorldData.Point(7.23,-4.71);
        assertEquals(List.of(target),finder.find(1,-6.17,3.28,target.x(),target.z()));
        assertEquals(List.of(target),finder.find(1,target.x(),target.z(),target.x(),target.z()));
    }

    @Test void detoursAroundHouseAndKeepsItsPlayerClearance(){
        var world=emptyWorld(35);world.floor(1).props.add(prop("house",0,0,3.1));
        var route=new WorldPathfinder(world).find(1,-6,0,6.25,.13);
        assertTrue(route.size()>1);assertClear(world,1,-6,0,route);
        assertEquals(new WorldData.Point(6.25,.13),route.get(route.size()-1));
        assertFalse(world.walkable(1,3.49,0));assertTrue(world.walkable(1,3.5,0));
    }

    @Test void continuousCollisionCheckRejectsCrossingsWithClearEndpoints(){
        var world=emptyWorld(35);world.floor(1).props.add(prop("resource",0,0,.9));
        assertTrue(world.walkable(1,-2,0));assertTrue(world.walkable(1,2,0));
        assertFalse(world.walkableSegment(1,-2,0,2,0));
        assertFalse(world.walkableSegment(1,-2,-2,2,2));
        assertTrue(world.walkableSegment(1,-2,1.3,2,1.3));
        assertFalse(world.walkableSegment(1,-2,1.3-1e-6,2,1.3-1e-6));
        assertTrue(world.walkableSegment(1,2,0,2,0));
        assertFalse(world.walkableSegment(1,0,0,0,0));
    }

    @Test void directMovementWorksInACorridorNarrowerThanTheGrid(){
        var world=emptyWorld(35);world.floor(1).props.add(prop("tree",0,1.1,.65));world.floor(1).props.add(prop("tree",0,-1.1,.65));
        assertEquals(List.of(new WorldData.Point(5,0)),new WorldPathfinder(world).find(1,-5,0,5,0));
    }

    @Test void floorBoundaryAndInvalidCoordinatesMatchWalkable(){
        var world=emptyWorld(35);var finder=new WorldPathfinder(world);
        assertTrue(world.walkableSegment(1,-34,0,34,0));
        assertFalse(world.walkableSegment(1,0,0,34.01,0));
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,35,-35}){
            assertThrows(IllegalArgumentException.class,()->finder.find(1,0,0,invalid,0));
            assertThrows(IllegalArgumentException.class,()->finder.find(1,invalid,0,0,0));
            assertFalse(world.walkableSegment(1,0,0,invalid,0));
        }
    }

    @Test void blockedGoalAndWalkableButEnclosedGoalAreRejected(){
        var world=emptyWorld(35);enclose(world,0,0);var finder=new WorldPathfinder(world);
        assertTrue(world.walkable(1,0,0));
        assertTrue(assertThrows(IllegalArgumentException.class,()->finder.find(1,-10,0,0,0)).getMessage().contains("inaccesible"));
        assertEquals("Destino bloqueado",assertThrows(IllegalArgumentException.class,()->finder.find(1,-10,0,5,0)).getMessage());
    }

    @Test void searchBudgetBoundsLargeUnreachableWorlds(){
        var world=emptyWorld(100);enclose(world,0,0);
        assertTrue(assertThrows(IllegalArgumentException.class,()->new WorldPathfinder(world).find(1,-10,0,0,0)).getMessage().contains("límite de búsqueda"));
    }

    @Test void repeatedObstaclesAndChangesUseCurrentWorldAndDeterministicRoutes(){
        var world=emptyWorld(35);world.floor(1).props.add(prop("house",-2,0,3.1));world.floor(1).props.add(prop("house",5,2,3.1));
        var finder=new WorldPathfinder(world);var route=finder.find(1,-9,.23,12,-.17);
        assertClear(world,1,-9,.23,route);assertEquals(route,finder.find(1,-9,.23,12,-.17));
        world.floor(1).props.clear();
        assertEquals(List.of(new WorldData.Point(12,-.17)),finder.find(1,-9,.23,12,-.17));
    }

    @Test void bothGeneratedFloorsUseTheirRealHouseTreeCrystalAndResourceProps()throws Exception{
        var world=WorldData.load(Path.of("../world/world.json"),new ObjectMapper());var finder=new WorldPathfinder(world);
        Set<String> kinds=new HashSet<>();int checked=0;
        for(var floor:world.floors){
            Set<String> onFloor=new HashSet<>();
            for(var prop:floor.props){
                if(onFloor.contains(prop.kind()))continue;
                double x=prop.x()-prop.radius()-.8,toX=prop.x()+prop.radius()+.8,z=prop.z();
                if(!world.walkable(floor.id,x,z)||!world.walkable(floor.id,toX,z))continue;
                var route=finder.find(floor.id,x,z,toX,z);assertTrue(route.size()>1,prop.kind());
                assertClear(world,floor.id,x,z,route);onFloor.add(prop.kind());kinds.add(prop.kind());checked++;
            }
            assertTrue(onFloor.contains("resource"));assertTrue(onFloor.contains("tree"));
        }
        assertTrue(kinds.containsAll(Set.of("house","tree","resource","crystal")));assertTrue(checked>=6);
    }
}
