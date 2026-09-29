package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Root is initialized exactly once; no embedded or default credential. */
public class AdminAuth {
    public record Credential(String username,String salt,String hash,int iterations) {}
    private final Path file;private final ObjectMapper json;private Credential credential;
    private final Map<String,Long> sessions=new ConcurrentHashMap<>();
    private final Map<String,List<Long>> attempts=new HashMap<>();
    public AdminAuth(Path dir,ObjectMapper json,String password)throws Exception {
        this.json=json;file=dir.resolve("admin.json");Files.createDirectories(dir);
        if(Files.exists(file))credential=json.readValue(file.toFile(),Credential.class);
        else {if(password==null||password.length()<12)throw new IllegalStateException("Primer inicio: define ROOT_PASSWORD (mínimo 12 caracteres) o ejecuta scripts/iniciar.sh");setPassword(password);}
    }
    static String derive(String password,String salt,int iterations)throws Exception{
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),Base64.getDecoder().decode(salt),iterations,256);
        try{return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());}finally{spec.clearPassword();}
    }
    public synchronized String login(String username,String password,String address)throws Exception {
        long now=System.currentTimeMillis();attempts.entrySet().removeIf(e->e.getValue().stream().allMatch(t->now-t>60000));
        List<Long> tries=attempts.computeIfAbsent(address,k->new ArrayList<>());tries.removeIf(t->now-t>60000);if(tries.size()>=5)throw new IllegalArgumentException("Demasiados intentos. Espera un minuto.");tries.add(now);
        if(password==null||password.length()>256||!MessageDigest.isEqual(derive(password,credential.salt(),credential.iterations()).getBytes(java.nio.charset.StandardCharsets.UTF_8),credential.hash().getBytes(java.nio.charset.StandardCharsets.UTF_8))||!"root".equals(username))throw new SecurityException("Usuario o contraseña incorrectos");
        tries.clear();sessions.entrySet().removeIf(e->e.getValue()<now);byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);sessions.put(GameWorld.hash(token),now+3600000);return token;
    }
    public boolean valid(String token){return token!=null&&sessions.getOrDefault(GameWorld.hash(token),0L)>System.currentTimeMillis();}
    public void logout(String token){if(token!=null)sessions.remove(GameWorld.hash(token));}
    public synchronized void setPassword(String password)throws Exception {
        if(password==null||password.length()<12||password.length()>128)throw new IllegalArgumentException("Contraseña: entre 12 y 128 caracteres");
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);String encoded=Base64.getEncoder().encodeToString(salt);
        Credential updated=new Credential("root",encoded,derive(password,encoded,210000),210000);
        Path tmp=file.resolveSibling("admin.partial");Files.write(tmp,json.writerWithDefaultPrettyPrinter().writeValueAsBytes(updated));
        try{Files.setPosixFilePermissions(tmp,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignored){}
        Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);credential=updated;sessions.clear();
    }
}
