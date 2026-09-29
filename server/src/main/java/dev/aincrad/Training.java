package dev.aincrad;

import java.util.*;

/** Practice never pays rewards and does not alter citizenship or character progression. */
public final class Training {
 public record Dummy(String id,String name,double x,double z,int maxHp,int defense,boolean ally){}
 public static class Zone {
  public String name="Patio de entrenamiento";
  public double x=0,z=27,radius=5;
  public int recoverySeconds=5,sessionResetSeconds=15;
  public List<Dummy> dummies=new ArrayList<>();
 }
 public static boolean inside(Zone z,double x,double y){return z!=null&&Math.hypot(x-z.x,y-z.z)<=z.radius;}
 static void validate(WorldData.Floor f,Set<String> ids){var z=f.training;if(z==null)return;
  if(!Progression.text(z.name,80)||!Double.isFinite(z.x)||!Double.isFinite(z.z)||!Double.isFinite(z.radius)||z.radius<3||z.radius>12||Math.hypot(z.x,z.z)+z.radius>f.radius-2||z.recoverySeconds<1||z.recoverySeconds>60||z.sessionResetSeconds<5||z.sessionResetSeconds>300||z.dummies==null||z.dummies.isEmpty()||z.dummies.size()>20)throw new IllegalArgumentException("Zona de entrenamiento inválida");
  for(var d:z.dummies){if(d==null||!Progression.id(d.id())||!ids.add(d.id())||!Progression.text(d.name(),80)||!Double.isFinite(d.x())||!Double.isFinite(d.z())||Math.hypot(d.x()-z.x,d.z()-z.z)>z.radius-1||d.maxHp()<100||d.maxHp()>100000||d.defense()<0||d.defense()>100)throw new IllegalArgumentException("Muñeco de entrenamiento inválido");}
  for(int i=0;i<z.dummies.size();i++)for(int j=0;j<i;j++)if(Math.hypot(z.dummies.get(i).x()-z.dummies.get(j).x(),z.dummies.get(i).z()-z.dummies.get(j).z())<1.5)throw new IllegalArgumentException("Muñecos superpuestos");
  if(f.monsters.stream().anyMatch(m->inside(z,m.x(),m.z())))throw new IllegalArgumentException("No coloques criaturas dentro del patio de entrenamiento");
 }
 public static class Session {
  public long damage,healing,hits,criticals,buffs,first,last;public String target="";
  void reset(){damage=healing=hits=criticals=buffs=first=last=0;target="";}
  void record(String name,int dealt,int healed,boolean critical,boolean buff,long now,int timeout){if(last==0||now-last>timeout*1000L){reset();first=now;}last=now;target=name;damage+=dealt;healing+=healed;if(dealt>0)hits++;if(critical)criticals++;if(buff)buffs++;}
  Map<String,Object> view(){double seconds=first==0?0:Math.max(1,(last-first)/1000.0);return Map.of("damage",damage,"healing",healing,"hits",hits,"criticals",criticals,"buffs",buffs,"seconds",seconds,"dps",seconds>0?Math.round(damage/seconds*10)/10.0:0,"hps",seconds>0?Math.round(healing/seconds*10)/10.0:0,"target",target);}
 }
}
