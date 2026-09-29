package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Persistent player credentials; opaque, expiring sessions stay on the server. */
public final class PlayerAuth {
    static final int ITERATIONS = 600_000;
    public record Account(String id, String username, String salt, String hash, int iterations, long createdAt,
                          String email, String resetHash, long resetExpiresAt) {}
    public record Recovery(String email, String token) {}
    public record Session(String accountId, long expiresAt) {}
    public static final class Unauthorized extends SecurityException {
        public Unauthorized(String message) { super(message); }
    }
    public static final class RateLimited extends IllegalArgumentException {
        public RateLimited() { super("Demasiados intentos. Espera un minuto y vuelve a intentarlo."); }
    }
    private final ObjectMapper json;
    private final Path file;
    private final LongSupplier clock;
    private final long duration;
    private final boolean registrationEnabled;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<Long>> attempts = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final String dummySalt;

    public PlayerAuth(Path dir, ObjectMapper json, boolean registrationEnabled, int hours) throws Exception {
        this(dir, json, registrationEnabled, hours, System::currentTimeMillis);
    }
    PlayerAuth(Path dir, ObjectMapper json, boolean registrationEnabled, int hours, LongSupplier clock) throws Exception {
        if (hours < 1 || hours > 168) throw new IllegalArgumentException("PLAYER_SESSION_HOURS debe estar entre 1 y 168");
        this.json = json; this.file = dir.resolve("accounts.json"); this.clock = clock;
        this.registrationEnabled = registrationEnabled; this.duration = hours * 3_600_000L;
        Files.createDirectories(dir);
        byte[] dummy = new byte[16]; random.nextBytes(dummy); dummySalt = Base64.getEncoder().encodeToString(dummy);
        if (Files.exists(file)) {
            Account[] saved = json.readValue(file.toFile(), Account[].class);
            Set<String> names = new HashSet<>(), emails = new HashSet<>();
            for (Account account : saved) {
                if (account == null || !validId(account.id()) || !username(account.username()).equals(account.username())
                    || !names.add(account.username()) || accounts.containsKey(account.id())
                    || account.iterations() < ITERATIONS || account.iterations() > 2_000_000
                    || Base64.getDecoder().decode(account.salt()).length != 16
                    || Base64.getDecoder().decode(account.hash()).length != 32
                    || (account.email() != null && (!email(account.email()).equals(account.email()) || !emails.add(account.email())))
                    || (account.resetHash() != null && (!account.resetHash().matches("[0-9a-f]{64}") || account.resetExpiresAt() <= 0 || account.email() == null)))
                    throw new IOException("Archivo de cuentas inválido; no se reinició ninguna cuenta");
                accounts.put(account.id(), account);
            }
        }
    }
    static boolean validId(String id) { return id != null && id.matches("[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}"); }
    static String username(String value) {
        String result = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (!result.matches("[a-z][a-z0-9_-]{2,31}") || Set.of("root", "admin").contains(result))
            throw new IllegalArgumentException("Usuario: 3–32 caracteres, comienza con una letra y utiliza a-z, 0-9, _ o -. Root y admin están reservados.");
        return result;
    }
    private static void password(String password) {
        if (password == null || password.isBlank() || password.length() < 12 || password.length() > 128)
            throw new IllegalArgumentException("Contraseña: entre 12 y 128 caracteres");
    }
    static String email(String value) {
        String email = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        try {
            if (email.length() > 254 || !email.matches("[\\x21-\\x7e]+") || !email.contains("@")) throw new Exception();
            var address = new jakarta.mail.internet.InternetAddress(email, true);
            address.validate();
            if (!email.equals(address.getAddress()) || address.getPersonal() != null || address.isGroup()) throw new Exception();
        } catch (Exception error) { throw new IllegalArgumentException("Introduce un correo válido, sin nombre ni espacios"); }
        return email;
    }
    private Account emailed(String email) {
        return accounts.values().stream().filter(a -> email.equals(a.email())).findFirst().orElse(null);
    }
    public boolean registrationEnabled() { return registrationEnabled; }
    public int sessionSeconds() { return Math.toIntExact(duration / 1000); }
    public Account account(String id) {
        Account account = accounts.get(id);
        if (account == null) throw new IllegalArgumentException("Cuenta inexistente");
        return account;
    }
    private Account named(String username) {
        return accounts.values().stream().filter(a -> a.username().equals(username)).findFirst().orElse(null);
    }
    public List<Map<String, Object>> publicAccounts() {
        return accounts.values().stream().sorted(Comparator.comparing(Account::username))
            .map(a -> Map.<String, Object>of("id", a.id(), "username", a.username(), "createdAt", a.createdAt())).toList();
    }
    private synchronized void limit(String key, int maximum) {
        long now = clock.getAsLong();
        attempts.entrySet().removeIf(e -> e.getValue().isEmpty() || now - e.getValue().peekLast() >= 60_000);
        if (!attempts.containsKey(key) && attempts.size() >= 4096) throw new RateLimited();
        ArrayDeque<Long> values = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!values.isEmpty() && now - values.peekFirst() >= 60_000) values.removeFirst();
        if (values.size() >= maximum) throw new RateLimited();
        values.addLast(now);
    }
    public void limitClaim(String id) { limit("claim:" + id, 6); }
    private Account credential(String id, String name, String password, long createdAt, String email) throws Exception {
        password(password);
        byte[] salt = new byte[16]; random.nextBytes(salt);
        String encoded = Base64.getEncoder().encodeToString(salt);
        return new Account(id, name, encoded, AdminAuth.derive(password, encoded, ITERATIONS), ITERATIONS, createdAt, email, null, 0);
    }
    public synchronized Account register(String name, String password, String email, String address) throws Exception {
        if (!registrationEnabled) throw new SecurityException("El registro está cerrado por el administrador");
        limit("register:" + address, 6);
        name = username(name); password(password); email = email(email);
        if (named(name) != null) throw new IllegalArgumentException("Ese nombre de usuario no está disponible");
        if (emailed(email) != null) throw new IllegalArgumentException("No se puede usar ese correo. Inicia sesión o solicita recuperar tu contraseña.");
        if (accounts.size() >= 1000) throw new IllegalArgumentException("Límite de cuentas alcanzado");
        Account account = credential(UUID.randomUUID().toString(), name, password, clock.getAsLong(), email);
        List<Account> updated = new ArrayList<>(accounts.values()); updated.add(account);
        write(updated); accounts.put(account.id(), account);
        return account;
    }
    private boolean matches(Account account, String password) throws Exception {
        if (password == null || password.length() > 128) return false;
        String candidate = AdminAuth.derive(password, account == null ? dummySalt : account.salt(), account == null ? ITERATIONS : account.iterations());
        return account != null && MessageDigest.isEqual(candidate.getBytes(StandardCharsets.US_ASCII), account.hash().getBytes(StandardCharsets.US_ASCII));
    }
    public synchronized String login(String name, String password, String address) throws Exception {
        limit("login-ip:" + address, 20);
        String normalized = name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 32) normalized = "invalid";
        limit("login-user:" + normalized, 8);
        Account account = named(normalized);
        if (!matches(account, password)) throw new Unauthorized("Usuario o contraseña incorrectos");
        long now = clock.getAsLong();
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt() <= now);
        var own = sessions.entrySet().stream().filter(e -> e.getValue().accountId().equals(account.id()))
            .sorted(Comparator.comparingLong(e -> e.getValue().expiresAt())).toList();
        if (own.size() >= 8) sessions.remove(own.get(0).getKey());
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(GameWorld.hash(token), new Session(account.id(), now + duration));
        return token;
    }
    public Session session(String token) {
        return token == null || token.length() > 128 ? null : sessionHash(GameWorld.hash(token));
    }
    public Session sessionHash(String hash) {
        Session session = hash == null ? null : sessions.get(hash);
        if (session == null) return null;
        if (session.expiresAt() <= clock.getAsLong() || !accounts.containsKey(session.accountId())) {
            sessions.remove(hash); return null;
        }
        return session;
    }
    public Session require(String token) {
        Session session = session(token);
        if (session == null) throw new Unauthorized("Inicia sesión para acceder al juego");
        return session;
    }
    public void logout(String token) { if (token != null && token.length() <= 128) sessions.remove(GameWorld.hash(token)); }
    public synchronized void changePassword(String id, String previous, String password) throws Exception {
        limit("password:" + id, 5);
        Account account = account(id);
        if (!matches(account, previous)) throw new Unauthorized("La contraseña actual es incorrecta");
        resetPassword(id, password);
    }
    public synchronized void resetPassword(String id, String password) throws Exception {
        Account old = account(id), updated = credential(id, old.username(), password, old.createdAt(), old.email());
        replace(updated);
        sessions.entrySet().removeIf(e -> e.getValue().accountId().equals(id));
    }
    private void replace(Account updated) throws IOException {
        String id = updated.id();
        List<Account> next = new ArrayList<>(accounts.values()); next.removeIf(a -> a.id().equals(id)); next.add(updated);
        write(next); accounts.put(id, updated);
    }
    public synchronized void changeEmail(String id, String currentPassword, String value) throws Exception {
        limit("email:" + id, 5);
        Account old = account(id);
        if (!matches(old, currentPassword)) throw new Unauthorized("La contraseña actual es incorrecta");
        String email = email(value); Account owner = emailed(email);
        if (owner != null && !owner.id().equals(id)) throw new IllegalArgumentException("No se puede usar ese correo");
        replace(new Account(id, old.username(), old.salt(), old.hash(), old.iterations(), old.createdAt(), email, null, 0));
        sessions.entrySet().removeIf(e -> e.getValue().accountId().equals(id));
    }
    public void limitRecovery(String email, String address) {
        limit("recovery-ip:" + address, 5); limit("recovery-email:" + GameWorld.hash(email), 3);
    }
    synchronized Recovery beginRecovery(String email, int minutes) throws IOException {
        if (minutes < 5 || minutes > 60) throw new IllegalArgumentException("Caducidad de recuperación inválida");
        Account old = emailed(email);
        if (old == null) return null;
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        replace(new Account(old.id(), old.username(), old.salt(), old.hash(), old.iterations(), old.createdAt(),
            old.email(), GameWorld.hash(token), clock.getAsLong() + minutes * 60_000L));
        return new Recovery(old.email(), token);
    }
    synchronized void discardRecovery(String token) throws IOException {
        String hash = GameWorld.hash(token);
        Account old = accounts.values().stream().filter(a -> hash.equals(a.resetHash())).findFirst().orElse(null);
        if (old != null) replace(new Account(old.id(), old.username(), old.salt(), old.hash(), old.iterations(), old.createdAt(), old.email(), null, 0));
    }
    public synchronized String recoverPassword(String token, String password, String address) throws Exception {
        limit("redeem-ip:" + address, 10);
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Enlace inválido o caducado. Solicita uno nuevo.");
        String hash = GameWorld.hash(token);
        Account old = accounts.values().stream().filter(a -> a.resetHash() != null && MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII), a.resetHash().getBytes(StandardCharsets.US_ASCII))).findFirst().orElse(null);
        if (old == null || old.resetExpiresAt() <= clock.getAsLong()) throw new IllegalArgumentException("Enlace inválido o caducado. Solicita uno nuevo.");
        resetPassword(old.id(), password); // Password, token consumption and persistence are one atomic account update.
        return old.email();
    }
    private void write(List<Account> values) throws IOException {
        values.sort(Comparator.comparing(Account::username));
        Path temporary = file.resolveSibling("accounts.partial");
        try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            try { Files.setPosixFilePermissions(temporary, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")); } catch (UnsupportedOperationException ignored) {}
            ByteBuffer bytes = ByteBuffer.wrap(json.writerWithDefaultPrettyPrinter().writeValueAsBytes(values));
            while (bytes.hasRemaining()) output.write(bytes); output.force(true);
        }
        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
