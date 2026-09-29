package dev.aincrad;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/player")
final class PlayerApi {
    private final Application.GameServer game;
    private final Set<String> origins;
    private final boolean secureCookie;
    private final PasswordRecovery recovery;
    PlayerApi(Application.GameServer game,PasswordRecovery recovery,@Value("${aincrad.origins}")String origins,@Value("${aincrad.player-cookie-secure:false}")boolean secureCookie) {
        this.game=game;this.recovery=recovery;this.origins=new HashSet<>(Arrays.asList(origins.split(",")));this.secureCookie=secureCookie;
    }
    @ModelAttribute void noCache(HttpServletResponse response){response.setHeader("Cache-Control","no-store");response.setHeader("Pragma","no-cache");}
    private void mutation(HttpServletRequest request){
        if(!"1".equals(request.getHeader("X-Aincrad-Player"))||!origins.contains(Objects.toString(request.getHeader("Origin"),"")))
            throw new SecurityException("Origen de la solicitud rechazado");
    }
    private String account(HttpServletRequest request){return game.players.require(PlayerHandshake.cookie(request)).accountId();}
    private String value(JsonNode input,String field){
        if(input==null||!input.isObject()||!input.path(field).isTextual())throw new IllegalArgumentException("Campo requerido: "+field);
        return input.get(field).textValue();
    }
    private ResponseCookie cookie(String token,HttpServletRequest request,int age){
        return ResponseCookie.from(PlayerHandshake.COOKIE,token).httpOnly(true).secure(secureCookie||request.isSecure()).sameSite("Strict").path("/").maxAge(age).build();
    }
    private Map<String,Object> status(PlayerAuth.Session session){
        if(session==null)return Map.of("authenticated",false,"registrationEnabled",game.players.registrationEnabled(),"recoveryEnabled",recovery.enabled());
        return Map.of("authenticated",true,"registrationEnabled",game.players.registrationEnabled(),"recoveryEnabled",recovery.enabled(),"email",Objects.toString(game.players.account(session.accountId()).email(),""),"username",game.players.account(session.accountId()).username(),
            "expiresAt",session.expiresAt(),"characters",game.world.ownedCharacters(session.accountId()));
    }
    @GetMapping("/session") Map<String,Object> session(HttpServletRequest request){return status(game.players.session(PlayerHandshake.cookie(request)));}
    @PostMapping("/register") ResponseEntity<?> register(@RequestBody JsonNode input,HttpServletRequest request)throws Exception {
        mutation(request);String password=value(input,"password");
        if(!password.equals(value(input,"confirmPassword")))throw new IllegalArgumentException("Las contraseñas no coinciden");
        var account=game.players.register(value(input,"username"),password,value(input,"email"),request.getRemoteAddr());
        return ResponseEntity.status(201).body(Map.of("username",account.username(),"message","Cuenta creada. Inicia sesión para jugar."));
    }
    @PostMapping("/login") ResponseEntity<?> login(@RequestBody JsonNode input,HttpServletRequest request)throws Exception {
        mutation(request);String previous=PlayerHandshake.cookie(request);
        String token=game.players.login(value(input,"username"),value(input,"password"),request.getRemoteAddr());
        game.players.logout(previous);game.disconnectExpiredSessions();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie(token,request,game.players.sessionSeconds()).toString()).body(status(game.players.session(token)));
    }
    @PostMapping("/logout") ResponseEntity<?> logout(HttpServletRequest request){
        mutation(request);game.players.logout(PlayerHandshake.cookie(request));game.disconnectExpiredSessions();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie("",request,0).toString()).body(Map.of("ok",true));
    }
    @PostMapping("/password") ResponseEntity<?> password(@RequestBody JsonNode input,HttpServletRequest request)throws Exception {
        mutation(request);String id=account(request),password=value(input,"password");
        if(!password.equals(value(input,"confirmPassword")))throw new IllegalArgumentException("Las contraseñas no coinciden");
        game.players.changePassword(id,value(input,"currentPassword"),password);game.disconnectExpiredSessions();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie("",request,0).toString()).body(Map.of("message","Contraseña actualizada. Inicia sesión de nuevo."));
    }
    @PostMapping("/claim-character") Map<String,Object> claim(@RequestBody JsonNode input,HttpServletRequest request)throws Exception {
        mutation(request);String id=account(request);game.players.limitClaim(id);
        synchronized(game){return game.world.claimLegacy(id,value(input,"legacyToken"));}
    }
    @PostMapping("/email") ResponseEntity<?> email(@RequestBody JsonNode input,HttpServletRequest request)throws Exception {
        mutation(request);game.players.changeEmail(account(request),value(input,"currentPassword"),value(input,"email"));game.disconnectExpiredSessions();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie("",request,0).toString()).body(Map.of("message","Correo actualizado. Inicia sesión de nuevo."));
    }
    @PostMapping("/forgot-password") ResponseEntity<?> forgot(@RequestBody JsonNode input,HttpServletRequest request){
        mutation(request);recovery.request(value(input,"email"),request.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of("message",PasswordRecovery.MESSAGE));
    }
    @PostMapping("/reset-password") ResponseEntity<?> reset(@RequestBody JsonNode input,HttpServletRequest request)throws Exception {
        mutation(request);String password=value(input,"password");
        if(!password.equals(value(input,"confirmPassword")))throw new IllegalArgumentException("Las contraseñas no coinciden");
        String email=game.players.recoverPassword(value(input,"token"),password,request.getRemoteAddr());
        game.players.logout(PlayerHandshake.cookie(request));game.disconnectExpiredSessions();recovery.changed(email);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie("",request,0).toString()).body(Map.of("message","Contraseña restablecida. Inicia sesión con tu nueva contraseña."));
    }
    @ExceptionHandler(MailService.Unavailable.class) ResponseEntity<?> mailUnavailable(Exception error){return ResponseEntity.status(503).body(Map.of("error",error.getMessage()));}
    @ExceptionHandler(PlayerAuth.Unauthorized.class) ResponseEntity<?> unauthorized(Exception error){return ResponseEntity.status(401).body(Map.of("error",error.getMessage()));}
    @ExceptionHandler(PlayerAuth.RateLimited.class) ResponseEntity<?> limited(Exception error){return ResponseEntity.status(429).header("Retry-After","60").body(Map.of("error",error.getMessage()));}
    @ExceptionHandler(SecurityException.class) ResponseEntity<?> forbidden(Exception error){return ResponseEntity.status(403).body(Map.of("error",error.getMessage()));}
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) ResponseEntity<?> malformed(){return ResponseEntity.badRequest().body(Map.of("error","JSON o estructura inválidos"));}
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalid(Exception error){return ResponseEntity.badRequest().body(Map.of("error",Objects.toString(error.getMessage(),"Datos inválidos")));}
    @ExceptionHandler(Exception.class) ResponseEntity<?> failed(Exception error){System.err.println("No se completó una operación de cuentas: "+error.getClass().getSimpleName());return ResponseEntity.internalServerError().body(Map.of("error","No se pudo guardar la operación. Revisa el servidor."));}
}
