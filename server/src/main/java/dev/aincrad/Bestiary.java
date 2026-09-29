package dev.aincrad;

import java.util.*;

/** Data-driven species and deterministic, bounded hunting populations. */
public final class Bestiary {
 public static final Set<String> MODELS=Set.of("boar","guardian","slime","dog","orc","goblin","troll");
 public record Species(String id,String name,String model,String color,int baseHp,int hpPerLevel,double baseDamage,double damagePerLevel,double baseDefense,double defensePerLevel,double speed,double attackRange,double aggroRange,double leashRange,double attackCooldown,double windupSeconds,int baseXp,int xpPerLevel,int baseCol,int colPerLevel,int respawnSeconds,boolean unlockPortal){
  public int hp(int level){return Math.min(200000,baseHp+hpPerLevel*(level-1));}
  public int damage(int level){return (int)Math.max(1,Math.min(1000,Math.round(baseDamage+damagePerLevel*(level-1))));}
  public int defense(int level){return (int)Math.max(0,Math.min(200,Math.round(baseDefense+defensePerLevel*(level-1))));}
  public int xp(int level){return Math.min(10000,baseXp+xpPerLevel*(level-1));}
  public int col(int level){return Math.min(100000,baseCol+colPerLevel*(level-1));}
 }
 public record Zone(String id,String name,String speciesId,int minLevel,int maxLevel,double x,double z,double radius,int population,int respawnSeconds){}
 public record Placed(String id,String speciesId,int level,double x,double z,Zone zone){}
 public static Species species(WorldData world,String id){return world.monsterSpecies.stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Especie de monstruo inexistente: "+id));}
 static void validate(WorldData w){
  if(w.monsterSpecies==null||w.monsterSpecies.isEmpty()||w.monsterSpecies.size()>50)throw new IllegalArgumentException("Catálogo de monstruos requerido (1–50 especies)");
  Set<String> ids=new HashSet<>();for(var s:w.monsterSpecies){if(s==null||!Progression.id(s.id())||s.id().equals("dummy")||!ids.add(s.id())||!Progression.text(s.name(),80)||s.model()==null||!MODELS.contains(s.model())||!WorldData.color(s.color())||s.baseHp()<1||s.baseHp()>50000||s.hpPerLevel()<0||s.hpPerLevel()>2000||!range(s.baseDamage(),1,250)||!range(s.damagePerLevel(),0,30)||!range(s.baseDefense(),0,100)||!range(s.defensePerLevel(),0,5)||!range(s.speed(),.3,6)||!range(s.attackRange(),1,6)||!range(s.aggroRange(),2,20)||!range(s.leashRange(),s.aggroRange(),35)||!range(s.attackCooldown(),.5,30)||!range(s.windupSeconds(),0,5)||s.baseXp()<0||s.baseXp()>10000||s.xpPerLevel()<0||s.xpPerLevel()>1000||s.baseCol()<0||s.baseCol()>100000||s.colPerLevel()<0||s.colPerLevel()>10000||s.respawnSeconds()<1||s.respawnSeconds()>86400)throw new IllegalArgumentException("Especie de monstruo inválida");}
  for(String id:w.crafting.monsterDrops.keySet())species(w,id);
 }
 private static boolean range(double n,double min,double max){return Double.isFinite(n)&&n>=min&&n<=max;}
 public static boolean inside(Zone zone,double x,double z){return zone==null||Math.hypot(x-zone.x(),z-zone.z())<=zone.radius();}
 static void validateZones(WorldData w,WorldData.Floor f,Set<String> ids){
  if(f.monsterZones==null||f.monsterZones.size()>12)throw new IllegalArgumentException("Máximo 12 zonas de monstruos por piso");int total=f.monsters.size();
  for(var z:f.monsterZones){if(z==null||z.id()==null||!z.id().matches("[A-Za-z0-9_-]{1,48}")||!ids.add(z.id())||!Progression.text(z.name(),80)||z.minLevel()<1||z.maxLevel()<z.minLevel()||z.maxLevel()>1000||!Double.isFinite(z.x())||!Double.isFinite(z.z())||!range(z.radius(),3,18)||Math.hypot(z.x(),z.z())+z.radius()>f.radius-2||z.population()<1||z.population()>30||z.respawnSeconds()<1||z.respawnSeconds()>86400)throw new IllegalArgumentException("Zona de monstruos inválida");species(w,z.speciesId());total+=z.population();
   if(Math.hypot(z.x()-f.spawn.x(),z.z()-f.spawn.z())<z.radius()+Math.max(8,w.pvp.safeRadius)||Math.hypot(z.x()-f.portal.x(),z.z()-f.portal.z())<z.radius()+Math.max(4,w.pvp.portalSafeRadius)||f.training!=null&&Math.hypot(z.x()-f.training.x,z.z()-f.training.z)<z.radius()+f.training.radius)throw new IllegalArgumentException("La zona invade refugio, portal o patio: "+z.name());
   for(int i=0;i<z.population();i++)if(!ids.add(z.id()+"-"+(i+1)))throw new IllegalArgumentException("ID de criatura generado duplicado");
  }
  if(total>200)throw new IllegalArgumentException("Máximo 200 criaturas por piso, incluidas las zonas");
  for(int i=0;i<f.monsterZones.size();i++)for(int j=0;j<i;j++){var a=f.monsterZones.get(i);var b=f.monsterZones.get(j);if(Math.hypot(a.x()-b.x(),a.z()-b.z())<a.radius()+b.radius())throw new IllegalArgumentException("Zonas de monstruos superpuestas");}
 }
 static List<Placed> placements(WorldData w,WorldData.Floor f){
  List<Placed> result=new ArrayList<>();for(var zone:f.monsterZones){Random random=new Random(w.seed+f.id*1009L+zone.id().hashCode()*31L);int count=0;
   for(int attempt=0;attempt<10000&&count<zone.population();attempt++){double angle=random.nextDouble()*Math.PI*2,dist=Math.sqrt(random.nextDouble())*(zone.radius()-1);double x=zone.x()+Math.cos(angle)*dist,z=zone.z()+Math.sin(angle)*dist;
    if(!w.walkable(f.id,x,z)||f.props.stream().anyMatch(p->Math.hypot(x-p.x(),z-p.z())<p.radius()+1)||f.monsters.stream().anyMatch(m->Math.hypot(x-m.x(),z-m.z())<2)||f.npcs.stream().anyMatch(n->Math.hypot(x-n.x(),z-n.z())<3)||result.stream().anyMatch(m->Math.hypot(x-m.x(),z-m.z())<2))continue;
    result.add(new Placed(zone.id()+"-"+(count+1),zone.speciesId(),zone.minLevel()+count%(zone.maxLevel()-zone.minLevel()+1),x,z,zone));count++;
   }
   if(count!=zone.population())throw new IllegalArgumentException("No hay espacio transitable para toda la población de "+zone.name());
  }return result;
 }
}
