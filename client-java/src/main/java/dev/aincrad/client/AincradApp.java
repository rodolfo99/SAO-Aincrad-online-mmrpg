package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.animation.AnimationTimer;
import javafx.application.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Native desktop shell. No browser, WebView, Angular runtime or embedded server. */
public final class AincradApp extends Application {
    private final GameModel model=new GameModel();
    private final InputController input=new InputController(this::send);
    private Stage stage;
    private Scene scene;
    private GameClient client;
    private WorldView world;
    private GamePanels drawer;
    private MiniMap map;
    private StackPane root;
    private Label toast,status,playerLabel,floorLabel,questLabel,targetLabel;
    private final javafx.animation.PauseTransition toastTimer=new javafx.animation.PauseTransition(javafx.util.Duration.seconds(7));
    private ProgressBar life,xp;
    private VBox connectionOverlay,dialogueBox;
    private TextArea chat;
    private final List<Button> skillButtons=new ArrayList<>(),actionButtons=new ArrayList<>();
    private String selectedCharacter="",characterName="Explorador";
    private JsonNode appearance=Json.EMPTY;
    private boolean entered;
    private long lastFrame,lastInput,lastHud,lastPanels;
    private AnimationTimer clock;
    @Override public void start(Stage stage) {
        this.stage=stage;stage.setTitle("Aincrad · Java Desktop 0.1.0");root=new StackPane();scene=new Scene(root,1280,820);scene.getStylesheets().add(AincradApp.class.getResource("/style.css").toExternalForm());stage.setScene(scene);stage.setMinWidth(1050);stage.setMinHeight(700);stage.setFullScreenExitHint("");
        scene.addEventFilter(KeyEvent.KEY_PRESSED,this::keyDown);scene.addEventFilter(KeyEvent.KEY_RELEASED,e->input.release(e.getCode().name()));scene.focusOwnerProperty().addListener((o,old,now)->{if(entered&&!(now==world))input.stop();});stage.focusedProperty().addListener((o,was,now)->{if(!now)input.stop();});stage.iconifiedProperty().addListener((o,was,now)->{if(now)input.stop();});stage.setOnCloseRequest(e->{if(clock!=null)clock.stop();if(world!=null)world.close();if(client!=null)client.close();});
        String server=getParameters().getNamed().getOrDefault("server",System.getenv().getOrDefault("SAO_SERVER_URL","http://localhost:8081"));landing(server);stage.show();
        clock=new AnimationTimer(){@Override public void handle(long now){if(!entered||world==null)return;if(lastFrame!=0&&now-lastFrame<16_000_000)return;double dt=lastFrame==0?.016:Math.min(.1,(now-lastFrame)/1e9);lastFrame=now;if(client!=null&&client.online()&&stage.isFocused()&&!stage.isIconified()){if(now-lastInput>=50_000_000){input.tick(world.yaw());lastInput=now;}world.frame(dt);}if(now-lastHud>150_000_000){hud();lastHud=now;}if(now-lastPanels>1_000_000_000){drawer.refresh();lastPanels=now;}}};clock.start();
        // Optional isolated QA uses environment credentials, never command-line passwords.
        if(getParameters().getNamed().containsKey("smoke"))smoke(server,getParameters().getNamed().get("smoke"));
    }
    private void bindClient(String url) {
        if(client!=null&&client.endpoint().equals(Endpoint.parse(url)))return;
        if(client!=null)client.close();model.clear();model.account=Json.EMPTY;
        client=new GameClient(Endpoint.parse(url),Platform::runLater,new GameClient.Listener(){
            @Override public void world(JsonNode w){model.world=w;}
            @Override public void message(JsonNode m){model.receive(m);if(m.path("type").asText().equals("welcome")){selectedCharacter=model.id;input.reset();}if(world!=null)world.event(m);if(m.path("type").asText().equals("dialogue")||m.path("type").asText().equals("npcReply")&&model.dialogue.has("npcId"))dialogue();if(m.path("type").asText().equals("error")||m.path("type").asText().equals("notice"))notice(m.path("text").asText());}
            @Override public void status(String state,String detail){model.connection=state;input.reset();if(status!=null)status.setText(detail);if(connectionOverlay!=null){connectionOverlay.setVisible(!state.equals("online"));connectionOverlay.setManaged(!state.equals("online"));}if(state.equals("expired"))notice(detail);}
        });
    }
    private <T> void async(CompletableFuture<T> future,Consumer<T> done) {future.whenComplete((value,error)->Platform.runLater(()->{if(error!=null)notice(Json.error(error));else done.accept(value);}));}
    private void landing(String url) {
        entered=false;if(world!=null){world.close();world=null;}root.getChildren().clear();toast=Ui.label("","toast");toast.setVisible(false);toast.setMouseTransparent(true);StackPane.setAlignment(toast,Pos.BOTTOM_CENTER);StackPane.setMargin(toast,new Insets(18));
        ImageView background=Ui.image("assets/aincrad-landscape.png",1280);background.setOpacity(.26);background.fitWidthProperty().bind(root.widthProperty());background.fitHeightProperty().bind(root.heightProperty());background.setPreserveRatio(false);root.getChildren().add(background);
        TextField server=Ui.input("http://localhost:8081",240);server.setText(url);server.setId("server-url");TextField username=Ui.input("Tu usuario",32);username.setId("username");PasswordField password=new PasswordField();password.setId("password");
        VBox login=Ui.card(Ui.label("AINCRAD","brand"),Ui.label("Ecos del primer piso","hero-title"),Ui.label("JAVA DESKTOP · MUNDO COMPARTIDO","eyebrow"),Ui.field("Servidor",server),Ui.field("Usuario",username),Ui.field("Contraseña",password));login.setPrefWidth(380);login.setMaxWidth(400);
        Button enter=Ui.primary("Entrar a mi cuenta →",()->{try{bindClient(server.getText());String secret=password.getText();password.clear();async(client.player("login",Json.object("username",username.getText(),"password",secret)).thenCombine(client.world(),(account,world)->Json.object("account",account,"world",world)),result->{model.account=result.path("account");model.world=result.path("world");selection();});}catch(Exception bad){notice(Json.error(bad));}});enter.setId("login-button");password.setOnAction(e->enter.fire());
        login.getChildren().addAll(enter,Ui.row(Ui.button("Crear cuenta",()->{try{bindClient(server.getText());accountForm("register");}catch(Exception bad){notice(Json.error(bad));}}),Ui.button("Recuperar",()->{try{bindClient(server.getText());accountForm("forgot-password");}catch(Exception bad){notice(Json.error(bad));}})),Ui.button("Restablecer con enlace",()->{try{bindClient(server.getText());accountForm("reset-password");}catch(Exception bad){notice(Json.error(bad));}}),Ui.button("Administración root",()->{try{bindClient(server.getText());new AdminPanel(client,stage);}catch(Exception bad){notice(Json.error(bad));}}),Ui.label("Usa la misma cuenta de Angular. El registro no inicia sesión automáticamente.","muted"));
        VBox story=Ui.column(Ui.label("TU SIGUIENTE\nAVENTURA","splash-title"),Ui.label("Dos pisos, habilidades, talleres y compañeros reales.\nEl mismo Aincrad, ahora desde Java.","splash-text"));story.setPrefWidth(480);HBox layout=Ui.row(login,story);layout.setSpacing(70);layout.setAlignment(Pos.CENTER);root.getChildren().addAll(layout,toast);
    }
    private void selection() {
        root.getChildren().removeIf(n->n!=toast);BorderPane page=new BorderPane();page.setPadding(new Insets(24));
        page.setTop(Ui.row(Ui.label("AINCRAD · "+model.account.path("username").asText(),"brand"),Ui.button("Mi cuenta",()->accountForm("account")),Ui.button("Administración",()->new AdminPanel(client,stage)),Ui.button("Cerrar sesión",this::logout)));
        ComboBox<Ui.Choice> characters=Ui.choices(model.account.path("characters"),selectedCharacter);characters.getItems().add(0,new Ui.Choice("","Crear un personaje nuevo"));if(selectedCharacter.isEmpty()){String first=model.account.path("characters").path(0).path("id").asText("");characters.getSelectionModel().select(characters.getItems().stream().filter(c->c.id().equals(first)).findFirst().orElse(characters.getItems().get(0)));}
        CharacterEditor editor=new CharacterEditor(model.world.path("characterOptions"),Json.EMPTY,characterName,appearance);editor.setMaxWidth(500);editor.setId("character-editor");VBox center=Ui.card(Ui.label("Elige tu personaje","hero-title"),Ui.field("Personajes de tu cuenta",characters),editor);center.setMaxWidth(550);ScrollPane scroll=Ui.scroll(center);scroll.setMaxWidth(570);characters.setOnAction(e->{editor.setDisable(!Ui.selected(characters).isEmpty());});editor.setDisable(!Ui.selected(characters).isEmpty());
        Button play=Ui.primary("Entrar al mundo →",()->{selectedCharacter=Ui.selected(characters);characterName=editor.name.getText();appearance=editor.appearance.deepCopy();enterWorld();});play.setId("play-button");center.getChildren().add(play);page.setCenter(new StackPane(scroll));root.getChildren().add(0,page);
    }
    private void enterWorld() {
        model.clear();input.reset();root.getChildren().removeIf(n->n!=toast);world=new WorldView(model,this::send);world.setId("world-view");root.getChildren().add(0,world);entered=true;
        playerLabel=Ui.label("Entrando…","player-title");playerLabel.setMinHeight(44);life=new ProgressBar(1);life.setPrefWidth(260);life.getStyleClass().add("life");xp=new ProgressBar(0);xp.setPrefWidth(260);xp.getStyleClass().add("xp");VBox player=Ui.card(playerLabel,life,xp);player.setPrefWidth(280);floorLabel=Ui.label("Conectando con Aincrad","floor-title");
        MenuButton menu=new MenuButton("Tu aventura");String[][] panels={{"inventory","Inventario"},{"bag","Bolsa"},{"shop","Mercado"},{"crafting","Oficios"},{"skills","Habilidades"},{"progression","Talentos"},{"character","Mi personaje"},{"training","Entrenamiento"},{"zones","Zonas"},{"pvp","PvP"},{"journal","Diario"},{"help","Controles"}};
        for(String[] panel:panels){MenuItem item=new MenuItem(panel[1]);item.setOnAction(e->{input.stop();drawer.open(panel[0]);map.setVisible(false);});menu.getItems().add(item);}MenuItem account=new MenuItem("Mi cuenta");account.setOnAction(e->accountForm("account"));menu.getItems().add(account);MenuItem admin=new MenuItem("Administración root");admin.setOnAction(e->{input.stop();new AdminPanel(client,stage);});menu.getItems().add(admin);
        ComboBox<String> quality=new ComboBox<>();quality.getItems().addAll("Ligera","Equilibrada","Alta");quality.getSelectionModel().select(Math.max(0,Math.min(2,java.util.prefs.Preferences.userNodeForPackage(AincradApp.class).getInt("graphics",2))));quality.setOnAction(e->{int tier=quality.getSelectionModel().getSelectedIndex();world.quality(tier);java.util.prefs.Preferences.userNodeForPackage(AincradApp.class).putInt("graphics",tier);});Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox header=Ui.row(player,floorLabel,spacer,quality,menu,Ui.button("Salir",this::exitWorld));header.setMaxHeight(118);StackPane.setAlignment(header,Pos.TOP_CENTER);StackPane.setMargin(header,new Insets(14));root.getChildren().add(header);
        map=new MiniMap(model,this::send);StackPane.setAlignment(map,Pos.TOP_RIGHT);StackPane.setMargin(map,new Insets(125,18,0,0));root.getChildren().add(map);
        questLabel=Ui.label("","quest");questLabel.setMaxWidth(310);StackPane.setAlignment(questLabel,Pos.TOP_LEFT);StackPane.setMargin(questLabel,new Insets(145,18,0,18));root.getChildren().add(questLabel);
        targetLabel=Ui.label("","target");targetLabel.setMaxWidth(400);StackPane.setAlignment(targetLabel,Pos.TOP_CENTER);StackPane.setMargin(targetLabel,new Insets(135,0,0,0));root.getChildren().add(targetLabel);
        chat=new TextArea("Bienvenido a Aincrad.");chat.setEditable(false);chat.setWrapText(true);chat.setPrefRowCount(4);chat.setFocusTraversable(false);TextField text=Ui.input("Conversación del piso…",180);text.setId("chat-input");Runnable say=()->{if(!text.getText().isBlank()){send(Commands.action("chat","text",text.getText()));text.clear();world.requestFocus();}};text.setOnAction(e->say.run());VBox conversation=Ui.card(Ui.label("CONVERSACIÓN DEL PISO","eyebrow"),chat,Ui.row(text,Ui.button("Enviar",say)));conversation.setMaxWidth(330);conversation.setMaxHeight(180);StackPane.setAlignment(conversation,Pos.BOTTOM_LEFT);StackPane.setMargin(conversation,new Insets(0,0,155,14));root.getChildren().add(conversation);
        HBox active=new HBox(8),actions=new HBox(8);active.setAlignment(Pos.CENTER);actions.setAlignment(Pos.CENTER);skillButtons.clear();actionButtons.clear();for(int i=0;i<4;i++){int slot=i;Button b=Ui.button((i+1)+" · Sin asignar",()->castSlot(slot));b.setPrefWidth(150);active.getChildren().add(b);skillButtons.add(b);}
        String[][] action={{"ESPACIO","Ataque","attack"},{"Q","Técnica","attack"},{"T","Racial","racial"},{"R","Poción","potion"},{"E","Hablar","interact"},{"F","Portal","portal"}};for(int i=0;i<action.length;i++){int index=i;Button b=Ui.button(action[i][0]+"\n"+action[i][1],()->send(index==1?Commands.action("attack","skill",true):Commands.action(action[index][2])));b.setPrefSize(112,62);actions.getChildren().add(b);actionButtons.add(b);}VBox dock=Ui.card(Ui.label("WASD / flechas · clic para caminar · botón derecho para cámara · rueda para zoom","muted"),active,actions);dock.setMaxWidth(760);dock.setMaxHeight(150);dock.setAlignment(Pos.CENTER);StackPane.setAlignment(dock,Pos.BOTTOM_CENTER);StackPane.setMargin(dock,new Insets(0,0,12,0));root.getChildren().add(dock);
        drawer=new GamePanels(model,this::send,()->{drawer.hide();map.setVisible(true);world.requestFocus();});StackPane.setAlignment(drawer,Pos.CENTER_RIGHT);StackPane.setMargin(drawer,new Insets(122,14,170,0));root.getChildren().add(drawer);
        dialogueBox=Ui.card();dialogueBox.setVisible(false);dialogueBox.setMaxWidth(650);dialogueBox.setMaxHeight(400);StackPane.setAlignment(dialogueBox,Pos.CENTER);root.getChildren().add(dialogueBox);
        status=Ui.label("Conectando…","muted");connectionOverlay=Ui.card(Ui.label("Enlace con Aincrad","hero-title"),status,Ui.button("Reintentar",()->client.connect(characterName,selectedCharacter,appearance)),Ui.button("Volver al inicio",this::exitWorld));connectionOverlay.setMaxWidth(500);connectionOverlay.setMaxHeight(250);root.getChildren().add(connectionOverlay);root.getChildren().remove(toast);root.getChildren().add(toast);client.connect(characterName,selectedCharacter,appearance);world.requestFocus();
    }
    private void hud() {
        if(!entered)return;JsonNode p=model.me();if(p.isMissingNode())return;
        playerLabel.setText(p.path("name").asText()+" · Nv. "+p.path("level").asText()+"\n"+p.path("hp").asText()+" / "+p.path("maxHp").asText()+" PV · "+p.path("col").asText()+" col");life.setProgress(p.path("hp").asDouble()/Math.max(1,p.path("maxHp").asDouble()));JsonNode progression=model.world.path("characterOptions").path("progression");int per=progression.path("xpPerLevel").asInt(100);xp.setProgress(p.path("level").asInt()>=progression.path("maxLevel").asInt(20)?1:(p.path("xp").asInt()%per)/(double)per);
        floorLabel.setText("PISO "+p.path("floor").asText()+"\n"+model.floor().path("name").asText());map.draw();questLabel.setText(p.path("reward").asBoolean()?"El Centinela\n"+(p.path("unlocked").asBoolean()?"Portal norte desbloqueado":"Derrota al Centinela y llega al portal norte"):p.path("quest").asBoolean()?"Misión de Lyra\nJabalíes: "+Math.min(3,p.path("kills").asInt())+" / 3":"Tu primer paso\nAcércate a Lyra y pulsa E");JsonNode target=model.target();targetLabel.setVisible(!target.isMissingNode());targetLabel.setText(target.isMissingNode()?"":target.path("name").asText()+" · "+target.path("hp").asText()+" / "+target.path("maxHp").asText()+" PV");StringBuilder lines=new StringBuilder();for(JsonNode line:model.chat)lines.append(line.path("name").asText()).append(": ").append(line.path("text").asText()).append('\n');String value=lines.toString();if(!chat.getText().equals(value)){chat.setText(value);chat.positionCaret(chat.getLength());}
        for(int i=0;i<4;i++){String id=p.path("skills").path("slots").path(i).asText();JsonNode s=Json.find(model.world.path("characterOptions").path("skills"),id);double cd=p.path("skills").path("cooldowns").path(id).asDouble();skillButtons.get(i).setText((i+1)+" · "+s.path("name").asText("Sin asignar")+(cd>0?"\n"+Json.number(cd)+" s":""));skillButtons.get(i).setDisable(id.isEmpty()||cd>0||p.path("hp").asInt()<=0);}
        for(int i=0;i<4;i++){String key=List.of("attack","skill","racial","potion").get(i);double cd=model.state.path("cooldowns").path(key).asDouble();actionButtons.get(i).setDisable(cd>0||p.path("hp").asInt()<=0||i==3&&p.path("potions").asInt()==0);}actionButtons.get(3).setText("R\nPoción ×"+p.path("potions").asText());
        if(model.lastStateNanos>0&&System.nanoTime()-model.lastStateNanos>5_000_000_000L)status.setText("Esperando actualizaciones del servidor…");
    }
    private void dialogue() {
        if(dialogueBox==null||!entered)return;input.stop();dialogueBox.getChildren().clear();JsonNode d=model.dialogue;dialogueBox.getChildren().addAll(Ui.label(d.path("name").asText(),"hero-title"),Ui.label(d.path("text").asText(),"splash-text"));
        if(d.path("aiEnabled").asBoolean()){TextField question=Ui.input("Pregunta al personaje…",2000);Runnable ask=()->{if(!question.getText().isBlank()){send(Commands.action("npcChat","npcId",d.path("npcId").asText(),"text",question.getText()));question.clear();}};question.setOnAction(e->ask.run());dialogueBox.getChildren().addAll(Ui.label(d.path("reply").asText(),"muted"),Ui.row(question,Ui.button("Preguntar",ask)));}
        String role=d.path("role").asText();if(Set.of("smith","tailor","merchant").contains(role))dialogueBox.getChildren().add(Ui.button(role.equals("merchant")?"Abrir mercado":"Abrir taller",()->{dialogueBox.setVisible(false);drawer.open(role.equals("merchant")?"shop":"crafting");map.setVisible(false);}));if(role.equals("smith"))dialogueBox.getChildren().add(Ui.button("Mejora general · 50 col",()->send(Commands.action("forgeUpgrade"))));dialogueBox.getChildren().add(Ui.button("Continuar →",()->{dialogueBox.setVisible(false);model.dialogue=Json.EMPTY;world.requestFocus();}));dialogueBox.setVisible(true);
    }
    private void keyDown(KeyEvent event) {
        if(event.getCode()==KeyCode.F11){stage.setFullScreen(!stage.isFullScreen());event.consume();return;}
        if(!entered||world==null)return;
        if(event.getCode()==KeyCode.ESCAPE){input.stop();drawer.hide();map.setVisible(true);dialogueBox.setVisible(false);model.dialogue=Json.EMPTY;world.requestFocus();event.consume();return;}
        Node focus=scene.getFocusOwner();if(focus instanceof TextInputControl||focus instanceof ComboBoxBase<?>||focus instanceof Spinner<?>||!world.isFocused())return;
        String key=event.getCode().name();boolean first=input.press(key);if(Set.of("W","A","S","D","UP","DOWN","LEFT","RIGHT","SPACE","Q","T","E","R","F","DIGIT1","DIGIT2","DIGIT3","DIGIT4").contains(key))event.consume();if(!first)return;
        switch(key){case "SPACE"->send(Commands.action("attack"));case "Q"->send(Commands.action("attack","skill",true));case "T"->send(Commands.action("racial"));case "E"->send(Commands.action("interact"));case "R"->send(Commands.action("potion"));case "F"->send(Commands.action("portal"));case "DIGIT1","DIGIT2","DIGIT3","DIGIT4"->castSlot(Integer.parseInt(key.substring(5))-1);}
    }
    private void castSlot(int index) {String id=model.me().path("skills").path("slots").path(index).asText();if(!id.isEmpty())send(Commands.action("castSkill","skillId",id));}
    private void send(JsonNode message) {if(client!=null)client.send(message);}
    private void notice(String message) {if(toast==null)return;toast.setText(message);toast.setVisible(!message.isBlank());toastTimer.stop();toastTimer.setOnFinished(e->toast.setVisible(false));toastTimer.playFromStart();}
    private void exitWorld() {input.stop();client.disconnect();entered=false;model.clear();async(client.player("session",null),account->{model.account=account;selection();});if(world!=null){world.close();world=null;}}
    private void logout() {if(client==null)return;async(client.player("logout",Json.object()),r->{client.disconnect();model.account=Json.EMPTY;landing(client.endpoint().origin());});}
    private void accountForm(String mode) {
        input.stop();Stage window=new Stage();window.initOwner(stage);window.setTitle("Aincrad · Mi cuenta");VBox form=Ui.column(Ui.label(mode.equals("account")?"Mi cuenta":mode.equals("register")?"Crear cuenta":"Recuperar cuenta","hero-title"));form.setPadding(new Insets(24));Scene accountScene=new Scene(Ui.scroll(form),540,640);accountScene.getStylesheets().add(AincradApp.class.getResource("/style.css").toExternalForm());window.setScene(accountScene);
        TextField username=Ui.input("Usuario",32),email=Ui.input("Correo",254),token=Ui.input("Token o enlace completo de recuperación",3000);PasswordField current=new PasswordField(),password=new PasswordField(),confirm=new PasswordField();Label result=Ui.label("","muted");
        if(mode.equals("account")){
            form.getChildren().addAll(Ui.label(model.account.path("username").asText()+" · "+model.account.path("email").asText(),"gold"),Ui.field("Contraseña actual",current),Ui.field("Nueva contraseña",password),Ui.field("Repetir contraseña",confirm),Ui.primary("Cambiar contraseña",()->accountMutation("password",Json.object("currentPassword",current.getText(),"password",password.getText(),"confirmPassword",confirm.getText()),window,result)),Ui.field("Nuevo correo",email),Ui.button("Cambiar correo",()->accountMutation("email",Json.object("currentPassword",current.getText(),"email",email.getText()),window,result)),Ui.field("Clave de personaje anterior",token),Ui.button("Vincular personaje anterior",()->async(client.player("claim-character",Json.object("legacyToken",token.getText())).thenCompose(r->client.player("session",null)),r->{model.account=r;result.setText("Personaje vinculado.");})),Ui.button("Cerrar sesión",()->{window.close();logout();}));
        }else{
            if(mode.equals("register"))form.getChildren().add(Ui.field("Usuario",username));if(!mode.equals("reset-password"))form.getChildren().add(Ui.field("Correo",email));if(mode.equals("reset-password"))form.getChildren().add(Ui.field("Token / enlace recibido",token));if(!mode.equals("forgot-password"))form.getChildren().addAll(Ui.field("Contraseña (12–128 caracteres)",password),Ui.field("Repetir contraseña",confirm));
            form.getChildren().add(Ui.primary("Enviar",()->{try{String reset=token.getText();if(reset.contains("reset-password=")){reset=reset.substring(reset.indexOf("reset-password=")+15).split("&")[0];reset=java.net.URLDecoder.decode(reset,java.nio.charset.StandardCharsets.UTF_8);}JsonNode body=mode.equals("register")?Json.object("username",username.getText(),"email",email.getText(),"password",password.getText(),"confirmPassword",confirm.getText()):mode.equals("forgot-password")?Json.object("email",email.getText()):Json.object("token",reset,"password",password.getText(),"confirmPassword",confirm.getText());password.clear();confirm.clear();async(client.player(mode,body),r->{result.setText(r.path("message").asText());if(!mode.equals("forgot-password")){if(entered)exitWorld();}});}catch(Exception bad){result.setText(Json.error(bad));}}));
        }form.getChildren().add(result);window.show();
    }
    private void accountMutation(String path,JsonNode body,Stage window,Label result) {async(client.player(path,body),r->{result.setText(r.path("message").asText());client.disconnect();model.account=Json.EMPTY;window.close();landing(client.endpoint().origin());});}
    private void smoke(String server,String directory) {
        try{bindClient(server);String user=System.getenv("SAO_SMOKE_USER"),password=System.getenv("SAO_SMOKE_PASSWORD");if(user==null||password==null)throw new IllegalArgumentException("Define SAO_SMOKE_USER y SAO_SMOKE_PASSWORD");async(client.player("login",Json.object("username",user,"password",password)).thenCombine(client.world(),(account,w)->Json.object("account",account,"world",w)),r->{model.account=r.path("account");model.world=r.path("world");selectedCharacter=model.account.path("characters").path(0).path("id").asText();appearance=CharacterEditor.defaults(model.world.path("characterOptions"));characterName="JavaVisual";enterWorld();SmokeCapture.start(scene,model,()->drawer,world,this::send,directory);});}catch(Exception bad){notice(Json.error(bad));}
    }
}
