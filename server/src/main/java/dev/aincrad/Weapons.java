package dev.aincrad;

import java.util.*;

/** Main/off-hand loadouts share the server's configurable attribute definitions. */
public final class Weapons {
    public record Item(String name,String kind,int hands,Map<String,Double> attributes,String primary,String secondary,String glow){}
    public record Loadout(String id,String name,String classId,int minLevel,boolean autoEquip,Item mainHand,Item offHand,List<String> specializationIds){public Loadout(String id,String name,String classId,int minLevel,boolean autoEquip,Item mainHand,Item offHand){this(id,name,classId,minLevel,autoEquip,mainHand,offHand,List.of());}}
    public static final Set<String> KINDS=Set.of("sword","dagger","greatsword","staff","wand","shield","orb","tome");
    public static Loadout resolve(WorldData.CharacterOptions c,String classId,int level,String selected){
        return resolve(c,classId,Specializations.first(c,classId),level,selected);
    }
    public static Loadout resolve(WorldData.CharacterOptions c,String classId,String specId,int level,String selected){
        var spec=Specializations.resolve(c,classId,specId);
        if(selected!=null&&!selected.isBlank()){
            var chosen=c.weaponSets.stream().filter(s->s.id().equals(selected)&&Specializations.weapons(spec,s)&&s.minLevel()<=level).findFirst();
            if(chosen.isPresent())return chosen.get();
        }
        return c.weaponSets.stream().filter(s->s.autoEquip()&&Specializations.weapons(spec,s)&&s.minLevel()<=level).max(Comparator.comparingInt(Loadout::minLevel)).orElseThrow();
    }
    public static double modifier(WorldData.CharacterOptions c,Loadout set,String effect){
        return c.attributeDefinitions.stream().filter(d->d.effect().equals(effect)).mapToDouble(d->set.mainHand().attributes().getOrDefault(d.id(),0.0)+(set.offHand()==null?0:set.offHand().attributes().getOrDefault(d.id(),0.0))).sum();
    }
    public static void validate(WorldData.CharacterOptions c){
        if(c.weaponSets==null||c.weaponSets.isEmpty()||c.weaponSets.size()>500)throw new IllegalArgumentException("Catálogo de armas requerido (1–500 conjuntos)");
        Set<String> ids=new HashSet<>(),automatic=new HashSet<>();
        for(var s:c.weaponSets){
            if(s==null||s.id()==null||!s.id().matches("[A-Za-z0-9_-]{1,64}")||!ids.add(s.id())||s.name()==null||s.name().isBlank()||s.name().length()>80||c.classes.stream().noneMatch(p->p.id().equals(s.classId()))||s.minLevel()<1||s.minLevel()>1000)throw new IllegalArgumentException("Conjunto de armas inválido o automático duplicado");
            item(c,s.mainHand(),false);if(s.offHand()!=null)item(c,s.offHand(),true);
            if(s.mainHand().hands()==2&&s.offHand()!=null)throw new IllegalArgumentException("Un arma de dos manos ocupa ambas ranuras");
        }
        for(var cls:c.classes)if(c.weaponSets.stream().noneMatch(s->s.classId().equals(cls.id())&&s.minLevel()==1&&s.autoEquip()))throw new IllegalArgumentException("Falta arma automática de nivel 1 para "+cls.name());
    }
    private static void item(WorldData.CharacterOptions c,Item i,boolean off){
        if(i==null||i.name()==null||i.name().isBlank()||i.name().length()>80||i.kind()==null||!KINDS.contains(i.kind())||i.hands()<1||i.hands()>2||off&&i.hands()!=1||!off&&Set.of("shield","orb","tome").contains(i.kind())||i.kind().equals("greatsword")&&i.hands()!=2||i.hands()==2&&!Set.of("staff","greatsword").contains(i.kind())||i.attributes()==null||i.attributes().size()>100||!WorldData.color(i.primary())||!WorldData.color(i.secondary())||!WorldData.color(i.glow()))throw new IllegalArgumentException("Arma, objeto o ranura inválidos");
        for(var e:i.attributes().entrySet()){
            var d=c.attributeDefinitions.stream().filter(a->a.id().equals(e.getKey())).findFirst().orElseThrow(()->new IllegalArgumentException("Atributo de arma no definido: "+e.getKey()));
            if(e.getValue()==null||!Double.isFinite(e.getValue())||e.getValue()<d.min()||e.getValue()>d.max())throw new IllegalArgumentException("Atributo de arma fuera de rango: "+e.getKey());
        }
    }
}
