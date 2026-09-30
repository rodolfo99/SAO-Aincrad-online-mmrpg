package dev.aincrad.client;

import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Consumer;

final class MiniMap extends Canvas {
    private final GameModel model;
    private final Consumer<JsonNode> send;
    MiniMap(GameModel model,Consumer<JsonNode> send) {
        super(210,210);this.model=model;this.send=send;setId("minimap");
        setOnMouseClicked(e->{double scale=scale(),x=(e.getX()-105)/scale,z=(e.getY()-105)/scale;send.accept(Commands.move(x,z));});
    }
    private double scale() {return 96/model.floor().path("radius").asDouble(92);}
    void draw() {
        GraphicsContext g=getGraphicsContext2D();g.setFill(Color.web("#10212a",.96));g.fillRoundRect(0,0,210,210,20,20);double s=scale();g.setStroke(Color.web("#a9bfa7"));g.strokeOval(9,9,192,192);
        for(JsonNode zone:Json.list(model.floor().path("monsterZones"))){g.setFill(Color.web("#b37a62",.19));double r=Json.num(zone,"radius")*s;g.fillOval(105+Json.num(zone,"x")*s-r,105+Json.num(zone,"z")*s-r,r*2,r*2);}
        for(JsonNode p:Json.list(model.floor().path("props"))){g.setFill(Color.web(p.path("kind").asText().equals("tree")?"#56765e":"#bfae86"));double r=Math.max(1.2,Json.num(p,"radius")*s);g.fillOval(105+Json.num(p,"x")*s-r,105+Json.num(p,"z")*s-r,r*2,r*2);}
        dot(g,model.floor().path("portal"),Color.web("#a8eeef"),5,s);
        for(JsonNode n:Json.list(model.floor().path("npcs")))dot(g,n,Color.web("#f3cb83"),3,s);
        for(JsonNode n:Json.list(model.state.path("monsters")))dot(g,n,n.path("training").asBoolean()?Color.web("#9faec4"):Color.web("#ef9378"),2.5,s);
        for(JsonNode n:Json.list(model.state.path("players")))dot(g,n,n.path("id").asText().equals(model.id)?Color.web("#c4ffe8"):Color.web("#85bfea"),4,s);
    }
    private void dot(GraphicsContext g,JsonNode n,Color color,double r,double s) {g.setFill(color);g.fillOval(105+Json.num(n,"x")*s-r,105+Json.num(n,"z")*s-r,r*2,r*2);}
}
