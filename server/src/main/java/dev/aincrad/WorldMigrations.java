package dev.aincrad;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.IOException;
import java.util.*;

/** Additive v1 migration: keep custom values and legacy races, including Cait. */
final class WorldMigrations {
    static boolean upgrade(ObjectNode root,ObjectMapper json)throws IOException{
        int revision=root.path("schemaRevision").asInt(1);
        if(revision>7)throw new IllegalArgumentException("Revisión de mundo no compatible");
        if(revision==7)return false;
        if(revision>=5){bestiary(root,json);root.put("schemaRevision",7);return true;}
        if(revision<2){
        JsonNode defaults;
        try(var stream=WorldMigrations.class.getResourceAsStream("/catalog-v2.json")){defaults=json.readTree(stream);}
        ObjectNode c=(ObjectNode)root.get("characterOptions");
        if(c==null)throw new IllegalArgumentException("Falta catálogo de personajes");
        ArrayNode races=(ArrayNode)c.get("races");
        if(races==null)throw new IllegalArgumentException("Falta catálogo de razas");
        for(var def:defaults.path("races")){
            ObjectNode found=null;for(var existing:races)if(existing.path("id").asText().equals(def.path("id").asText()))found=(ObjectNode)existing;
            if(found==null)races.add(def.deepCopy());else{var fields=def.fields();while(fields.hasNext()){var entry=fields.next();if(!found.has(entry.getKey()))found.set(entry.getKey(),entry.getValue().deepCopy());}}
        }
        for(var race:races)if(!race.has("model"))((ObjectNode)race).put("model",race.path("ears").asText().equals("cat")?"cat":"human");
        if(!c.has("weaponSets")){
            ArrayNode weapons=c.putArray("weaponSets");
            for(var cls:c.path("classes")){
                String id=cls.path("id").asText();boolean found=false;
                for(var def:defaults.path("weaponSets"))if(def.path("classId").asText().equals(id)){weapons.add(def.deepCopy());found=true;}
                if(!found){var basic=json.createObjectNode();basic.put("id",id+"-starter").put("name","Arma inicial").put("classId",id).put("minLevel",1).put("autoEquip",true);
                    var item=basic.putObject("mainHand");item.put("name","Espada inicial").put("kind","sword").put("hands",1).put("primary","#d9e5e6").put("secondary","#947b4c").put("glow","#609687");item.putObject("attributes");weapons.add(basic);}
            }
        }
        if(!root.has("pvp"))root.set("pvp",json.valueToTree(new Pvp.Rules()));
        }
        if(!root.has("crafting")){
            ObjectNode crafting;try(var stream=WorldMigrations.class.getResourceAsStream("/crafting-v3.json")){crafting=(ObjectNode)json.readTree(stream);}
            var c=root.path("characterOptions");Set<String> weapons=new HashSet<>(),armor=new HashSet<>(),attrs=new HashSet<>();c.path("weaponSets").forEach(w->weapons.add(w.path("id").asText()));c.path("equipmentSets").forEach(w->armor.add(w.path("id").asText()));c.path("attributeDefinitions").forEach(w->attrs.add(w.path("id").asText()));
            ArrayNode keep=json.createArrayNode();for(var recipe:crafting.path("recipes")){if((recipe.path("kind").asText().equals("weapon")?weapons:armor).contains(recipe.path("targetId").asText())){for(String key:List.of("bonusAttributes","upgradeAttributes")){ObjectNode a=(ObjectNode)recipe.get(key);List<String> remove=new ArrayList<>();a.fieldNames().forEachRemaining(id->{if(!attrs.contains(id))remove.add(id);});a.remove(remove);}keep.add(recipe);}}crafting.set("recipes",keep);root.set("crafting",crafting);
        }
        // Preserve existing layouts. New stations are placed at the already validated spawn.
        var floors=root.withArray("floors");for(var f:floors)if(!f.has("buildings"))((ObjectNode)f).putArray("buildings");
        if(!floors.isEmpty()){var f=(ObjectNode)floors.get(0);var npcs=f.withArray("npcs");for(String role:List.of("tailor","merchant")){boolean present=false;for(var floor:floors)for(var npc:floor.path("npcs"))if(npc.path("role").asText().equals(role))present=true;if(!present&&npcs.size()<100){var npc=npcs.addObject();npc.put("id","station-"+UUID.randomUUID().toString()).put("name",role.equals("tailor")?"Mira":"Tessa").put("role",role).put("x",f.path("spawn").path("x").asDouble()).put("z",f.path("spawn").path("z").asDouble());}}}
        JsonNode resources;try(var stream=WorldMigrations.class.getResourceAsStream("/gathering-v4.json")){resources=json.readTree(stream);}if(!root.has("gathering"))root.set("gathering",resources.path("gathering").deepCopy());
        var crafting=(ObjectNode)root.get("crafting");for(String key:List.of("materials","professions")){var list=crafting.withArray(key);for(var add:resources.path(key)){boolean found=false;for(var current:list)if(current.path("id").asText().equals(add.path("id").asText()))found=true;if(!found)list.add(add.deepCopy());}}
        for(var floor:floors)if(!floor.has("resources"))((ObjectNode)floor).putArray("resources");
        JsonNode specDefaults;try(var stream=WorldMigrations.class.getResourceAsStream("/specializations-v5.json")){specDefaults=json.readTree(stream);}var options=(ObjectNode)root.get("characterOptions");Set<String> classIds=new HashSet<>();options.path("classes").forEach(c->classIds.add(c.path("id").asText()));
        if(!options.has("specializations")){var specs=options.putArray("specializations");for(var s:specDefaults.path("specializations"))if(classIds.contains(s.path("classId").asText()))specs.add(s.deepCopy());for(String classId:classIds)if(!Set.of("warrior","mage","healer").contains(classId)){var s=specs.addObject();s.put("id",classId+"-default").put("classId",classId).put("name","General").put("description","Especialidad compatible con la clase personalizada.");s.set("weaponKinds",json.valueToTree(Weapons.KINDS));s.set("armorStyles",json.valueToTree(List.of("tunic","leather","mail","plate")));s.putObject("effects");}
            // Older custom warrior equipment retains its legal styles during additive migration.
            for(var s:specs)if(s.path("id").asText().equals("swordsman")){Set<String> kinds=new LinkedHashSet<>(List.of("sword","dagger","greatsword")),styles=new LinkedHashSet<>(List.of("leather","mail"));for(var w:options.path("weaponSets"))if(w.path("classId").asText().equals("warrior")){kinds.add(w.path("mainHand").path("kind").asText());if(w.hasNonNull("offHand"))kinds.add(w.path("offHand").path("kind").asText());}for(var a:options.path("equipmentSets"))if(a.path("classId").asText().equals("warrior"))styles.add(a.path("style").asText());((ObjectNode)s).set("weaponKinds",json.valueToTree(kinds));((ObjectNode)s).set("armorStyles",json.valueToTree(styles));}
        }
        Set<String> attributes=new HashSet<>();options.path("attributeDefinitions").forEach(a->attributes.add(a.path("id").asText()));
        for(String key:List.of("equipmentSets","weaponSets")){var list=options.withArray(key);Set<String> existing=new HashSet<>();for(var item:list){existing.add(item.path("id").asText());if(!item.has("specializationIds")){var ids=((ObjectNode)item).putArray("specializationIds");if(item.path("classId").asText().equals("warrior"))ids.add(key.equals("weaponSets")&&item.path("id").asText().contains("-guardia-")?"tank":"swordsman");}}
            for(var item:specDefaults.path(key))if(classIds.contains(item.path("classId").asText())&&!existing.contains(item.path("id").asText()))list.add(item.deepCopy());
            // Ensure the new swordsman has an automatic starter/tier without changing an existing eligible custom automatic set.
            if(key.equals("weaponSets"))for(var item:list)if(item.path("id").asText().contains("warrior-duelista-")){boolean active=false;for(var other:list)if(other.path("autoEquip").asBoolean()&&other.path("classId").asText().equals("warrior")&&other.path("minLevel").asInt()==item.path("minLevel").asInt()&&other.path("specializationIds").toString().contains("swordsman"))active=true;if(!active)((ObjectNode)item).put("autoEquip",true);}
        }
        var talents=((ObjectNode)options.get("progression")).withArray("talents");Set<String> talentIds=new HashSet<>();talents.forEach(t->talentIds.add(t.path("id").asText()));for(var t:specDefaults.path("talents"))if(classIds.contains(t.path("classId").asText())&&!talentIds.contains(t.path("id").asText())&&t.path("minLevel").asInt()<=options.path("progression").path("maxLevel").asInt()){talents.add(t.deepCopy());talentIds.add(t.path("id").asText());}
        if(!options.has("skills")){var list=options.putArray("skills");for(var s:specDefaults.path("skills"))if(classIds.contains(s.path("classId").asText())&&talentIds.contains(s.path("talentId").asText()))list.add(s.deepCopy());}
        Set<String> targets=new HashSet<>();options.path("weaponSets").forEach(w->targets.add(w.path("id").asText()));options.path("equipmentSets").forEach(w->targets.add(w.path("id").asText()));var recipes=crafting.withArray("recipes");Set<String> recipeIds=new HashSet<>();recipes.forEach(r->recipeIds.add(r.path("id").asText()));for(var r:specDefaults.path("recipes"))if(!recipeIds.contains(r.path("id").asText())&&targets.contains(r.path("targetId").asText()))recipes.add(r.deepCopy());
        bestiary(root,json);root.put("schemaRevision",7);return true;
    }
    private static void bestiary(ObjectNode root,ObjectMapper json)throws IOException{
        JsonNode defaults;try(var stream=WorldMigrations.class.getResourceAsStream("/bestiary-v7.json")){defaults=json.readTree(stream);}
        if(!root.has("monsterSpecies"))root.set("monsterSpecies",defaults.path("monsterSpecies").deepCopy());
        for(var floor:root.path("floors")){if(!floor.has("monsterZones"))((ObjectNode)floor).putArray("monsterZones");for(var monster:floor.path("monsters"))if(!monster.has("level"))((ObjectNode)monster).put("level",1);}
        // Existing drops and layouts remain authoritative. Filter defaults to known materials/species.
        var craft=(ObjectNode)root.get("crafting");var loot=craft.withObject("monsterDrops");Set<String> materials=new HashSet<>(),species=new HashSet<>();craft.path("materials").forEach(m->materials.add(m.path("id").asText()));root.path("monsterSpecies").forEach(m->species.add(m.path("id").asText()));
        var fields=defaults.path("monsterDrops").fields();while(fields.hasNext()){var e=fields.next();if(species.contains(e.getKey())&&!loot.has(e.getKey())){var values=(ObjectNode)e.getValue().deepCopy();List<String> remove=new ArrayList<>();values.fieldNames().forEachRemaining(id->{if(!materials.contains(id))remove.add(id);});values.remove(remove);loot.set(e.getKey(),values);}}
    }

}
