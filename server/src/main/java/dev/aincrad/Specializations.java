package dev.aincrad;
import java.util.*;

public final class Specializations {
 public record Specialty(String id,String classId,String name,String description,List<String> weaponKinds,List<String> armorStyles,Map<String,Double> effects){}
 public static String defaultId(String classId){return switch(classId){case "warrior"->"swordsman";case "mage"->"fire";case "healer"->"healer";default->classId+"-default";};}
 public static Specialty resolve(WorldData.CharacterOptions c,String classId,String id){return c.specializations.stream().filter(s->s.classId().equals(classId)&&s.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Especialidad incompatible con la clase"));}
 public static String first(WorldData.CharacterOptions c,String classId){return c.specializations.stream().filter(s->s.classId().equals(classId)).findFirst().orElseThrow(()->new IllegalArgumentException("La clase no tiene especialidades")).id();}
 public static boolean permits(List<String> ids,String spec){return ids==null||ids.isEmpty()||ids.contains(spec);}
 public static boolean armor(Specialty s,WorldData.Outfit a){return s.classId().equals(a.classId())&&permits(a.specializationIds(),s.id())&&s.armorStyles().contains(a.style());}
 public static boolean weapons(Specialty s,Weapons.Loadout w){return s.classId().equals(w.classId())&&permits(w.specializationIds(),s.id())&&s.weaponKinds().contains(w.mainHand().kind())&&(w.offHand()==null||s.weaponKinds().contains(w.offHand().kind()));}
 public static void references(List<String> ids,String classId,WorldData.CharacterOptions c){if(ids!=null&&(ids.size()>30||new HashSet<>(ids).size()!=ids.size()||ids.stream().anyMatch(id->c.specializations.stream().noneMatch(s->s.id().equals(id)&&s.classId().equals(classId)))))throw new IllegalArgumentException("Referencia a especialidad inválida");}
 public static void validate(WorldData.CharacterOptions c){
  if(c.specializations==null||c.specializations.isEmpty()||c.specializations.size()>60)throw new IllegalArgumentException("Catálogo de especialidades requerido");Set<String> ids=new HashSet<>();
  for(var s:c.specializations){if(s==null||!Progression.id(s.id())||!ids.add(s.id())||c.classes.stream().noneMatch(p->p.id().equals(s.classId()))||!Progression.text(s.name(),80)||!Progression.text(s.description(),600)||s.weaponKinds()==null||s.weaponKinds().isEmpty()||s.weaponKinds().size()>8||!Weapons.KINDS.containsAll(s.weaponKinds())||s.armorStyles()==null||s.armorStyles().isEmpty()||!Set.of("tunic","leather","mail","plate").containsAll(s.armorStyles()))throw new IllegalArgumentException("Especialidad inválida");Progression.effects(s.effects());}
  for(var cls:c.classes)if(c.specializations.stream().noneMatch(s->s.classId().equals(cls.id())))throw new IllegalArgumentException("Falta especialidad para "+cls.name());
  for(var a:c.equipmentSets)references(a.specializationIds(),a.classId(),c);for(var w:c.weaponSets)references(w.specializationIds(),w.classId(),c);for(var t:c.progression.talents)references(t.specializationIds(),t.classId(),c);
  for(var s:c.specializations){for(var g:c.genders){Set<Integer> levels=new HashSet<>();for(var a:c.equipmentSets)if(a.gender().equals(g.id())&&armor(s,a)&&!levels.add(a.minLevel()))throw new IllegalArgumentException("Dos armaduras automáticas para la misma especialidad, género y nivel");if(!levels.contains(1))throw new IllegalArgumentException("Falta armadura inicial para "+s.name()+" / "+g.name());}
   Set<Integer> levels=new HashSet<>();for(var w:c.weaponSets)if(w.autoEquip()&&weapons(s,w)&&!levels.add(w.minLevel()))throw new IllegalArgumentException("Armas automáticas duplicadas para "+s.name());if(!levels.contains(1))throw new IllegalArgumentException("Faltan armas iniciales para "+s.name());
  }
 }
 public static void build(WorldData.CharacterOptions c,String spec,Map<String,Integer> ranks){for(var t:c.progression.talents)if(ranks.getOrDefault(t.id(),0)>0&&!permits(t.specializationIds(),spec))throw new IllegalArgumentException("Talento ajeno a tu especialidad");}
}
