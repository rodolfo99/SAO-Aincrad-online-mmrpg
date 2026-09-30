package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;

/** Presentation filters mirror the catalogs; Java's server remains the final validator. */
final class Catalog {
    private Catalog() {}
    static boolean permits(JsonNode ids,String id) {return !ids.isArray()||ids.isEmpty()||Json.list(ids).stream().anyMatch(v->v.asText().equals(id));}
    static boolean contains(JsonNode ids,String id) {return Json.list(ids).stream().anyMatch(v->v.asText().equals(id));}
    static JsonNode specialty(JsonNode options,JsonNode look) {return Json.find(options.path("specializations"),look.path("specializationId").asText());}
    static boolean fits(JsonNode options,JsonNode source,JsonNode player,boolean armor) {
        JsonNode look=player.path("appearance"),specialty=specialty(options,look);
        if(!source.path("classId").asText().equals(look.path("classId").asText())||!permits(source.path("specializationIds"),look.path("specializationId").asText()))return false;
        if(armor)return source.path("gender").asText().equals(look.path("gender").asText())&&contains(specialty.path("armorStyles"),source.path("style").asText());
        return contains(specialty.path("weaponKinds"),source.path("mainHand").path("kind").asText())&&(!source.path("offHand").isObject()||contains(specialty.path("weaponKinds"),source.path("offHand").path("kind").asText()));
    }
    static JsonNode source(JsonNode options,JsonNode product) {return Json.find(options.path(product.path("kind").asText().equals("armor")?"equipmentSets":"weaponSets"),product.path("targetId").asText());}
}
