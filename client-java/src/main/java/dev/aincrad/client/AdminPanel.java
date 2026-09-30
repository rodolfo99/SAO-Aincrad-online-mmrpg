package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Native administrator using only the original /api/admin endpoints and server backups. */
final class AdminPanel {
    private final GameClient client;
    private final Stage stage=new Stage();
    private final BorderPane root=new BorderPane();
    private final Label status=Ui.label("","muted");
    private final ProgressIndicator busy=new ProgressIndicator();
    private JsonNode draft=Json.EMPTY,ai=Json.EMPTY,characters=Json.EMPTY,accounts=Json.EMPTY;
    AdminPanel(GameClient client,Stage owner) {
        this.client=client;stage.initOwner(owner);stage.setTitle("Aincrad · Administración Java");Scene scene=new Scene(root,1050,760);scene.getStylesheets().add(Ui.class.getResource("/style.css").toExternalForm());root.setPadding(new Insets(18));busy.setPrefSize(24,24);busy.setVisible(false);root.setBottom(Ui.row(busy,status));stage.setOnCloseRequest(e->client.admin("logout","POST",null));
        login();stage.show();
    }
    private <T> void run(CompletableFuture<T> future,Consumer<T> done) {
        root.getCenter().setDisable(true);busy.setVisible(true);status.setText("Procesando…");future.whenComplete((value,error)->Platform.runLater(()->{root.getCenter().setDisable(false);busy.setVisible(false);if(error!=null)status.setText(Json.error(error));else{status.setText(value instanceof JsonNode n?n.path("message").asText("Operación completada"):"Operación completada");done.accept(value);}}));
    }
    private void runJson(JsonEditor editor,java.util.function.Function<JsonNode,CompletableFuture<JsonNode>> request,Consumer<JsonNode> done) {
        try{JsonNode value=editor.value();run(request.apply(value),result->{editor.setValue(value);done.accept(result);});}catch(RuntimeException error){status.setText(Json.error(error));}
    }
    private void login() {
        PasswordField password=new PasswordField();password.setPromptText("Contraseña de root");VBox form=Ui.card(Ui.label("Administración del mundo","hero-title"),Ui.label("Conectada a "+client.endpoint().origin(),"muted"),Ui.field("Usuario: root",password),Ui.primary("Iniciar sesión",()->{String secret=password.getText();password.clear();run(client.admin("login","POST",Json.object("username","root","password",secret)),result->load());}));form.setMaxWidth(460);root.setCenter(new StackPane(form));
    }
    private void load() {
        run(client.admin("world","GET",null).thenCompose(w->{draft=w;return client.admin("ai","GET",null);}).thenCompose(a->{ai=a;return client.admin("characters","GET",null);}).thenCompose(c->{characters=c;return client.admin("accounts","GET",null);}),a->{accounts=a;build();});
    }
    private Tab tab(String title,javafx.scene.Node child) {Tab tab=new Tab(title,child);tab.setClosable(false);return tab;}
    private void build() {
        TabPane tabs=new TabPane();JsonEditor world=new JsonEditor(draft);
        VBox worldPage=Ui.column(Ui.label("Semilla, pisos, NPC, portales, edificios, zonas, razas, armas, armaduras, talentos, habilidades, PvP y oficios.","muted"),world,Ui.row(Ui.primary("Guardar y validar",()->runJson(world,value->client.admin("world","PUT",value),r->{})),Ui.button("Aplicar y reconectar",()->run(client.admin("reload","POST",null),r->load()))));VBox.setVgrow(world,Priority.ALWAYS);
        JsonEditor aiEditor=new JsonEditor(ai);
        TextField npc=Ui.input("ID del NPC (por ejemplo lyra)",64);
        TextField question=Ui.input("Pregunta de prueba",2000);
        Label answer=Ui.label("","muted");
        Button testAi=Ui.button("Probar conversación",()->runJson(aiEditor,value->
            client.admin("ai/test","POST",Json.object("config",value,"npcId",npc.getText(),"text",question.getText())),
            r->answer.setText(r.path("text").asText()+(r.path("fallback").asBoolean()?" · respaldo: "+r.path("reason").asText():""))));
        VBox aiPage=Ui.column(aiEditor,
            Ui.row(Ui.primary("Guardar IA",()->runJson(aiEditor,value->client.admin("ai","PUT",value),r->{})),
                   Ui.button("Modelos de Ollama",()->runJson(aiEditor,value->client.admin("ai/models","POST",value),r->answer.setText(Ui.pretty(r))))),
            Ui.field("NPC",npc),Ui.field("Mensaje",question),testAi,answer);
        VBox.setVgrow(aiEditor,Priority.ALWAYS);
        tabs.getTabs().addAll(tab("Mundo y catálogos",worldPage),tab("Personajes",characters()),tab("Cuentas",accounts()),tab("NPC / Ollama",aiPage),tab("Correo / servidor",mail()),tab("Contraseña root",password()));root.setCenter(tabs);
        root.setTop(Ui.row(Ui.label("AINCRAD · ROOT","gold"),Ui.button("Actualizar datos",this::load),Ui.button("Cerrar sesión",()->run(client.admin("logout","POST",null),r->login()))));
    }
    private javafx.scene.Node characters() {
        ComboBox<Ui.Choice> selected=Ui.choices(characters,"");VBox form=new VBox(12);BorderPane page=new BorderPane();page.setTop(Ui.field("Personaje del mundo (conectado o desconectado)",selected));page.setCenter(Ui.scroll(form));
        Runnable fill=()->{
            form.getChildren().clear();JsonNode p=Json.find(characters,Ui.selected(selected));if(p.isMissingNode())return;String id=p.path("id").asText();
            JsonEditor editor=new JsonEditor(Json.object("name",p.path("name"),"appearance",p.path("appearance"),"xp",p.path("xp"),"col",p.path("col"),"potions",p.path("potions"),"attributeRanks",p.path("attributeRanks"),"talentRanks",p.path("talentRanks")));editor.setPrefHeight(440);
            form.getChildren().addAll(editor,Ui.primary("Guardar personaje con respaldo",()->runJson(editor,value->client.admin("characters/"+id,"PATCH",value),r->load())));
            ComboBox<Ui.Choice> account=Ui.choices(accounts,p.path("accountId").asText());form.getChildren().addAll(Ui.field("Cuenta propietaria",account),Ui.button("Vincular a la cuenta",()->run(client.admin("characters/"+id+"/account","PATCH",Json.object("accountId",Ui.selected(account))),r->load())));
            ComboBox<Ui.Choice> weapon=Ui.choices(draft.path("characterOptions").path("weaponSets"),p.path("weaponSetId").asText());weapon.getItems().add(0,new Ui.Choice("","Automático por nivel"));if(p.path("weaponSetId").asText().isEmpty())weapon.getSelectionModel().select(0);form.getChildren().addAll(Ui.field("Conjunto de armas",weapon),Ui.button("Guardar armas",()->run(client.admin("characters/"+id+"/weapons","PATCH",Json.object("weaponSetId",Ui.selected(weapon))),r->load())));
            Spinner<Integer> citizenship=Ui.integer(draft.path("pvp").path("minCitizenship").asInt(-100),draft.path("pvp").path("maxCitizenship").asInt(100),p.path("pvp").path("citizenship").asInt());CheckBox pardon=new CheckBox("Indultar marca de asesino");form.getChildren().addAll(Ui.field("Ciudadanía",citizenship),pardon,Ui.button("Guardar ciudadanía",()->run(client.admin("characters/"+id+"/citizenship","PATCH",Json.object("citizenship",citizenship.getValue(),"pardon",pardon.isSelected())),r->load())));
            JsonEditor crafting=new JsonEditor(Json.object("professionXp",p.path("crafting").path("professionXp"),"materials",p.path("crafting").path("materials")));crafting.setPrefHeight(320);form.getChildren().addAll(Ui.label("Experiencia de oficios y materiales","gold"),crafting,Ui.button("Guardar oficios con respaldo",()->runJson(crafting,value->client.admin("characters/"+id+"/crafting","PATCH",value),r->load())));
        };selected.setOnAction(e->fill.run());fill.run();return page;
    }
    private javafx.scene.Node accounts() {
        VBox page=new VBox(12);ComboBox<Ui.Choice> choice=new ComboBox<>();for(JsonNode a:Json.list(accounts))choice.getItems().add(new Ui.Choice(a.path("id").asText(),a.path("username").asText()+" · "+a.path("email").asText()));choice.getSelectionModel().selectFirst();PasswordField password=new PasswordField();Label detail=Ui.label("","muted");choice.setOnAction(e->detail.setText(Ui.pretty(Json.find(accounts,Ui.selected(choice)))));page.getChildren().addAll(Ui.field("Cuenta registrada",choice),detail,Ui.field("Nueva contraseña",password),Ui.primary("Restablecer contraseña del jugador",()->{String secret=password.getText();password.clear();run(client.admin("accounts/"+Ui.selected(choice)+"/password","POST",Json.object("password",secret)),r->{});}));return Ui.scroll(page);
    }
    private javafx.scene.Node mail() {
        VBox page=new VBox(12);Label health=Ui.label("","muted"),mail=Ui.label("","muted");TextField recipient=Ui.input("Correo destinatario",254);page.getChildren().addAll(Ui.button("Consultar servidor y correo",()->run(client.health().thenCombine(client.admin("mail","GET",null),(h,m)->Json.object("health",h,"mail",m)),r->{health.setText(Ui.pretty(r.path("health")));mail.setText(Ui.pretty(r.path("mail")));})),health,mail,Ui.field("Correo de prueba",recipient),Ui.button("Enviar correo de prueba",()->run(client.admin("mail/test","POST",Json.object("email",recipient.getText())),r->{})));return Ui.scroll(page);
    }
    private javafx.scene.Node password() {PasswordField password=new PasswordField();return Ui.column(Ui.field("Nueva contraseña root (12–128 caracteres)",password),Ui.primary("Cambiar contraseña root",()->{String secret=password.getText();password.clear();run(client.admin("password","POST",Json.object("password",secret)),r->login());}));}
}
