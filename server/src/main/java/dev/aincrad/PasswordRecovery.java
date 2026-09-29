package dev.aincrad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.Set;

@Service
public final class PasswordRecovery {
    public static final String MESSAGE = "Si el correo corresponde a una cuenta, recibirás un enlace para restablecer la contraseña. Revisa también la carpeta de spam.";
    private final PlayerAuth players;
    private final MailService mail;
    private final String baseUrl;
    private final int minutes;

    public PasswordRecovery(Application.GameServer game, MailService mail,
                            @Value("${aincrad.public-url}") String baseUrl,
                            @Value("${aincrad.recovery-minutes:30}") int minutes) {
        this.players = game.players; this.mail = mail; this.baseUrl = validateUrl(baseUrl); this.minutes = minutes;
        if (minutes < 5 || minutes > 60) throw new IllegalArgumentException("PASSWORD_RESET_MINUTES debe estar entre 5 y 60");
    }
    static String validateUrl(String value) {
        try {
            URI uri = URI.create(value);
            boolean local = Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
            boolean ip = uri.getHost() != null && uri.getHost().matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")
                && java.util.Arrays.stream(uri.getHost().split("\\.")).allMatch(part -> Integer.parseInt(part) <= 255);
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))
                || !("https".equals(uri.getScheme()) || ((local || ip) && "http".equals(uri.getScheme())))) throw new Exception();
            return value.replaceAll("/$", "");
        } catch (Exception error) { throw new IllegalArgumentException("APP_PUBLIC_URL debe ser el origen del juego, sin rutas. Usa HTTPS; HTTP sólo se permite para localhost o una IP literal."); }
    }
    public boolean enabled() { return mail.enabled(); }
    public String publicUrl() { return baseUrl; }
    public void request(String value, String address) {
        mail.requireEnabled(); String email = PlayerAuth.email(value); players.limitRecovery(email, address);
        // Identical asynchronous path and public response for known and unknown addresses.
        mail.submit(() -> {
            PlayerAuth.Recovery recovery = null;
            try {
                recovery = players.beginRecovery(email, minutes);
                if (recovery == null) return;
                String link = baseUrl + "/#reset-password=" + recovery.token();
                mail.send(recovery.email(), "Aincrad · Restablecer tu contraseña",
                    "Solicitaste restablecer tu contraseña de Aincrad.\n\n" + link
                    + "\n\nEl enlace caduca en " + minutes + " minutos y sólo puede usarse una vez. Un enlace nuevo invalida el anterior."
                    + "\nSi no lo solicitaste, ignora este mensaje. Tu contraseña actual sigue siendo válida.\nNunca te pediremos la contraseña por correo.");
            } catch (Exception error) {
                if (recovery != null) try { players.discardRecovery(recovery.token()); } catch (Exception ignored) {}
                System.err.println("No se envió una recuperación de cuenta: " + error.getClass().getSimpleName());
            }
        });
    }
    public void changed(String email) {
        if (!mail.enabled() || email == null) return;
        try { mail.submit(() -> {
            try { mail.send(email, "Aincrad · Contraseña actualizada", "Tu contraseña de Aincrad se ha restablecido y las sesiones anteriores se han cerrado.\nSi no fuiste tú, solicita una nueva recuperación y contacta con el administrador.\n" + baseUrl); }
            catch (Exception error) { System.err.println("No se envió la notificación de contraseña: " + error.getClass().getSimpleName()); }
        }); } catch (PlayerAuth.RateLimited ignored) { System.err.println("Notificación de contraseña pendiente: cola de correo llena"); }
    }
}
