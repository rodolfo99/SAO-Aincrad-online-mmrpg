package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

/** Catalog-driven character editor with the same options as Angular and native 3D preview. */
final class CharacterEditor extends VBox {
    final TextField name=Ui.input("Nombre de tu personaje",20);
    final ObjectNode appearance;
    private final JsonNode catalog,player;
    private final Group stage=new Group();
    private final PerspectiveCamera camera=new PerspectiveCamera(true);
    private Group avatar;
    private double angle=0,dragX;
    CharacterEditor(JsonNode catalog,JsonNode player,String initialName,JsonNode look) {
        super(10);this.catalog=catalog;this.player=player;appearance=look.isObject()?look.deepCopy():defaults(catalog);name.setText(initialName);
        Group root=new Group(stage,new AmbientLight(Color.web("#b9c0c7")));PointLight key=new PointLight(Color.web("#ffe3bd"));key.setTranslateY(-5);key.setTranslateX(-3);key.setTranslateZ(-4);root.getChildren().addAll(key,camera);
        camera.setNearClip(.05);camera.setFarClip(100);camera.setFieldOfView(35);camera.setTranslateY(-1.4);camera.setTranslateZ(-5.6);
        SubScene preview=new SubScene(root,350,300,true,SceneAntialiasing.BALANCED);preview.setFill(Color.web("#172c38"));preview.setCamera(camera);preview.setId("avatar-preview");preview.setOnMousePressed(e->dragX=e.getSceneX());preview.setOnMouseDragged(e->{angle+=(e.getSceneX()-dragX)*.6;dragX=e.getSceneX();rotate();});
        StackPane viewport=new StackPane(preview);preview.widthProperty().bind(viewport.widthProperty());viewport.setPrefHeight(280);viewport.setMinHeight(200);
        ComboBox<Ui.Choice> gender=choice("gender","genders"),face=choice("face","faces"),race=choice("race","races"),profession=choice("classId","classes");
        ComboBox<Ui.Choice> specialty=new ComboBox<>();specialty.setMaxWidth(Double.MAX_VALUE);
        Runnable fillSpecialty=()->{var list=Json.MAPPER.createArrayNode();Json.list(catalog.path("specializations")).stream().filter(n->n.path("classId").asText().equals(appearance.path("classId").asText())).forEach(list::add);specialty.getItems().setAll(Ui.choices(list,appearance.path("specializationId").asText()).getItems());specialty.getSelectionModel().select(specialty.getItems().stream().filter(c->c.id().equals(appearance.path("specializationId").asText())).findFirst().orElse(specialty.getItems().isEmpty()?null:specialty.getItems().get(0)));};
        specialty.setOnAction(e->{if(specialty.getValue()!=null){appearance.put("specializationId",Ui.selected(specialty));rebuild();}});fillSpecialty.run();
        profession.setOnAction(e->{appearance.put("classId",Ui.selected(profession));fillSpecialty.run();rebuild();});
        ColorPicker hair=new ColorPicker(Models.color(appearance.path("hairColor").asText())),skin=new ColorPicker(Models.color(appearance.path("skinColor").asText()));
        hair.setOnAction(e->{appearance.put("hairColor",hex(hair.getValue()));rebuild();});skin.setOnAction(e->{appearance.put("skinColor",hex(skin.getValue()));rebuild();});
        Label detail=Ui.label("","muted");
        Runnable raceDetails=()->{JsonNode r=Json.find(catalog.path("races"),Ui.selected(race));detail.setText(r.path("description").asText()+"\nT · "+r.path("ability").path("name").asText()+" · "+r.path("ability").path("cooldown").asText()+" s");};
        race.setOnAction(e->{appearance.put("race",Ui.selected(race));JsonNode r=Json.find(catalog.path("races"),Ui.selected(race));if(r.path("hairColors").isArray()&&!r.path("hairColors").isEmpty()){appearance.put("hairColor",r.path("hairColors").get(0).asText());hair.setValue(Models.color(appearance.path("hairColor").asText()));}if(r.path("skinColors").isArray()&&!r.path("skinColors").isEmpty()){appearance.put("skinColor",r.path("skinColors").get(0).asText());skin.setValue(Models.color(appearance.path("skinColor").asText()));}raceDetails.run();rebuild();});raceDetails.run();
        getChildren().addAll(viewport,Ui.row(Ui.button("Cuerpo",()->{camera.setTranslateY(-1.4);camera.setTranslateZ(-5.6);}),Ui.button("Rostro",()->{camera.setTranslateY(-2.20*avatar.getScaleY());camera.setTranslateZ(-1.3*avatar.getScaleY());})),Ui.field("Nombre",name),Ui.field("Género",gender),Ui.field("Rostro",face),Ui.field("Raza",race),Ui.row(Ui.field("Cabello",hair),Ui.field("Piel",skin)),Ui.field("Clase",profession),Ui.field("Especialidad",specialty),detail);rebuild();
    }
    private ComboBox<Ui.Choice> choice(String key,String list) {ComboBox<Ui.Choice> c=Ui.choices(catalog.path(list),appearance.path(key).asText());c.setOnAction(e->{appearance.put(key,Ui.selected(c));rebuild();});return c;}
    private void rebuild() {
        // The preview resolves outfit/weapon options for the edited appearance; the game uses server equipment.
        avatar=ArtLibrary.preview(appearance,catalog,Json.object("level",player.path("level").asInt(1)));stage.getChildren().setAll(avatar);rotate();
    }
    private void rotate() {if(avatar!=null){avatar.setRotationAxis(Rotate.Y_AXIS);avatar.setRotate(angle);}}
    private static String hex(Color c) {return String.format("#%02x%02x%02x",Math.round(c.getRed()*255),Math.round(c.getGreen()*255),Math.round(c.getBlue()*255));}
    static ObjectNode defaults(JsonNode c) {
        String profession=c.path("classes").path(0).path("id").asText("warrior");
        String specialty=Json.list(c.path("specializations")).stream().filter(n->n.path("classId").asText().equals(profession)).map(n->n.path("id").asText()).findFirst().orElse("");
        return Json.object("gender",c.path("genders").path(0).path("id").asText("male"),"face",c.path("faces").path(0).path("id").asText("calm"),"race",c.path("races").path(0).path("id").asText("human"),"classId",profession,"specializationId",specialty,"hairColor",c.path("hairColors").path(0).asText("#342f32"),"skinColor",c.path("skinColors").path(1).asText("#dfb18b"));
    }
}
