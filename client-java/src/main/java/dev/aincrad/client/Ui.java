package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import java.util.*;

final class Ui {
    private Ui() {}
    static Label label(String text,String... styles) {Label l=new Label(text);l.setWrapText(true);l.getStyleClass().addAll(styles);return l;}
    static Button button(String text,Runnable action) {Button b=new Button(text);b.setOnAction(e->action.run());b.setFocusTraversable(false);return b;}
    static Button primary(String text,Runnable action) {Button b=button(text,action);b.getStyleClass().add("primary");return b;}
    static VBox column(Node... nodes) {VBox box=new VBox(10,nodes);return box;}
    static HBox row(Node... nodes) {HBox box=new HBox(8,nodes);box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);return box;}
    static VBox card(Node... nodes) {VBox box=column(nodes);box.getStyleClass().add("card");return box;}
    static VBox field(String title,Node control) {return column(label(title,"field-title"),control);}
    static ScrollPane scroll(Node child) {ScrollPane s=new ScrollPane(child);s.setFitToWidth(true);s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);return s;}
    static TextField input(String placeholder,int length) {TextField f=new TextField();f.setPromptText(placeholder);f.setTextFormatter(new TextFormatter<String>(change->change.getControlNewText().length()<=length?change:null));return f;}
    static ImageView image(String path,double width) {String safe=path.startsWith("/")?path:"/"+path;var url=Ui.class.getResource(safe);ImageView v=new ImageView();if(url!=null)v.setImage(new Image(url.toExternalForm(),true));v.setFitWidth(width);v.setPreserveRatio(true);v.setSmooth(true);return v;}
    static String pretty(JsonNode node) {try{return Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);}catch(Exception e){return node.toString();}}
    static String stats(JsonNode values) {if(!values.isObject())return "";List<String> parts=new ArrayList<>();values.fields().forEachRemaining(e->parts.add(e.getKey()+": "+e.getValue().asText()));return String.join(" · ",parts);}
    static Spinner<Integer> integer(int min,int max,int value) {Spinner<Integer> s=new Spinner<>(min,Math.max(min,max),Math.max(min,Math.min(max,value)));s.setEditable(false);s.setMaxWidth(130);return s;}
    record Choice(String id,String name) {@Override public String toString(){return name;}}
    static ComboBox<Choice> choices(JsonNode items,String id) {ComboBox<Choice> c=new ComboBox<>();for(JsonNode n:Json.list(items))c.getItems().add(new Choice(n.path("id").asText(),n.path("name").asText(n.path("username").asText(n.path("id").asText()))));c.getSelectionModel().select(c.getItems().stream().filter(x->x.id().equals(id)).findFirst().orElse(c.getItems().isEmpty()?null:c.getItems().get(0)));c.setMaxWidth(Double.MAX_VALUE);return c;}
    static String selected(ComboBox<Choice> choice) {return choice.getValue()==null?"":choice.getValue().id();}
    static void pad(Region region) {if(region instanceof Pane p)p.setPadding(new Insets(18));}
}
