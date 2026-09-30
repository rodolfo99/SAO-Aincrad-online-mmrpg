package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

/** Catalog-independent tree inspector: typed fields and whole-section JSON for admin parity. */
final class JsonEditor extends BorderPane {
    private JsonNode document;
    private final TreeView<Entry> tree=new TreeView<>();
    private final VBox fields=new VBox(10);
    private final TextArea raw=new TextArea();
    private final Label error=Ui.label("","error");
    private Entry selected;
    private final Set<String> invalidFields=new HashSet<>();
    private record Entry(JsonNode parent,String key,JsonNode value) {
        @Override public String toString() {return key+ (value.isObject()?" · "+value.path("name").asText(value.path("id").asText()):value.isArray()?" ["+value.size()+"]":" = "+value.asText());}
    }
    JsonEditor(JsonNode value) {
        document=value.deepCopy();tree.setPrefWidth(265);setLeft(tree);setCenter(Ui.scroll(fields));tree.getSelectionModel().selectedItemProperty().addListener((o,old,item)->{if(item!=null){selected=item.getValue();show();}});reload();
    }
    JsonNode value() {
        if(!invalidFields.isEmpty())throw new IllegalArgumentException("Corrige los campos inválidos: "+String.join(", ",invalidFields));
        try{if(selected!=null&&!raw.getText().equals(Ui.pretty(selected.value))){JsonNode parsed=Json.MAPPER.readTree(raw.getText());if(parsed==null)throw new IllegalArgumentException("Sección vacía");replace(selected,parsed);}return document.deepCopy();}
        catch(Exception bad){error.setText("JSON inválido: "+Json.error(bad));throw new IllegalArgumentException(error.getText(),bad);}
    }
    void setValue(JsonNode value) {document=value.deepCopy();reload();}
    private void reload() {TreeItem<Entry> root=item(null,"Configuración",document);root.setExpanded(true);tree.setRoot(root);tree.getSelectionModel().select(root);}
    private TreeItem<Entry> item(JsonNode parent,String key,JsonNode value) {
        TreeItem<Entry> item=new TreeItem<>(new Entry(parent,key,value));
        if(value.isObject())value.fields().forEachRemaining(e->item.getChildren().add(item(value,e.getKey(),e.getValue())));
        else if(value.isArray())for(int i=0;i<value.size();i++)item.getChildren().add(item(value,Integer.toString(i),value.get(i)));
        return item;
    }
    private void show() {
        fields.getChildren().clear();error.setText("");invalidFields.clear();JsonNode value=selected.value;fields.getChildren().add(Ui.label(selected.key,"panel-title"));
        if(value.isObject())value.fields().forEachRemaining(e->{if(e.getValue().isContainerNode())return;String key=e.getKey();JsonNode original=e.getValue();
            if(original.isBoolean()){CheckBox b=new CheckBox(key);b.setSelected(original.asBoolean());b.setOnAction(event->{((ObjectNode)value).put(key,b.isSelected());raw.setText(Ui.pretty(value));});fields.getChildren().add(b);}
            else{TextField field=new TextField(original.asText(""));field.textProperty().addListener((o,old,text)->setPrimitive((ObjectNode)value,key,original,text));fields.getChildren().add(Ui.field(key,field));}
        });
        raw.setPrefRowCount(18);raw.setWrapText(false);raw.setText(Ui.pretty(value));
        fields.getChildren().addAll(Ui.label("Sección JSON (para objetos, listas y campos avanzados)","muted"),raw,Ui.primary("Actualizar esta sección",()->{try{JsonNode parsed=Json.MAPPER.readTree(raw.getText());if(parsed==null)throw new IllegalArgumentException("Sección vacía");replace(selected,parsed);reload();}catch(Exception bad){error.setText(Json.error(bad));}}));
        if(selected.parent!=null&&selected.parent.isArray()){fields.getChildren().add(Ui.row(Ui.button("Duplicar elemento",()->{((ArrayNode)selected.parent).add(selected.value.deepCopy());reload();}),Ui.button("Eliminar elemento",()->{((ArrayNode)selected.parent).remove(Integer.parseInt(selected.key));reload();})));}
        if(value.isArray())fields.getChildren().add(Ui.button("Añadir objeto",()->{((ArrayNode)value).addObject();reload();}));
        fields.getChildren().add(error);
    }
    private void setPrimitive(ObjectNode parent,String key,JsonNode original,String text) {
        try{if(original.isIntegralNumber())parent.put(key,Long.parseLong(text));else if(original.isFloatingPointNumber()){double number=Double.parseDouble(text);if(!Double.isFinite(number))throw new IllegalArgumentException("Número finito requerido");parent.put(key,number);}else parent.put(key,text);invalidFields.remove(key);raw.setText(Ui.pretty(selected.value));error.setText("");}catch(Exception invalid){invalidFields.add(key);error.setText("Campo "+key+": valor inválido");}
    }
    private void replace(Entry entry,JsonNode value) {if(entry.parent==null)document=value;else if(entry.parent.isObject())((ObjectNode)entry.parent).set(entry.key,value);else ((ArrayNode)entry.parent).set(Integer.parseInt(entry.key),value);}
}
