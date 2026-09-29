package dev.aincrad;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Configurable point budgets and acyclic class trees. The server owns every allocation. */
public final class Progression {
    public static final Set<String> EFFECTS=Set.of("maxHp","damage","defense","speed","criticalChance","healing","skillPower","cooldownReduction","none");
    public static class Rules {
        public int xpPerLevel=100,maxLevel=100,attributePointsPerLevel=3,talentPointsPerLevel=1;
        public double hpPerLevel=15,damagePerLevel=2;
        public boolean allowRespec=true;
        public List<Attribute> attributes=new ArrayList<>();
        public List<Talent> talents=new ArrayList<>();
    }
    public record Attribute(String id,String name,String description,int maxRank,Map<String,Double> effects){}
    public record Talent(String id,String classId,String name,String description,String branch,int minLevel,int maxRank,int cost,List<String> requires,Map<String,Double> effects,List<String> specializationIds){public Talent(String id,String classId,String name,String description,String branch,int minLevel,int maxRank,int cost,List<String> requires,Map<String,Double> effects){this(id,classId,name,description,branch,minLevel,maxRank,cost,requires,effects,List.of());}}
    public record Build(Map<String,Integer> attributes,Map<String,Integer> talents) {
        public Build {attributes=Map.copyOf(attributes);talents=Map.copyOf(talents);}
    }
    static boolean id(String value){return value!=null&&value.matches("[A-Za-z0-9_-]{1,64}");}
    static boolean text(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max;}
    static void effects(Map<String,Double> values){if(values==null||values.size()>EFFECTS.size())throw new IllegalArgumentException("Efectos inválidos");for(var e:values.entrySet())if(!EFFECTS.contains(e.getKey())||e.getValue()==null||!Double.isFinite(e.getValue())||Math.abs(e.getValue())>200)throw new IllegalArgumentException("Efecto desconocido o fuera de rango");}
    public static void validateRules(Rules r,List<WorldData.CharacterClass> classes){
        if(r==null||r.xpPerLevel<1||r.xpPerLevel>10000||r.maxLevel<2||r.maxLevel>1000||r.attributePointsPerLevel<0||r.attributePointsPerLevel>20||r.talentPointsPerLevel<0||r.talentPointsPerLevel>10||!Double.isFinite(r.hpPerLevel)||r.hpPerLevel<0||r.hpPerLevel>50||!Double.isFinite(r.damagePerLevel)||r.damagePerLevel<0||r.damagePerLevel>10||r.attributes==null||r.attributes.size()>30||r.talents==null||r.talents.size()>200)throw new IllegalArgumentException("Reglas de progresión inválidas");
        Set<String> ids=new HashSet<>();for(var a:r.attributes){if(a==null||!id(a.id())||!ids.add(a.id())||!text(a.name(),80)||!text(a.description(),300)||a.maxRank()<1||a.maxRank()>500)throw new IllegalArgumentException("Atributo de progresión inválido");effects(a.effects());}
        Map<String,Talent> nodes=new HashMap<>();for(var t:r.talents){if(t==null||!id(t.id())||nodes.put(t.id(),t)!=null||classes.stream().noneMatch(c->c.id().equals(t.classId()))||!text(t.name(),80)||!text(t.description(),300)||!text(t.branch(),80)||t.minLevel()<2||t.minLevel()>r.maxLevel||t.maxRank()<1||t.maxRank()>20||t.cost()<1||t.cost()>20||t.requires()==null||t.requires().size()>8||new HashSet<>(t.requires()).size()!=t.requires().size())throw new IllegalArgumentException("Talento inválido");effects(t.effects());}
        for(var t:r.talents)for(String parent:t.requires()){var p=nodes.get(parent);if(p==null||!p.classId().equals(t.classId())||p.minLevel()>t.minLevel())throw new IllegalArgumentException("Requisito de talento inválido: "+t.id());}
        Set<String> done=new HashSet<>();for(String key:nodes.keySet())visit(key,nodes,new HashSet<>(),done);
    }
    private static void visit(String id,Map<String,Talent> nodes,Set<String> active,Set<String> done){if(done.contains(id))return;if(!active.add(id))throw new IllegalArgumentException("El árbol contiene un ciclo");for(String p:nodes.get(id).requires())visit(p,nodes,active,done);active.remove(id);done.add(id);}
    public static int level(Rules r,int xp){return Math.min(r.maxLevel,1+Math.max(0,xp)/r.xpPerLevel);}
    public static int usedAttributes(Map<String,Integer> ranks){return ranks.values().stream().mapToInt(Integer::intValue).sum();}
    public static int usedTalents(Rules r,Map<String,Integer> ranks){return r.talents.stream().mapToInt(t->t.cost()*ranks.getOrDefault(t.id(),0)).sum();}
    public static Build validate(Rules r,String classId,int xp,Map<String,Integer> attrs,Map<String,Integer> talents){
        if(attrs==null||talents==null||attrs.size()>30||talents.size()>200)throw new IllegalArgumentException("Distribución de puntos inválida");int level=level(r,xp);
        for(var e:attrs.entrySet()){var a=r.attributes.stream().filter(v->v.id().equals(e.getKey())).findFirst().orElseThrow(()->new IllegalArgumentException("Atributo desconocido"));if(e.getValue()==null||e.getValue()<0||e.getValue()>a.maxRank())throw new IllegalArgumentException("Rango de atributo inválido");}
        for(var e:talents.entrySet()){var t=r.talents.stream().filter(v->v.id().equals(e.getKey())&&v.classId().equals(classId)).findFirst().orElseThrow(()->new IllegalArgumentException("Talento ajeno a la clase"));int rank=e.getValue()==null?-1:e.getValue();if(rank<0||rank>t.maxRank()||rank>0&&(level<t.minLevel()||t.requires().stream().anyMatch(p->talents.getOrDefault(p,0)<1)))throw new IllegalArgumentException("Nivel, rango o requisitos del talento incompletos");}
        if(usedAttributes(attrs)>(level-1)*r.attributePointsPerLevel||usedTalents(r,talents)>(level-1)*r.talentPointsPerLevel)throw new IllegalArgumentException("No tienes suficientes puntos");return new Build(attrs,talents);
    }
    public static Map<String,Integer> ranks(JsonNode node){if(!node.isObject())throw new IllegalArgumentException("Se esperaba un mapa de rangos");Map<String,Integer> map=new HashMap<>();node.fields().forEachRemaining(e->{if(!e.getValue().isIntegralNumber()||!e.getValue().canConvertToInt())throw new IllegalArgumentException("Los rangos deben ser enteros");map.put(e.getKey(),e.getValue().intValue());});return map;}
    public static double modifier(Rules r,String classId,Map<String,Integer> attributes,Map<String,Integer> talents,String effect){return r.attributes.stream().mapToDouble(a->attributes.getOrDefault(a.id(),0)*a.effects().getOrDefault(effect,0.0)).sum()+r.talents.stream().filter(t->t.classId().equals(classId)).mapToDouble(t->talents.getOrDefault(t.id(),0)*t.effects().getOrDefault(effect,0.0)).sum();}
    public static boolean lowers(Map<String,Integer> before,Map<String,Integer> after){return before.entrySet().stream().anyMatch(e->after.getOrDefault(e.getKey(),0)<e.getValue());}
}
