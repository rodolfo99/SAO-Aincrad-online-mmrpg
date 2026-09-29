package dev.aincrad;

import java.util.*;

/** Server-owned citizenship and combat rules. Deadlines use epoch milliseconds. */
public final class Pvp {
    public static class Rules {
        public boolean enabled=true,selfDefense=true,penalizeMurdererKills=true;
        public int minLevel=1,initialCitizenship=100,minCitizenship=-1000,maxCitizenship=100;
        public int murderPenalty=25,murdererThreshold=50,murdererSeconds=600,aggressionSeconds=60,combatSeconds=20,spawnProtectionSeconds=15;
        public double safeRadius=10,portalSafeRadius=4,damageMultiplier=.65;
        public int redemptionCost=100,redemptionPoints=5,redemptionCooldownSeconds=60;
        public void validate(){
            if(minLevel<1||minLevel>1000||minCitizenship< -100000||maxCitizenship>100000||minCitizenship>=maxCitizenship||initialCitizenship<minCitizenship||initialCitizenship>maxCitizenship||murderPenalty<1||murderPenalty>10000||murdererThreshold<minCitizenship||murdererThreshold>=maxCitizenship||murdererSeconds<1||murdererSeconds>604800||aggressionSeconds<1||aggressionSeconds>600||combatSeconds<1||combatSeconds>120||spawnProtectionSeconds<0||spawnProtectionSeconds>300||!Double.isFinite(safeRadius)||safeRadius<0||safeRadius>30||!Double.isFinite(portalSafeRadius)||portalSafeRadius<0||portalSafeRadius>15||!Double.isFinite(damageMultiplier)||damageMultiplier<=0||damageMultiplier>2||redemptionCost<1||redemptionCost>1000000||redemptionPoints<1||redemptionPoints>1000||redemptionCooldownSeconds<1||redemptionCooldownSeconds>86400)throw new IllegalArgumentException("Reglas de PvP y ciudadanía inválidas");
        }
    }
    public static class State {
        public int citizenship,murders,kills,deaths;
        public long murdererUntil,aggressorUntil,combatUntil,spawnProtectedUntil,redemptionAt,racialReadyAt,lastEvent;
        public Map<String,Long> defendAgainst=new HashMap<>();
        public State(){}
        public State(int citizenship){this.citizenship=citizenship;}
        public State copy(){var s=new State(citizenship);s.murders=murders;s.kills=kills;s.deaths=deaths;s.murdererUntil=murdererUntil;s.aggressorUntil=aggressorUntil;s.combatUntil=combatUntil;s.spawnProtectedUntil=spawnProtectedUntil;s.redemptionAt=redemptionAt;s.racialReadyAt=racialReadyAt;s.lastEvent=lastEvent;s.defendAgainst=new HashMap<>(defendAgainst);return s;}
        public boolean murderer(Rules r,long now){return murders>0&&(murdererUntil>now||citizenship<=r.murdererThreshold);}
        public String status(Rules r,long now){return murderer(r,now)?"Asesino":aggressorUntil>now?"Agresor":"Ciudadano";}
        public Map<String,Object> view(Rules r,long now){return Map.ofEntries(Map.entry("citizenship",citizenship),Map.entry("murders",murders),Map.entry("kills",kills),Map.entry("deaths",deaths),Map.entry("status",status(r,now)),Map.entry("murderer",murderer(r,now)),Map.entry("murdererSeconds",Math.max(0,(murdererUntil-now)/1000)),Map.entry("combatSeconds",Math.max(0,(combatUntil-now)/1000.0)),Map.entry("protectedSeconds",Math.max(0,(spawnProtectedUntil-now)/1000.0)));}
    }
    public static boolean justified(State attacker,String victimId,State victim,Rules r,long now){return r.selfDefense&&attacker.defendAgainst.getOrDefault(victimId,0L)>now||!r.penalizeMurdererKills&&victim.murderer(r,now);}
}
