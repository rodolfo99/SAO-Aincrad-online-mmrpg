package dev.aincrad;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
class AdminAuthTest {
 @TempDir Path dir;ObjectMapper json=new ObjectMapper();
 @Test void requiresPasswordOnFirstStartAndHashesIt()throws Exception{assertThrows(IllegalStateException.class,()->new AdminAuth(dir,json,""));var auth=new AdminAuth(dir,json,"TestOnly-password-123");assertFalse(Files.readString(dir.resolve("admin.json")).contains("TestOnly-password-123"));String token=auth.login("root","TestOnly-password-123","test");assertTrue(auth.valid(token));assertThrows(SecurityException.class,()->auth.login("other","TestOnly-password-123","test"));auth.logout(token);assertFalse(auth.valid(token));}
 @Test void subsequentEnvironmentDoesNotResetPasswordAndChangeRevokesSessions()throws Exception{new AdminAuth(dir,json,"TestOnly-password-123");var auth=new AdminAuth(dir,json,"different-password");String token=auth.login("root","TestOnly-password-123","test");auth.setPassword("Replacement-password-123");assertFalse(auth.valid(token));assertThrows(SecurityException.class,()->auth.login("root","TestOnly-password-123","test"));assertTrue(auth.valid(auth.login("root","Replacement-password-123","test")));}
 @Test void blocksRepeatedPasswordGuessing()throws Exception{var auth=new AdminAuth(dir,json,"TestOnly-password-123");for(int i=0;i<5;i++)assertThrows(SecurityException.class,()->auth.login("root","incorrect","same-client"));assertThrows(IllegalArgumentException.class,()->auth.login("root","TestOnly-password-123","same-client"));}
}
