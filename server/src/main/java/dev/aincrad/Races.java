package dev.aincrad;

import java.util.*;

public final class Races {
    public record Ability(String name,String kind,double value,double duration,double cooldown,double range){}
    public static class Race {
        public String id,name,description,model="human",ears="round",portrait="",eyeColor="#263d37";
        public double scale=1,width=1;public boolean horns,tail,wings,beard;
        public List<String> skinColors=new ArrayList<>(List.of("#dfb18b","#bb835d","#784f3b")),hairColors=new ArrayList<>(List.of("#342f32","#c39757","#dad7cb"));
        public Map<String,Double> effects=new LinkedHashMap<>();
        public Ability ability=new Ability("Segundo aliento","heal",20,0,30,0);
        public String id(){return id;}public String name(){return name;}public String description(){return description;}public double scale(){return scale;}public String ears(){return ears;}
    }
    public static void validate(Race r){
        if(r.model==null||!Set.of("human","dwarf","elf","darkelf","draconian","cat").contains(r.model)||!Double.isFinite(r.width)||r.width<.6||r.width>1.5||r.description==null||r.description.length()>600||!WorldData.color(r.eyeColor)||r.portrait==null||!r.portrait.isEmpty()&&!r.portrait.matches("assets/[A-Za-z0-9_-]+\\.(png|webp|jpg)")||r.skinColors==null||r.skinColors.isEmpty()||r.skinColors.size()>30||r.hairColors==null||r.hairColors.isEmpty()||r.hairColors.size()>30||r.skinColors.stream().anyMatch(v->!WorldData.color(v))||r.hairColors.stream().anyMatch(v->!WorldData.color(v)))throw new IllegalArgumentException("Aspecto racial inválido");
        Progression.effects(r.effects);var a=r.ability;if(a==null||a.name()==null||a.name().isBlank()||a.name().length()>80||a.kind()==null||!Set.of("heal","shield","sprint","critical","flame").contains(a.kind())||!Double.isFinite(a.value())||a.value()<0||a.value()>100||!Double.isFinite(a.duration())||a.duration()<0||a.duration()>30||!Double.isFinite(a.cooldown())||a.cooldown()<5||a.cooldown()>300||!Double.isFinite(a.range())||a.range()<0||a.range()>15)throw new IllegalArgumentException("Habilidad racial inválida");
    }
}
