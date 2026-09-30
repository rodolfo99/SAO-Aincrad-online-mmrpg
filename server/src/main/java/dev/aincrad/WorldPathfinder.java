package dev.aincrad;

import java.util.*;

/** Server-only navigation lattice; all nodes and edges use WorldData's collision model. */
final class WorldPathfinder {
    static final double CELL_SIZE=1.0;
    static final int MAX_EXPANDED_NODES=8192;
    private final WorldData world;

    WorldPathfinder(WorldData world){this.world=world;}

    List<WorldData.Point> find(int floor,double fromX,double fromZ,double toX,double toZ){
        if(!world.walkable(floor,toX,toZ))throw new IllegalArgumentException("Destino bloqueado");
        if(!world.walkable(floor,fromX,fromZ))throw new IllegalArgumentException("Origen bloqueado");
        WorldData.Point destination=new WorldData.Point(toX,toZ);
        if(world.walkableSegment(floor,fromX,fromZ,toX,toZ))return List.of(destination);

        Grid grid=new Grid(floor);
        boolean[] goals=new boolean[grid.size];
        var endNodes=grid.connections(toX,toZ);
        for(int node:endNodes)goals[node]=true;
        if(endNodes.isEmpty())throw new IllegalArgumentException("Destino inaccesible en la malla de navegación");

        double[] costs=new double[grid.size];Arrays.fill(costs,Double.POSITIVE_INFINITY);
        int[] parents=new int[grid.size];Arrays.fill(parents,-1);
        boolean[] closed=new boolean[grid.size];
        PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingDouble(Node::estimate)
            .thenComparingDouble(Node::cost).thenComparingInt(Node::index));
        for(int node:grid.connections(fromX,fromZ)){
            costs[node]=Math.hypot(grid.x(node)-fromX,grid.z(node)-fromZ);
            open.add(new Node(node,costs[node],costs[node]+distance(grid,node,toX,toZ)));
        }

        int expanded=0;
        while(!open.isEmpty()){
            Node current=open.remove();int node=current.index;
            if(closed[node]||current.cost>costs[node])continue;
            if(goals[node])return simplify(floor,fromX,fromZ,reconstruct(grid,parents,node,destination));
            if(expanded++>=MAX_EXPANDED_NODES)throw new IllegalArgumentException("Ruta no encontrada: límite de búsqueda alcanzado");
            closed[node]=true;
            int column=node%grid.side,row=node/grid.side;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                if(dx==0&&dz==0)continue;
                int nextColumn=column+dx,nextRow=row+dz;
                if(nextColumn<0||nextColumn>=grid.side||nextRow<0||nextRow>=grid.side)continue;
                int next=nextRow*grid.side+nextColumn;
                if(closed[next]||!grid.walkable(next))continue;
                double cost=costs[node]+Math.hypot(dx,dz)*CELL_SIZE;
                if(cost>=costs[next]||!world.walkableSegment(floor,grid.x(node),grid.z(node),grid.x(next),grid.z(next)))continue;
                costs[next]=cost;parents[next]=node;
                open.add(new Node(next,cost,cost+distance(grid,next,toX,toZ)));
            }
        }
        throw new IllegalArgumentException("Destino inaccesible en la malla de navegación");
    }

    private static double distance(Grid grid,int node,double x,double z){return Math.hypot(grid.x(node)-x,grid.z(node)-z);}

    private static List<WorldData.Point> reconstruct(Grid grid,int[] parents,int node,WorldData.Point destination){
        List<WorldData.Point> points=new ArrayList<>();
        for(int current=node;current!=-1;current=parents[current])points.add(new WorldData.Point(grid.x(current),grid.z(current)));
        Collections.reverse(points);points.add(destination);return points;
    }

    /** Linear scan removes needless waypoints only when the full shortcut is collision-free. */
    private List<WorldData.Point> simplify(int floor,double x,double z,List<WorldData.Point> points){
        List<WorldData.Point> route=new ArrayList<>();WorldData.Point previous=points.get(0);
        for(int i=1;i<points.size();i++){
            WorldData.Point next=points.get(i);
            if(!world.walkableSegment(floor,x,z,next.x(),next.z())){
                route.add(previous);x=previous.x();z=previous.z();
            }
            previous=next;
        }
        route.add(previous);return List.copyOf(route);
    }

    private record Node(int index,double cost,double estimate){}

    private final class Grid {
        final int floor,extent,side,size;final byte[] validity;
        Grid(int floor){this.floor=floor;extent=(int)Math.ceil(world.floor(floor).radius/CELL_SIZE);side=extent*2+1;size=side*side;validity=new byte[size];}
        double x(int node){return (node%side-extent)*CELL_SIZE;}
        double z(int node){return (node/side-extent)*CELL_SIZE;}
        boolean walkable(int node){if(validity[node]==0)validity[node]=(byte)(world.walkable(floor,x(node),z(node))?1:-1);return validity[node]==1;}
        List<Integer> connections(double x,double z){
            int cx=(int)Math.round(x/CELL_SIZE)+extent,cz=(int)Math.round(z/CELL_SIZE)+extent;
            List<Integer> nodes=new ArrayList<>(9);
            for(int row=cz-1;row<=cz+1;row++)for(int column=cx-1;column<=cx+1;column++){
                if(column<0||column>=side||row<0||row>=side)continue;
                int node=row*side+column;
                if(walkable(node)&&world.walkableSegment(floor,x,z,x(node),z(node)))nodes.add(node);
            }
            return nodes;
        }
    }
}
