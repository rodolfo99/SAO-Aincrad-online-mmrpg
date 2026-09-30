package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import java.util.*;
import java.util.function.Consumer;

/** Native OpenGL engine frames and JavaFX HUD; no browser or WebView. */
final class WorldView extends StackPane implements AutoCloseable {
    private final GameModel model;
    private final ImageView image=new ImageView();
    private final Pane tags=new Pane();
    private final Label error=new Label("Preparando el motor 3D…");
    private final Map<String,Label> labels=new HashMap<>();
    private final JmeWorld engine;
    private WritableImage frameImage;
    private double yaw,zoom=18,pitch=.62,dragX,dragY,startYaw,startPitch;
    private boolean closed;
    private int presentedFrames;
    WorldView(GameModel model,Consumer<JsonNode> send) {
        this.model=model;setFocusTraversable(true);setId("world-view");image.setPreserveRatio(false);image.fitWidthProperty().bind(widthProperty());image.fitHeightProperty().bind(heightProperty());tags.setMouseTransparent(true);tags.setPickOnBounds(false);error.getStyleClass().add("toast");getChildren().addAll(image,tags,error);
        engine=new JmeWorld(frame->Platform.runLater(()->{
            try{
                if(closed)return;
                if(frameImage==null||frameImage.getWidth()!=frame.width()||frameImage.getHeight()!=frame.height()){frameImage=new WritableImage(frame.width(),frame.height());image.setImage(frameImage);}
                frameImage.getPixelWriter().setPixels(0,0,frame.width(),frame.height(),PixelFormat.getByteBgraPreInstance(),frame.pixels(),0,frame.width()*4);error.setVisible(false);
                presentedFrames++;
                Set<String> live=new HashSet<>();for(JmeWorld.Tag tag:frame.tags()){live.add(tag.id());Label l=labels.computeIfAbsent(tag.id(),id->{Label v=new Label();v.getStyleClass().add("entity-label");tags.getChildren().add(v);return v;});l.setText(tag.text());l.autosize();l.relocate(tag.x()*getWidth()/frame.width()-l.getWidth()/2,tag.y()*getHeight()/frame.height()-l.getHeight());}
                labels.entrySet().removeIf(e->{if(live.contains(e.getKey()))return false;tags.getChildren().remove(e.getValue());return true;});
            }finally{release(frame);}
        }),message->Platform.runLater(()->{error.setText(message);error.setVisible(true);}),intent->Platform.runLater(()->send.accept(intent)));
        setOnMousePressed(e->{requestFocus();if(e.getButton()==MouseButton.SECONDARY||e.getButton()==MouseButton.MIDDLE){dragX=e.getSceneX();dragY=e.getSceneY();startYaw=yaw;startPitch=pitch;}else if(e.getButton()==MouseButton.PRIMARY&&model.connection.equals("online"))engine.pick(e.getX()/Math.max(1,getWidth()),e.getY()/Math.max(1,getHeight()));e.consume();});
        setOnMouseDragged(e->{if(e.isSecondaryButtonDown()||e.isMiddleButtonDown()){yaw=startYaw-(e.getSceneX()-dragX)*.006;pitch=Math.max(.25,Math.min(1.2,startPitch+(e.getSceneY()-dragY)*.003));}e.consume();});
        setOnScroll(e->{zoom=Math.max(6,Math.min(38,zoom-e.getDeltaY()*.035));e.consume();});engine.launch();engine.quality(java.util.prefs.Preferences.userNodeForPackage(AincradApp.class).getInt("graphics",2));
    }
    private void release(JmeWorld.Frame frame) {engine.release(frame);}
    double yaw() {return yaw;}
    int presentedFrames() {return presentedFrames;}
    void quality(int tier) {engine.quality(tier);}
    void frame(double dt) {if(!closed){double scale=getScene()!=null&&getScene().getWindow()!=null?getScene().getWindow().getOutputScaleX():1;engine.update(model.world,model.state,model.id,yaw,pitch,zoom,(int)(getWidth()*scale),(int)(getHeight()*scale));}}
    void event(JsonNode event) {if(!closed)engine.event(event);}
    @Override public void close() {closed=true;engine.stop(false);}
}
