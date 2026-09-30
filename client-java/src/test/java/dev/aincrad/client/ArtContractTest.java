package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.*;
import org.junit.jupiter.api.Test;
import java.nio.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Checks the imported art without requiring a GPU or simulating server state. */
class ArtContractTest {
    @Test void everyRaceFaceArmorAndWeaponHasLoadableFiniteNativeGeometry() throws Exception {
        JsonNode world=Json.MAPPER.readTree(getClass().getResourceAsStream("/world.json"));
        ArtLibrary art=new ArtLibrary(new DesktopAssetManager(true));assertTrue(ArtLibrary.modelCount()>500);
        Set<Mesh> checked=Collections.newSetFromMap(new IdentityHashMap<>());
        for(JsonNode race:world.path("characterOptions").path("races"))for(String gender:List.of("male","female"))for(String face:List.of("calm","bold","kind"))for(String style:List.of("leather","mail","plate","tunic"))for(int helmet=0;helmet<2;helmet++)for(int cape=0;cape<2;cape++){
            ArtLibrary.Rig model=art.load("avatar/"+race.path("id").asText()+"/"+gender+"/"+face+"/"+style+"/"+helmet+"/"+cape);
            assertTrue(model.joints.keySet().containsAll(List.of("head","arm0","arm1","leg0","leg1","hand0","hand1")));verify(model.root,checked);
        }
        for(String kind:List.of("sword","greatsword","dagger","staff","wand","shield","orb","tome"))for(int hands:List.of(1,2))for(int side:List.of(-1,1))verify(art.load("weapon/"+kind+"/"+hands+"/"+side).root,checked);
        for(JsonNode species:world.path("monsterSpecies"))verify(art.load("creature/"+species.path("model").asText()).root,checked);
        for(String kind:List.of("house","smithy","tailor","shop"))for(int biome:List.of(0,1))verify(art.load("building/"+biome+"/"+kind).root,checked);
        for(JsonNode resource:world.path("gathering").path("resources"))verify(art.load("resource/"+resource.path("id").asText()).root,checked);
        for(String element:List.of("fire","ice","water","earth","light","shadow","physical"))verify(art.load("summon/"+element).root,checked);
        verify(art.load("portal").root,checked);assertTrue(checked.size()>200);
    }
    @Test void liveCatalogColorsScaleAndEquipmentApplyToNativeAvatar() throws Exception {
        JsonNode world=Json.MAPPER.readTree(getClass().getResourceAsStream("/world.json")),catalog=world.path("characterOptions");
        var look=CharacterEditor.defaults(catalog);look.put("race","draconian");look.put("skinColor","#53677e");look.put("hairColor","#682e28");
        var outfit=Json.object("style","plate","helmet",true,"cape",false,"primary","#304f67","metal","#b9c9cc","accent","#e4c88f");
        var weapons=Json.object("mainHand",Json.object("kind","greatsword","hands",2,"primary","#bbdddd","secondary","#dfc089","glow","#acddff"));
        ArtLibrary.Rig avatar=new ArtLibrary(new DesktopAssetManager(true)).avatar(look,catalog,Json.object("outfit",outfit,"weapons",weapons));
        assertEquals(1.12f,avatar.root.getLocalScale().y,.001);assertTrue(avatar.joints.get("hand1").getChildren().stream().anyMatch(n->n.getName().startsWith("weapon/greatsword")));verify(avatar.root,Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private void verify(Node root,Set<Mesh> checked){int[] geometries={0};root.depthFirstTraversal(s->{if(s instanceof Geometry geometry){geometries[0]++;assertNotNull(geometry.getMaterial());Mesh mesh=geometry.getMesh();if(!checked.add(mesh))return;assertTrue(mesh.getVertexCount()>2);for(var type:List.of(VertexBuffer.Type.Position,VertexBuffer.Type.Normal,VertexBuffer.Type.Tangent,VertexBuffer.Type.Color)){FloatBuffer data=mesh.getFloatBuffer(type);if(data!=null)for(int i=0;i<data.limit();i++)assertTrue(Float.isFinite(data.get(i)),"Non-finite "+type);}var index=mesh.getIndexBuffer();for(int i=0;i<index.size();i++)assertTrue(index.get(i)>=0&&index.get(i)<mesh.getVertexCount());}});assertTrue(geometries[0]>0);}
}
