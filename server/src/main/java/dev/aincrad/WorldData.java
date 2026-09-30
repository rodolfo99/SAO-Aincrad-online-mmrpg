package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** All geometry that affects navigation is generated here, once, from the shared seed. */
public class WorldData {
    private static final double PLAYER_CLEARANCE=.4;
    public List<Bestiary.Species> monsterSpecies;public int schemaRevision=7;public Gathering.Rules gathering;public Crafting.Rules crafting;public Pvp.Rules pvp=new Pvp.Rules();public int version; public long seed; public String name; public List<Floor> floors;public CharacterOptions characterOptions;
    public record Choice(String id,String name){}
    public record Ability(String name,String kind,double range,double power,double cooldown){}
    public record CharacterClass(String id,String name,String description,int hp,int damage,double speed,double attackRange,Ability ability){}
    public record Outfit(String id,String name,String classId,String gender,int minLevel,Map<String,Double> attributes,String style,String primary,String metal,String accent,boolean helmet,boolean cape,List<String> specializationIds){public Outfit(String id,String name,String classId,String gender,int minLevel,Map<String,Double> attributes,String style,String primary,String metal,String accent,boolean helmet,boolean cape){this(id,name,classId,gender,minLevel,attributes,style,primary,metal,accent,helmet,cape,List.of());}}
    public record AttributeDefinition(String id,String name,String effect,double min,double max,double step){}
    public static class CharacterOptions {public List<Specializations.Specialty> specializations;public List<Skills.Skill> skills;public Progression.Rules progression;public List<AttributeDefinition> attributeDefinitions;public List<Outfit> equipmentSets;public List<Weapons.Loadout> weaponSets;public List<Choice> genders,faces;public List<CharacterClass> classes;public List<Races.Race> races;public List<String> hairColors,skinColors;}
    public record Appearance(String gender,String face,String hairColor,String skinColor,String classId,String race,String specializationId){public Appearance(String gender,String face,String hairColor,String skinColor,String classId,String race){this(gender,face,hairColor,skinColor,classId,race,Specializations.defaultId(classId));}}
    public record Point(double x,double z) {}
    public record Portal(double x,double z,int to,boolean requiresBoss) {}
    public record Npc(String id,String name,String role,double x,double z) {}
    public record Spawn(String id,String kind,String name,double x,double z,int level) {public Spawn(String id,String kind,String name,double x,double z){this(id,kind,name,x,z,1);}}
    public record Building(String id,String kind,String name,double x,double z,double rotation){}
    public record Prop(String kind,double x,double z,double scale,double rotation,double radius) {}
    public static class Floor {
        public List<Bestiary.Zone> monsterZones=new ArrayList<>();@com.fasterxml.jackson.annotation.JsonIgnore public List<Bestiary.Placed> zoneSpawns=new ArrayList<>();public Training.Zone training;public int id,treeCount; public String name,subtitle,biome; public double radius;
        public List<Gathering.Node> resources=new ArrayList<>();public List<Building> buildings=new ArrayList<>();public Point spawn; public Portal portal; public List<Npc> npcs; public List<Spawn> monsters;
        public List<Prop> props=new ArrayList<>();
    }
    public static WorldData load(Path path,ObjectMapper json) throws IOException {
        var tree=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(path.toFile());boolean migrated=WorldMigrations.upgrade(tree,json);
        WorldData w=json.treeToValue(tree,WorldData.class);w.validateAndGenerate();
        if(migrated){Files.copy(path,path.resolveSibling(path.getFileName()+".pre-migration-"+UUID.randomUUID()+".bak"));Path temp=path.resolveSibling(path.getFileName()+".partial");Files.write(temp,json.writerWithDefaultPrettyPrinter().writeValueAsBytes(tree));Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        return w;
    }
    public Floor floor(int id) { return floors.stream().filter(f->f.id==id).findFirst().orElseThrow(()->new IllegalArgumentException("Piso inexistente")); }
    public CharacterClass profession(String id){return characterOptions.classes.stream().filter(c->c.id().equals(id)).findFirst().orElse(characterOptions.classes.get(0));}
    public Appearance defaultAppearance(){return new Appearance(characterOptions.genders.get(0).id(),characterOptions.faces.get(0).id(),characterOptions.hairColors.get(0),characterOptions.skinColors.get(1),characterOptions.classes.get(0).id(),characterOptions.races.get(0).id(),Specializations.first(characterOptions,characterOptions.classes.get(0).id()));}
    public Appearance validateAppearance(Appearance a){
        if(a==null)a=defaultAppearance();
        if(a.specializationId()==null||a.specializationId().isBlank())a=new Appearance(a.gender(),a.face(),a.hairColor(),a.skinColor(),a.classId(),a.race(),Specializations.first(characterOptions,a.classId()));
        final Appearance value=a;Specializations.resolve(characterOptions,a.classId(),a.specializationId());
        if(characterOptions.genders.stream().noneMatch(v->v.id().equals(value.gender()))||characterOptions.faces.stream().noneMatch(v->v.id().equals(value.face()))||characterOptions.classes.stream().noneMatch(v->v.id().equals(value.classId()))||characterOptions.races.stream().noneMatch(v->v.id().equals(value.race()))||!color(a.hairColor())||!color(a.skinColor()))throw new IllegalArgumentException("Apariencia, clase o raza no válida");return a;
    }
    static boolean color(String value){return value!=null&&value.matches("#[0-9a-fA-F]{6}");}
    private void validateCharacters(){
        var c=characterOptions;if(c==null||c.genders==null||c.faces==null||c.classes==null||c.races==null||c.hairColors==null||c.skinColors==null||c.genders.isEmpty()||c.faces.isEmpty()||c.classes.isEmpty()||c.races.isEmpty()||c.hairColors.isEmpty()||c.skinColors.size()<2||c.classes.size()>20||c.races.size()>20||c.faces.size()>20||c.genders.size()>10||c.skinColors.size()>30||c.hairColors.size()>30)throw new IllegalArgumentException("Catálogo de personajes incompleto o excesivo");
        Set<String> ids=new HashSet<>();for(var p:c.classes)if(p==null||!validId(p.id())||!validName(p.name())||p.description()==null||p.description().length()>300||!ids.add(p.id())||p.hp()<40||p.hp()>300||p.damage()<1||p.damage()>60||!Double.isFinite(p.speed())||p.speed()<2||p.speed()>9)throw new IllegalArgumentException("Clase inválida");
        for(var p:c.classes){var a=p.ability();if(!Double.isFinite(p.attackRange())||p.attackRange()<1||p.attackRange()>15||a==null||!validName(a.name())||!Set.of("strike","arcane","heal").contains(a.kind())||!Double.isFinite(a.range())||a.range()<1||a.range()>15||!Double.isFinite(a.power())||a.power()<.1||a.power()>100||!Double.isFinite(a.cooldown())||a.cooldown()<1||a.cooldown()>60)throw new IllegalArgumentException("Habilidad de clase inválida");}
        Progression.validateRules(c.progression,c.classes);
        ids.clear();for(var r:c.races)if(r==null||!validId(r.id())||!validName(r.name())||!ids.add(r.id())||!Double.isFinite(r.scale())||r.scale()<.6||r.scale()>1.4||r.ears()==null||!Set.of("round","pointed","cat","horned").contains(r.ears()))throw new IllegalArgumentException("Raza inválida");
        for(var race:c.races)Races.validate(race);
        for(var list:List.of(c.genders,c.faces)){ids.clear();for(var v:list)if(v==null||!validId(v.id())||!validName(v.name())||!ids.add(v.id()))throw new IllegalArgumentException("Rostro o género inválido");}
        if(c.attributeDefinitions==null||c.attributeDefinitions.size()>100)throw new IllegalArgumentException("Catálogo de atributos inválido");
        Set<String> attrIds=new HashSet<>();for(var a:c.attributeDefinitions)if(a==null||!validId(a.id())||!validName(a.name())||!attrIds.add(a.id())||a.effect()==null||!Progression.EFFECTS.contains(a.effect())||!Double.isFinite(a.min())||!Double.isFinite(a.max())||!Double.isFinite(a.step())||a.min()< -1000||a.max()>1000||a.min()>a.max()||a.step()<=0||a.step()>1000)throw new IllegalArgumentException("Definición de atributo inválida");
        if(c.equipmentSets==null||c.equipmentSets.isEmpty()||c.equipmentSets.size()>500)throw new IllegalArgumentException("Catálogo de armaduras requerido");
        ids.clear();Set<String> levels=new HashSet<>();for(var gear:c.equipmentSets){if(gear==null||!validId(gear.id())||!validName(gear.name())||!ids.add(gear.id())||!levels.add(gear.classId()+":"+gear.gender()+":"+gear.minLevel()+":"+gear.specializationIds())||c.classes.stream().noneMatch(v->v.id().equals(gear.classId()))||c.genders.stream().noneMatch(v->v.id().equals(gear.gender()))||gear.minLevel()<1||gear.minLevel()>1000||gear.attributes()==null||gear.attributes().size()>100||gear.style()==null||!Set.of("tunic","leather","mail","plate").contains(gear.style())||!color(gear.primary())||!color(gear.metal())||!color(gear.accent()))throw new IllegalArgumentException("Conjunto de armadura inválido");for(var entry:gear.attributes().entrySet()){var def=c.attributeDefinitions.stream().filter(v->v.id().equals(entry.getKey())).findFirst().orElseThrow(()->new IllegalArgumentException("Atributo no definido: "+entry.getKey()));if(entry.getValue()==null||!Double.isFinite(entry.getValue())||entry.getValue()<def.min()||entry.getValue()>def.max())throw new IllegalArgumentException("Atributo fuera de rango: "+entry.getKey());}}
        for(var profession:c.classes)for(var gender:c.genders)if(c.equipmentSets.stream().noneMatch(g->g.classId().equals(profession.id())&&g.gender().equals(gender.id())&&g.minLevel()==1))throw new IllegalArgumentException("Falta equipo de nivel 1 para "+profession.id()+" / "+gender.id());
        Weapons.validate(c);Specializations.validate(c);Skills.validate(c);
        if(c.hairColors.stream().anyMatch(v->!color(v))||c.skinColors.stream().anyMatch(v->!color(v)))throw new IllegalArgumentException("Color de personaje inválido");
    }
    public void validateAndGenerate() {
        validateCharacters();if(schemaRevision!=7)throw new IllegalArgumentException("Revisión de mundo incorrecta");Crafting.validate(crafting,characterOptions);Gathering.validate(gathering,crafting);if(pvp==null)throw new IllegalArgumentException("Falta configuración PvP");pvp.validate();Bestiary.validate(this);
        if(name==null||name.isBlank()||name.length()>120||version!=1||floors==null||floors.isEmpty()||floors.size()>100) throw new IllegalArgumentException("Mundo v1: entre 1 y 100 pisos");
        Set<Integer> ids=new HashSet<>(); Set<String> entities=new HashSet<>();
        for(Floor f:floors) {
            if(f==null||f.name==null||f.name.isBlank()||f.name.length()>100||f.subtitle==null||f.subtitle.length()>160||f.biome==null||f.id<1||!ids.add(f.id)||!Double.isFinite(f.radius)||f.radius<35||f.radius>100||f.treeCount<0||f.treeCount>300||f.spawn==null||f.portal==null||f.npcs==null||f.monsters==null||f.npcs.size()>100||f.monsters.size()>200||!Set.of("meadow","crystal").contains(f.biome)) throw new IllegalArgumentException("Piso inválido: "+f.id);
            Training.validate(f,entities);checkPoint(f,f.spawn.x(),f.spawn.z());checkPoint(f,f.portal.x(),f.portal.z());
            if(f.buildings==null||f.buildings.size()>40)throw new IllegalArgumentException("Edificios inválidos");
            for(var b:f.buildings){if(b==null||!validId(b.id())||!entities.add(b.id())||!validName(b.name())||!Set.of("smithy","tailor","shop","house").contains(b.kind())||!Double.isFinite(b.rotation()))throw new IllegalArgumentException("Edificio inválido");checkPoint(f,b.x(),b.z());if(Math.hypot(b.x(),b.z())>f.radius-5)throw new IllegalArgumentException("Edificio demasiado cerca del borde");}
            if(f.resources==null||f.resources.size()>200)throw new IllegalArgumentException("Nodos de recursos inválidos");for(var node:f.resources){if(node==null||!validId(node.id())||!entities.add(node.id()))throw new IllegalArgumentException("Nodo de recurso inválido");Gathering.resource(gathering,node.typeId());checkPoint(f,node.x(),node.z());}
            for(Npc n:f.npcs){if(n==null||!validId(n.id())||!validName(n.name())||n.role()==null||!entities.add(n.id())||!Set.of("guide","smith","tailor","merchant").contains(n.role()))throw new IllegalArgumentException("NPC inválido");checkPoint(f,n.x(),n.z());}
            for(Spawn m:f.monsters){if(m==null||!validId(m.id())||!validName(m.name())||m.kind()==null||!entities.add(m.id())||(m.level()<1||m.level()>1000))throw new IllegalArgumentException("Monstruo inválido");Bestiary.species(this,m.kind());checkPoint(f,m.x(),m.z());}
            Bestiary.validateZones(this,f,entities);
        }
        for(Floor f:floors) {
            floor(f.portal.to());f.props.clear();Random r=new Random(seed+f.id*1009L);
            for(var b:f.buildings){if(f.props.stream().anyMatch(p->Math.hypot(p.x()-b.x(),p.z()-b.z())<p.radius()+3.1))throw new IllegalArgumentException("Edificios superpuestos");f.props.add(new Prop(b.kind(),b.x(),b.z(),1,b.rotation(),3.1));}
            if(f.biome.equals("meadow"))for(double x:new double[]{-10,10})for(double z:new double[]{19,27})if(f.props.stream().noneMatch(p->Math.hypot(p.x()-x,p.z()-z)<p.radius()+3.1))f.props.add(new Prop("house",x,z,1,r.nextDouble()*.15,3.1));
            for(var node:f.resources){if(f.props.stream().anyMatch(p->Math.hypot(p.x()-node.x(),p.z()-node.z())<p.radius()+1.2))throw new IllegalArgumentException("Recurso bloqueado por edificio u otro recurso");f.props.add(new Prop("resource",node.x(),node.z(),1,0,.9));}
            if(f.training!=null&&f.props.stream().anyMatch(p->Math.hypot(p.x()-f.training.x,p.z()-f.training.z)<p.radius()+f.training.radius))throw new IllegalArgumentException("Patio de entrenamiento bloqueado por edificios o recursos");
            for(int attempt=0,count=0;count<f.treeCount&&attempt<10000;attempt++) {
                double x=(r.nextDouble()*2-1)*(f.radius-3),z=(r.nextDouble()*2-1)*(f.radius-3);
                if(Training.inside(f.training,x,z)||Math.hypot(x,z)>f.radius-4||Math.abs(x)<6||Math.hypot(x-f.spawn.x(),z-f.spawn.z())<8||Math.hypot(x-f.portal.x(),z-f.portal.z())<6)continue;
                if(f.monsters.stream().anyMatch(m->Math.hypot(x-m.x(),z-m.z())<4)||f.npcs.stream().anyMatch(n->Math.hypot(x-n.x(),z-n.z())<4)||f.props.stream().anyMatch(p->Math.hypot(x-p.x(),z-p.z())<p.radius()+2))continue;
                double scale=.8+r.nextDouble()*.65;f.props.add(new Prop(f.biome.equals("crystal")&&count%3==0?"crystal":"tree",x,z,scale,r.nextDouble()*Math.PI*2,.65*scale));count++;
            }
            if(!walkable(f.id,f.spawn.x(),f.spawn.z())||!walkable(f.id,f.portal.x(),f.portal.z()))throw new IllegalArgumentException("Aparición o portal bloqueado por una vivienda");
            for(Npc n:f.npcs)if(!walkable(f.id,n.x(),n.z()))throw new IllegalArgumentException("NPC bloqueado: "+n.id());
            for(Spawn m:f.monsters)if(!walkable(f.id,m.x(),m.z()))throw new IllegalArgumentException("Monstruo bloqueado: "+m.id());
            f.zoneSpawns=Bestiary.placements(this,f);
        }
    }
    private static boolean validId(String id){return id!=null&&id.matches("[A-Za-z0-9_-]{1,64}");}
    private static boolean validName(String name){return name!=null&&!name.isBlank()&&name.length()<=80;}
    private static void checkPoint(Floor f,double x,double z) {if(!Double.isFinite(x)||!Double.isFinite(z)||Math.hypot(x,z)>f.radius-2)throw new IllegalArgumentException("Coordenada fuera del piso "+f.id);}
    public boolean walkable(int floor,double x,double z) {
        Floor f=floor(floor);if(!Double.isFinite(x)||!Double.isFinite(z)||Math.hypot(x,z)>f.radius-1)return false;
        return f.props.stream().noneMatch(p->Math.hypot(x-p.x(),z-p.z())<p.radius()+PLAYER_CLEARANCE);
    }
    /** Swept clearance for the same circular blockers and player margin as walkable. */
    public boolean walkableSegment(int floor,double fromX,double fromZ,double toX,double toZ) {
        if(!walkable(floor,fromX,fromZ)||!walkable(floor,toX,toZ))return false;
        // The floor boundary is a convex disk, so valid endpoints keep the segment inside it.
        double dx=toX-fromX,dz=toZ-fromZ,lengthSquared=dx*dx+dz*dz;
        if(lengthSquared==0)return true;
        for(Prop prop:floor(floor).props){
            double t=Math.max(0,Math.min(1,((prop.x()-fromX)*dx+(prop.z()-fromZ)*dz)/lengthSquared));
            if(Math.hypot(fromX+t*dx-prop.x(),fromZ+t*dz-prop.z())<prop.radius()+PLAYER_CLEARANCE)return false;
        }
        return true;
    }
}
