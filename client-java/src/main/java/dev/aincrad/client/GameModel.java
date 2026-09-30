package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** UI-thread-owned state. Render interpolation never writes into server snapshots. */
public final class GameModel {
    public JsonNode world=Json.EMPTY,state=Json.EMPTY,account=Json.EMPTY,dialogue=Json.EMPTY;
    public String id="",connection="offline",notice="";
    public final Deque<JsonNode> chat=new ArrayDeque<>();
    public long lastStateNanos;
    public boolean receive(JsonNode message) {
        switch(message.path("type").asText()) {
            case "welcome" -> id=message.path("id").asText();
            case "state" -> {state=message;lastStateNanos=System.nanoTime();}
            case "dialogue" -> dialogue=message;
            case "npcReply" -> {if(dialogue.path("npcId").asText().equals(message.path("npcId").asText())){var copy=(com.fasterxml.jackson.databind.node.ObjectNode)dialogue.deepCopy();copy.put("reply",message.path("text").asText());copy.put("fallback",message.path("fallback").asBoolean());copy.put("reason",message.path("reason").asText());dialogue=copy;}}
            case "notice","error" -> notice=message.path("text").asText();
            case "chat" -> {if(message.path("floor").asInt()==state.path("floor").asInt()){chat.addLast(message);while(chat.size()>30)chat.removeFirst();}}
        }
        return true;
    }
    public JsonNode me() { return Json.find(state.path("players"),id); }
    public JsonNode floor() { return Json.list(world.path("floors")).stream().filter(f->f.path("id").asInt()==state.path("floor").asInt()).findFirst().orElse(Json.EMPTY); }
    public JsonNode target() { return Json.find(state.path(me().path("targetKind").asText().equals("player")?"players":"monsters"),me().path("target").asText()); }
    public void clear() {state=dialogue=Json.EMPTY;id="";chat.clear();lastStateNanos=0;}
}
