package dev.aincrad;

import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.*;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** Cookie authentication precedes the WebSocket upgrade, including direct clients. */
final class PlayerHandshake implements HandshakeInterceptor {
    static final String COOKIE = "aincrad_player";
    private final PlayerAuth auth;
    private final Set<String> origins;
    PlayerHandshake(PlayerAuth auth, Set<String> origins) { this.auth=auth; this.origins=origins; }
    static String cookie(HttpServletRequest request) {
        if(request.getCookies()!=null)for(Cookie cookie:request.getCookies())if(COOKIE.equals(cookie.getName()))return cookie.getValue();
        return null;
    }
    @Override public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Map<String,Object> attributes) {
        String origin=request.getHeaders().getOrigin();
        if(origin==null||!origins.contains(origin)){response.setStatusCode(HttpStatus.FORBIDDEN);return false;}
        String token=request instanceof ServletServerHttpRequest servlet?cookie(servlet.getServletRequest()):null;
        PlayerAuth.Session session=auth.session(token);
        if(session==null){response.setStatusCode(HttpStatus.UNAUTHORIZED);return false;}
        attributes.put("playerSessionHash",GameWorld.hash(token));attributes.put("accountId",session.accountId());
        return true;
    }
    @Override public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception exception) {}
}
