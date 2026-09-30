package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.*;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    public static final JsonNode EMPTY = MissingNode.getInstance();
    private Json() {}
    public static ObjectNode object(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Pares clave/valor requeridos");
        var node = MAPPER.createObjectNode();
        for (int i = 0; i < pairs.length; i += 2) node.set((String) pairs[i], MAPPER.valueToTree(pairs[i + 1]));
        return node;
    }
    public static List<JsonNode> list(JsonNode node) {
        var result = new ArrayList<JsonNode>();
        if (node != null && node.isArray()) node.forEach(result::add);
        return result;
    }
    public static JsonNode find(JsonNode nodes, String id) {
        return list(nodes).stream().filter(n -> n.path("id").asText().equals(id)).findFirst().orElse(EMPTY);
    }
    public static String text(JsonNode node, String key) { return node.path(key).asText(""); }
    public static double num(JsonNode node, String key) { return node.path(key).asDouble(); }
    public static double distance(JsonNode a, JsonNode b) { return Math.hypot(num(a,"x")-num(b,"x"), num(a,"z")-num(b,"z")); }
    public static String number(double n) { return String.format(Locale.ROOT, "%.1f", n); }
    public static String error(Throwable error) {
        while ((error instanceof java.util.concurrent.CompletionException || error instanceof java.util.concurrent.ExecutionException) && error.getCause()!=null) error=error.getCause();
        return Objects.toString(error.getMessage(), error.getClass().getSimpleName());
    }
}
