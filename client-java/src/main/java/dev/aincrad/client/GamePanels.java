package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import java.util.*;
import java.util.function.*;

/** Native game panels: every write uses an existing server command. */
final class GamePanels extends BorderPane {
    private final GameModel model;
    private final Consumer<JsonNode> send;
    private final Runnable close;
    private final ScrollPane scroll=Ui.scroll(new VBox());
    private String current="";
    private String profession="blacksmith";
    private final Map<String,Integer> quantities=new HashMap<>();
    private boolean compatibleOnly=true;
    private VBox content;
    GamePanels(GameModel model,Consumer<JsonNode> send,Runnable close) {
        this.model=model;this.send=send;this.close=close;setPrefWidth(390);setMaxWidth(390);setId("game-drawer");getStyleClass().add("drawer");setCenter(scroll);setVisible(false);
    }
    String current() {return current;}
    void open(String id) {current=id;setVisible(true);build();}
    void hide() {current="";setVisible(false);}
    void refresh() {
        if(!isVisible()||Set.of("character","progression","help","journal","graphics").contains(current))return;
        Node focus=getScene()==null?null:getScene().getFocusOwner();if(focus instanceof TextInputControl||focus instanceof Spinner<?>)return;
        double position=scroll.getVvalue();build();scroll.setVvalue(position);
    }
    private JsonNode me() {return model.me();}
    private JsonNode options() {return model.world.path("characterOptions");}
    private void action(String type,Object... values) {send.accept(Commands.action(type,values));}
    private void move(JsonNode point) {send.accept(Commands.move(Json.num(point,"x"),Json.num(point,"z")));}
    private Button command(String label,String type,Object... values) {return Ui.button(label,()->action(type,values));}
    private void build() {
        content=new VBox(12);content.setPadding(new Insets(18));scroll.setContent(content);
        HBox header=Ui.row(Ui.label(title(),"panel-title"),Ui.button("×",close));header.getChildren().get(0).setStyle("-fx-font-size: 21px;");HBox.setHgrow(header.getChildren().get(0),Priority.ALWAYS);setTop(header);BorderPane.setMargin(header,new Insets(16,16,0,16));
        if(me().isMissingNode()){content.getChildren().add(Ui.label("Esperando el estado del servidor…"));return;}
        switch(current){case "inventory"->inventory();case "bag"->bag();case "shop"->shop();case "crafting"->crafting();case "skills"->skills();case "training"->training();case "zones"->zones();case "character"->character();case "progression"->progression();case "pvp"->pvp();case "journal"->journal();default->help();}
    }
    private String title() {return switch(current){case "inventory"->"Inventario y equipo";case "bag"->"Bolsa de recursos";case "shop"->"Mercado de suministros";case "crafting"->"Oficios y talleres";case "skills"->"Habilidades activas";case "training"->"Patio de entrenamiento";case "zones"->"Zonas de monstruos";case "character"->"Mi personaje";case "progression"->"Atributos y talentos";case "pvp"->"PvP y ciudadanía";case "journal"->"Diario de campo";default->"Controles";};}
    private void add(Node... nodes) {content.getChildren().addAll(nodes);}
    private JsonNode npc(String role) {return Json.list(model.floor().path("npcs")).stream().filter(n->n.path("role").asText().equals(role)).findFirst().orElse(Json.EMPTY);}
    private boolean near(String role) {JsonNode npc=npc(role);return !npc.isMissingNode()&&Json.distance(npc,me())<model.world.path("crafting").path("stationRange").asDouble(4);}
    private void station(String role) {JsonNode npc=npc(role);Button go=Ui.button("Ir a "+npc.path("name").asText("taller"),()->move(npc));go.setDisable(npc.isMissingNode());add(Ui.label(near(role)?"Taller a tu alcance":"Acércate al encargado para operar.","muted"),go);}
    private void inventory() {
        JsonNode p=me();add(Ui.card(Ui.label(p.path("outfit").path("name").asText(),"gold"),Ui.label(Ui.stats(p.path("outfit").path("attributes")),"muted"),Ui.label("Daño "+p.path("damage").asText()+" · defensa "+p.path("defense").asText()+" · crítico "+p.path("critical").asText()+"%"),Ui.label("Velocidad "+p.path("speed").asText()+" · pociones "+p.path("potions").asText())));
        for(String slot:List.of("mainHand","offHand")){JsonNode w=p.path("weapons").path(slot);add(Ui.card(Ui.label((slot.equals("mainHand")?"Principal: ":"Secundaria: ")+w.path("name").asText("Libre")),Ui.label(Ui.stats(w.path("attributes")),"muted")));}
        add(Ui.label("Cambia de equipo en el refugio, fuera de combate.","muted"),command("Automático por nivel","equip","weaponSetId",""));
        for(JsonNode w:Json.list(options().path("weaponSets")))if(Catalog.fits(options(),w,p,false)){Button b=command(w.path("name").asText(),"equip","weaponSetId",w.path("id").asText());b.setDisable(w.path("minLevel").asInt()>p.path("level").asInt());add(b);}
        add(Ui.label("Forja general +"+p.path("weapon").asText()+" · Brann cobra 50 col.","muted"),command("Mejora general · 50 col","forgeUpgrade"));items();
    }
    private Spinner<Integer> quantity(String key,int maximum) {
        Spinner<Integer> q=Ui.integer(1,maximum,quantities.getOrDefault(key,1));q.valueProperty().addListener((o,old,value)->quantities.put(key,value));return q;
    }
    private void bag() {
        station("merchant");JsonNode rules=model.world.path("crafting"),bag=me().path("crafting").path("materials");add(Ui.label(me().path("col").asText()+" col · materiales guardados en tu personaje","gold"));
        for(JsonNode m:Json.list(rules.path("materials"))){int owned=bag.path(m.path("id").asText()).asInt();if(owned<=0)continue;Spinner<Integer> q=quantity("sell-"+m.path("id").asText(),owned);double multiplier=rules.path("saleMultiplier").asDouble();int price=m.hasNonNull("sellPrice")?m.path("sellPrice").asInt():multiplier==0?0:Math.max(1,(int)Math.floor(m.path("price").asInt()*multiplier));Button b=Ui.button("Vender",()->action("shopSellMaterial","materialId",m.path("id").asText(),"quantity",q.getValue()));b.setDisable(!near("merchant")||price<=0);add(Ui.card(Ui.label(m.path("name").asText()+" ×"+owned),Ui.label(price+" col / unidad","muted"),Ui.row(q,b,Ui.button("Todo",()->q.getValueFactory().setValue(owned)))));}
        if(Json.list(rules.path("materials")).stream().noneMatch(m->bag.path(m.path("id").asText()).asInt()>0))add(Ui.label("Tu bolsa está vacía. Recolecta o consigue botín.","muted"));items();
    }
    private void shop() {
        JsonNode rules=model.world.path("crafting");station("merchant");Spinner<Integer> q=quantity("buy",50);add(Ui.field("Unidades por compra",q));
        var materials=new ArrayList<>(Json.list(rules.path("materials")));materials.add(Json.object("id","potion","name","Poción de vida","price",rules.path("potionPrice").asInt()));
        for(JsonNode m:materials){Button buy=Ui.button("Comprar",()->action("shopBuy","materialId",m.path("id").asText(),"quantity",q.getValue()));buy.setDisable(!near("merchant")||me().path("col").asInt()<m.path("price").asInt()*q.getValue());add(Ui.card(Ui.label(m.path("name").asText()+" · "+m.path("price").asText()+" col / unidad"),buy));}
        CheckBox filter=new CheckBox("Sólo equipo para mi personaje");filter.setSelected(compatibleOnly);filter.setOnAction(e->{compatibleOnly=filter.isSelected();build();});add(Ui.label("Armas, ropa y armaduras","panel-title"),filter);
        for(JsonNode product:Json.list(rules.path("shopProducts"))){if(!product.path("enabled").asBoolean())continue;JsonNode source=Catalog.source(options(),product);boolean fits=Catalog.fits(options(),source,me(),product.path("kind").asText().equals("armor"));if(compatibleOnly&&!fits)continue;Button buy=command("Comprar · "+product.path("price").asText()+" col","shopBuyEquipment","productId",product.path("id").asText());buy.setDisable(!near("merchant")||!fits||source.path("minLevel").asInt()>me().path("level").asInt()||me().path("col").asInt()<product.path("price").asInt());add(Ui.card(Ui.label(product.path("name").asText()),Ui.label("Nv. "+source.path("minLevel").asText()+" · "+source.path("classId").asText()+" · "+product.path("slot").asText(),"muted"),buy));}
    }
    private void items() {
        JsonNode inventory=me().path("crafting");add(Ui.label("Equipo en bolsa · "+inventory.path("items").size()+" / "+inventory.path("capacity").asText(),"gold"));
        for(JsonNode item:Json.list(inventory.path("items"))){String id=item.path("id").asText();VBox card=Ui.card(Ui.label(item.path("name").asText()+" +"+item.path("upgrade").asText()),Ui.label(Ui.stats(item.path("weapon").isObject()?item.path("weapon").path("attributes"):item.path("armor").path("attributes")),"muted"));
            FlowPane actions=new FlowPane(6,6);
            if(item.path("armor").isObject())actions.getChildren().add(command("Vestir","craftEquip","itemId",id,"slot","armor"));
            else{if(!Set.of("shield","orb","tome").contains(item.path("weapon").path("kind").asText()))actions.getChildren().add(command("Principal","craftEquip","itemId",id,"slot","mainHand"));if(item.path("weapon").path("hands").asInt()==1)actions.getChildren().add(command("Secundaria","craftEquip","itemId",id,"slot","offHand"));}
            boolean equipped=false;for(String slot:List.of("armor","mainHand","offHand"))if(inventory.path("equipped").path(slot).asText().equals(id)){equipped=true;actions.getChildren().add(command("Desequipar "+slot,"craftUnequip","slot",slot));}
            Button sell=command("Vender","shopSell","itemId",id);sell.setDisable(equipped||!near("merchant"));actions.getChildren().add(sell);
            JsonNode recipe=Json.find(model.world.path("crafting").path("recipes"),item.path("recipeId").asText());if(!recipe.isMissingNode()){Button upgrade=command("Mejorar","craftUpgrade","itemId",id);upgrade.setDisable(item.path("upgrade").asInt()>=recipe.path("maxUpgrade").asInt());actions.getChildren().add(upgrade);card.getChildren().add(Ui.label("Próxima mejora: "+recipe.path("upgradeColCost").asInt()*(item.path("upgrade").asInt()+1)+" col · "+costs(recipe.path("upgradeMaterials"),item.path("upgrade").asInt()+1),"muted"));}
            card.getChildren().add(actions);add(card);
        }
    }
    private String costs(JsonNode costs,int multiplier) {List<String> parts=new ArrayList<>();costs.fields().forEachRemaining(e->parts.add(e.getValue().asInt()*multiplier+" "+Json.find(model.world.path("crafting").path("materials"),e.getKey()).path("name").asText(e.getKey())));return String.join(" · ",parts);}
    private void crafting() {
        JsonNode rules=model.world.path("crafting"),p=me().path("crafting");ComboBox<Ui.Choice> jobs=Ui.choices(rules.path("professions"),profession);jobs.setOnAction(e->{profession=Ui.selected(jobs);build();});add(Ui.field("Oficio",jobs));JsonNode job=Json.find(rules.path("professions"),profession);String role=job.path("stationRole").asText();add(Ui.label("Nv. "+p.path("levels").path(profession).asText("1")+" · "+p.path("professionXp").path(profession).asText("0")+" EXP","gold"),Ui.label(job.path("description").asText(),"muted"));
        if(Set.of("mine","forest").contains(role)){
            for(JsonNode node:Json.list(model.state.path("resources"))){JsonNode type=Json.find(model.world.path("gathering").path("resources"),node.path("typeId").asText());if(!type.path("professionId").asText().equals(profession))continue;Button gather=command("Recolectar","gather","nodeId",node.path("id").asText());gather.setDisable(!node.path("available").asBoolean()||Json.distance(node,me())>model.world.path("gathering").path("range").asDouble(3));Button go=Ui.button("Acercarse",()->approach(node,1.8));add(Ui.card(Ui.label(type.path("name").asText()),Ui.label(Json.number(Json.distance(node,me()))+" m · "+(node.path("available").asBoolean()?"Disponible":"Regenera en "+Json.number(node.path("readyInSeconds").asDouble())+" s"),"muted"),Ui.row(go,gather)));}
        }else{
            station(role);CheckBox filter=new CheckBox("Sólo recetas de mi clase y género");filter.setSelected(compatibleOnly);filter.setOnAction(e->{compatibleOnly=filter.isSelected();build();});add(filter);
            for(JsonNode r:Json.list(rules.path("recipes"))){if(!r.path("professionId").asText().equals(profession))continue;JsonNode source=Catalog.source(options(),r);if(compatibleOnly&&!Catalog.fits(options(),source,me(),r.path("kind").asText().equals("armor")))continue;Button craft=command("Crear","craft","recipeId",r.path("id").asText());craft.setDisable(!near(role)||p.path("levels").path(profession).asInt(1)<r.path("minProfessionLevel").asInt());add(Ui.card(Ui.label(r.path("name").asText()),Ui.label("Oficio Nv. "+r.path("minProfessionLevel").asText()+" · "+r.path("experience").asText()+" EXP","muted"),Ui.label(costs(r.path("materials"),1)+" · "+r.path("colCost").asText()+" col","muted"),craft));}
        }items();
    }
    private void approach(JsonNode point,double gap) {
        double dx=Json.num(me(),"x")-Json.num(point,"x"),dz=Json.num(me(),"z")-Json.num(point,"z"),length=Math.hypot(dx,dz);if(length<.001){dx=1;length=1;}send.accept(Commands.move(Json.num(point,"x")+dx/length*gap,Json.num(point,"z")+dz/length*gap));
    }
    private void skills() {
        JsonNode own=me().path("skills");var learned=Json.MAPPER.createArrayNode();for(JsonNode s:Json.list(options().path("skills")))if(Catalog.contains(own.path("learned"),s.path("id").asText()))learned.add(s);
        add(Ui.label("Aprende talentos en el refugio y asígnalos a las teclas 1–4.","muted"));
        for(int i=0;i<4;i++){int slot=i;ComboBox<Ui.Choice> choices=Ui.choices(learned,own.path("slots").path(i).asText());choices.setOnAction(e->{if(choices.getValue()!=null)action("bindSkill","slot",slot,"skillId",Ui.selected(choices));});add(Ui.field("Tecla "+(i+1),choices));}
        for(JsonNode s:Json.list(learned)){double cd=own.path("cooldowns").path(s.path("id").asText()).asDouble();Button cast=command(cd>0?Json.number(cd)+" s":"Usar","castSkill","skillId",s.path("id").asText());cast.setDisable(cd>0||me().path("hp").asInt()<=0);add(Ui.card(Ui.label(s.path("name").asText(),"gold"),Ui.label(s.path("description").asText(),"muted"),Ui.label("Alcance "+s.path("range").asText()+" · radio "+s.path("radius").asText()+" · recarga "+s.path("cooldown").asText()+" s","muted"),cast));}
        for(JsonNode e:Json.list(me().path("effects")))add(Ui.label(e.isTextual()?e.asText():e.path("name").asText(e.path("id").asText()),"gold"));
    }
    private void training() {
        JsonNode z=model.floor().path("training"),t=me().path("training");if(!z.isObject()){add(Ui.label("Este piso no tiene patio de entrenamiento."));return;}add(Ui.button("Ir al patio",()->move(z)),Ui.card(Ui.label(t.path("dps").asText("0")+" DPS · "+t.path("hps").asText("0")+" HPS","gold"),Ui.label(t.path("damage").asText("0")+" daño · "+t.path("healing").asText("0")+" curación"),Ui.label(t.path("hits").asText("0")+" impactos · "+t.path("criticals").asText("0")+" críticos · "+t.path("buffs").asText("0")+" mejoras","muted")),command("Reiniciar contadores","resetTraining"));
        for(JsonNode m:Json.list(model.state.path("monsters")))if(m.path("training").asBoolean())add(Ui.card(Ui.label(m.path("name").asText()),Ui.label(m.path("hp").asText()+" / "+m.path("maxHp").asText()+" PV · defensa "+m.path("defense").asText()),command("Seleccionar","target","id",m.path("id").asText(),"kind","monster")));
        add(Ui.label("Zona protegida. No concede EXP, col ni materiales.","muted"));
    }
    private void zones() {
        for(JsonNode floor:Json.list(model.world.path("floors"))){add(Ui.label("Piso "+floor.path("id").asText()+" · "+floor.path("name").asText(),"gold"));for(JsonNode z:Json.list(floor.path("monsterZones"))){Button go=Ui.button("Acercarse",()->approach(z,Math.max(0,z.path("radius").asDouble()-1)));go.setDisable(floor.path("id").asInt()!=me().path("floor").asInt());add(Ui.card(Ui.label(z.path("name").asText()),Ui.label("Nv. "+z.path("minLevel").asText()+"–"+z.path("maxLevel").asText()+" · "+z.path("population").asText()+" criaturas","muted"),go));}}
    }
    private void character() {CharacterEditor editor=new CharacterEditor(options(),me(),me().path("name").asText(),me().path("appearance"));editor.setId("profile-editor");add(Ui.label("Edita en el refugio, fuera de combate. El servidor conserva tu progreso.","muted"),editor,Ui.primary("Guardar personaje",()->action("profile","name",editor.name.getText(),"appearance",editor.appearance)));}
    private void progression() {
        JsonNode rules=options().path("progression");ObjectNode attributes=me().path("attributeRanks").deepCopy(),talents=me().path("talentRanks").deepCopy();Label points=Ui.label("","gold");
        List<Spinner<Integer>> ranks=new ArrayList<>();Runnable update=()->{int usedA=0,usedT=0;for(JsonNode v:attributes)usedA+=v.asInt();for(JsonNode t:Json.list(rules.path("talents")))usedT+=talents.path(t.path("id").asText()).asInt()*t.path("cost").asInt();points.setText("Atributos: "+((me().path("level").asInt()-1)*rules.path("attributePointsPerLevel").asInt()-usedA)+" · talentos: "+((me().path("level").asInt()-1)*rules.path("talentPointsPerLevel").asInt()-usedT)+" puntos libres");};update.run();add(points,Ui.label("Guarda en el refugio. Las condiciones y costes son validados por el servidor.","muted"),Ui.label("Atributos","panel-title"));
        for(JsonNode a:Json.list(rules.path("attributes")))ranks.add(rank(a,attributes,rules,update));
        add(Ui.label("Árbol de talentos","panel-title"));for(JsonNode t:Json.list(rules.path("talents")))if(t.path("classId").asText().equals(me().path("appearance").path("classId").asText())&&Catalog.permits(t.path("specializationIds"),me().path("appearance").path("specializationId").asText()))ranks.add(rank(t,talents,rules,update));
        add(Ui.primary("Guardar reparto",()->action("progression","attributes",attributes,"talents",talents)));if(rules.path("allowRespec").asBoolean())add(Ui.button("Restablecer reparto",()->{ranks.forEach(spinner->spinner.getValueFactory().setValue(0));attributes.removeAll();talents.removeAll();update.run();}));
    }
    private Spinner<Integer> rank(JsonNode item,ObjectNode ranks,JsonNode rules,Runnable update) {
        String id=item.path("id").asText();int initial=ranks.path(id).asInt();Spinner<Integer> rank=Ui.integer(rules.path("allowRespec").asBoolean()?0:initial,item.path("maxRank").asInt(),initial);rank.setDisable(item.path("minLevel").asInt()>me().path("level").asInt());rank.valueProperty().addListener((o,old,value)->{if(value==0)ranks.remove(id);else ranks.put(id,value);update.run();});add(Ui.card(Ui.label(item.path("name").asText()),Ui.label(item.path("description").asText(),"muted"),Ui.label(Ui.stats(item.path("effects")),"muted"),Ui.label("Nv. "+item.path("minLevel").asInt(1)+" · coste "+item.path("cost").asInt(1)+" · requiere "+item.path("requires").toString(),"muted"),rank));return rank;
    }
    private void pvp() {
        JsonNode p=me().path("pvp"),r=model.world.path("pvp");add(Ui.label(p.path("status").asText()+" · ciudadanía "+p.path("citizenship").asText(),"gold"),Ui.label(p.path("kills").asText()+" bajas · "+p.path("murders").asText()+" asesinatos · "+p.path("deaths").asText()+" derrotas"),Ui.label("Combate: "+Json.number(p.path("combatSeconds").asDouble())+" s · protección: "+Json.number(p.path("protectedSeconds").asDouble())+" s","muted"),command(me().path("pvpMode").asBoolean()?"Desactivar ataques":"Activar ataques PvP","pvpMode","enabled",!me().path("pvpMode").asBoolean()),Ui.label("Salir durante combate deja el cuerpo expuesto hasta terminar. Cada asesinato resta "+r.path("murderPenalty").asText()+" puntos.","muted"));
        for(JsonNode other:Json.list(model.state.path("players")))if(!other.path("id").asText().equals(model.id))add(command(other.path("name").asText()+" · "+other.path("pvp").path("status").asText(),"target","id",other.path("id").asText(),"kind","player"));add(command("Redención · "+r.path("redemptionCost").asText()+" col","redeem"));
    }
    private void journal() {
        for(JsonNode race:Json.list(options().path("races")))add(Ui.card(Ui.image(race.path("portrait").asText("assets/race-"+race.path("id").asText()+".png"),240),Ui.label(race.path("name").asText(),"gold"),Ui.label(race.path("description").asText(),"muted"),Ui.label(Ui.stats(race.path("effects")),"muted")));
        add(Ui.card(Ui.image("assets/lyra.png",240),Ui.label("Lyra, la cartógrafa","gold")),Ui.card(Ui.image("assets/boar.png",240),Ui.label("Jabalí de la pradera")),Ui.card(Ui.image("assets/oak.png",240),Ui.label("Roble de Aincrad")));
        for(JsonNode species:Json.list(model.world.path("monsterSpecies")))add(Ui.card(Ui.label(species.path("name").asText()),Ui.label("Modelo "+species.path("model").asText(),"muted")));
    }
    private void help() {add(Ui.label("WASD / flechas · mover respecto a la cámara\nClic en suelo / mapa · caminar\nClic en enemigo · seleccionar\nClic en recurso · recolectar si estás cerca\nBotón derecho + arrastrar · girar / inclinar\nRueda · zoom\nEspacio · ataque\nQ · técnica de clase\n1–4 · habilidades aprendidas\nT · habilidad racial\nE · interactuar con NPC\nR · poción\nF · portal\nEsc · parar / cerrar panel\nF11 · pantalla completa","muted"),Ui.label("Las posiciones, colisiones, recargas, recompensas y reglas se ejecutan en el servidor. El movimiento por clic mantiene el comportamiento actual del servidor.","muted"));}
}
