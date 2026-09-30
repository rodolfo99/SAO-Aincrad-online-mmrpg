package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.*;
import com.jme3.light.*;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.post.*;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.shape.*;
import com.jme3.shadow.*;
import com.jme3.system.*;
import com.jme3.texture.*;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.*;
import jme3tools.optimize.GeometryBatchFactory;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** GPU lighting, filtered shadows, SSAO, bloom and original procedural scenery. */
final class JmeWorld extends SimpleApplication {
    record Tag(String id,float x,float y,String text) {}
    record Frame(int width,int height,byte[] pixels,List<Tag> tags) {}
    private record State(JsonNode world,JsonNode snapshot,String id,double yaw,double pitch,double zoom,int width,int height) {}
    private final Consumer<Frame> output;
    private final Consumer<String> error;
    private final Consumer<JsonNode> send;
    private final ConcurrentLinkedQueue<byte[]> freeFrames=new ConcurrentLinkedQueue<>();
    private final Node scenery=new Node("world"),actors=new Node("actors"),fx=new Node("effects");
    private final Map<String,Actor> entities=new LinkedHashMap<>();
    private final Map<String,Node> resources=new HashMap<>();
    private final Map<String,Material> paints=new HashMap<>();
    private ArtLibrary art;
    private SceneProcessor captureProcessor;
    private volatile State incoming=new State(Json.EMPTY,Json.EMPTY,"",0,.62,18,1280,820);
    private State state=incoming;
    private JsonNode built=Json.EMPTY;
    private int floorId=-1;
    private volatile int width=1280,height=820;
    private float time;
    private FrameBuffer buffer;
    private ByteBuffer readBuffer;
    private FilterPostProcessor filters;
    private DirectionalLight sun;
    private DirectionalLightShadowRenderer shadows;
    private PointLight portalLight;
    private final List<Transient> transients=new ArrayList<>();
    private Geometry destination;
    private float destinationUntil;
    private boolean cameraReady,high=true;
    private volatile int quality=2;
    private Vector3f look=new Vector3f();
    private static final class Actor {
        Node root;ArtLibrary.Rig rig;Geometry ring;JsonNode value;String visual,kind;float height,swing;
    }
    private record Transient(Node node,float until) {}
    JmeWorld(Consumer<Frame> output,Consumer<String> error,Consumer<JsonNode> send) {super(new com.jme3.app.StatsAppState());this.output=output;this.error=error;this.send=send;}
    void launch() {
        AppSettings settings=new AppSettings(true);settings.setResolution(width,height);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setUseInput(false);settings.setFrameRate(30);settings.setGammaCorrection(true);settings.setSamples(0);setSettings(settings);setShowSettings(false);setPauseOnLostFocus(false);start(JmeContext.Type.OffscreenSurface);
    }
    void update(JsonNode world,JsonNode snapshot,String id,double yaw,double pitch,double zoom,int width,int height) {double limit=quality==0?1000:quality==1?1600:2560;double scale=Math.min(1,Math.min(limit/Math.max(1,width),(limit*.625)/Math.max(1,height)));incoming=new State(world,snapshot,id,yaw,pitch,zoom,Math.max(64,(int)(width*scale)),Math.max(64,(int)(height*scale)));}
    void quality(int tier) {enqueue(()->{quality=Math.max(0,Math.min(2,tier));this.high=quality==2;postProcessing();built=Json.EMPTY;return null;});}
    void release(Frame frame) {if(frame.width==width&&frame.height==height)freeFrames.offer(frame.pixels);}
    @Override public void handleError(String message,Throwable failure) {error.accept("No se pudo iniciar OpenGL: "+message+". Revisa el controlador y docs/CLIENTE-JAVA.md.");if(failure!=null)failure.printStackTrace();stop(false);}
    @Override public void simpleInitApp() {
        setDisplayFps(false);setDisplayStatView(false);if(flyCam!=null)flyCam.setEnabled(false);rootNode.attachChild(scenery);rootNode.attachChild(actors);rootNode.attachChild(fx);
        art=new ArtLibrary(assetManager);
        rootNode.addLight(new AmbientLight(new ColorRGBA(.57f,.63f,.70f,1)));
        sun=new DirectionalLight(new Vector3f(-.45f,-.83f,-.3f).normalizeLocal(),new ColorRGBA(.95f,.89f,.77f,1));rootNode.addLight(sun);
        rootNode.addLight(new DirectionalLight(new Vector3f(.6f,-.4f,.6f).normalizeLocal(),new ColorRGBA(.10f,.14f,.20f,1)));
        cam.setFrustumPerspective(42,width/(float)height,.1f,380);resize(width,height);postProcessing();
        captureProcessor=new com.jme3.post.SceneProcessor(){
            @Override public void initialize(RenderManager manager,ViewPort port){}
            @Override public void reshape(ViewPort port,int w,int h){}
            @Override public boolean isInitialized(){return true;}
            @Override public void preFrame(float tpf){}
            @Override public void postQueue(RenderQueue queue){}
            @Override public void postFrame(FrameBuffer frame){capture();}
            @Override public void cleanup(){}
            @Override public void setProfiler(com.jme3.profile.AppProfiler profiler){}
        };viewPort.addProcessor(captureProcessor);
    }
    private void resize(int w,int h) {
        if(buffer!=null)renderer.deleteFrameBuffer(buffer);width=w;height=h;cam.resize(w,h,true);cam.setFrustumPerspective(42,w/(float)h,.1f,380);buffer=new FrameBuffer(w,h,1);buffer.setDepthBuffer(Image.Format.Depth);buffer.setColorBuffer(Image.Format.RGBA8);viewPort.setOutputFrameBuffer(buffer);readBuffer=BufferUtils.createByteBuffer(w*h*4);freeFrames.clear();freeFrames.add(new byte[w*h*4]);freeFrames.add(new byte[w*h*4]);if(filters!=null)filters.reshape(viewPort,w,h);
    }
    private void postProcessing() {
        if(captureProcessor!=null)viewPort.removeProcessor(captureProcessor);if(shadows!=null)viewPort.removeProcessor(shadows);if(filters!=null)viewPort.removeProcessor(filters);
        shadows=null;if(quality>0){shadows=new DirectionalLightShadowRenderer(assetManager,high?2048:1024,3);shadows.setLight(sun);shadows.setShadowIntensity(.48f);shadows.setEdgeFilteringMode(EdgeFilteringMode.PCF4);shadows.setShadowZExtend(85);viewPort.addProcessor(shadows);}
        filters=new FilterPostProcessor(assetManager);if(high){SSAOFilter ao=new SSAOFilter(.22f,.85f,.3f,.12f);ao.setApproximateNormals(true);filters.addFilter(ao);}BloomFilter bloom=new BloomFilter(BloomFilter.GlowMode.Objects);bloom.setBloomIntensity(.75f);bloom.setBlurScale(1.7f);filters.addFilter(bloom);filters.addFilter(new FXAAFilter());FogFilter fog=new FogFilter();fog.setFogColor(new ColorRGBA(.68f,.78f,.80f,1));fog.setFogDistance(135);fog.setFogDensity(.35f);filters.addFilter(fog);viewPort.addProcessor(filters);if(captureProcessor!=null)viewPort.addProcessor(captureProcessor);
    }
    @Override public void simpleUpdate(float dt) {
        state=incoming;time+=Math.min(dt,.1f);if(state.width!=width||state.height!=height)resize(state.width,state.height);
        if(state.snapshot.isMissingNode())return;JsonNode floor=floor();if(floor.isMissingNode())return;
        if(built!=state.world||floorId!=state.snapshot.path("floor").asInt())buildFloor(floor);
        JsonNode me=Json.find(state.snapshot.path("players"),state.id);if(me.isMissingNode())return;
        Set<String> live=new HashSet<>();for(String kind:List.of("player","monster","summon"))for(JsonNode v:Json.list(state.snapshot.path(kind.equals("player")?"players":kind.equals("monster")?"monsters":"summons"))){
            String id=v.path("id").asText();live.add(id);Actor a=entities.get(id);String visual=v.path("appearance").toString()+v.path("outfit").toString()+v.path("weapons").toString()+v.path("model").asText()+v.path("color").asText()+v.path("element").asText();
            if(a!=null&&!a.visual.equals(visual)){a.root.removeFromParent();a.ring.removeFromParent();entities.remove(id);a=null;}
            if(a==null){a=actor(kind,v);a.visual=visual;a.kind=kind;a.root.setUserData("id",id);a.root.setUserData("kind",kind);a.root.setLocalTranslation((float)Json.num(v,"x"),0,(float)Json.num(v,"z"));actors.attachChild(a.root);a.ring=ring("#a9e7d0",kind.equals("player")?.66f:1.0f,.04f);actors.attachChild(a.ring);entities.put(id,a);}
            a.value=v;Vector3f goal=new Vector3f((float)Json.num(v,"x"),0,(float)Json.num(v,"z"));float delta=a.root.getLocalTranslation().distance(goal);float alpha=delta>15?1:1-FastMath.exp(-Math.min(dt,.1f)*15);a.root.getLocalTranslation().interpolateLocal(goal,alpha);a.root.setLocalRotation(new Quaternion().fromAngles(0,(float)Json.num(v,"heading"),0));
            float stride=delta>.004?FastMath.sin(time*10)*.46f:FastMath.sin(time*2)*.015f,swing=Math.max(0,(a.swing-time)/.3f);
            if(a.rig!=null)a.rig.animate(time,delta>.004,swing);
            boolean warning=v.path("windup").asBoolean(),selected=id.equals(me.path("target").asText())||id.equals(state.id);a.ring.setCullHint(warning||selected?Spatial.CullHint.Inherit:Spatial.CullHint.Always);a.ring.setLocalTranslation(goal.x,.07f,goal.z);a.ring.setLocalScale(warning?3:1);a.ring.setMaterial(unshaded(warning?"#ff9955":id.equals(state.id)?"#b1eccc":"#f6d18a",true));a.root.setCullHint(Json.distance(v,me)>(high?80:50)&&!selected?Spatial.CullHint.Always:Spatial.CullHint.Inherit);
        }
        entities.entrySet().removeIf(e->{if(live.contains(e.getKey()))return false;e.getValue().root.removeFromParent();e.getValue().ring.removeFromParent();return true;});
        Actor own=entities.get(state.id);Vector3f target=own.root.getLocalTranslation().add(0,1.2f,0);if(!cameraReady)look.set(target);else look.interpolateLocal(target,1-FastMath.exp(-dt*8));cameraReady=true;
        float horizontal=(float)(state.zoom*Math.cos(state.pitch));cam.setLocation(look.add((float)Math.sin(state.yaw)*horizontal,(float)(state.zoom*Math.sin(state.pitch)),(float)Math.cos(state.yaw)*horizontal));cam.lookAt(look,Vector3f.UNIT_Y);
        for(JsonNode resource:Json.list(state.snapshot.path("resources"))){Node n=resources.get(resource.path("id").asText());if(n!=null)n.setCullHint(resource.path("available").asBoolean()?Spatial.CullHint.Inherit:Spatial.CullHint.Always);}
        if(destination!=null)destination.setCullHint(time<destinationUntil?Spatial.CullHint.Inherit:Spatial.CullHint.Always);transients.removeIf(e->{if(e.until>time){e.node.setLocalScale(1+(time-(e.until-.75f))*.8f);return false;}e.node.removeFromParent();return true;});
    }
    private JsonNode floor() {return Json.list(state.world.path("floors")).stream().filter(f->f.path("id").asInt()==state.snapshot.path("floor").asInt()).findFirst().orElse(Json.EMPTY);}
    private Material paint(String hex) {
        return paints.computeIfAbsent(hex,key->{ColorRGBA color=color(key);Material m=new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md");m.setBoolean("UseMaterialColors",true);m.setColor("Diffuse",color);m.setColor("Ambient",color.mult(.82f));m.setColor("Specular",new ColorRGBA(.22f,.25f,.25f,1));m.setFloat("Shininess",40);return m;});
    }
    private ColorRGBA color(String hex) {javafx.scene.paint.Color c=Models.color(hex);return new ColorRGBA((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),1).setAsSrgb((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),1);}
    private Material unshaded(String hex,boolean glow) {return paints.computeIfAbsent("unlit:"+hex+glow,key->{Material m=new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");m.setColor("Color",color(hex));if(glow)m.setColor("GlowColor",color(hex));return m;});}
    private Actor actor(String kind,JsonNode value) {
        Actor a=new Actor();if(kind.equals("player"))a.rig=art.avatar(value.path("appearance"),state.world.path("characterOptions"),value);else if(kind.equals("summon")){String element=value.path("element").asText("shadow");if(!Set.of("fire","ice","water","earth","light","shadow","physical").contains(element))element="shadow";a.rig=art.load("summon/"+element);}else if(value.path("model").asText().equals("dummy"))a.rig=art.load("dummy/"+(value.path("ally").asBoolean()?1:0)+"/"+(value.path("defense").asInt()>0?1:0));else a.rig=art.creature(value);a.root=a.rig.root;a.height=a.rig.height;return a;
    }
    private Geometry box(Node parent,String hex,float x,float y,float z,float w,float h,float d) {Geometry g=new Geometry("scenery",new Box(w/2,h/2,d/2));g.setMaterial(paint(hex));g.setLocalTranslation(x,y,z);g.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);parent.attachChild(g);return g;}
    private Geometry disk(Node parent,String hex,float radius,float h,float x,float y,float z) {
        int count=96;float[] positions=new float[(count+2)*3],normals=new float[positions.length],uv=new float[(count+2)*2];int[] index=new int[count*3];normals[1]=1;uv[0]=uv[1]=.5f;
        for(int i=0;i<=count;i++){float angle=-i*FastMath.TWO_PI/count;int p=(i+1)*3;positions[p]=FastMath.cos(angle)*radius;positions[p+2]=FastMath.sin(angle)*radius;normals[p+1]=1;uv[(i+1)*2]=positions[p]*.18f;uv[(i+1)*2+1]=positions[p+2]*.18f;if(i<count){index[i*3]=0;index[i*3+1]=i+1;index[i*3+2]=i+2;}}
        Mesh mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.Normal,3,normals);mesh.setBuffer(VertexBuffer.Type.TexCoord,2,uv);mesh.setBuffer(VertexBuffer.Type.Index,3,index);mesh.updateBound();Geometry g=new Geometry("ground",mesh);g.setLocalTranslation(x,y+h/2,z);g.setMaterial(paint(hex));g.setShadowMode(RenderQueue.ShadowMode.Receive);parent.attachChild(g);return g;
    }
    private Geometry ring(String hex,float radius,float thickness) {
        int count=64;float[] positions=new float[count*6],normals=new float[count*6];int[] indices=new int[count*6];
        for(int i=0;i<count;i++){float angle=i*FastMath.TWO_PI/count;for(int j=0;j<2;j++){int k=i*6+j*3;positions[k]=FastMath.cos(angle)*(radius+j*thickness);positions[k+2]=FastMath.sin(angle)*(radius+j*thickness);normals[k+1]=1;}int a=i*2,b=((i+1)%count)*2;int k=i*6;indices[k]=a;indices[k+1]=b;indices[k+2]=a+1;indices[k+3]=a+1;indices[k+4]=b;indices[k+5]=b+1;}
        Mesh mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.Normal,3,normals);mesh.setBuffer(VertexBuffer.Type.Index,3,indices);mesh.updateBound();Geometry geometry=new Geometry("intent-marker",mesh);geometry.setMaterial(unshaded(hex,true));geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);geometry.setShadowMode(RenderQueue.ShadowMode.Off);return geometry;
    }
    private void buildFloor(JsonNode floor) {
        built=state.world;floorId=floor.path("id").asInt();cameraReady=false;scenery.detachAllChildren();actors.detachAllChildren();fx.detachAllChildren();entities.clear();resources.clear();transients.clear();if(portalLight!=null)rootNode.removeLight(portalLight);float radius=(float)Json.num(floor,"radius");boolean crystal=floor.path("biome").asText().equals("crystal");viewPort.setBackgroundColor(color(crystal?"#8ba5bf":"#b8d1df"));
        disk(scenery,"#707f79",radius+1,1.7f,0,-1.15f,0);Geometry grass=disk(scenery,crystal?"#64838a":"#849c63",radius,.30f,0,-.17f,0);Material ground=paint(crystal?"#64838a":"#849c63").clone();ground.setTexture("DiffuseMap",texture());ground.setColor("Specular",ColorRGBA.Black);grass.setMaterial(ground);
        box(scenery,crystal?"#8b9ea4":"#b3b29c",0,.008f,0,5.6f,.03f,(radius-6)*2);JsonNode spawn=floor.path("spawn");disk(scenery,"#a6a58e",8.9f,.035f,(float)Json.num(spawn,"x"),.035f,(float)Json.num(spawn,"z")+1);
        Random decorative=new Random(state.world.path("seed").asLong()+floorId*1009L);Map<String,Node> cells=new LinkedHashMap<>();
        int variant=0;for(JsonNode prop:Json.list(floor.path("props"))){String kind=prop.path("kind").asText();if(kind.equals("resource"))continue;String key=Set.of("house","smithy","tailor","shop").contains(kind)?"building/"+(crystal?1:0)+"/"+kind:"tree/"+(crystal||kind.equals("crystal")?1:0)+"/"+(variant++%6);Node object=art.load(key).root;object.setLocalTranslation((float)Json.num(prop,"x"),0,(float)Json.num(prop,"z"));object.rotate(0,(float)Json.num(prop,"rotation"),0);object.setLocalScale((float)prop.path("scale").asDouble(1));String cell=(int)Math.floor(Json.num(prop,"x")/20)+":"+(int)Math.floor(Json.num(prop,"z")/20);cells.computeIfAbsent(cell,k->new Node("spatial-batch-"+k)).attachChild(object);}
        if(high)grassBlades(decorative,radius,spawn);
        for(float z=-radius+8;z<radius-8;z+=1.05f)for(float x=-2.3f;x<2.4f;x+=1.15f){Geometry paver=box(scenery,decorative.nextBoolean()?"#bbb9a4":"#aaa997",x,.047f,z,1.10f,.022f,1.00f);}
        float sx=(float)Json.num(spawn,"x"),sz=(float)Json.num(spawn,"z")+1;for(float z=-8;z<=8;z+=.86f)for(float x=-8;x<=8;x+=1.18f){if(x*x+z*z>72||Math.abs(x+sx)<2.7)continue;Geometry tile=box(scenery,decorative.nextBoolean()?"#b8b6a1":"#a9a992",sx+x+(Math.round(z/.86f)%2)*.18f,.052f,sz+z,1.12f,.030f,.80f);tile.rotate(0,.025f*(decorative.nextFloat()-.5f),0);}
        GeometryBatchFactory.optimize(scenery);for(Node cell:cells.values()){GeometryBatchFactory.optimize(cell);scenery.attachChild(cell);}scenery.depthFirstTraversal(n->{if(n instanceof Geometry)n.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);});
        for(JsonNode npc:Json.list(floor.path("npcs"))){String role=npc.path("role").asText("guide");if(!Set.of("guide","smith","tailor","shopkeeper").contains(role))role="shopkeeper";Node object=art.load("npc/"+role).root;object.setLocalTranslation((float)Json.num(npc,"x"),0,(float)Json.num(npc,"z"));object.setLocalRotation(new Quaternion().fromAngles(0,(float)npc.path("heading").asDouble(FastMath.PI),0));object.setUserData("id",npc.path("id").asText());object.setUserData("kind","npc");scenery.attachChild(object);}
        for(JsonNode resource:Json.list(floor.path("resources"))){String type=resource.path("typeId").asText();JsonNode definition=Json.find(state.world.path("gathering").path("resources"),type);Node n=art.resource(definition).root;n.setLocalTranslation((float)Json.num(resource,"x"),0,(float)Json.num(resource,"z"));n.setUserData("kind","resource");n.setUserData("id",resource.path("id").asText());resources.put(resource.path("id").asText(),n);scenery.attachChild(n);GeometryBatchFactory.optimize(n);}
        JsonNode portal=floor.path("portal");Node gate=art.load("portal").root;gate.setLocalTranslation((float)Json.num(portal,"x"),0,(float)Json.num(portal,"z"));portalLight=new PointLight(gate.getLocalTranslation().add(0,2,0),new ColorRGBA(.2f,.8f,.75f,1),8);rootNode.addLight(portalLight);scenery.attachChild(gate);GeometryBatchFactory.optimize(gate);
        JsonNode training=floor.path("training");if(training.isObject())disk(scenery,"#bea780",(float)Json.num(training,"radius"),.04f,(float)Json.num(training,"x"),.055f,(float)Json.num(training,"z"));
        destination=ring("#ffe0a4",.55f,.07f);fx.attachChild(destination);destinationUntil=0;sky();
    }
    private void grassBlades(Random random,float radius,JsonNode spawn) {
        List<Float> pos=new ArrayList<>(),normals=new ArrayList<>(),colors=new ArrayList<>();
        for(int i=0;i<16000;i++){float x=(random.nextFloat()*2-1)*(radius-4),z=(random.nextFloat()*2-1)*(radius-4);if(Math.hypot(x,z)>radius-4||Math.abs(x)<3.3||Math.hypot(x-Json.num(spawn,"x"),z-Json.num(spawn,"z")-1)<9)continue;float h=.15f+random.nextFloat()*.27f,a=random.nextFloat()*FastMath.TWO_PI,w=.026f+random.nextFloat()*.014f,dx=FastMath.cos(a)*w,dz=FastMath.sin(a)*w;float[] verts={x-dx,.015f,z-dz,x+dx,.015f,z+dz,x+dx*1.8f,h,z+dz*1.8f};ColorRGBA bottom=color("#647c43"),top=color("#a0b575");for(int j=0;j<3;j++){pos.add(verts[j*3]);pos.add(verts[j*3+1]);pos.add(verts[j*3+2]);normals.add(0f);normals.add(1f);normals.add(0f);ColorRGBA c=j==2?top:bottom;colors.add(c.r);colors.add(c.g);colors.add(c.b);colors.add(1f);}}
        Mesh mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,array(pos));mesh.setBuffer(VertexBuffer.Type.Normal,3,array(normals));mesh.setBuffer(VertexBuffer.Type.Color,4,array(colors));mesh.updateBound();Geometry blades=new Geometry("fine-grass",mesh);Material material=paint("#ffffff").clone();material.setBoolean("UseVertexColor",true);material.setColor("Specular",ColorRGBA.Black);material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);blades.setMaterial(material);blades.setShadowMode(RenderQueue.ShadowMode.Receive);scenery.attachChild(blades);
    }
    private float[] array(List<Float> values){float[] result=new float[values.size()];for(int i=0;i<result.length;i++)result[i]=values.get(i);return result;}
    private void sky() {
        Geometry sky=new Geometry("gradient-sky",new Sphere(24,40,320));FloatBuffer positions=sky.getMesh().getFloatBuffer(VertexBuffer.Type.Position);float[] colors=new float[positions.limit()/3*4];for(int i=0;i<positions.limit()/3;i++){float y=positions.get(i*3+1)/320;ColorRGBA color=new ColorRGBA(.38f,.59f,.78f,1).interpolateLocal(new ColorRGBA(.84f,.88f,.82f,1),Math.max(0,Math.min(1,.75f-y)));colors[i*4]=color.r;colors[i*4+1]=color.g;colors[i*4+2]=color.b;colors[i*4+3]=1;}sky.getMesh().setBuffer(VertexBuffer.Type.Color,4,colors);Material m=new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");m.setBoolean("VertexColor",true);m.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);m.getAdditionalRenderState().setDepthWrite(false);sky.setMaterial(m);sky.setQueueBucket(RenderQueue.Bucket.Sky);sky.setCullHint(Spatial.CullHint.Never);sky.setShadowMode(RenderQueue.ShadowMode.Off);scenery.attachChild(sky);
    }
    private Texture2D texture() {
        int size=256;ByteBuffer pixels=BufferUtils.createByteBuffer(size*size*4);Random r=new Random(17029);for(int y=0;y<size;y++)for(int x=0;x<size;x++){int n=230+r.nextInt(35)-17;pixels.put((byte)n).put((byte)n).put((byte)n).put((byte)255);}pixels.flip();Texture2D texture=new Texture2D(new Image(Image.Format.RGBA8,size,size,pixels,ColorSpace.sRGB));texture.setWrap(Texture.WrapMode.Repeat);texture.setMagFilter(Texture.MagFilter.Bilinear);texture.setMinFilter(Texture.MinFilter.Trilinear);return texture;
    }
    void pick(double nx,double ny) {enqueue(()->{
        if(state.snapshot.isMissingNode())return null;Vector2f screen=new Vector2f((float)nx*width,(1-(float)ny)*height);Vector3f start=cam.getWorldCoordinates(screen,0),direction=cam.getWorldCoordinates(screen,1).subtract(start).normalizeLocal();Ray ray=new Ray(start,direction);CollisionResults results=new CollisionResults();actors.collideWith(ray,results);scenery.collideWith(ray,results);
        for(CollisionResult hit:results){Spatial node=hit.getGeometry();while(node!=null&&node.getUserData("kind")==null)node=node.getParent();if(node==null)continue;String kind=node.getUserData("kind"),id=node.getUserData("id");if(kind.equals("resource"))send.accept(Commands.action("gather","nodeId",id));else if(kind.equals("npc"))send.accept(Commands.action("interact"));else if(Set.of("monster","player").contains(kind)&&!id.equals(state.id))send.accept(Commands.target(id,kind));else continue;return null;}
        if(Math.abs(direction.y)>.0001f){float distance=-start.y/direction.y;if(distance>0){Vector3f p=start.add(direction.mult(distance));send.accept(Commands.move(p.x,p.z));destination.setLocalTranslation(p.x,.075f,p.z);destinationUntil=time+1;}}return null;
    });}
    void event(JsonNode event) {enqueue(()->{if(event.has("floor")&&event.path("floor").asInt()!=floorId)return null;String type=event.path("type").asText();if(!Set.of("hit","heal","skillEffect").contains(type))return null;Actor source=entities.get(event.path("source").asText());if(source!=null)source.swing=time+.3f;Actor target=entities.get(event.path("target").asText());if(target==null)target=source;if(target==null)return null;Node effect=new Node();effect.setLocalTranslation(target.root.getLocalTranslation());String hex=type.equals("heal")?"#9bffc3":type.equals("skillEffect")?element(event.path("element").asText()):"#ffcc84";Geometry ring=ring(hex,Math.max(.7f,(float)event.path("radius").asDouble(.8)),.12f);ring.setLocalTranslation(0,.15f,0);effect.attachChild(ring);for(int i=0;i<8;i++){Geometry mote=new Geometry("spell-mote",new Sphere(6,8,.09f));mote.setMaterial(unshaded(hex,true));mote.setLocalTranslation(FastMath.cos(i*.78f)*.6f,.5f+i*.14f,FastMath.sin(i*.78f)*.6f);effect.attachChild(mote);}fx.attachChild(effect);transients.add(new Transient(effect,time+.75f));while(transients.size()>32)transients.remove(0).node.removeFromParent();return null;});}
    private void capture() {
        byte[] pixels=freeFrames.poll();if(pixels==null)return;if(pixels.length!=width*height*4)return;readBuffer.clear();renderer.readFrameBufferWithFormat(buffer,readBuffer,Image.Format.RGBA8);for(int y=0;y<height;y++){int row=(height-1-y)*width*4;for(int x=0;x<width;x++){int from=row+x*4,to=(y*width+x)*4;pixels[to]=readBuffer.get(from+2);pixels[to+1]=readBuffer.get(from+1);pixels[to+2]=readBuffer.get(from);pixels[to+3]=(byte)255;}}
        List<Tag> tags=new ArrayList<>();JsonNode me=Json.find(state.snapshot.path("players"),state.id);for(var e:entities.entrySet()){Actor a=e.getValue();if(a.root.getCullHint()==Spatial.CullHint.Always||!a.kind.equals("player")&&!e.getKey().equals(me.path("target").asText())&&Json.distance(a.value,me)>13)continue;Vector3f p=cam.getScreenCoordinates(a.root.getLocalTranslation().add(0,a.height,0));if(p.z<0||p.z>1||p.x<0||p.x>width||p.y<0||p.y>height)continue;tags.add(new Tag(e.getKey(),p.x,height-p.y,a.value.path("name").asText()+" · Nv. "+a.value.path("level").asText("1")+"\n"+a.value.path("hp").asText()+" / "+a.value.path("maxHp").asText()+" PV"));}
        output.accept(new Frame(width,height,pixels,tags));
    }
    static String element(String e) {return switch(e){case "fire"->"#f9aa57";case "ice"->"#a6edff";case "water"->"#72b8ff";case "earth"->"#c6cf8c";case "shadow"->"#d5a6ff";case "light"->"#fff0bb";default->"#bcecdf";};}
}
