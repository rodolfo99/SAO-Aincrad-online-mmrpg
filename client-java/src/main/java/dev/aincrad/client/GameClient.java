package dev.aincrad.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** HTTP cookie login and authenticated WS, including fragments, heartbeats and bounded retry. */
public final class GameClient implements AutoCloseable {
    public interface Listener {
        void world(JsonNode world);
        void message(JsonNode message);
        void status(String status,String detail);
    }
    private final Endpoint endpoint;
    private final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    private final HttpClient http;
    private final HttpClient sockets;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"aincrad-network");t.setDaemon(true);return t;});
    private final Executor ui;
    private final Listener listener;
    private volatile WebSocket socket;
    private volatile boolean online,desired,closed;
    private long generation;
    private String name="Explorador",characterId="";
    private JsonNode appearance=Json.EMPTY;
    private int retries;
    private ScheduledFuture<?> heartbeat,timeout,retry;
    private CompletableFuture<WebSocket> opening;
    private CompletableFuture<Void> outbound=CompletableFuture.completedFuture(null);
    public GameClient(Endpoint endpoint,Executor ui,Listener listener) {
        this.endpoint=endpoint;this.ui=ui;this.listener=listener;
        http=HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
        sockets=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    public Endpoint endpoint() {return endpoint;}
    public boolean online() {return online;}
    public CompletableFuture<JsonNode> player(String action,JsonNode body) { return request("api/player/"+action,body); }
    public CompletableFuture<JsonNode> world() { return request("api/world",null); }
    public CompletableFuture<JsonNode> admin(String path,String method,JsonNode body) {return request("api/admin/"+path,method,body);}
    public CompletableFuture<JsonNode> health() {return request("api/health",null);}
    private CompletableFuture<JsonNode> request(String path,JsonNode body) {
        return request(path,body==null?"GET":"POST",body);
    }
    private CompletableFuture<JsonNode> request(String path,String method,JsonNode body) {
        var request=HttpRequest.newBuilder(endpoint.api(path)).timeout(Duration.ofSeconds(path.endsWith("ai/test")?130:15)).header("Accept","application/json").header("Origin",endpoint.origin()).header(path.startsWith("api/admin/")?"X-Aincrad-Admin":"X-Aincrad-Player","1");
        request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body.toString()));
        if(body!=null)request.header("Content-Type","application/json");
        return http.sendAsync(request.build(),HttpResponse.BodyHandlers.ofString()).thenApply(response->{
            try {
                JsonNode result=Json.MAPPER.readTree(response.body());
                if(result==null)throw new IllegalArgumentException("Respuesta vacía del servidor");
                if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalArgumentException(result.path("error").asText("Servidor respondió HTTP "+response.statusCode()));
                if(result==null)throw new IllegalArgumentException("Respuesta vacía del servidor");
                return result;
            } catch (java.io.IOException malformed) {throw new CompletionException(new IllegalArgumentException("Respuesta del servidor inválida (HTTP "+response.statusCode()+")"));}
        });
    }
    public synchronized void connect(String name,String characterId,JsonNode appearance) {
        disconnect();if(closed)return;this.name=name;this.characterId=characterId;this.appearance=appearance.deepCopy();desired=true;retries=0;attempt(++generation);
    }
    private synchronized void attempt(long epoch) {
        if(!active(epoch))return;
        status(epoch,"connecting",retries==0?"Conectando con Aincrad…":"Reconectando ("+retries+"/5)…");
        player("session",null).thenCombine(world(),(session,world)->{
            if(!session.path("authenticated").asBoolean())throw new SessionExpired();
            dispatch(epoch,()->listener.world(world));return world;
        }).thenCompose(world->{
            synchronized(this) {
                if(!active(epoch))return CompletableFuture.failedFuture(new CancellationException());
                String token=cookies.getCookieStore().get(endpoint.base()).stream().filter(c->c.getName().equals("aincrad_player")&&!c.hasExpired()&&(!c.getSecure()||endpoint.base().getScheme().equals("https"))).map(HttpCookie::getValue).findFirst().orElseThrow(SessionExpired::new);
                opening=sockets.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(8)).header("Origin",endpoint.origin()).header("Cookie","aincrad_player="+token).buildAsync(endpoint.socket(),new SocketListener(epoch));
                return opening;
            }
        }).whenComplete((ws,error)->{if(error!=null && !(cause(error) instanceof CancellationException))failed(epoch,error);});
    }
    private synchronized void failed(long epoch,Throwable error) {
        if(!active(epoch))return;
        cancel(heartbeat);cancel(timeout);online=false;
        WebSocket old=socket;socket=null;if(old!=null)old.abort();
        Throwable reason=cause(error);
        if(reason instanceof SessionExpired || reason instanceof WebSocketHandshakeException handshake && handshake.getResponse().statusCode()==401){desired=false;status(epoch,"expired","Tu sesión ha finalizado. Inicia sesión de nuevo.");return;}
        if(reason instanceof WebSocketHandshakeException handshake && handshake.getResponse().statusCode()==403){desired=false;status(epoch,"offline","Origen rechazado. Añade "+endpoint.origin()+" a ALLOWED_ORIGINS del servidor.");return;}
        long next=++generation;
        if(retries++<5){status(next,"offline","Conexión interrumpida; recuperando el personaje…");retry=worker.schedule(()->attempt(next),Math.min(retries,5),TimeUnit.SECONDS);}
        else {desired=false;status(next,"offline","No se pudo reconectar: "+Json.error(error));}
    }
    private static Throwable cause(Throwable t) {while(t instanceof CompletionException && t.getCause()!=null)t=t.getCause();return t;}
    public synchronized void send(JsonNode message) {
        WebSocket ws=socket;long epoch=generation;
        if(ws==null||!online)return;
        // HttpClient allows only one outstanding sendText; serialize heartbeat and gameplay.
        outbound=outbound.handle((ok,error)->null).thenCompose(ignored->{synchronized(this){if(!active(epoch)||socket!=ws)return CompletableFuture.completedFuture(null);}return ws.sendText(message.toString(),true).thenApply(sent->null);});
        outbound.exceptionally(error->{failed(epoch,error);return null;});
    }
    public synchronized void disconnect() {
        desired=false;online=false;++generation;cancel(heartbeat);cancel(timeout);cancel(retry);
        if(opening!=null)opening.cancel(true);opening=null;
        WebSocket old=socket;socket=null;outbound=CompletableFuture.completedFuture(null);
        if(old!=null && !old.isOutputClosed())old.sendClose(WebSocket.NORMAL_CLOSURE,"Salir").orTimeout(2,TimeUnit.SECONDS).whenComplete((ok,error)->{if(error!=null)old.abort();});
    }
    private boolean active(long epoch) {return !closed&&desired&&generation==epoch;}
    private void dispatch(long epoch,Runnable action) {ui.execute(()->{synchronized(this){if(closed||generation!=epoch)return;}action.run();});}
    private void status(long epoch,String state,String detail) {dispatch(epoch,()->listener.status(state,detail));}
    private static void cancel(Future<?> task) {if(task!=null)task.cancel(false);}
    @Override public synchronized void close() {disconnect();closed=true;cookies.getCookieStore().removeAll();worker.shutdownNow();}
    private static final class SessionExpired extends RuntimeException {private SessionExpired(){super("Sesión finalizada");}}
    private final class SocketListener implements WebSocket.Listener {
        private final long epoch;
        private final StringBuilder fragments=new StringBuilder();
        private SocketListener(long epoch) {this.epoch=epoch;}
        @Override public void onOpen(WebSocket ws) {
            synchronized(GameClient.this) {
                if(!active(epoch)){ws.abort();return;}
                socket=ws;outbound=CompletableFuture.completedFuture(null);
                timeout=worker.schedule(()->failed(epoch,new TimeoutException("No se recibió welcome")),8,TimeUnit.SECONDS);
            }
            ws.sendText(Commands.join(name,characterId,appearance).toString(),true).whenComplete((ok,error)->{if(error!=null)failed(epoch,error);});ws.request(1);
        }
        @Override public CompletionStage<?> onText(WebSocket ws,CharSequence part,boolean last) {
            synchronized(GameClient.this){if(!active(epoch)||socket!=ws){ws.abort();return null;}}
            fragments.append(part);
            if(fragments.length()>2*1024*1024){failed(epoch,new IllegalArgumentException("Mensaje demasiado grande"));ws.abort();return null;}
            if(last) {
                try {
                    JsonNode message=Json.MAPPER.readTree(fragments.toString());fragments.setLength(0);
                    if(message==null||!message.isObject())throw new IllegalArgumentException("JSON inválido");
                    synchronized(GameClient.this) {
                        if(message.path("type").asText().equals("welcome")) {
                            online=true;characterId=message.path("id").asText();retries=0;cancel(timeout);
                            heartbeat=worker.scheduleAtFixedRate(()->{synchronized(GameClient.this){if(active(epoch))send(Commands.action("ping"));}},5,5,TimeUnit.SECONDS);
                            status(epoch,"online","Conectado a "+endpoint.origin());
                        }
                        if(message.path("type").asText().equals("error")&&!online) {dispatch(epoch,()->listener.message(message));desired=false;cancel(timeout);ws.sendClose(1000,"Entrada rechazada");status(epoch,"offline",message.path("text").asText());return null;}
                    }
                    dispatch(epoch,()->listener.message(message));
                } catch (Exception malformed) {failed(epoch,malformed);ws.abort();return null;}
            }
            ws.request(1);return null;
        }
        @Override public CompletionStage<?> onClose(WebSocket ws,int code,String reason) {
            if(code==4401)failed(epoch,new SessionExpired());else failed(epoch,new IllegalStateException("WebSocket "+code+": "+reason));return null;
        }
        @Override public void onError(WebSocket ws,Throwable error) {failed(epoch,error);}
    }
}
