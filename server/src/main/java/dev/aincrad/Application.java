package dev.aincrad;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args){SpringApplication.run(Application.class,args);}
    @Bean GameServer gameServer(ObjectMapper json,@Value("${aincrad.world}") String world,@Value("${aincrad.data}")String data,@Value("${aincrad.root-password:${ROOT_PASSWORD:}}")String password,@Value("${aincrad.player-registration:true}")boolean registration,@Value("${aincrad.player-session-hours:12}")int hours)throws Exception{return new GameServer(json,Path.of(world),Path.of(data),password,registration,hours);}
    @Configuration @EnableWebSocket static class SocketConfig implements WebSocketConfigurer {
        private final GameServer game;private final String[] origins;
        SocketConfig(GameServer game,@Value("${aincrad.origins}")String origins){this.game=game;this.origins=origins.split(",");}
        public void registerWebSocketHandlers(WebSocketHandlerRegistry r){r.addHandler(game,"/ws").addInterceptors(new PlayerHandshake(game.players,new HashSet<>(Arrays.asList(origins)))).setAllowedOrigins(origins);}
    }
    @Controller static class Pages {@GetMapping("/admin")String admin(){return "forward:/index.html";}}

    static class Peer {
        final WebSocketSession session;final String accountId,sessionHash;String player;long last=System.currentTimeMillis(),window=last;int messages;
        Peer(WebSocketSession session){this.session=new ConcurrentWebSocketSessionDecorator(session,3000,65536);accountId=(String)session.getAttributes().get("accountId");sessionHash=(String)session.getAttributes().get("playerSessionHash");}
    }
    public static class GameServer extends TextWebSocketHandler {
        final ObjectMapper json;final Path worldPath,dataPath;final PlayerStore store;final AdminAuth auth;final PlayerAuth players;final NpcAi ai;
        volatile GameWorld world;final Map<String,Peer> peers=new ConcurrentHashMap<>();volatile String saveError="";
        GameServer(ObjectMapper json,Path worldPath,Path dataPath,String password,boolean registration,int hours)throws Exception {
            this.json=json;this.worldPath=worldPath;this.dataPath=dataPath;
            WorldData data=WorldData.load(worldPath,json);
            store=new PlayerStore(dataPath.resolve("players"),json);
            try{auth=new AdminAuth(dataPath,json,password);players=new PlayerAuth(dataPath,json,registration,hours);ai=new NpcAi(dataPath,json);world=new GameWorld(data,store,this::emit);}catch(Exception e){store.close();throw e;}
        }
        @Override public synchronized void afterConnectionEstablished(WebSocketSession session)throws Exception {
            if(players.sessionHash((String)session.getAttributes().get("playerSessionHash"))==null){session.close(new CloseStatus(4401,"Inicia sesión"));return;}if(peers.size()>=40){session.close(CloseStatus.SERVICE_OVERLOAD);return;}session.setTextMessageSizeLimit(4096);peers.put(session.getId(),new Peer(session));
        }
        @Override protected synchronized void handleTextMessage(WebSocketSession session,TextMessage message)throws Exception {
            Peer p=peers.get(session.getId());if(p==null)return;if(players.sessionHash(p.sessionHash)==null){disconnectExpiredSessions();return;}long now=System.currentTimeMillis();
            if(now-p.window>=1000){p.messages=0;p.window=now;}if(++p.messages>40){session.close(CloseStatus.POLICY_VIOLATION);return;}p.last=now;
            try{
                if(message.getPayloadLength()>4096)throw new IllegalArgumentException("Mensaje demasiado grande");JsonNode n=json.readTree(message.getPayload());if(n==null||!n.isObject())throw new IllegalArgumentException("Se esperaba JSON objeto");
                if(p.player==null){if(!n.path("type").asText().equals("join"))throw new IllegalArgumentException("Primero entra al mundo");if(n.has("token"))throw new IllegalArgumentException("Vincula la clave anterior desde tu cuenta antes de jugar");Map<String,Object> welcome=world.joinAccount(p.accountId,n.path("characterId").asText(),n.path("name").asText(),n.has("appearance")?json.treeToValue(n.get("appearance"),WorldData.Appearance.class):null);p.player=(String)welcome.get("id");send(p,welcome);}
                else if(n.path("type").asText().equals("npcChat")){
                    String npc=n.path("npcId").asText(),player=p.player;var context=world.npcContext(player,npc);
                    ai.ask(player,npc,context.get("context"),n.path("text").asText(),reply->emit(player,Map.of("type","npcReply","npcId",npc,"name",context.get("name"),"text",reply.text(),"fallback",reply.fallback(),"reason",reply.reason())));
                }else world.command(p.player,n);
            }catch(IllegalArgumentException|com.fasterxml.jackson.core.JsonProcessingException e){send(p,Map.of("type","error","text",e instanceof com.fasterxml.jackson.core.JsonProcessingException?"JSON inválido":e.getMessage()));}
        }
        @Override public synchronized void afterConnectionClosed(WebSocketSession session,CloseStatus status)throws Exception {Peer p=peers.remove(session.getId());if(p!=null&&p.player!=null)try{world.leave(p.player);ai.forget(p.player);}catch(IOException e){saveError="No se pudo guardar el personaje";System.err.println(saveError+": "+e.getMessage());}}
        @Override public void handleTransportError(WebSocketSession session,Throwable error)throws Exception {session.close(CloseStatus.SERVER_ERROR);}
        private void send(Peer peer,Map<String,Object> value){try{if(peer.session.isOpen()&&players.sessionHash(peer.sessionHash)!=null)peer.session.sendMessage(new TextMessage(json.writeValueAsString(value)));}catch(Exception e){try{peer.session.close(CloseStatus.SERVER_ERROR);}catch(Exception ignored){}}}
        void emit(String player,Map<String,Object> event){if("dialogue".equals(event.get("type"))){event=new java.util.HashMap<>(event);event.put("aiEnabled",ai.enabled((String)event.get("npcId")));}for(Peer p:peers.values())if(p.player!=null&&(player.equals("*")||p.player.equals(player)))send(p,event);}
        synchronized void disconnectExpiredSessions(){
            for(Peer peer:new ArrayList<>(peers.values()))if(players.sessionHash(peer.sessionHash)==null){
                peers.remove(peer.session.getId());
                if(peer.player!=null)try{world.leave(peer.player);ai.forget(peer.player);}catch(IOException e){saveError="No se pudo guardar al cerrar la sesión";}
                try{peer.session.close(new CloseStatus(4401,"Sesión finalizada. Inicia sesión de nuevo."));}catch(IOException ignored){}
            }
        }
        private int tick;
        @Scheduled(fixedRate=50) public synchronized void update(){disconnectExpiredSessions();world.tick(.05);if(++tick%2==0)for(Peer p:peers.values())if(p.player!=null)send(p,world.snapshot(p.player));long now=System.currentTimeMillis();for(Peer p:peers.values())if(now-p.last>(p.player==null?10000:20000))try{p.session.close(CloseStatus.SESSION_NOT_RELIABLE);}catch(Exception ignored){}}
        @Scheduled(fixedDelay=5000) public synchronized void save(){try{world.saveAll();saveError="";}catch(IOException e){saveError="Error de guardado: revisa espacio y permisos";System.err.println(saveError+": "+e.getMessage());}}
        synchronized ObjectNode editable() {ObjectNode n=json.valueToTree(world.data);n.withArray("floors").forEach(f->((ObjectNode)f).remove("props"));return n;}
        synchronized void writeWorld(JsonNode input)throws Exception {
            byte[] content=json.writeValueAsBytes(input);if(content.length>262144)throw new IllegalArgumentException("Mundo demasiado grande (máximo 256 KiB)");
            WorldData proposed=json.treeToValue(input,WorldData.class);proposed.validateAndGenerate();
            ObjectNode clean=json.valueToTree(proposed);clean.withArray("floors").forEach(f->((ObjectNode)f).remove("props"));
            Path backups=dataPath.resolve("backups");Files.createDirectories(backups);Files.copy(worldPath,backups.resolve("world-"+System.currentTimeMillis()+"-"+UUID.randomUUID()+".json"));
            Path temp=worldPath.resolveSibling(worldPath.getFileName()+".partial");Files.write(temp,json.writerWithDefaultPrettyPrinter().writeValueAsBytes(clean));Files.move(temp,worldPath,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }
        synchronized void reload()throws Exception {
            WorldData next=WorldData.load(worldPath,json);world.saveAll();
            for(Peer p:peers.values()){send(p,Map.of("type","notice","text","El administrador actualizó el mundo. Reconectando…"));if(p.player!=null){world.leave(p.player);ai.forget(p.player);p.player=null;}try{p.session.close(CloseStatus.SERVICE_RESTARTED);}catch(Exception ignored){}}
            peers.clear();world=new GameWorld(next,store,this::emit);
        }
        @PreDestroy synchronized void shutdown()throws IOException {ai.close();world.saveAll();store.close();}
    }
    @RestController static class Api {
        private final GameServer game;private final Set<String> origins;private final MailService mail;private final PasswordRecovery recovery;
        Api(GameServer game,MailService mail,PasswordRecovery recovery,@Value("${aincrad.origins}")String origins){this.game=game;this.mail=mail;this.recovery=recovery;this.origins=Set.of(origins.split(","));}
        @GetMapping("/api/health") Map<String,Object> health(){return Map.of("status",game.saveError.isEmpty()?"ok":"degraded","version","0.2.0","players",game.world.count(),"seed",game.world.data.seed,"storage",game.saveError.isEmpty()?"ok":game.saveError);}
        @GetMapping("/api/world") WorldData world(){return game.world.data;}
        String cookie(HttpServletRequest req){if(req.getCookies()!=null)for(Cookie c:req.getCookies())if(c.getName().equals("aincrad_admin"))return c.getValue();return null;}
        void require(HttpServletRequest req){if(!game.auth.valid(cookie(req)))throw new SecurityException("Sesión administrativa requerida");}
        void mutation(HttpServletRequest req){String origin=req.getHeader("Origin");if(!"1".equals(req.getHeader("X-Aincrad-Admin"))||origin!=null&&!origins.contains(origin))throw new SecurityException("Origen administrativo rechazado");}
        ResponseCookie sessionCookie(String token,HttpServletRequest req,int age){return ResponseCookie.from("aincrad_admin",token).httpOnly(true).secure(req.isSecure()).sameSite("Strict").path("/api/admin").maxAge(age).build();}
        @GetMapping("/api/admin/status") Map<String,Object> status(HttpServletRequest req){boolean valid=game.auth.valid(cookie(req));return Map.of("authenticated",valid,"username",valid?"root":"");}
        @GetMapping("/api/admin/mail") Map<String,Object> mailStatus(HttpServletRequest req){require(req);Map<String,Object> result=new HashMap<>(mail.status());result.put("publicUrl",recovery.publicUrl());return result;}
        @PostMapping("/api/admin/mail/test") Map<String,Object> testMail(@RequestBody JsonNode input,HttpServletRequest req){require(req);mutation(req);mail.test(input.path("email").asText());return Map.of("message","Correo aceptado por la utilidad de envío. Comprueba el buzón y la carpeta de spam; la aceptación no confirma su entrega final.");}
        @ExceptionHandler(MailService.Unavailable.class) ResponseEntity<?> mailUnavailable(Exception error){return ResponseEntity.status(503).body(Map.of("error",error.getMessage()));}
        @PostMapping("/api/admin/login") ResponseEntity<?> login(@RequestBody JsonNode n,HttpServletRequest req)throws Exception {mutation(req);String token=game.auth.login(n.path("username").asText(),n.path("password").asText(),req.getRemoteAddr());return ResponseEntity.ok().header("Set-Cookie",sessionCookie(token,req,3600).toString()).body(Map.of("username","root"));}
        @PostMapping("/api/admin/logout") ResponseEntity<?> logout(HttpServletRequest req){mutation(req);game.auth.logout(cookie(req));return ResponseEntity.ok().header("Set-Cookie",sessionCookie("",req,0).toString()).body(Map.of("ok",true));}
        @GetMapping("/api/admin/world") ObjectNode edit(HttpServletRequest req)throws Exception {require(req);ObjectNode n=(ObjectNode)game.json.readTree(game.worldPath.toFile());n.withArray("floors").forEach(f->((ObjectNode)f).remove("props"));if(n.path("crafting").path("shopProducts").isMissingNode()||n.path("crafting").path("shopProducts").isNull())((ObjectNode)n.get("crafting")).set("shopProducts",game.json.valueToTree(game.world.data.crafting.shopProducts));return n;}
        @PutMapping("/api/admin/world") Map<String,Object> save(@RequestBody JsonNode n,HttpServletRequest req)throws Exception {require(req);mutation(req);game.writeWorld(n);return Map.of("ok",true,"message","Mundo validado y guardado. La versión anterior está respaldada. Aplica los cambios para reconectar a los jugadores.");}
        @PostMapping("/api/admin/reload") Map<String,Object> reload(HttpServletRequest req)throws Exception {require(req);mutation(req);game.reload();return Map.of("ok",true,"message","Mundo aplicado. Los clientes se reconectarán automáticamente.");}
        @PostMapping("/api/admin/password") Map<String,Object> password(@RequestBody JsonNode n,HttpServletRequest req)throws Exception {require(req);mutation(req);game.auth.setPassword(n.path("password").asText());return Map.of("ok",true,"message","Contraseña actualizada. Inicia sesión nuevamente.");}
        @GetMapping("/api/admin/accounts") List<Map<String,Object>> accounts(HttpServletRequest req){require(req);return game.players.publicAccounts();}
        @PostMapping("/api/admin/accounts/{id}/password") Map<String,Object> resetPlayerPassword(@PathVariable String id,@RequestBody JsonNode n,HttpServletRequest req)throws Exception{require(req);mutation(req);game.players.resetPassword(id,n.path("password").asText());game.disconnectExpiredSessions();return Map.of("message","Contraseña actualizada. Se cerraron las sesiones de esa cuenta.");}
        @PatchMapping("/api/admin/characters/{id}/account") Map<String,Object> assignAccount(@PathVariable String id,@RequestBody JsonNode n,HttpServletRequest req)throws Exception{require(req);mutation(req);String accountId=n.path("accountId").asText();game.players.account(accountId);synchronized(game){return game.world.assignAccount(id,accountId);}}
        @GetMapping("/api/admin/characters") List<Map<String,Object>> characters(HttpServletRequest req){require(req);return game.world.characters();}
        @PatchMapping("/api/admin/characters/{id}") Map<String,Object> character(@PathVariable String id,@RequestBody JsonNode n,HttpServletRequest req)throws Exception {require(req);mutation(req);for(String key:List.of("xp","col","potions"))if(n.has(key)&&(!n.path(key).isIntegralNumber()||!n.path(key).canConvertToInt()))throw new IllegalArgumentException("Progreso inválido");return game.world.updateCharacter(id,n.path("name").asText(),game.json.treeToValue(n.get("appearance"),WorldData.Appearance.class),n.has("xp")?n.get("xp").intValue():null,n.has("col")?n.get("col").intValue():null,n.has("potions")?n.get("potions").intValue():null,true,n.has("attributeRanks")||n.has("talentRanks")?new Progression.Build(Progression.ranks(n.path("attributeRanks")),Progression.ranks(n.path("talentRanks"))):null);}
        @PatchMapping("/api/admin/characters/{id}/weapons") Map<String,Object> weapons(@PathVariable String id,@RequestBody JsonNode n,HttpServletRequest req){require(req);mutation(req);return game.world.equip(id,n.path("weaponSetId").asText(),true);}
        @PatchMapping("/api/admin/characters/{id}/citizenship") Map<String,Object> citizenship(@PathVariable String id,@RequestBody JsonNode n,HttpServletRequest req){require(req);mutation(req);if(!n.path("citizenship").isIntegralNumber()||!n.path("citizenship").canConvertToInt())throw new IllegalArgumentException("Ciudadanía entera requerida");return game.world.citizenship(id,n.path("citizenship").intValue(),n.path("pardon").asBoolean());}
        @PatchMapping("/api/admin/characters/{id}/crafting") Map<String,Object> crafting(@PathVariable String id,@RequestBody JsonNode n,HttpServletRequest req){require(req);mutation(req);return game.world.craftingProgress(id,n);}
        @GetMapping("/api/admin/ai") NpcAi.Config aiConfig(HttpServletRequest req)throws Exception {require(req);return game.ai.config();}
        @PutMapping("/api/admin/ai") Map<String,Object> saveAi(@RequestBody NpcAi.Config config,HttpServletRequest req)throws Exception {require(req);mutation(req);game.ai.save(config,game.world.data);return Map.of("ok",true,"message","Configuración IA guardada y aplicada. No requiere reiniciar el mundo.");}
        @PostMapping("/api/admin/ai/test") NpcAi.Reply testAi(@RequestBody JsonNode n,HttpServletRequest req)throws Exception {require(req);mutation(req);return game.ai.test(game.json.treeToValue(n.get("config"),NpcAi.Config.class),n.path("npcId").asText(),n.path("text").asText());}
        @PostMapping("/api/admin/ai/models") Object aiModels(@RequestBody NpcAi.Config config,HttpServletRequest req){require(req);mutation(req);return game.ai.models(config);}
        @ExceptionHandler(SecurityException.class) ResponseEntity<?> forbidden(Exception e){return ResponseEntity.status(403).body(Map.of("error",e.getMessage()));}
        @ExceptionHandler({IllegalArgumentException.class,com.fasterxml.jackson.core.JsonProcessingException.class}) ResponseEntity<?> bad(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e instanceof com.fasterxml.jackson.core.JsonProcessingException?"JSON o estructura inválidos":Objects.toString(e.getMessage(),"Datos inválidos")));}
        @ExceptionHandler(Exception.class) ResponseEntity<?> failed(Exception e){System.err.println("API: "+e.getMessage());return ResponseEntity.status(500).body(Map.of("error","No se pudo completar la operación. Revisa el registro del servidor."));}
    }
}
