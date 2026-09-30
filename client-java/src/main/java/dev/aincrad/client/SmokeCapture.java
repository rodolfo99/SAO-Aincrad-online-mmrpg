package dev.aincrad.client;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.util.Duration;
import java.nio.file.*;
import java.util.function.Supplier;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import javax.imageio.ImageIO;

/** Explicit opt-in QA command; uses a separately configured test account and directory. */
final class SmokeCapture {
    static void start(Scene scene,GameModel model,Supplier<GamePanels> panels,WorldView world,Consumer<JsonNode> send,String directory) {
        Path out=Path.of(directory);try{Files.createDirectories(out);}catch(Exception error){throw new IllegalArgumentException(error);}
        boolean mixed="1".equals(System.getenv("SAO_SMOKE_MIXED"));
        java.util.concurrent.atomic.AtomicBoolean shared=new java.util.concurrent.atomic.AtomicBoolean();
        Timeline monitor=new Timeline(new KeyFrame(Duration.millis(200),e->{if(model.state.path("players").size()>1)shared.set(true);}));monitor.setCycleCount(Animation.INDEFINITE);monitor.play();
        Timeline sequence=new Timeline(
            step(9,()->{if(mixed){send.accept(Commands.action("chat","text","Java y Angular comparten Aincrad ⚔"));JsonNode p=model.me();send.accept(Commands.move(Json.num(p,"x"),Json.num(p,"z")-.8));}}),
            step(12,()->capture(scene,out,"01-java-world.png")),
            step(13,()->panels.get().open("inventory")),step(16,()->capture(scene,out,"02-java-inventory.png")),
            step(17,()->panels.get().open("character")),step(20,()->capture(scene,out,"03-java-character.png")),
            step(21,()->{world.quality(1);panels.get().open("skills");}),step(24,()->capture(scene,out,"04-java-skills.png")),
            step(25,()->{world.quality(2);panels.get().open("crafting");}),step(28,()->capture(scene,out,"05-java-crafting.png")),
            step(29,()->panels.get().open("training")),step(32,()->capture(scene,out,"06-java-training.png")),
            step(33,()->{monitor.stop();try{Files.writeString(out.resolve("visual-result.json"),Ui.pretty(Json.object("connected",model.connection.equals("online"),"presentedFrames",world.presentedFrames(),"floor",model.state.path("floor"),"player",model.me(),"sharedPlayers",shared.get(),"chat",Json.MAPPER.valueToTree(model.chat),"capturedScreens",6,"engine","jMonkeyEngine 3.8.1 / OpenGL")));}catch(Exception e){e.printStackTrace();}world.close();Platform.exit();})
        );sequence.play();
    }
    private static KeyFrame step(int seconds,Runnable action) {return new KeyFrame(Duration.seconds(seconds),e->action.run());}
    private static void capture(Scene scene,Path out,String filename) {try{ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null),null),"png",out.resolve(filename).toFile());}catch(Exception error){throw new IllegalStateException(error);}}
}
