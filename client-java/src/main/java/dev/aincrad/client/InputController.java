package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Consumer;

/** Camera-relative input, sent at 20 Hz; the server determines position and speed. */
public final class InputController {
    private final Set<String> keys = new HashSet<>();
    private final Consumer<JsonNode> sender;
    private long sequence;
    public InputController(Consumer<JsonNode> sender) { this.sender=sender; }
    public boolean press(String key) { return keys.add(key); }
    public void release(String key) { keys.remove(key); }
    public boolean moving() { double[] v=movement(keys,0);return v[0]!=0||v[1]!=0; }
    public void tick(double yaw) { double[] v=movement(keys,yaw);sender.accept(Commands.input(++sequence,v[0],v[1])); }
    public void stop() { keys.clear();sender.accept(Commands.action("stop")); }
    public void reset() { keys.clear();sequence=0; }
    public static double[] movement(Set<String> keys,double yaw) {
        double x=(any(keys,"D","RIGHT")?1:0)-(any(keys,"A","LEFT")?1:0);
        double z=(any(keys,"S","DOWN")?1:0)-(any(keys,"W","UP")?1:0);
        double length=Math.max(1,Math.hypot(x,z));x/=length;z/=length;
        return new double[]{x*Math.cos(yaw)+z*Math.sin(yaw),z*Math.cos(yaw)-x*Math.sin(yaw)};
    }
    private static boolean any(Set<String> set,String a,String b) { return set.contains(a)||set.contains(b); }
}
