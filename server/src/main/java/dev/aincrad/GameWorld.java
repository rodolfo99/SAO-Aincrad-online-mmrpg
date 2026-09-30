package dev.aincrad;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.io.IOException;

/** Serialized simulation: clients submit intentions, never position, HP, damage, currency or XP. */
public class GameWorld {
    public static final double SPEED=5.5;
    public static class Player {
        public Training.Session training=new Training.Session();public Skills.State skills;public List<Skills.Active> effects=new ArrayList<>();public Crafting.State crafting;public Crafting.Rules craftingRules;public String id,name,hash;public String accountId="";public String weaponSetId="",targetKind="monster";public boolean pvpMode;public Pvp.State pvp;public Pvp.Rules pvpRules;public java.util.function.LongSupplier clock;public double racialUntil,racialShield,racialBonus;public String racialEffect="";public WorldData.Appearance appearance;public WorldData.CharacterClass profession;public WorldData.CharacterOptions catalog; public int floor=1,hp=100,xp,col,potions=5,kills,weapon;
        public double x,z,heading,dx,dz,inputUntil,attackAt,skillAt,potionAt,deadUntil,chatAt,portalAt;
        public boolean quest,reward,unlocked,online; public long sequence=-1;
        public Map<String,Integer> attributeRanks=new HashMap<>(),talentRanks=new HashMap<>();
        public Double goalX,goalZ; public String target="";
        private final Deque<WorldData.Point> route=new ArrayDeque<>();
        WorldData.Outfit outfit(){var made=Crafting.equipped(this,"armor");if(made!=null)return made.armor;return catalog.equipmentSets.stream().filter(g->Specializations.armor(specialty(),g)&&g.gender().equals(appearance.gender())&&g.minLevel()<=level()).max(Comparator.comparingInt(WorldData.Outfit::minLevel)).orElseThrow();}
        Specializations.Specialty specialty(){return Specializations.resolve(catalog,appearance.classId(),appearance.specializationId());}
        Races.Race race(){return catalog.races.stream().filter(r->r.id.equals(appearance.race())).findFirst().orElseThrow();}
        Weapons.Loadout weapons(){var set=Weapons.resolve(catalog,appearance.classId(),appearance.specializationId(),level(),weaponSetId);var main=Crafting.equipped(this,"mainHand");var off=Crafting.equipped(this,"offHand");var first=main==null?set.mainHand():main.weapon;var second=first.hands()==2?null:off==null?set.offHand():off.weapon;return new Weapons.Loadout(set.id(),main==null&&off==null?set.name():"Equipo artesanal",set.classId(),set.minLevel(),set.autoEquip(),first,second);}
        double modifier(String effect){return Skills.modifier(effects,effect,clock.getAsLong())+specialty().effects().getOrDefault(effect,0.0)+race().effects.getOrDefault(effect,0.0)+Weapons.modifier(catalog,weapons(),effect)+catalog.attributeDefinitions.stream().filter(d->d.effect().equals(effect)).mapToDouble(d->outfit().attributes().getOrDefault(d.id(),0.0)).sum()+Progression.modifier(catalog.progression,appearance.classId(),attributeRanks,talentRanks,effect);}
        int damage(){return (int)Math.max(1,Math.min(150,profession.damage()+weapon*4+(level()-1)*catalog.progression.damagePerLevel+Math.round(modifier("damage"))));}
        int defense(){return (int)Math.max(0,Math.min(40,Math.round(modifier("defense"))));}
        double speed(){return Math.max(2,Math.min(10,profession.speed()+modifier("speed")+(racialEffect.equals("sprint")?racialBonus:0)));}
        double critical(){return Math.max(0,Math.min(50,modifier("criticalChance")+(racialEffect.equals("critical")?racialBonus:0)));}
        int level(){return Progression.level(catalog.progression,xp);} int maxHp(){return (int)Math.max(30,Math.min(500,profession.hp()+(level()-1)*catalog.progression.hpPerLevel+Math.round(modifier("maxHp"))));}
        PlayerStore.Profile profile(){return new PlayerStore.Profile(id,hash,name,floor,x,z,hp,xp,col,potions,kills,quest,reward,unlocked,weapon,appearance,Map.copyOf(attributeRanks),Map.copyOf(talentRanks),pvp.copy(),weaponSetId,crafting.copy(),skills.copy(),accountId);}
        Map<String,Object> view(){Map<String,Object> v=new LinkedHashMap<>();v.put("id",id);v.put("name",name);v.put("floor",floor);v.put("x",x);v.put("z",z);v.put("heading",heading);v.put("hp",hp);v.put("maxHp",maxHp());v.put("level",level());v.put("xp",xp);v.put("col",col);v.put("potions",potions);v.put("kills",kills);v.put("quest",quest);v.put("reward",reward);v.put("unlocked",unlocked);v.put("weapon",weapon);v.put("appearance",appearance);v.put("outfit",outfit());v.put("weapons",weapons());v.put("specialization",specialty());v.put("training",training.view());v.put("skills",Skills.view(this));v.put("effects",Skills.names(effects,clock.getAsLong()));v.put("crafting",Crafting.view(craftingRules,crafting));v.put("weaponSetId",weaponSetId);v.put("automaticWeapons",weaponSetId.isEmpty()&&!crafting.equipped.containsKey("mainHand")&&!crafting.equipped.containsKey("offHand"));v.put("race",race());v.put("pvp",pvp.view(pvpRules,clock.getAsLong()));v.put("pvpMode",pvpMode);v.put("online",online);v.put("targetKind",targetKind);v.put("racialShield",racialShield);v.put("defense",defense());v.put("damage",damage());v.put("speed",speed());v.put("critical",critical());v.put("attributeRanks",Map.copyOf(attributeRanks));v.put("talentRanks",Map.copyOf(talentRanks));v.put("attributePoints",(level()-1)*catalog.progression.attributePointsPerLevel-Progression.usedAttributes(attributeRanks));v.put("talentPoints",(level()-1)*catalog.progression.talentPointsPerLevel-Progression.usedTalents(catalog.progression,talentRanks));v.put("ability",profession.ability());v.put("target",target);v.put("sequence",sequence);return v;}
    }
    public static class Monster {
        Training.Dummy dummy;Training.Zone training;long trainingLast;boolean practice(){return dummy!=null;}boolean support(){return dummy!=null&&dummy.ally();}
        Bestiary.Species species;Bestiary.Zone zone;int level=1;boolean returning;String id,name,kind;int floor,hp,maxHp;double x,z,homeX,homeZ,heading,attackAt,respawnAt,windupUntil,evadeAt;String victim="";
        List<Skills.Active> effects=new ArrayList<>();String tauntedBy="";long tauntedUntil;Set<String> participants=new HashSet<>();
        Map<String,Object> view(){return Map.ofEntries(Map.entry("id",id),Map.entry("name",name),Map.entry("kind",kind),Map.entry("model",practice()?"dummy":species.model()),Map.entry("color",practice()?"#d0ac6f":species.color()),Map.entry("level",level),Map.entry("zoneId",zone==null?"":zone.id()),Map.entry("returning",returning),Map.entry("damage",practice()?0:species.damage(level)),Map.entry("xpReward",practice()?0:species.xp(level)),Map.entry("colReward",practice()?0:species.col(level)),Map.entry("floor",floor),Map.entry("x",x),Map.entry("z",z),Map.entry("heading",heading),Map.entry("hp",hp),Map.entry("maxHp",maxHp),Map.entry("windup",windupUntil>0),Map.entry("training",practice()),Map.entry("ally",support()),Map.entry("defense",practice()?dummy.defense():species.defense(level)),Map.entry("effects",effects.stream().map(e->e.name).distinct().toList()));}
    }
    public final WorldData data; final PlayerStore store;
    private final WorldPathfinder pathfinder;
    final Map<String,Player> profiles=new LinkedHashMap<>();final Map<String,Player> online=new LinkedHashMap<>();final Map<String,Monster> monsters=new LinkedHashMap<>();
    private final Gathering.Store gatheringStore;private final PvpJournal journal;private final java.util.function.LongSupplier clock;private final BiConsumer<String,Map<String,Object>> events;private double time;private int ticks;
    public GameWorld(WorldData data,PlayerStore store,BiConsumer<String,Map<String,Object>> events)throws IOException {
        this(data,store,events,System::currentTimeMillis);
    }
    GameWorld(WorldData data,PlayerStore store,BiConsumer<String,Map<String,Object>> events,java.util.function.LongSupplier clock)throws IOException {
        this.data=data;this.pathfinder=new WorldPathfinder(data);this.store=store;this.events=events;this.clock=clock;this.journal=store.journal();this.gatheringStore=store.gathering();
        for(var p:store.load()){
            Player a=new Player();initialize(a);a.skills=p.skills()==null?new Skills.State():p.skills().copy();a.crafting=p.crafting()==null?Crafting.initial(data.crafting):p.crafting().copy();a.pvp=p.pvp()==null?new Pvp.State(data.pvp.initialCitizenship):p.pvp().copy();a.pvp.citizenship=Math.max(data.pvp.minCitizenship,Math.min(data.pvp.maxCitizenship,a.pvp.citizenship));a.weaponSetId=Objects.toString(p.weaponSetId(),"");a.catalog=data.characterOptions;a.appearance=p.appearance();try{a.appearance=data.validateAppearance(a.appearance);}catch(IllegalArgumentException invalid){a.appearance=data.defaultAppearance();}a.profession=data.profession(a.appearance.classId());a.id=p.id();a.accountId=Objects.toString(p.accountId(),"");a.hash=p.tokenHash();a.name=p.name();a.floor=p.floor();a.x=p.x();a.z=p.z();a.hp=p.hp();a.xp=p.xp();a.col=p.col();a.potions=p.potions();a.kills=p.kills();a.quest=p.quest();a.reward=p.reward();a.unlocked=p.unlocked();a.weapon=p.weapon();a.attributeRanks=new HashMap<>(p.attributeRanks()==null?Map.of():p.attributeRanks());a.talentRanks=new HashMap<>(p.talentRanks()==null?Map.of():p.talentRanks());reconcile(a);a.hp=Math.min(a.hp,a.maxHp());
            if(data.floors.stream().noneMatch(f->f.id==a.floor)){a.floor=data.floors.get(0).id;spawn(a);}if(!data.walkable(a.floor,a.x,a.z))spawn(a);profiles.put(a.hash,a);
        }
        for(var receipt:gatheringStore.receipts)for(Player p:profiles.values())if(p.id.equals(receipt.playerId())&&receipt.sequence()>p.crafting.lastGatherEvent){applyHarvest(p,receipt);store.save(p.profile());}
        for(var receipt:journal.receipts())for(Player a:profiles.values())if(receipt.sequence()>a.pvp.lastEvent&&receipt.states().containsKey(a.id)){a.pvp=receipt.states().get(a.id).copy();if(receipt.kind().equals("kill")&&a.id.equals(receipt.victim()))a.hp=0;store.save(a.profile());}
        for(Player a:profiles.values()){if(a.hp<=0)spawn(a);if(a.pvp.combatUntil>clock.getAsLong())online.put(a.id,a);}
        for(var f:data.floors){for(var spawn:f.monsters)addMonster(spawn.id(),spawn.name(),spawn.kind(),spawn.level(),f.id,spawn.x(),spawn.z(),null);for(var spawn:f.zoneSpawns)addMonster(spawn.id(),Bestiary.species(data,spawn.speciesId()).name(),spawn.speciesId(),spawn.level(),f.id,spawn.x(),spawn.z(),spawn.zone());}
        for(var f:data.floors)if(f.training!=null)for(var d:f.training.dummies){Monster m=new Monster();m.dummy=d;m.training=f.training;m.kind="dummy";m.id=d.id();m.name=d.name();m.floor=f.id;m.homeX=m.x=d.x();m.homeZ=m.z=d.z();m.maxHp=d.maxHp();m.hp=d.ally()?m.maxHp/2:m.maxHp;monsters.put(m.id,m);}
    }
    private void addMonster(String id,String name,String kind,int level,int floor,double x,double z,Bestiary.Zone zone){Monster m=new Monster();m.id=id;m.name=name;m.kind=kind;m.level=level;m.species=Bestiary.species(data,kind);m.zone=zone;m.floor=floor;m.homeX=m.x=x;m.homeZ=m.z=z;m.hp=m.maxHp=m.species.hp(level);monsters.put(m.id,m);}
    private void initialize(Player p){p.skills=new Skills.State();p.craftingRules=data.crafting;p.crafting=Crafting.initial(data.crafting);p.catalog=data.characterOptions;p.pvpRules=data.pvp;p.clock=clock;p.pvp=new Pvp.State(data.pvp.initialCitizenship);}
    static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    // Legacy fixture/migration support only. The network uses joinAccount exclusively.
    synchronized Map<String,Object> join(String name,String token)throws IOException {return join(name,token,null);}
    synchronized Map<String,Object> join(String name,String token,WorldData.Appearance appearance)throws IOException {
        if(online.size()>=32&&(token.isBlank()||!profiles.containsKey(hash(token))||!online.containsKey(profiles.get(hash(token)).id)))throw new IllegalArgumentException("Mundo lleno (32 personajes activos)");
        Player p;String returned=token;
        if(!token.isBlank()){p=profiles.get(hash(token));if(p==null||!p.accountId.isEmpty())throw new IllegalArgumentException("Clave anterior no disponible; utiliza tu cuenta.");}
        else {
            name=name.strip();if(!name.matches("[\\p{L}\\p{N} _-]{2,20}"))throw new IllegalArgumentException("Nombre: 2–20 letras, números, espacios, guion o _");
            if(profiles.size()>=1000)throw new IllegalArgumentException("Límite de perfiles alcanzado");
            byte[] secret=new byte[32];new SecureRandom().nextBytes(secret);returned=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
            p=new Player();initialize(p);p.catalog=data.characterOptions;p.appearance=data.validateAppearance(appearance);p.profession=data.profession(p.appearance.classId());p.id=UUID.randomUUID().toString();p.name=name;p.hash=hash(returned);p.floor=data.floors.get(0).id;spawn(p);store.save(p.profile());profiles.put(p.hash,p);
        }
        if(p.online)throw new IllegalArgumentException("Este personaje ya está conectado en otra pestaña");
        p.online=true;p.dx=p.dz=0;cancelRoute(p);p.sequence=-1;p.target="";online.put(p.id,p);
        return Map.of("type","welcome","id",p.id,"token",returned,"player",p.view());
    }
    public synchronized List<Map<String,Object>> ownedCharacters(String accountId) {
        return profiles.values().stream().filter(p->p.accountId.equals(accountId))
            .map(p->Map.<String,Object>of("id",p.id,"name",p.name,"level",p.level(),"floor",p.floor,"online",p.online)).toList();
    }
    public synchronized Map<String,Object> joinAccount(String accountId,String characterId,String name,WorldData.Appearance appearance)throws IOException {
        if(!PlayerAuth.validId(accountId))throw new PlayerAuth.Unauthorized("Inicia sesión para acceder al juego");
        Player p=null;
        if(characterId!=null&&!characterId.isBlank()) {
            p=profiles.values().stream().filter(a->a.id.equals(characterId)&&a.accountId.equals(accountId)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Personaje no disponible en esta cuenta"));
        }
        if(online.size()>=32&&(p==null||!online.containsKey(p.id)))throw new IllegalArgumentException("Mundo lleno (32 personajes activos)");
        if(p==null) {
            if(name==null||!name.strip().matches("[\\p{L}\\p{N} _-]{2,20}"))throw new IllegalArgumentException("Nombre: 2–20 letras, números, espacios, guion o _");
            if(profiles.size()>=1000)throw new IllegalArgumentException("Límite de perfiles alcanzado");
            p=new Player();initialize(p);p.accountId=accountId;p.appearance=data.validateAppearance(appearance);
            p.profession=data.profession(p.appearance.classId());p.id=UUID.randomUUID().toString();p.name=name.strip();
            byte[] secret=new byte[32];new SecureRandom().nextBytes(secret);p.hash=hash(Base64.getEncoder().encodeToString(secret));
            p.floor=data.floors.get(0).id;spawn(p);store.save(p.profile());profiles.put(p.hash,p);
        }
        if(p.online)throw new IllegalArgumentException("Este personaje ya está conectado en otra pestaña");
        p.online=true;p.dx=p.dz=0;cancelRoute(p);p.sequence=-1;p.target="";online.put(p.id,p);
        return Map.of("type","welcome","id",p.id,"player",p.view());
    }
    public synchronized Map<String,Object> claimLegacy(String accountId,String token)throws IOException {
        if(!PlayerAuth.validId(accountId))throw new PlayerAuth.Unauthorized("Inicia sesión para vincular personajes");
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw new IllegalArgumentException("Clave anterior no disponible");
        Player p=profiles.get(hash(token));
        if(p==null||!p.accountId.isEmpty())throw new IllegalArgumentException("Clave anterior no disponible o personaje ya vinculado");
        return assignAccount(p.id,accountId);
    }
    public synchronized Map<String,Object> assignAccount(String characterId,String accountId)throws IOException {
        if(!PlayerAuth.validId(accountId))throw new IllegalArgumentException("Cuenta no válida");
        Player p=profiles.values().stream().filter(a->a.id.equals(characterId)).findFirst().orElseThrow(()->new IllegalArgumentException("Personaje inexistente"));
        if(online.containsKey(p.id))throw new IllegalArgumentException("Desconecta el personaje y espera a que termine el combate antes de vincularlo");
        var previous=p.profile();store.backup(previous);p.accountId=accountId;
        try{store.save(p.profile());}catch(IOException e){p.accountId=Objects.toString(previous.accountId(),"");throw e;}
        return Map.of("id",p.id,"name",p.name,"level",p.level());
    }
    private static void cancelRoute(Player p){p.goalX=p.goalZ=null;p.route.clear();}
    public synchronized void leave(String id)throws IOException {Player p=online.get(id);if(p!=null){p.online=false;p.dx=p.dz=0;cancelRoute(p);p.pvpMode=false;store.save(p.profile());if(p.pvp.combatUntil<=clock.getAsLong()||p.hp<=0)online.remove(id);}}
    private void spawn(Player p){var spawn=data.floor(p.floor).spawn;p.x=spawn.x();p.z=spawn.z();p.hp=p.maxHp();p.dx=p.dz=0;cancelRoute(p);p.target="";p.targetKind="monster";p.effects.clear();p.racialShield=0;p.racialEffect="";p.racialBonus=0;p.pvp.spawnProtectedUntil=clock.getAsLong()+data.pvp.spawnProtectionSeconds*1000L;}
    private static double num(JsonNode n,String k){JsonNode v=n.get(k);if(v==null||!v.isNumber()||!Double.isFinite(v.doubleValue()))throw new IllegalArgumentException("Número inválido: "+k);return v.doubleValue();}
    public synchronized void command(String id,JsonNode n){
        Player p=online.get(id);if(p==null||!p.online)throw new IllegalArgumentException("Primero entra al mundo");
        String type=n.path("type").asText();if(type.equals("ping")){return;}if(p.hp<=0)return;
        switch(type){
            case "input" -> {if(!n.path("seq").isIntegralNumber()||!n.path("seq").canConvertToLong()||n.path("seq").asLong()<0)throw new IllegalArgumentException("Secuencia inválida");long seq=n.path("seq").asLong(-1);if(seq<=p.sequence)return;double x=num(n,"dx"),z=num(n,"dz");if(Math.abs(x)>1||Math.abs(z)>1)throw new IllegalArgumentException("Entrada fuera de rango");double len=Math.max(1,Math.hypot(x,z));p.sequence=seq;p.dx=x/len;p.dz=z/len;p.inputUntil=time+.25;if(x!=0||z!=0)cancelRoute(p);}
            case "move" -> {
                double x=num(n,"x"),z=num(n,"z");
                // Plan before mutating intentions: invalid/unreachable clicks preserve the current move.
                var route=pathfinder.find(p.floor,p.x,p.z,x,z);
                cancelRoute(p);p.route.addAll(route);p.goalX=x;p.goalZ=z;p.dx=p.dz=0;
            }
            case "stop" -> {p.dx=p.dz=0;cancelRoute(p);}
            case "target" -> {String target=n.path("id").asText();if(n.path("kind").asText().equals("player")){Player other=online.get(target);if(other!=null&&other!=p&&other.floor==p.floor&&other.hp>0){p.target=target;p.targetKind="player";}}else{Monster m=monsters.get(target);if(m!=null&&m.floor==p.floor&&m.hp>0){p.target=target;p.targetKind="monster";}}}
            case "pvpMode" -> {p.pvpMode=n.path("enabled").asBoolean();notify(p,p.pvpMode?"Ataques PvP activados. Un asesinato reduce tu ciudadanía.":"Ataques PvP desactivados. Fuera del refugio otros pueden atacarte.");}
            case "racial" -> racial(p);
            case "equip" -> {equip(p.id,n.path("weaponSetId").asText(),false);notify(p,"Conjunto de armas equipado.");}
            case "redeem" -> redeem(p);
            case "gather" -> gather(p,n.path("nodeId").asText());
            case "resetTraining" -> p.training.reset();
            case "castSkill" -> castSkill(p,n.path("skillId").asText());
            case "bindSkill" -> bindSkill(p,n);
            case "craft", "craftUpgrade", "craftEquip", "craftUnequip", "shopBuy", "shopBuyEquipment", "shopSell", "shopSellMaterial", "forgeUpgrade" -> craftAction(p,n);
            case "attack" -> attack(p,n.path("skill").asBoolean());
            case "potion" -> {if(time>=p.potionAt&&p.potions>0&&p.hp<p.maxHp()){p.potions--;p.hp=Math.min(p.maxHp(),p.hp+65);p.potionAt=time+2;notify(p,"Recuperaste 65 PV.");}}
            case "interact" -> interact(p);
            case "portal" -> portal(p);
            case "progression" -> {updateProgression(p,Progression.ranks(n.path("attributes")),Progression.ranks(n.path("talents")),false);notify(p,"Atributos y talentos guardados.");}
            case "profile" -> {JsonNode a=n.path("appearance");updateCharacter(p.id,n.path("name").asText(),new WorldData.Appearance(a.path("gender").asText(),a.path("face").asText(),a.path("hairColor").asText(),a.path("skinColor").asText(),a.path("classId").asText(),a.path("race").asText(),a.path("specializationId").asText()),null,null,null,false);notify(p,"Personaje actualizado y guardado.");}
            case "chat" -> {if(time<p.chatAt) return;String text=n.path("text").asText().replaceAll("[\\p{Cntrl}]","").strip();if(text.isEmpty()||text.length()>180)throw new IllegalArgumentException("Mensaje de 1–180 caracteres");p.chatAt=time+1;events.accept("*",Map.of("type","chat","name",p.name,"text",text,"floor",p.floor));}
            default -> throw new IllegalArgumentException("Comando desconocido");
        }
    }
    private void attack(Player p,boolean skill){
        if(time<(skill?p.skillAt:p.attackAt))return;
        var ability=p.profession.ability();
        if(skill&&ability.kind().equals("heal")){
            int amount=(int)Math.max(1,Math.min(250,Math.round(ability.power()+p.modifier("healing")+p.damage()*.5)));
            var nearby=online.values().stream().filter(a->a.floor==p.floor&&a.hp>0&&(a==p||a.pvp.combatUntil<=clock.getAsLong()&&p.pvp.combatUntil<=clock.getAsLong())&&a.hp<a.maxHp()&&Math.hypot(a.x-p.x,a.z-p.z)<=ability.range()).toList();
            var practice=supportDummies(p,ability.range(),ability.range());if(nearby.isEmpty()&&practice.stream().noneMatch(m->m.hp<m.maxHp)){notify(p,"No hay aliados heridos a tu alcance.");return;}
            p.skillAt=time+skillCooldown(p);for(var m:practice)healDummy(p,m,amount);for(Player a:nearby){int healed=Math.min(amount,a.maxHp()-a.hp);a.hp+=healed;events.accept("*",Map.of("type","heal","source",p.id,"target",a.id,"amount",healed,"floor",p.floor));for(Monster foe:monsters.values())if(foe.hp>0&&foe.floor==p.floor&&foe.participants.contains(a.id)&&Math.hypot(foe.x-p.x,foe.z-p.z)<=22)foe.participants.add(p.id);}return;
        }
        if(p.targetKind.equals("player")){Player victim=online.get(p.target);if(victim==null){notify(p,"El jugador ya no está en este piso.");return;}attackPlayer(p,victim,skill,skill?ability.range():p.profession.attackRange(),0);return;}
        Monster m=monsters.get(p.target);if(m==null||m.hp<=0||m.floor!=p.floor){m=monsters.values().stream().filter(a->a.floor==p.floor&&a.hp>0&&!a.support()).min(Comparator.comparingDouble(a->Math.hypot(a.x-p.x,a.z-p.z))).orElse(null);}
        if(m==null||m.support()||Math.hypot(m.x-p.x,m.z-p.z)>(skill?ability.range():p.profession.attackRange())){notify(p,"Acércate al objetivo para atacar.");return;}
        p.target=m.id;p.targetKind="monster";p.attackAt=time+.65;if(skill)p.skillAt=time+skillCooldown(p);
        p.heading=Math.atan2(m.x-p.x,m.z-p.z);int damage=(int)Math.max(1,Math.min(1000,Math.round(p.damage()*(skill?Math.max(.1,Math.min(5,ability.power()+p.modifier("skillPower"))):1))));boolean critical=java.util.concurrent.ThreadLocalRandom.current().nextDouble(100)<p.critical();if(critical)damage=(int)Math.round(damage*1.5);
        damageMonster(p,m,damage,skill,critical);
    }
    private void damageMonster(Player p,Monster m,int damage,boolean skill,boolean critical){
        if(m.support()||m.returning)return;damage=Math.max(1,damage-(int)Math.max(0,Math.round((m.practice()?m.dummy.defense():m.species.defense(m.level))+Skills.modifier(m.effects,"defense",clock.getAsLong()))));if(m.practice())damage=Math.min(m.hp,damage);m.hp=Math.max(0,m.hp-damage);m.participants.add(p.id);
        events.accept("*",Map.of("type","hit","source",p.id,"target",m.id,"damage",damage,"skill",skill,"critical",critical,"floor",p.floor));
        if(m.practice()){recordTraining(p,m,damage,0,critical,false);m.participants.clear();if(m.hp==0)m.hp=m.maxHp;return;}
        if(m.hp==0){m.respawnAt=time+(m.zone==null?m.species.respawnSeconds():m.zone.respawnSeconds());m.windupUntil=0;m.effects.clear();m.tauntedUntil=0;
            for(String who:m.participants){Player a=online.get(who);if(a==null||a.floor!=m.floor||a.hp<=0||Math.hypot(a.x-m.x,a.z-m.z)>22)continue;int previous=a.level();a.xp=Math.min(100000,a.xp+m.species.xp(m.level));a.col=Math.min(10000000,a.col+m.species.col(m.level));data.crafting.monsterDrops.getOrDefault(m.kind,Map.of()).forEach((id,amount)->a.crafting.materials.merge(id,amount,(old,add)->Math.min(100000,old+add)));if(m.kind.equals("boar")&&a.quest)a.kills++;if(m.species.unlockPortal()){a.unlocked=true;notify(a,"Guardián derrotado. El portal responde a tu cristal.");}if(a.level()>previous){a.hp=a.maxHp();notify(a,"¡Nivel "+a.level()+"! Salud restaurada. Reparte tus puntos en Talentos.");}}
            m.participants.clear();
        }
    }
    private static final class Summon {
        String id,owner,name,element;int floor;double x,z,power,range;long expires,nextAttack;
        Map<String,Object> view(){return Map.of("id",id,"owner",owner,"name",name,"element",element,"floor",floor,"x",x,"z",z);}
    }
    private final Map<String,Summon> summons=new LinkedHashMap<>();
    private void bindSkill(Player p,JsonNode n){if(!n.path("slot").isIntegralNumber()||!n.path("slot").canConvertToInt())throw new IllegalArgumentException("Ranura de habilidad inválida");int slot=n.path("slot").asInt();String id=n.path("skillId").asText();if(slot<0||slot>3||p.catalog.skills.stream().noneMatch(s->s.id().equals(id)&&Skills.learned(p,s)))throw new IllegalArgumentException("La habilidad no está aprendida");var old=p.skills.copy();p.skills.slots=new ArrayList<>(Skills.slots(p));for(int i=0;i<4;i++)if(p.skills.slots.get(i).equals(id))p.skills.slots.set(i,"");p.skills.slots.set(slot,id);try{store.save(p.profile());}catch(IOException e){p.skills=old;throw new IllegalArgumentException("No se guardó la barra de habilidades");}}
    private void recordTraining(Player p,Monster m,int damage,int healing,boolean critical,boolean buff){long now=clock.getAsLong();m.trainingLast=now;p.training.record(m.name,damage,healing,critical,buff,now,m.training.sessionResetSeconds);}
    private List<Monster> supportDummies(Player p,double range,double radius){return monsters.values().stream().filter(m->m.support()&&m.floor==p.floor&&Math.hypot(m.x-p.x,m.z-p.z)<=(radius>0?radius:range)&&(radius>0||p.targetKind.equals("monster")&&p.target.equals(m.id))).toList();}
    private void healDummy(Player p,Monster m,int amount){int healed=Math.min(amount,m.maxHp-m.hp);if(healed<=0)return;m.hp+=healed;recordTraining(p,m,0,healed,false,false);events.accept("*",Map.of("type","heal","source",p.id,"target",m.id,"amount",healed,"floor",p.floor));}
    private List<Player> supportTargets(Player p,Skills.Skill s){Player center=p;if(s.radius()==0&&p.targetKind.equals("player")){var ally=online.get(p.target);if(ally!=null&&ally.hp>0&&ally.floor==p.floor&&Math.hypot(ally.x-p.x,ally.z-p.z)<=s.range()&&ally.pvp.combatUntil<=clock.getAsLong()&&p.pvp.combatUntil<=clock.getAsLong())center=ally;}
        if(s.radius()==0)return List.of(center);return online.values().stream().filter(a->a.floor==p.floor&&a.hp>0&&Math.hypot(a.x-p.x,a.z-p.z)<=s.radius()&&(a==p||a.pvp.combatUntil<=clock.getAsLong()&&p.pvp.combatUntil<=clock.getAsLong())).toList();}
    private void castSkill(Player p,String id){
        long now=clock.getAsLong();var s=p.catalog.skills.stream().filter(v->v.id().equals(id)&&Skills.learned(p,v)).findFirst().orElseThrow(()->new IllegalArgumentException("Aprende la habilidad en tu rama antes de usarla"));if(p.skills.readyAt.getOrDefault(id,0L)>now)return;
        if(s.kind().equals("buff")||s.kind().equals("heal")){
            var practice=supportDummies(p,s.range(),s.radius());var targets=s.radius()==0&&!practice.isEmpty()?List.<Player>of():supportTargets(p,s);if(s.kind().equals("heal")&&targets.stream().noneMatch(a->a.hp<a.maxHp())&&practice.stream().noneMatch(m->m.hp<m.maxHp)){notify(p,"No hay aliados heridos al alcance.");return;}
            for(var m:practice)if(s.kind().equals("buff")){Skills.apply(m.effects,p,s,now);recordTraining(p,m,0,0,false,true);}else healDummy(p,m,(int)Math.max(1,Math.min(250,Math.round(s.power()+p.modifier("healing")+.3*p.damage()))));
            for(Player a:targets)if(s.kind().equals("buff"))Skills.apply(a.effects,p,s,now);else{int amount=(int)Math.max(1,Math.min(250,Math.round(s.power()+p.modifier("healing")+.3*p.damage())));int healed=Math.min(amount,a.maxHp()-a.hp);a.hp+=healed;events.accept("*",Map.of("type","heal","source",p.id,"target",a.id,"amount",healed,"floor",p.floor));for(Monster m:monsters.values())if(m.floor==p.floor&&m.hp>0&&m.participants.contains(a.id)&&Math.hypot(m.x-p.x,m.z-p.z)<=22)m.participants.add(p.id);}
        }else if(s.kind().equals("summon")){
            Summon pet=new Summon();pet.id="summon-"+p.id;pet.owner=p.id;pet.name=s.name();pet.element=s.element().equals("specialization")?p.appearance.specializationId():s.element();pet.floor=p.floor;pet.x=p.x+1;pet.z=p.z+1;pet.power=s.power();pet.range=s.range();pet.expires=now+(long)(s.duration()*1000);summons.put(p.id,pet);
        }else if(s.kind().equals("taunt")){
            var targets=monsters.values().stream().filter(m->!m.support()&&m.hp>0&&m.floor==p.floor&&Math.hypot(m.x-p.x,m.z-p.z)<=s.radius()).toList();if(targets.isEmpty()){notify(p,"No hay criaturas al alcance del desafío.");return;}for(var m:targets){m.tauntedBy=p.id;m.tauntedUntil=now+(long)(s.duration()*1000);m.participants.add(p.id);if(m.practice())recordTraining(p,m,0,0,false,true);}
        }else{
            int damage=s.kind().equals("curse")?1:(int)Math.max(1,Math.min(1000,Math.round(p.damage()*(s.power()+p.modifier("skillPower")))));
            if(p.targetKind.equals("player")){
                Player main=online.get(p.target);if(main==null)throw new IllegalArgumentException("Selecciona un jugador válido");if(!attackPlayer(p,main,true,s.range(),damage))return;if(s.duration()>0&&main.hp>0)Skills.apply(main.effects,p,s,now);
                if(s.radius()>0)for(Player a:new ArrayList<>(online.values()))if(a!=p&&a!=main&&a.hp>0&&a.floor==p.floor&&Math.hypot(a.x-main.x,a.z-main.z)<=s.radius()&&Math.hypot(a.x-p.x,a.z-p.z)<=s.range()+s.radius()&&!safeZone(a)&&a.pvp.spawnProtectedUntil<=now&&a.level()>=data.pvp.minLevel){attackPlayer(p,a,true,s.range()+s.radius(),damage);if(s.duration()>0&&a.hp>0)Skills.apply(a.effects,p,s,now);}
            }else{
                Monster main=monsters.get(p.target);if(main==null||main.support()||main.hp<=0||main.floor!=p.floor||Math.hypot(main.x-p.x,main.z-p.z)>s.range()){notify(p,"Selecciona una criatura al alcance de la habilidad.");return;}
                var targets=monsters.values().stream().filter(m->!m.support()&&m.hp>0&&m.floor==p.floor&&(m==main||s.radius()>0&&Math.hypot(m.x-main.x,m.z-main.z)<=s.radius())).toList();for(var m:targets){damageMonster(p,m,damage,true,false);if(s.duration()>0&&m.hp>0)Skills.apply(m.effects,p,s,now);}
            }
        }
        p.skills.readyAt.put(id,now+(long)(Math.max(1,s.cooldown()-Math.max(0,Math.min(10,p.modifier("cooldownReduction"))))*1000));
        events.accept("*",Map.of("type","skillEffect","source",p.id,"target",(s.kind().equals("buff")||s.kind().equals("heal"))&&s.radius()>0||s.kind().equals("taunt")||s.kind().equals("summon")?p.id:p.target,"element",s.element(),"radius",s.radius(),"floor",p.floor,"name",s.name()));
        try{store.save(p.profile());}catch(IOException e){throw new IllegalArgumentException("Habilidad aplicada; no se pudo guardar la recarga");}
    }
    private void tickSkills(double dt){
        long now=clock.getAsLong();for(Player p:new ArrayList<>(online.values())){p.effects.removeIf(e->e.expires<=now);p.hp=Math.min(p.hp,p.maxHp());for(var e:new ArrayList<>(p.effects))if(p.hp>0&&e.damage>0&&e.nextTick<=now){e.nextTick=now+1000;Player source=online.get(e.source);if(source!=null&&source.hp>0&&source.floor==p.floor&&!safeZone(source)&&!safeZone(p)&&p.pvp.spawnProtectedUntil<=now&&source.pvp.spawnProtectedUntil<=now&&p.level()>=data.pvp.minLevel&&source.level()>=data.pvp.minLevel&&data.pvp.enabled){boolean mode=source.pvpMode;try{source.pvpMode=true;attackPlayer(source,p,true,10000,(int)Math.max(1,Math.round(e.damage)),true);}finally{source.pvpMode=mode;}}}}
        for(Monster m:monsters.values()){m.effects.removeIf(e->e.expires<=now);for(var e:new ArrayList<>(m.effects))if(m.hp>0&&e.damage>0&&e.nextTick<=now){e.nextTick=now+1000;Player p=online.get(e.source);if(p!=null&&p.hp>0&&p.floor==m.floor)damageMonster(p,m,(int)Math.max(1,Math.round(e.damage)),true,false);}}
        for(var it=summons.values().iterator();it.hasNext();){var pet=it.next();Player p=online.get(pet.owner);if(p==null||!p.online||p.hp<=0||p.floor!=pet.floor||pet.expires<=now){it.remove();continue;}pet.x+=(p.x+1-pet.x)*Math.min(1,dt*5);pet.z+=(p.z+1-pet.z)*Math.min(1,dt*5);Monster target=p.targetKind.equals("monster")?monsters.get(p.target):null;if(now>=pet.nextAttack&&target!=null&&target.hp>0&&target.floor==p.floor&&Math.hypot(target.x-p.x,target.z-p.z)<=pet.range){pet.nextAttack=now+1500;damageMonster(p,target,(int)Math.max(1,Math.min(300,Math.round(pet.power+p.damage()*.2))),true,false);}}
    }

    private List<Map<String,Object>> resourceViews(int floor){long now=clock.getAsLong();return data.floor(floor).resources.stream().map(n->Map.<String,Object>of("id",n.id(),"typeId",n.typeId(),"x",n.x(),"z",n.z(),"readyInSeconds",Math.max(0,(gatheringStore.ready.getOrDefault(n.id(),0L)-now)/1000.0),"available",gatheringStore.ready.getOrDefault(n.id(),0L)<=now)).toList();}
    private void applyHarvest(Player p,Gathering.Receipt receipt){p.crafting.materials.merge(receipt.materialId(),receipt.amount(),(old,add)->Math.min(100000,old+add));Crafting.gain(p,data.crafting,receipt.professionId(),receipt.experience());p.crafting.lastGatherEvent=receipt.sequence();p.crafting.gatherReadyAt=receipt.playerReadyAt();}
    private void gather(Player p,String nodeId){
        long now=clock.getAsLong();if(!data.gathering.enabled||p.pvp.combatUntil>now||p.crafting.gatherReadyAt>now)throw new IllegalArgumentException("Recolección desactivada, en recarga o en combate PvP");
        var node=data.floor(p.floor).resources.stream().filter(n->n.id().equals(nodeId)).findFirst().orElseThrow(()->new IllegalArgumentException("Recurso inexistente en este piso"));var resource=Gathering.resource(data.gathering,node.typeId());
        if(Math.hypot(node.x()-p.x,node.z()-p.z)>data.gathering.range)throw new IllegalArgumentException("Acércate al recurso para recolectar");if(gatheringStore.ready.getOrDefault(node.id(),0L)>now)throw new IllegalArgumentException("Recurso agotado; espera su regeneración");if(Crafting.level(data.crafting,p.crafting,resource.professionId())<resource.minProfessionLevel())throw new IllegalArgumentException("Sube el oficio para recolectar este recurso");if(p.crafting.materials.getOrDefault(resource.materialId(),0)+resource.amount()>100000)throw new IllegalArgumentException("Almacén de material lleno");
        Gathering.Receipt receipt;try{receipt=gatheringStore.commit(p.id,node,resource,now,data.gathering);}catch(IOException e){throw new IllegalArgumentException("No se guardó la recolección; el recurso permanece disponible");}applyHarvest(p,receipt);
        try{store.save(p.profile());}catch(IOException e){System.err.println("Perfil de recolección pendiente; el recibo conserva la recompensa: "+e.getMessage());}
        notify(p,"Recolectaste "+resource.amount()+" de "+data.crafting.materials.stream().filter(m->m.id().equals(resource.materialId())).findFirst().orElseThrow().name()+". +"+resource.experience()+" EXP de oficio.");events.accept("*",Map.of("type","harvest","source",p.id,"nodeId",node.id(),"floor",p.floor));
    }

    private void station(Player p,String role){if(p.pvp.combatUntil>clock.getAsLong()||!p.online||p.hp<=0||data.floor(p.floor).npcs.stream().noneMatch(n->n.role().equals(role)&&Math.hypot(n.x()-p.x,n.z()-p.z)<=data.crafting.stationRange))throw new IllegalArgumentException("Acércate al encargado del taller o tienda, fuera de combate");}
    private void recipeStation(Player p,Crafting.Recipe recipe){var job=data.crafting.professions.stream().filter(j->j.id().equals(recipe.professionId())).findFirst().orElseThrow();station(p,job.stationRole());if(Crafting.level(data.crafting,p.crafting,job.id())<recipe.minProfessionLevel())throw new IllegalArgumentException("Nivel de oficio insuficiente");}
    private Crafting.Item crafted(Player p,String id){var item=p.crafting.items.get(id);if(item==null)throw new IllegalArgumentException("El objeto no pertenece a tu inventario");return item;}
    private void craftAction(Player p,JsonNode n){
        if(!data.crafting.enabled)throw new IllegalArgumentException("Los talleres y la tienda están desactivados por root");
        var old=p.profile();var rules=data.crafting;String type=n.path("type").asText();
        try{
            switch(type){
                case "craft" -> {var recipe=Crafting.recipe(rules,n.path("recipeId").asText());recipeStation(p,recipe);if(p.crafting.items.size()>=rules.maxInventory)throw new IllegalArgumentException("Inventario artesanal lleno");Crafting.pay(p,recipe.colCost(),recipe.materials(),1);var item=Crafting.make(recipe,p.catalog);item.investedCol=recipe.colCost();p.crafting.items.put(item.id,item);Crafting.gain(p,rules,recipe.professionId(),recipe.experience());}
                case "craftUpgrade" -> {var item=crafted(p,n.path("itemId").asText());var recipe=Crafting.recipe(rules,item.recipeId);recipeStation(p,recipe);if(item.upgrade>=recipe.maxUpgrade())throw new IllegalArgumentException("Mejora máxima alcanzada");if(Crafting.level(rules,p.crafting,recipe.professionId())<Math.min(rules.maxProfessionLevel,recipe.minProfessionLevel()+item.upgrade))throw new IllegalArgumentException("Sube tu oficio para mejorar este objeto");int factor=item.upgrade+1;Crafting.pay(p,recipe.upgradeColCost(),recipe.upgradeMaterials(),factor);item=item.copy();item.upgrade++;item.investedCol+=recipe.upgradeColCost()*factor;if(item.weapon!=null)item.weapon=Crafting.add(item.weapon,recipe.upgradeAttributes(),p.catalog);if(item.armor!=null)item.armor=Crafting.add(item.armor,recipe.upgradeAttributes(),p.catalog);p.crafting.items.put(item.id,item);Crafting.gain(p,rules,recipe.professionId(),rules.upgradeExperience);}
                case "craftEquip" -> {safeToEdit(p);var item=crafted(p,n.path("itemId").asText());String slot=n.path("slot").asText();if(!Crafting.compatible(item,p))throw new IllegalArgumentException("El objeto requiere otra clase, género o nivel");
                    if(item.armor!=null){if(!slot.equals("armor"))throw new IllegalArgumentException("Esta prenda ocupa la ranura de armadura");}
                    else{if(!Set.of("mainHand","offHand").contains(slot)||slot.equals("mainHand")&&Set.of("shield","orb","tome").contains(item.weapon.kind())||slot.equals("offHand")&&(item.weapon.hands()==2||p.weapons().mainHand().hands()==2))throw new IllegalArgumentException("Ranura incompatible o principal de dos manos");}
                    p.crafting.equipped.values().removeIf(id->id.equals(item.id));p.crafting.equipped.put(slot,item.id);if(item.weapon!=null&&item.weapon.hands()==2)p.crafting.equipped.remove("offHand");}
                case "craftUnequip" -> {safeToEdit(p);String slot=n.path("slot").asText();if(!Set.of("mainHand","offHand","armor").contains(slot))throw new IllegalArgumentException("Ranura inválida");p.crafting.equipped.remove(slot);}
                case "shopBuy" -> {station(p,"merchant");if(!n.path("quantity").isIntegralNumber()||!n.path("quantity").canConvertToInt())throw new IllegalArgumentException("Cantidad entera requerida");int quantity=n.path("quantity").asInt();if(quantity<1||quantity>50)throw new IllegalArgumentException("Compra entre 1 y 50 unidades");String id=n.path("materialId").asText();boolean potion=id.equals("potion");int price=potion?rules.potionPrice:rules.materials.stream().filter(m->m.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Material inexistente")).price();long total=(long)price*quantity;if(p.col<total)throw new IllegalArgumentException("Col insuficiente");if(potion&&p.potions+quantity>999||!potion&&p.crafting.materials.getOrDefault(id,0)+quantity>100000)throw new IllegalArgumentException("Capacidad excedida");p.col-=(int)total;if(potion)p.potions+=quantity;else p.crafting.materials.merge(id,quantity,Integer::sum);}
                case "shopSell" -> {station(p,"merchant");var item=crafted(p,n.path("itemId").asText());if(p.crafting.equipped.containsValue(item.id))throw new IllegalArgumentException("Desequipa el objeto antes de venderlo");int price=(int)Math.floor(item.investedCol*rules.saleMultiplier);if((long)p.col+price>10000000)throw new IllegalArgumentException("Límite de col alcanzado");p.col+=price;p.crafting.items.remove(item.id);}
                case "shopBuyEquipment" -> {
                    station(p,"merchant");var product=rules.shopProducts.stream().filter(v->v.id().equals(n.path("productId").asText())&&v.enabled()).findFirst().orElseThrow(()->new IllegalArgumentException("Producto no disponible"));
                    if(p.crafting.items.size()>=rules.maxInventory)throw new IllegalArgumentException("Bolsa de equipo llena");if(p.col<product.price())throw new IllegalArgumentException("Col insuficiente");
                    var item=Crafting.buyProduct(rules,p.catalog,product);if(!Crafting.compatible(item,p))throw new IllegalArgumentException("El producto requiere otra clase, especialidad, género o nivel");
                    p.col-=product.price();p.crafting.items.put(item.id,item);
                }
                case "shopSellMaterial" -> {
                    station(p,"merchant");
                    if(!n.path("quantity").isIntegralNumber()||!n.path("quantity").canConvertToInt())throw new IllegalArgumentException("Cantidad entera requerida");
                    int quantity=n.path("quantity").asInt();if(quantity<1||quantity>100000)throw new IllegalArgumentException("Vende entre 1 y 100000 unidades");
                    String id=n.path("materialId").asText();var material=rules.materials.stream().filter(m->m.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Material inexistente"));
                    int owned=p.crafting.materials.getOrDefault(id,0),price=Crafting.materialSalePrice(rules,material);
                    if(price<=0)throw new IllegalArgumentException("El mercado no compra este material");if(quantity>owned)throw new IllegalArgumentException("No tienes suficientes unidades en la bolsa");
                    long total=(long)price*quantity;if((long)p.col+total>10000000)throw new IllegalArgumentException("Límite de col alcanzado");
                    p.crafting.materials.put(id,owned-quantity);p.col+=(int)total;
                }
                case "forgeUpgrade" -> {station(p,"smith");if(p.weapon>=5||p.col<50)throw new IllegalArgumentException("Mejora general: necesitas 50 col y un rango menor de +5");p.col-=50;p.weapon++;}
                default -> throw new IllegalArgumentException("Acción de oficio desconocida");
            }
            p.hp=Math.min(p.hp,p.maxHp());store.save(p.profile());notify(p,switch(type){case "craft"->"Objeto creado y experiencia de oficio obtenida.";case "craftUpgrade"->"Mejora artesanal guardada.";case "shopBuy"->"Compra realizada.";case "shopBuyEquipment"->"Equipo comprado y guardado en tu bolsa.";case "shopSell"->"Objeto vendido.";case "shopSellMaterial"->"Materiales vendidos. Bolsa y monedas actualizadas.";default->"Equipo actualizado.";});
        }catch(IOException|IllegalArgumentException e){p.crafting=old.crafting();p.col=old.col();p.potions=old.potions();p.weapon=old.weapon();p.hp=old.hp();if(e instanceof IllegalArgumentException bad)throw bad;throw new IllegalArgumentException("No se guardó la operación del taller o tienda; no se cobró");}
    }
    public synchronized Map<String,Object> craftingProgress(String id,JsonNode input){
        Player p=character(id);Map<String,Integer> xp=craftValues(input.path("professionXp"),data.crafting.professions.stream().map(Crafting.Profession::id).toList(),data.crafting.xpPerLevel*(data.crafting.maxProfessionLevel-1));Map<String,Integer> materials=craftValues(input.path("materials"),data.crafting.materials.stream().map(Crafting.Material::id).toList(),100000);
        var old=p.profile();try{store.backup(old);p.crafting=p.crafting.copy();p.crafting.professionXp=xp;p.crafting.materials=materials;store.save(p.profile());return p.view();}catch(IOException e){p.crafting=old.crafting();throw new IllegalArgumentException("No se pudo guardar el progreso de oficios");}
    }
    private static Map<String,Integer> craftValues(JsonNode node,List<String> ids,int max){if(!node.isObject()||node.size()>ids.size())throw new IllegalArgumentException("Valores de oficio inválidos");Map<String,Integer> values=new LinkedHashMap<>();var fields=node.fields();while(fields.hasNext()){var e=fields.next();if(!ids.contains(e.getKey())||!e.getValue().isIntegralNumber()||!e.getValue().canConvertToInt()||e.getValue().intValue()<0||e.getValue().intValue()>max)throw new IllegalArgumentException("Material o experiencia de oficio fuera de rango");values.put(e.getKey(),e.getValue().intValue());}return values;}

    private boolean safeZone(Player p){var f=data.floor(p.floor);return Training.inside(f.training,p.x,p.z)||Math.hypot(p.x-f.spawn.x(),p.z-f.spawn.z())<=data.pvp.safeRadius||Math.hypot(p.x-f.portal.x(),p.z-f.portal.z())<=data.pvp.portalSafeRadius;}
    private int absorb(Player p,int damage){if(time>=p.racialUntil)return damage;int shield=(int)Math.min(damage,p.racialShield);p.racialShield-=shield;return damage-shield;}
    private boolean attackPlayer(Player p,Player victim,boolean skill,double range,int racialDamage){return attackPlayer(p,victim,skill,range,racialDamage,false);}
    private boolean attackPlayer(Player p,Player victim,boolean skill,double range,int racialDamage,boolean ongoing){
        long now=clock.getAsLong();var r=data.pvp;
        if(!r.enabled||!p.pvpMode)throw new IllegalArgumentException("Activa los ataques PvP para atacar a otro jugador; root debe permitir PvP");
        if(p==victim||victim.hp<=0||victim.floor!=p.floor||p.level()<r.minLevel||victim.level()<r.minLevel)throw new IllegalArgumentException("Objetivo PvP no válido o nivel insuficiente");
        if(safeZone(p)||safeZone(victim)||p.pvp.spawnProtectedUntil>now||victim.pvp.spawnProtectedUntil>now)throw new IllegalArgumentException("El refugio, el patio de entrenamiento, los portales y la protección de aparición impiden este ataque");
        if(Math.hypot(victim.x-p.x,victim.z-p.z)>range){notify(p,"Acércate al jugador seleccionado.");return false;}
        boolean justified=Pvp.justified(p.pvp,victim.id,victim.pvp,r,now);
        int raw=racialDamage>0?racialDamage:(int)Math.round(p.damage()*(skill?Math.max(.1,Math.min(5,p.profession.ability().power()+p.modifier("skillPower"))):1));
        boolean critical=java.util.concurrent.ThreadLocalRandom.current().nextDouble(100)<p.critical();
        int damage=Math.max(1,(int)Math.round(Math.min(1000,raw)*(critical?1.5:1)*r.damageMultiplier)-victim.defense());
        int shield=time<victim.racialUntil?(int)Math.min(damage,victim.racialShield):0;damage-=shield;
        var nextAttacker=p.pvp.copy();var nextVictim=victim.pvp.copy();
        nextAttacker.combatUntil=nextVictim.combatUntil=now+r.combatSeconds*1000L;
        if(!justified){nextAttacker.aggressorUntil=now+r.aggressionSeconds*1000L;nextVictim.defendAgainst.put(p.id,now+r.aggressionSeconds*1000L);}
        boolean killed=damage>=victim.hp;
        if(killed){
            nextAttacker.kills=Math.min(Integer.MAX_VALUE-1,nextAttacker.kills)+1;nextVictim.deaths=Math.min(Integer.MAX_VALUE-1,nextVictim.deaths)+1;nextVictim.combatUntil=0;
            if(!justified){nextAttacker.murders=Math.min(Integer.MAX_VALUE-1,nextAttacker.murders)+1;nextAttacker.citizenship=Math.max(r.minCitizenship,nextAttacker.citizenship-r.murderPenalty);nextAttacker.murdererUntil=Math.max(nextAttacker.murdererUntil,now+r.murdererSeconds*1000L);}
            try{var receipt=journal.commit("kill",p.id,victim.id,!justified,now,Map.of(p.id,nextAttacker,victim.id,nextVictim));nextAttacker=receipt.states().get(p.id);nextVictim=receipt.states().get(victim.id);}catch(IOException e){throw new IllegalArgumentException("No se pudo registrar la baja PvP; el golpe no se aplicó");}
        }
        p.pvp=nextAttacker;victim.pvp=nextVictim;victim.racialShield=Math.max(0,victim.racialShield-shield);victim.hp=Math.max(0,victim.hp-damage);
        p.heading=Math.atan2(victim.x-p.x,victim.z-p.z);if(!ongoing)p.attackAt=time+.65;if(skill&&racialDamage==0)p.skillAt=time+skillCooldown(p);
        if(killed){victim.deadUntil=time+3;victim.dx=victim.dz=0;cancelRoute(victim);notify(victim,"Caíste en combate PvP. Regresarás al refugio conservando tu progreso.");notify(p,justified?"Baja en defensa propia: sin penalización de ciudadanía.":"Asesinato: −"+r.murderPenalty+" ciudadanía. Ahora estás marcado como asesino.");}
        try{store.save(p.profile());store.save(victim.profile());}catch(IOException e){System.err.println("Guardado PvP pendiente; la baja conserva recibo: "+e.getMessage());}
        events.accept("*",Map.of("type","hit","source",p.id,"target",victim.id,"damage",damage,"skill",skill,"critical",critical,"floor",p.floor));return true;
    }
    private void racial(Player p){
        long now=clock.getAsLong();if(now<p.pvp.racialReadyAt)return;var a=p.race().ability;
        if(a.kind().equals("heal")&&p.hp>=p.maxHp()){notify(p,"Ya tienes todos tus PV.");return;}
        if(a.kind().equals("flame")){
            int damage=(int)Math.max(1,Math.round(a.value()+p.damage()*.5));
            if(p.targetKind.equals("player")){var victim=online.get(p.target);if(victim==null||!attackPlayer(p,victim,true,a.range(),damage))return;}
            else{var m=monsters.get(p.target);if(m==null||m.hp<=0||m.floor!=p.floor||Math.hypot(p.x-m.x,p.z-m.z)>a.range()){notify(p,"Selecciona un enemigo al alcance del aliento.");return;}damageMonster(p,m,damage,true,false);}
        }
        p.pvp.racialReadyAt=now+(long)(a.cooldown()*1000);p.racialUntil=time+a.duration();p.racialEffect=a.kind();p.racialBonus=a.value();
        if(a.kind().equals("heal")){int healed=Math.min(p.maxHp()-p.hp,(int)Math.round(a.value()));p.hp+=healed;events.accept("*",Map.of("type","heal","source",p.id,"target",p.id,"amount",healed,"floor",p.floor));}
        if(a.kind().equals("shield"))p.racialShield=a.value();
        try{store.save(p.profile());}catch(IOException e){throw new IllegalArgumentException("Habilidad aplicada, pero no se pudo guardar su recarga");}
        notify(p,a.name()+" activado.");
    }
    public synchronized Map<String,Object> equip(String id,String selected,boolean admin){
        Player p=character(id);if(!admin)safeToEdit(p);String chosen=Objects.toString(selected,"");
        if(!chosen.isEmpty()&&p.catalog.weaponSets.stream().noneMatch(s->s.id().equals(chosen)&&Specializations.weapons(p.specialty(),s)&&s.minLevel()<=p.level()))throw new IllegalArgumentException("Conjunto de armas no permitido para esta clase o nivel");
        var old=p.profile();try{if(admin)store.backup(old);p.weaponSetId=chosen;p.crafting.equipped.remove("mainHand");p.crafting.equipped.remove("offHand");p.hp=Math.min(p.hp,p.maxHp());store.save(p.profile());return p.view();}catch(IOException e){p.crafting=old.crafting();p.weaponSetId=old.weaponSetId();p.hp=old.hp();throw new IllegalArgumentException("No se pudo guardar el equipamiento");}
    }
    private Player character(String id){return profiles.values().stream().filter(p->p.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Personaje inexistente"));}
    public synchronized Map<String,Object> citizenship(String id,int value,boolean pardon){
        Player p=character(id);if(value<data.pvp.minCitizenship||value>data.pvp.maxCitizenship||pardon&&value<=data.pvp.murdererThreshold)throw new IllegalArgumentException("Ciudadanía fuera de rango; el indulto necesita superar el umbral de asesino");
        var previous=p.profile();try{store.backup(previous);p.pvp=p.pvp.copy();p.pvp.citizenship=value;if(pardon){p.pvp.murdererUntil=0;p.pvp.aggressorUntil=0;p.pvp.defendAgainst.clear();}store.save(p.profile());return p.view();}catch(IOException e){p.pvp=previous.pvp();throw new IllegalArgumentException("No se pudo guardar la ciudadanía");}
    }
    private void redeem(Player p){
        var r=data.pvp;long now=clock.getAsLong();
        if(p.pvp.combatUntil>now||p.pvp.redemptionAt>now||p.pvp.citizenship>=r.maxCitizenship||p.col<r.redemptionCost||data.floor(p.floor).npcs.stream().noneMatch(n->n.role().equals("guide")&&Math.hypot(p.x-n.x(),p.z-n.z())<4))throw new IllegalArgumentException("Redención: visita al guía fuera de combate, reúne "+r.redemptionCost+" col y espera la recarga");
        var old=p.profile();p.col-=r.redemptionCost;p.pvp.citizenship=Math.min(r.maxCitizenship,p.pvp.citizenship+r.redemptionPoints);p.pvp.redemptionAt=now+r.redemptionCooldownSeconds*1000L;
        try{store.save(p.profile());}catch(IOException e){p.pvp=old.pvp();p.col=old.col();throw new IllegalArgumentException("No se pudo guardar la redención");}notify(p,"Ciudadanía recuperada: +"+r.redemptionPoints+". La marca temporal cumple su duración.");
    }
    private void interact(Player p){
        WorldData.Npc n=data.floor(p.floor).npcs.stream().filter(a->Math.hypot(a.x()-p.x,a.z()-p.z)<4).min(Comparator.comparingDouble(a->Math.hypot(a.x()-p.x,a.z()-p.z))).orElse(null);
        if(n==null){notify(p,"Acércate a Lyra o a Brann (E).");return;}
        String text;
        if(n.role().equals("smith"))text="Bienvenido a la herrería. Abre el taller para forjar y mejorar equipo, o elige la mejora general por 50 col (+4 daño).";
        else if(n.role().equals("tailor"))text="Bienvenido a mi sastrería. Con fibra y cuero podemos crear y mejorar ropa. Abre el taller para elegir tu receta.";
        else if(n.role().equals("merchant"))text="Tengo materiales y pociones. También compro objetos artesanales. Abre la tienda para consultar precios.";
        else if(!p.quest){p.quest=true;text="Bienvenido. Derrota tres jabalíes y vuelve conmigo. Después, busca al Centinela junto al portal norte.";}
        else if(p.kills>=3&&!p.reward){p.reward=true;p.col=Math.min(10000000,p.col+60);p.xp=Math.min(100000,p.xp+40);p.potions+=2;text="Bien hecho. Recibe 60 col, 40 EXP y dos pociones. El Centinela anuncia su golpe: aléjate del círculo naranja.";}
        else if(p.reward)text="El portal norte lleva al Bosque de cristal. Derrota al Centinela para activarlo. Puedes cooperar con otros jugadores.";
        else text="Llevas "+Math.min(3,p.kills)+" de 3 jabalíes. Acércate, selecciona al enemigo y pulsa Espacio. Q activa la habilidad de tu clase.";
        events.accept(p.id,Map.of("type","dialogue","name",n.name(),"role",n.role(),"npcId",n.id(),"text",text));
    }
    private void portal(Player p){if(p.pvp.combatUntil>clock.getAsLong())throw new IllegalArgumentException("No puedes usar portales durante combate PvP");var f=data.floor(p.floor);if(time<p.portalAt)return;if(Math.hypot(p.x-f.portal.x(),p.z-f.portal.z())>4){notify(p,"Acércate al cristal del portal (norte del primer piso).");return;}if(f.portal.requiresBoss()&&!p.unlocked){notify(p,"Derrota al Centinela para abrir este portal.");return;}p.floor=f.portal.to();spawn(p);p.portalAt=time+2;notify(p,"Llegaste a "+data.floor(p.floor).name);}
    private void notify(Player p,String text){events.accept(p.id,Map.of("type","notice","text",text));}
    public synchronized void tick(double dt){
        dt=Math.max(0,Math.min(.1,dt));time+=dt;ticks++;
        for(var it=online.values().iterator();it.hasNext();){Player p=it.next();
            if(!p.online&&(p.hp<=0||p.pvp.combatUntil<=clock.getAsLong())){try{store.save(p.profile());it.remove();}catch(IOException e){System.err.println("No se pudo guardar personaje desconectado: "+e.getMessage());}continue;}
            if(time>=p.racialUntil){p.racialEffect="";p.racialBonus=0;p.racialShield=0;}
            p.pvp.defendAgainst.entrySet().removeIf(e->e.getValue()<=clock.getAsLong());
            if(p.hp<=0){if(time>=p.deadUntil)spawn(p);continue;}
            if(p.goalX!=null)followRoute(p,p.speed()*dt);
            else{
                // Preserve the existing WASD normalization, expiry and separate-axis sliding.
                double dx=time<p.inputUntil?p.dx:0,dz=time<p.inputUntil?p.dz:0;
                if(dx!=0||dz!=0){double step=p.speed()*dt,x=p.x+dx*step,z=p.z+dz*step;
                    if(data.walkable(p.floor,x,p.z))p.x=x;if(data.walkable(p.floor,p.x,z))p.z=z;p.heading=Math.atan2(dx,dz);}
            }
        }
        tickSkills(dt);
        for(Monster m:monsters.values()){
            if(m.practice()){if(m.trainingLast>0&&clock.getAsLong()-m.trainingLast>=m.training.recoverySeconds*1000L){m.hp=m.support()?m.maxHp/2:m.maxHp;m.effects.clear();m.tauntedUntil=0;m.trainingLast=0;}continue;}
            if(m.hp<=0){if(time>=m.respawnAt){m.hp=m.maxHp;m.x=m.homeX;m.z=m.homeZ;m.returning=false;m.participants.clear();}else continue;}
            if(m.returning){returnHome(m,dt);continue;}
            Player p=online.values().stream().filter(a->canChase(m,a)&&(Math.hypot(a.x-m.x,a.z-m.z)<=m.species.aggroRange()||m.participants.contains(a.id))).min(Comparator.comparingDouble(a->Math.hypot(a.x-m.x,a.z-m.z))).orElse(null);
            Player taunt=online.get(m.tauntedBy);if(m.tauntedUntil>clock.getAsLong()&&taunt!=null&&canChase(m,taunt))p=taunt;
            if(m.windupUntil>0){if(time>=m.windupUntil){Player victim=online.get(m.victim);if(victim!=null&&canChase(m,victim)&&Math.hypot(victim.x-m.x,victim.z-m.z)<m.species.attackRange()+1.7)hurt(victim,m,m.species.damage(m.level));m.windupUntil=0;m.attackAt=time+m.species.attackCooldown();}continue;}
            if(p==null){if(m.zone!=null&&(m.hp<m.maxHp||Math.hypot(m.x-m.homeX,m.z-m.homeZ)>.2)){m.returning=true;m.evadeAt=time;m.effects.clear();m.tauntedUntil=0;m.participants.clear();}returnHome(m,dt);continue;}
            double dx=p.x-m.x,dz=p.z-m.z,d=Math.hypot(dx,dz);m.heading=Math.atan2(dx,dz);
            if(d>m.species.attackRange())moveMonster(m,dx/d,dz/d,dt);else if(time>=m.attackAt){if(m.species.windupSeconds()>0){m.windupUntil=time+m.species.windupSeconds();m.victim=p.id;}else{hurt(p,m,m.species.damage(m.level));m.attackAt=time+m.species.attackCooldown();}}
        }
    }
    private void followRoute(Player p,double remaining){
        while(!p.route.isEmpty()){
            var next=p.route.peekFirst();double dx=next.x()-p.x,dz=next.z()-p.z,distance=Math.hypot(dx,dz);
            if(distance==0){p.route.removeFirst();continue;}
            if(remaining<=0)break;
            // Recheck against the current authoritative world; never slide away from a route segment.
            if(!data.walkableSegment(p.floor,p.x,p.z,next.x(),next.z())){
                cancelRoute(p);notify(p,"La ruta dejó de estar disponible.");return;
            }
            double step=Math.min(remaining,distance);p.heading=Math.atan2(dx,dz);
            if(step==distance){p.x=next.x();p.z=next.z();p.route.removeFirst();}
            else{p.x+=dx/distance*step;p.z+=dz/distance*step;}
            remaining-=step;
        }
        if(p.route.isEmpty())cancelRoute(p);
    }
    private boolean canChase(Monster m,Player p){return p.hp>0&&p.floor==m.floor&&!Training.inside(data.floor(p.floor).training,p.x,p.z)&&Bestiary.inside(m.zone,p.x,p.z)&&Math.hypot(p.x-m.homeX,p.z-m.homeZ)<m.species.leashRange();}
    private void returnHome(Monster m,double dt){if(m.returning&&time-m.evadeAt>=8){m.x=m.homeX;m.z=m.homeZ;}double d=Math.hypot(m.x-m.homeX,m.z-m.homeZ);if(d>.1)moveMonster(m,(m.homeX-m.x)/d,(m.homeZ-m.z)/d,dt);if(d<.4){if(m.zone!=null){m.hp=m.maxHp;m.returning=false;m.effects.clear();m.participants.clear();}else m.hp=Math.min(m.maxHp,m.hp+1);}}
    private void moveMonster(Monster m,double dx,double dz,double dt){double speed=Math.max(.3,Math.min(6,m.species.speed()+Skills.modifier(m.effects,"speed",clock.getAsLong())));double x=m.x+dx*speed*dt,z=m.z+dz*speed*dt;if(monsterWalkable(m,x,m.z))m.x=x;if(monsterWalkable(m,m.x,z))m.z=z;}
    private boolean monsterWalkable(Monster m,double x,double z){return Bestiary.inside(m.zone,x,z)&&!Training.inside(data.floor(m.floor).training,x,z)&&data.walkable(m.floor,x,z);}
    private void hurt(Player p,Monster m,int damage){if(Training.inside(data.floor(p.floor).training,p.x,p.z))return;damage=absorb(p,Math.max(1,damage+(int)Math.round(Skills.modifier(m.effects,"damage",clock.getAsLong()))-p.defense()));p.hp=Math.max(0,p.hp-damage);events.accept("*",Map.of("type","hit","source",m.id,"target",p.id,"damage",damage,"skill",false,"floor",p.floor));if(p.hp==0){p.deadUntil=time+3;p.dx=p.dz=0;cancelRoute(p);notify(p,"Has caído. Regresarás al refugio en tres segundos; conservas tu progreso.");}}
    public synchronized Map<String,Object> snapshot(String id){Player p=online.get(id);if(p==null)return Map.of();return Map.of("type","state","tick",ticks,"time",time,"you",id,"floor",p.floor,"players",online.values().stream().filter(a->a.floor==p.floor).map(a->{var v=a.view();if(!a.id.equals(id)){v.remove("crafting");v.remove("training");}return v;}).toList(),"summons",summons.values().stream().filter(s->s.floor==p.floor).map(Summon::view).toList(),"resources",resourceViews(p.floor),"monsters",monsters.values().stream().filter(m->m.floor==p.floor&&m.hp>0).map(Monster::view).toList(),"cooldowns",Map.of("attack",Math.max(0,p.attackAt-time),"skill",Math.max(0,p.skillAt-time),"potion",Math.max(0,p.potionAt-time),"racial",Math.max(0,(p.pvp.racialReadyAt-clock.getAsLong())/1000.0),"gather",Math.max(0,(p.crafting.gatherReadyAt-clock.getAsLong())/1000.0)));}
    public synchronized List<Map<String,Object>> characters(){return profiles.values().stream().map(p->{Map<String,Object> v=p.view();v.put("online",p.online);v.put("accountId",p.accountId);return v;}).toList();}
    private double skillCooldown(Player p){return Math.max(1,p.profession.ability().cooldown()-Math.max(0,Math.min(10,p.modifier("cooldownReduction"))));}
    private void safeToEdit(Player p){var start=data.floor(p.floor).spawn;if(!p.online||p.hp<=0||p.pvp.combatUntil>clock.getAsLong()||Math.hypot(p.x-start.x(),p.z-start.z())>6||monsters.values().stream().anyMatch(m->!m.practice()&&m.floor==p.floor&&m.hp>0&&Math.hypot(m.x-p.x,m.z-p.z)<8))throw new IllegalArgumentException("Edita tu personaje en el refugio, lejos de enemigos");}
    private void reconcile(Player p){p.crafting.equipped.entrySet().removeIf(e->!Crafting.compatible(p.crafting.items.get(e.getValue()),p));if(!p.weaponSetId.isEmpty()&&!Weapons.resolve(p.catalog,p.appearance.classId(),p.appearance.specializationId(),p.level(),p.weaponSetId).id().equals(p.weaponSetId))p.weaponSetId="";try{Specializations.build(p.catalog,p.appearance.specializationId(),p.talentRanks);Progression.validate(p.catalog.progression,p.appearance.classId(),p.xp,p.attributeRanks,p.talentRanks);}catch(IllegalArgumentException incompatible){p.attributeRanks.clear();p.talentRanks.clear();}}
    private void updateProgression(Player p,Map<String,Integer> attributes,Map<String,Integer> talents,boolean admin){
        if(!admin)safeToEdit(p);Specializations.build(p.catalog,p.appearance.specializationId(),talents);var rules=p.catalog.progression;var build=Progression.validate(rules,p.appearance.classId(),p.xp,attributes,talents);
        if(!admin&&!rules.allowRespec&&(Progression.lowers(p.attributeRanks,attributes)||Progression.lowers(p.talentRanks,talents)))throw new IllegalArgumentException("La redistribución está desactivada por root");
        var old=p.profile();try{if(admin)store.backup(old);p.attributeRanks=new HashMap<>(build.attributes());p.talentRanks=new HashMap<>(build.talents());p.hp=Math.min(p.hp,p.maxHp());store.save(p.profile());}catch(IOException e){p.attributeRanks=new HashMap<>(old.attributeRanks());p.talentRanks=new HashMap<>(old.talentRanks());p.hp=old.hp();throw new IllegalArgumentException("No se guardó la distribución de puntos");}
    }
    public synchronized Map<String,Object> updateCharacter(String id,String name,WorldData.Appearance appearance,Integer xp,Integer col,Integer potions,boolean admin){return updateCharacter(id,name,appearance,xp,col,potions,admin,null);}
    public synchronized Map<String,Object> updateCharacter(String id,String name,WorldData.Appearance appearance,Integer xp,Integer col,Integer potions,boolean admin,Progression.Build build){
        Player p=profiles.values().stream().filter(a->a.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Personaje inexistente"));
        if(name==null||!name.strip().matches("[\\p{L}\\p{N} _-]{2,20}"))throw new IllegalArgumentException("Nombre de 2–20 letras o números");
        appearance=data.validateAppearance(appearance);
        if(!admin){safeToEdit(p);xp=null;col=null;potions=null;build=null;if(!p.catalog.progression.allowRespec&&(!p.appearance.classId().equals(appearance.classId())||!p.appearance.specializationId().equals(appearance.specializationId())))throw new IllegalArgumentException("El cambio de clase está desactivado junto a la redistribución");}
        if(xp!=null&&(xp<0||xp>100000)||col!=null&&(col<0||col>10000000)||potions!=null&&(potions<0||potions>999))throw new IllegalArgumentException("Progreso fuera de rango");
        if(build!=null)Specializations.build(p.catalog,appearance.specializationId(),build.talents());
        if(build!=null)Progression.validate(p.catalog.progression,appearance.classId(),xp==null?p.xp:xp,build.attributes(),build.talents());
        var previous=p.profile();try{
            if(admin)store.backup(previous);
            p.name=name.strip();p.appearance=appearance;p.profession=data.profession(appearance.classId());if(xp!=null)p.xp=xp;if(col!=null)p.col=col;if(potions!=null)p.potions=potions;if(build!=null){p.attributeRanks=new HashMap<>(build.attributes());p.talentRanks=new HashMap<>(build.talents());}else{if(!previous.appearance().classId().equals(appearance.classId())||!previous.appearance().specializationId().equals(appearance.specializationId()))p.talentRanks.clear();reconcile(p);}p.hp=Math.min(p.hp,p.maxHp());p.effects.clear();p.racialEffect="";p.racialBonus=0;p.racialShield=0;reconcile(p);
            store.save(p.profile());return p.view();
        }catch(IOException error){p.crafting=previous.crafting();p.weaponSetId=previous.weaponSetId();p.name=previous.name();p.appearance=previous.appearance();p.profession=data.profession(p.appearance.classId());p.xp=previous.xp();p.col=previous.col();p.potions=previous.potions();p.hp=previous.hp();p.attributeRanks=new HashMap<>(previous.attributeRanks());p.talentRanks=new HashMap<>(previous.talentRanks());throw new IllegalArgumentException("No se guardó el personaje; revisa almacenamiento y permisos");}
    }
    public synchronized Map<String,String> npcContext(String player,String npc){
        Player p=online.get(player);if(p==null||p.hp<=0)throw new IllegalArgumentException("Personaje no disponible");
        var f=data.floor(p.floor);var n=f.npcs.stream().filter(a->a.id().equals(npc)&&Math.hypot(a.x()-p.x,a.z()-p.z)<4).findFirst().orElseThrow(()->new IllegalArgumentException("Acércate al NPC para conversar"));
        return Map.of("name",n.name(),"context","Piso: "+f.name+". NPC: "+n.name()+". Jugador: "+p.name+", nivel "+p.level()+", PV "+p.hp+", col "+p.col+". Misión aceptada: "+p.quest+"; jabalíes "+Math.min(3,p.kills)+"/3; recompensa recibida: "+p.reward+"; portal desbloqueado: "+p.unlocked+". La forja cuesta 50 col y añade +4 daño; solo el botón de mejora general realiza esa acción. Los talleres y la tienda requieren sus botones; la IA no entrega objetos.");
    }
    public synchronized void saveAll()throws IOException {for(Player p:online.values())store.save(p.profile());}
    public synchronized int count(){return (int)online.values().stream().filter(p->p.online).count();}
}
