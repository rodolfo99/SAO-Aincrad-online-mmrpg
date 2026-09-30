package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.asset.AssetManager;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.CullFace;
import javafx.scene.transform.Affine;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Original Angular geometry, loaded as native Java meshes. No browser, JS engine or network art. */
final class ArtLibrary {
    private static final class Pack {
        static final JsonNode DATA=read();
        private static JsonNode read() {
            try(InputStream source=ArtLibrary.class.getResourceAsStream("/art/models.json.gz")) {
                if(source==null)throw new IOException("Falta art/models.json.gz");
                JsonNode data=Json.MAPPER.readTree(new GZIPInputStream(source));
                if(data.path("format").asInt()!=1)throw new IOException("Formato de arte desconocido");return data;
            }catch(IOException error){throw new UncheckedIOException("No se pudo cargar el arte original",error);}
        }
    }
    static final class Rig {
        final Node root;final Map<String,Node> joints=new HashMap<>();final Map<String,Quaternion> rest=new HashMap<>();
        final boolean slime,quadruped;float height;
        Rig(Node root,JsonNode model){this.root=root;slime=model.path("slime").asBoolean();quadruped=model.path("quadruped").asBoolean();height=(float)model.path("height").asDouble(2.8)*root.getLocalScale().y;}
        void animate(float time,boolean moving,float attack) {
            float phase=time*(quadruped?10:9.2f);
            Node body=joints.get("body");if(body!=null)body.getLocalTranslation().y=slime?Math.max(0,FastMath.sin(time*(moving?9:3)))*(moving?.14f:.025f):moving?Math.abs(FastMath.sin(phase))*.018f:FastMath.sin(time*1.8f)*.004f;
            if(slime&&body!=null){float pulse=FastMath.sin(time*(moving?9:3));body.setLocalScale(1+pulse*.05f,1-pulse*.09f,1+pulse*.05f);}
            for(int i=0;i<4;i++){float cycle=FastMath.sin(phase+i*FastMath.PI);rotate("leg"+i,moving?cycle*.40f:0,0);rotate("knee"+i,moving?Math.max(0,-cycle)*.54f:0,0);}
            for(int i=0;i<2;i++){float cycle=FastMath.sin(phase+i*FastMath.PI);rotate("arm"+i,(moving?-cycle*.28f:0)-(i==1?attack*1.4f:0),0);rotate("elbow"+i,moving?-Math.max(0,cycle)*.17f:0,0);}
            rotate("head",0,FastMath.sin(time*.7f)*.025f);rotate("tail",0,FastMath.sin(time*(moving?4:1.2f))*.15f);rotate("cloak",FastMath.sin(time*2.2f)*.013f+(moving?.06f:0),0);
        }
        private void rotate(String name,float x,float y){Node n=joints.get(name);if(n!=null)n.setLocalRotation(rest.get(name).mult(new Quaternion().fromAngles(x,y,0)));}
    }
    private final AssetManager assets;
    private final Map<String,Mesh> meshes=new HashMap<>();
    private final Map<String,Material> materials=new HashMap<>();
    private TextureCubeMap reflections;
    private final Map<String,Texture2D> surfaceMaps=new HashMap<>();
    ArtLibrary(AssetManager assets){this.assets=assets;}
    static JsonNode model(String key){JsonNode m=Pack.DATA.path("models").path(key);if(m.isMissingNode())throw new IllegalArgumentException("Modelo visual desconocido: "+key);return m;}
    static int modelCount(){return Pack.DATA.path("models").size();}
    Rig avatar(JsonNode look,JsonNode catalog,JsonNode player) {
        JsonNode outfit=outfit(look,catalog,player),race=Json.find(catalog.path("races"),look.path("race").asText());
        String key=avatarKey(look,race,outfit);Map<String,ColorRGBA> colors=palette(look,race,outfit);Rig rig=load(key,colors);
        float scale=(float)race.path("scale").asDouble(1),width=(float)race.path("width").asDouble(1);rig.root.setLocalScale(scale*width,scale,scale);rig.height=(float)model(key).path("height").asDouble(2.8)*scale;
        JsonNode weapons=weapons(look,catalog,player);weapon(rig,"hand1",weapons.path("mainHand"),1);weapon(rig,"hand0",weapons.path("offHand"),-1);return rig;
    }
    private void weapon(Rig rig,String hand,JsonNode item,int side) {
        if(!item.isObject()||!rig.joints.containsKey(hand))return;String kind=item.path("kind").asText("sword");
        if(!Set.of("sword","greatsword","dagger","staff","wand","shield","orb","tome").contains(kind))kind="sword";
        Map<String,ColorRGBA> colors=Map.of("weaponPrimary",srgb(item.path("primary").asText("#cedae1")),"weaponSecondary",srgb(item.path("secondary").asText("#b2935e")),"weaponGlow",srgb(item.path("glow").asText("#6edbd1")));
        Rig weapon=load("weapon/"+kind+"/"+(item.path("hands").asInt(1)==2?2:1)+"/"+side,colors);rig.joints.get(hand).attachChild(weapon.root);
    }
    Rig load(String key){return load(key,Map.of());}
    Rig creature(JsonNode value){String key="creature/"+value.path("model").asText("boar");if(!Pack.DATA.path("models").has(key))key="creature/guardian";ColorRGBA color=srgb(value.path("color").asText("#788b61"));return load(key,Map.of("creatureSkin",color,"creatureShade",color.mult(.73f),"creatureLight",color.mult(1.2f)));}
    Rig resource(JsonNode type){String key="resource/"+type.path("id").asText();if(!Pack.DATA.path("models").has(key))key=type.path("kind").asText().equals("tree")?"resource/oak-tree":"resource/copper-vein";return load(key,Map.of("resourceTint",srgb(type.path("color").asText("#839589"))));}
    private Rig load(String key,Map<String,ColorRGBA> colors){JsonNode model=model(key);Rig rig=new Rig(new Node(key),model);Node visual=node(model.path("node"),colors,rig);rig.root.setLocalTransform(visual.getLocalTransform());for(Spatial child:new ArrayList<>(visual.getChildren()))rig.root.attachChild(child);rig.height=(float)model.path("height").asDouble(2.8)*rig.root.getLocalScale().y;return rig;}
    private Node node(JsonNode value,Map<String,ColorRGBA> colors,Rig rig) {
        Node n=new Node(value.path("name").asText("art"));transform(n,value);String joint=value.path("r").asText();if(!joint.isEmpty()){rig.joints.put(joint,n);rig.rest.put(joint,n.getLocalRotation().clone());}
        if(value.has("g")){JsonNode spec=Pack.DATA.path("materials").get(value.path("m").asInt());Geometry g=new Geometry("original-art",mesh(value.path("g").asInt(),spec.path("role").asText().equals("skinFace")?colors.get("faceSkin"):null));g.setMaterial(material(spec,colors));g.setShadowMode(value.path("shadow").path(0).asBoolean()?RenderQueue.ShadowMode.CastAndReceive:value.path("shadow").path(1).asBoolean()?RenderQueue.ShadowMode.Receive:RenderQueue.ShadowMode.Off);if(spec.path("transparent").asBoolean())g.setQueueBucket(RenderQueue.Bucket.Transparent);n.attachChild(g);}
        for(JsonNode c:Json.list(value.path("c")))n.attachChild(node(c,colors,rig));return n;
    }
    private void transform(Node n,JsonNode v){n.setLocalTranslation(vector(v.path("p"),Vector3f.ZERO));n.setLocalScale(vector(v.path("s"),Vector3f.UNIT_XYZ));JsonNode q=v.path("q");n.setLocalRotation(new Quaternion((float)q.path(0).asDouble(),(float)q.path(1).asDouble(),(float)q.path(2).asDouble(),(float)q.path(3).asDouble(1)));}
    private static Vector3f vector(JsonNode v,Vector3f fallback){return v.isArray()?new Vector3f((float)v.path(0).asDouble(),(float)v.path(1).asDouble(),(float)v.path(2).asDouble()):fallback.clone();}
    private Mesh mesh(int id,ColorRGBA skin) {
        String key=id+":"+(skin==null?"":skin.toString());return meshes.computeIfAbsent(key,k->{JsonNode source=Pack.DATA.path("geometries").get(id);Mesh mesh=new Mesh();float[] positions=floats(source.path("position")),normal=floats(source.path("normal"));mesh.setBuffer(VertexBuffer.Type.Position,3,positions);if(normal.length>0)mesh.setBuffer(VertexBuffer.Type.Normal,3,normal);float[] uv=floats(source.path("uv"));if(uv.length>0)mesh.setBuffer(VertexBuffer.Type.TexCoord,2,uv);int[] index=indices(source.path("index"),positions.length/3);mesh.setBuffer(VertexBuffer.Type.Index,3,index);float[] colors=floats(source.path("color"));if(colors.length>0){if(skin!=null)recolorFace(colors,skin);float[] rgba=new float[colors.length/3*4];for(int i=0;i<colors.length/3;i++){System.arraycopy(colors,i*3,rgba,i*4,3);rgba[i*4+3]=1;}mesh.setBuffer(VertexBuffer.Type.Color,4,rgba);}if(uv.length>0&&normal.length>0)com.jme3.util.mikktspace.MikktspaceTangentGenerator.generate(mesh);mesh.setStatic();mesh.updateBound();return mesh;});
    }
    private Material material(JsonNode spec,Map<String,ColorRGBA> colors) {
        ColorRGBA diffuse=colors.getOrDefault(spec.path("role").asText(),rgba(spec.path("color"),1));float opacity=(float)spec.path("opacity").asDouble(1);ColorRGBA tint=diffuse.clone();tint.a=opacity;
        ColorRGBA glow=rgba(spec.path("emissive"),1);if(spec.path("role").asText().equals("weaponGlow"))glow=diffuse.mult(.3f);
        String key=spec.toString()+tint+glow;ColorRGBA finalGlow=glow;return materials.computeIfAbsent(key,k->{Material m=new Material(assets,"Common/MatDefs/Light/Lighting.j3md");m.setBoolean("UseMaterialColors",true);m.setColor("Diffuse",tint);m.setColor("Ambient",tint.mult(.88f));float metal=(float)spec.path("metalness").asDouble();m.setColor("Specular",new ColorRGBA(.10f,.10f,.10f,1).interpolateLocal(tint,metal*.75f));float roughness=(float)spec.path("roughness").asDouble(.8);m.setFloat("Shininess",4+(1-roughness)*(1-roughness)*180);if(spec.path("vertexColors").asBoolean())m.setBoolean("UseVertexColor",true);String surface=spec.path("surface").asText();if(Set.of("cloth","leather","wood","stone","metal").contains(surface)){m.setTexture("NormalMap",normalTexture(surface));m.setFloat("NormalType",1);}if(finalGlow.r+finalGlow.g+finalGlow.b>.01f)m.setColor("GlowColor",finalGlow);if(metal>.5f){m.setTexture("EnvMap",reflectionMap());m.setVector3("FresnelParams",new Vector3f(.04f,.14f,4));}if(spec.path("doubleSide").asBoolean())m.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);if(spec.path("transparent").asBoolean()){m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);m.getAdditionalRenderState().setDepthWrite(false);}return m;});
    }
    private TextureCubeMap reflectionMap(){if(reflections!=null)return reflections;int size=32;ArrayList<ByteBuffer> faces=new ArrayList<>();for(int side=0;side<6;side++){ByteBuffer pixels=BufferUtils.createByteBuffer(size*size*4);for(int y=0;y<size;y++)for(int x=0;x<size;x++){float t=side==2?1:side==3?0:1-y/(float)(size-1);ColorRGBA c=new ColorRGBA(.20f,.24f,.14f,1).interpolateLocal(new ColorRGBA(.52f,.68f,.87f,1),t);pixels.put((byte)Math.round(c.r*255)).put((byte)Math.round(c.g*255)).put((byte)Math.round(c.b*255)).put((byte)255);}pixels.flip();faces.add(pixels);}reflections=new TextureCubeMap(new Image(Image.Format.RGBA8,size,size,0,faces,ColorSpace.sRGB));reflections.setMagFilter(Texture.MagFilter.Bilinear);reflections.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);return reflections;}
    private Texture2D normalTexture(String kind) {
        return surfaceMaps.computeIfAbsent(kind,key->{int size=128;ByteBuffer pixels=BufferUtils.createByteBuffer(size*size*4);for(int y=0;y<size;y++)for(int x=0;x<size;x++){float gx,gy;if(kind.equals("cloth")){gx=FastMath.sin(x*FastMath.TWO_PI/8)*.16f;gy=FastMath.sin(y*FastMath.TWO_PI/8)*.16f;}else if(kind.equals("wood")){gx=FastMath.sin(x*.34f+FastMath.sin(y*.08f))*.12f;gy=FastMath.sin(y*.08f)*.02f;}else{gx=FastMath.sin(x*1.7f+y*.35f)*.045f;gy=FastMath.cos(y*1.8f+x*.4f)*.045f;}Vector3f normal=new Vector3f(-gx,-gy,1).normalizeLocal();pixels.put((byte)Math.round((normal.x*.5f+.5f)*255)).put((byte)Math.round((normal.y*.5f+.5f)*255)).put((byte)Math.round((normal.z*.5f+.5f)*255)).put((byte)255);}pixels.flip();Texture2D texture=new Texture2D(new Image(Image.Format.RGBA8,size,size,pixels,ColorSpace.Linear));texture.setWrap(Texture.WrapMode.Repeat);texture.setMagFilter(Texture.MagFilter.Bilinear);texture.setMinFilter(Texture.MinFilter.Trilinear);return texture;});
    }
    static Group preview(JsonNode look,JsonNode catalog,JsonNode player) {
        JsonNode outfit=outfit(look,catalog,player),race=Json.find(catalog.path("races"),look.path("race").asText());Map<String,ColorRGBA> colors=palette(look,race,outfit);Map<String,Group> joints=new HashMap<>();Group root=fxNode(model(avatarKey(look,race,outfit)).path("node"),colors,joints);float scale=(float)race.path("scale").asDouble(1),width=(float)race.path("width").asDouble(1);root.getTransforms().clear();root.setScaleX(scale*width);root.setScaleY(scale);root.setScaleZ(scale);JsonNode weapons=weapons(look,catalog,player);fxWeapon(joints,"hand1",weapons.path("mainHand"),1);fxWeapon(joints,"hand0",weapons.path("offHand"),-1);return root;
    }
    private static void fxWeapon(Map<String,Group> joints,String hand,JsonNode item,int side){if(!item.isObject()||!joints.containsKey(hand))return;String kind=item.path("kind").asText("sword");if(!Set.of("sword","greatsword","dagger","staff","wand","shield","orb","tome").contains(kind))kind="sword";Map<String,ColorRGBA> palette=Map.of("weaponPrimary",srgb(item.path("primary").asText("#cedae1")),"weaponSecondary",srgb(item.path("secondary").asText("#b2935e")),"weaponGlow",srgb(item.path("glow").asText("#6edbd1")));joints.get(hand).getChildren().add(fxNode(model("weapon/"+kind+"/"+(item.path("hands").asInt(1)==2?2:1)+"/"+side).path("node"),palette,new HashMap<>()));}
    private static Group fxNode(JsonNode value,Map<String,ColorRGBA> colors,Map<String,Group> joints){
        Group group=new Group();if(value.has("r"))joints.put(value.path("r").asText(),group);JsonNode q=value.path("q");Matrix3f r=new Quaternion((float)q.path(0).asDouble(),(float)q.path(1).asDouble(),(float)q.path(2).asDouble(),(float)q.path(3).asDouble(1)).toRotationMatrix();Vector3f s=vector(value.path("s"),Vector3f.UNIT_XYZ),p=vector(value.path("p"),Vector3f.ZERO);group.getTransforms().add(new Affine(r.get(0,0)*s.x,-r.get(0,1)*s.y,-r.get(0,2)*s.z,p.x,-r.get(1,0)*s.x,r.get(1,1)*s.y,r.get(1,2)*s.z,-p.y,-r.get(2,0)*s.x,r.get(2,1)*s.y,r.get(2,2)*s.z,-p.z));
        if(value.has("g")){JsonNode source=Pack.DATA.path("geometries").get(value.path("g").asInt()),spec=Pack.DATA.path("materials").get(value.path("m").asInt());float[] positions=floats(source.path("position")),normals=floats(source.path("normal")),uv=floats(source.path("uv"));for(int i=0;i<positions.length;i+=3){positions[i+1]*=-1;positions[i+2]*=-1;}for(int i=0;i<normals.length;i+=3){normals[i+1]*=-1;normals[i+2]*=-1;}if(uv.length==0)uv=new float[]{0,0};int[] idx=indices(source.path("index"),positions.length/3),faces=new int[idx.length*3];for(int i=0;i<idx.length;i++){faces[i*3]=idx[i];faces[i*3+1]=idx[i];faces[i*3+2]=uv.length>2?idx[i]:0;}TriangleMesh mesh=new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);mesh.getPoints().setAll(positions);mesh.getNormals().setAll(normals);mesh.getTexCoords().setAll(uv);mesh.getFaces().setAll(faces);MeshView view=new MeshView(mesh);ColorRGBA color=spec.path("role").asText().equals("skinFace")?colors.getOrDefault("faceSkin",rgba(spec.path("color"),1)):colors.getOrDefault(spec.path("role").asText(),rgba(spec.path("color"),1));PhongMaterial m=new PhongMaterial(fxColor(color));m.setSpecularColor(javafx.scene.paint.Color.web("#aab3bc"));m.setSpecularPower(12+80*(1-spec.path("roughness").asDouble(.8)));view.setMaterial(m);if(spec.path("doubleSide").asBoolean())view.setCullFace(CullFace.NONE);view.setOpacity(spec.path("opacity").asDouble(1));group.getChildren().add(view);}
        for(JsonNode c:Json.list(value.path("c")))group.getChildren().add(fxNode(c,colors,joints));return group;
    }
    private static String avatarKey(JsonNode look,JsonNode race,JsonNode outfit){String raceId=race.path("model").asText(look.path("race").asText("human"));if(!Set.of("human","dwarf","elf","darkelf","draconian").contains(raceId))raceId="human";String gender=look.path("gender").asText("male"),face=look.path("face").asText("calm"),style=outfit.path("style").asText("leather");if(!Set.of("calm","bold","kind").contains(face))face="calm";if(!Set.of("leather","mail","plate","tunic").contains(style))style="leather";return "avatar/"+raceId+"/"+(gender.equals("female")?"female":"male")+"/"+face+"/"+style+"/"+(outfit.path("helmet").asBoolean()?1:0)+"/"+(outfit.path("cape").asBoolean()?1:0);}
    static JsonNode outfit(JsonNode look,JsonNode catalog,JsonNode player){if(player.path("outfit").isObject())return player.path("outfit");JsonNode p=Json.object("appearance",look);return Json.list(catalog.path("equipmentSets")).stream().filter(o->Catalog.fits(catalog,o,p,true)&&o.path("minLevel").asInt(1)<=player.path("level").asInt(1)).max(Comparator.comparingInt(o->o.path("minLevel").asInt(1))).orElse(Json.object("style","leather","primary","#326c65","metal","#d7d6c6","accent","#b8995a","cape",true));}
    static JsonNode weapons(JsonNode look,JsonNode catalog,JsonNode player){if(player.path("weapons").isObject())return player.path("weapons");JsonNode p=Json.object("appearance",look);List<JsonNode> eligible=Json.list(catalog.path("weaponSets")).stream().filter(o->Catalog.fits(catalog,o,p,false)&&o.path("minLevel").asInt(1)<=player.path("level").asInt(1)).toList();JsonNode chosen=Json.find(Json.MAPPER.valueToTree(eligible),player.path("weaponSetId").asText());return chosen.isMissingNode()?eligible.stream().filter(w->w.path("autoEquip").asBoolean()).max(Comparator.comparingInt(w->w.path("minLevel").asInt(1))).orElse(Json.EMPTY):chosen;}
    private static Map<String,ColorRGBA> palette(JsonNode look,JsonNode race,JsonNode outfit){ColorRGBA skin=srgb(look.path("skinColor").asText("#dfb18b")),hair=srgb(look.path("hairColor").asText("#342f32")),metal=srgb(outfit.path("metal").asText("#c6d3d0")),accent=srgb(outfit.path("accent").asText("#c6a365"));Map<String,ColorRGBA> m=new HashMap<>();m.put("skin",skin.mult(.83f));m.put("faceSkin",skin);m.put("lipSkin",skin.clone().interpolateLocal(srgb("#a8534b"),.24f));m.put("lips",skin.clone().interpolateLocal(srgb("#9e4d43"),.62f).mult(.72f));m.put("shadowSkin",skin.mult(.36f));m.put("mouth",skin.mult(.37f));m.put("lash",hair.mult(.4f));m.put("hair",hair.mult(.75f));m.put("hairLight",hair.clone().interpolateLocal(srgb("#d7b28a"),.1f).mult(.8f));m.put("cloth",srgb(outfit.path("primary").asText("#326c65")));m.put("cloak",m.get("cloth"));m.put("leather",srgb(look.path("specializationId").asText().equals("assassin")?"#292e35":"#514137"));m.put("trousers",srgb(look.path("classId").asText().equals("healer")?"#8b8677":"#30373b"));m.put("metal",metal);m.put("gold",accent);m.put("trim",accent);m.put("iris",srgb(race.path("eyeColor").asText("#43604b")));m.put("gem",m.get("iris"));m.put("scale",skin.mult(.76f));m.put("membrane",skin.clone().interpolateLocal(srgb("#c9ac7c"),.47f));return m;}
    private static void recolorFace(float[] colors,ColorRGBA skin){ColorRGBA warm=srgb("#a7483e");for(int i=0;i<colors.length;i+=3){float b=(colors[i]-colors[i+1])/(warm.r-warm.g),a=colors[i]-b*warm.r;colors[i]=a*skin.r+b*warm.r;colors[i+1]=a*skin.g+b*warm.g;colors[i+2]=a*skin.b+b*warm.b;}}
    static ColorRGBA srgb(String hex){javafx.scene.paint.Color c=Models.color(hex);return new ColorRGBA().setAsSrgb((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),1);}
    private static ColorRGBA rgba(JsonNode values,float alpha){return new ColorRGBA((float)values.path(0).asDouble(),(float)values.path(1).asDouble(),(float)values.path(2).asDouble(),alpha);}
    private static javafx.scene.paint.Color fxColor(ColorRGBA c){return new javafx.scene.paint.Color(toSrgb(c.r),toSrgb(c.g),toSrgb(c.b),1);}
    private static double toSrgb(float x){return Math.max(0,Math.min(1,x<=.0031308f?x*12.92:1.055*Math.pow(x,1/2.4)-.055));}
    private static float[] floats(JsonNode base64){if(!base64.isTextual())return new float[0];ByteBuffer b=ByteBuffer.wrap(Base64.getDecoder().decode(base64.asText())).order(ByteOrder.LITTLE_ENDIAN);float[] v=new float[b.remaining()/4];b.asFloatBuffer().get(v);return v;}
    private static int[] indices(JsonNode base64,int count){if(!base64.isTextual()){int[] v=new int[count];for(int i=0;i<count;i++)v[i]=i;return v;}ByteBuffer b=ByteBuffer.wrap(Base64.getDecoder().decode(base64.asText())).order(ByteOrder.LITTLE_ENDIAN);int[] v=new int[b.remaining()/4];b.asIntBuffer().get(v);return v;}
}
