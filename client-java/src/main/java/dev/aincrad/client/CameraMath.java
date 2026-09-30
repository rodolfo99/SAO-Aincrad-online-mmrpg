package dev.aincrad.client;

/** JavaFX uses Y down; server Z is mirrored only for drawing, never on the wire. */
public final class CameraMath {
    private CameraMath() {}
    public static double[] project(double x,double y,double renderZ,double targetX,double targetY,double targetZ,double yaw,double pitch,double distance,double width,double height) {
        double dx=x-targetX,dy=y-targetY,dz=renderZ-targetZ;
        double vx=Math.cos(yaw)*dx-Math.sin(yaw)*dz,vz=Math.sin(yaw)*dx+Math.cos(yaw)*dz;
        double vy=Math.cos(pitch)*dy+Math.sin(pitch)*vz,depth=-Math.sin(pitch)*dy+Math.cos(pitch)*vz+distance;
        if(depth<.15)return new double[]{Double.NaN,Double.NaN,depth};
        double focal=height/(2*Math.tan(Math.toRadians(42)/2));
        return new double[]{width/2+vx*focal/depth,height/2+vy*focal/depth,depth};
    }
    public static double approachAngle(double current,double target,double alpha) {return current+Math.atan2(Math.sin(target-current),Math.cos(target-current))*alpha;}
}
