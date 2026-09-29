package dev.aincrad;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Reusable outbound mail utility. The bundled Postfix service owns delivery and retries. */
@Service
public final class MailService {
    public static final class Unavailable extends IllegalStateException {
        public Unavailable(String message) { super(message); }
    }
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from, host;
    private final int port;
    private final AtomicLong lastTest = new AtomicLong();
    private final ThreadPoolExecutor queue = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100), task -> { Thread t = new Thread(task, "aincrad-mail"); t.setDaemon(true); return t; });

    public MailService(JavaMailSender sender, @Value("${aincrad.mail.enabled:false}") boolean enabled,
                       @Value("${aincrad.mail.from}") String from, @Value("${spring.mail.host}") String host,
                       @Value("${spring.mail.port}") int port) {
        this.sender = sender; this.enabled = enabled; this.from = PlayerAuth.email(from); this.host = host; this.port = port;
        if (enabled && (host.isBlank() || port < 1 || port > 65535)) throw new IllegalArgumentException("Dirección de la utilidad de correo inválida");
    }
    public boolean enabled() { return enabled; }
    public Map<String,Object> status() {
        return Map.of("enabled", enabled, "from", from, "host", host, "port", port, "pending", queue.getQueue().size());
    }
    public void requireEnabled() {
        if (!enabled) throw new Unavailable("El envío de correo está desactivado. Contacta con el administrador.");
    }
    /** No recipient/subject supplied here is forwarded as a shell command. */
    public void send(String recipient, String subject, String text) {
        requireEnabled();
        if (subject == null || subject.length() > 200 || subject.contains("\r") || subject.contains("\n")
            || text == null || text.length() > 32_768) throw new IllegalArgumentException("Contenido de correo inválido");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(PlayerAuth.email(recipient)); message.setSubject(subject); message.setText(text);
        sender.send(message);
    }
    public void submit(Runnable work) {
        requireEnabled();
        try { queue.execute(work); }
        catch (RejectedExecutionException error) { throw new PlayerAuth.RateLimited(); }
    }
    public void test(String recipient) {
        requireEnabled(); long now = System.currentTimeMillis(), previous = lastTest.get();
        if (now - previous < 30_000 || !lastTest.compareAndSet(previous, now)) throw new PlayerAuth.RateLimited();
        try { send(recipient, "Aincrad · Prueba de correo", "La utilidad de correo de Aincrad ha enviado este mensaje de prueba solicitado desde World Studio."); }
        catch (org.springframework.mail.MailException error) {
            System.err.println("Prueba de correo fallida: " + error.getClass().getSimpleName());
            throw new Unavailable("La utilidad no aceptó el correo. Revisa el servicio mail y su cola.");
        }
    }
    @PreDestroy void stop() { queue.shutdownNow(); }
}
