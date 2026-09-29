package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class PlayerAuthTest {
    @TempDir Path dir;
    ObjectMapper json=new ObjectMapper();AtomicLong now=new AtomicLong(1_800_000_000_000L);
    PlayerAuth auth;
    static final String PASSWORD="Prueba-con-clave-987";
    @BeforeEach void setup()throws Exception{auth=new PlayerAuth(dir,json,true,1,now::get);}

    @Test void passwordsAreSaltedAndAccountsSurviveRestartButSessionsDoNot()throws Exception{
        var a=auth.register("Alicia",PASSWORD,"Alicia".strip().toLowerCase()+"@example.test","one");var b=auth.register("beatriz",PASSWORD,"beatriz".strip().toLowerCase()+"@example.test","two");
        assertEquals("alicia",a.username());assertNotEquals(a.salt(),b.salt());assertNotEquals(a.hash(),b.hash());
        assertEquals(600000,a.iterations());String token=auth.login("ALICIA",PASSWORD,"one");
        assertEquals(a.id(),auth.require(token).accountId());String disk=Files.readString(dir.resolve("accounts.json"));
        assertFalse(disk.contains(PASSWORD));assertFalse(disk.contains(token));
        var restarted=new PlayerAuth(dir,json,true,1,now::get);assertNull(restarted.session(token));
        assertEquals(a.id(),restarted.require(restarted.login("alicia",PASSWORD,"one")).accountId());
        assertFalse(json.writeValueAsString(auth.publicAccounts()).contains("hash"));
    }
    @Test void invalidDuplicateAndReservedAccountsCannotBeCreated()throws Exception{
        assertThrows(IllegalArgumentException.class,()->auth.register("root",PASSWORD,"root".strip().toLowerCase()+"@example.test","one"));
        assertThrows(IllegalArgumentException.class,()->auth.register("../otra",PASSWORD,"../otra".strip().toLowerCase()+"@example.test","one"));
        assertThrows(IllegalArgumentException.class,()->auth.register("alicia","corta","alicia".strip().toLowerCase()+"@example.test","one"));
        assertThrows(IllegalArgumentException.class,()->auth.register("alicia"," ".repeat(12),"alicia".strip().toLowerCase()+"@example.test","one"));
        auth.register("alicia",PASSWORD,"alicia".strip().toLowerCase()+"@example.test","one");
        assertThrows(IllegalArgumentException.class,()->auth.register(" ALICIA ",PASSWORD," ALICIA ".strip().toLowerCase()+"@example.test","one"));
        assertEquals(1,auth.publicAccounts().size());
    }
    @Test void closedRegistrationStillAllowsExistingPlayersToLogin()throws Exception{
        auth.register("alicia",PASSWORD,"alicia".strip().toLowerCase()+"@example.test","one");var closed=new PlayerAuth(dir,json,false,12,now::get);
        assertThrows(SecurityException.class,()->closed.register("otro",PASSWORD,"otro".strip().toLowerCase()+"@example.test","two"));
        assertNotNull(closed.session(closed.login("alicia",PASSWORD,"one")));
    }
    @Test void sessionsExpireAndLogoutInvalidatesOnlyTheGivenSession()throws Exception{
        auth.register("alicia",PASSWORD,"alicia".strip().toLowerCase()+"@example.test","one");String a=auth.login("alicia",PASSWORD,"one"),b=auth.login("alicia",PASSWORD,"one");
        assertNotEquals(a,b);auth.logout(a);assertNull(auth.session(a));assertNotNull(auth.session(b));
        now.addAndGet(3_600_000);assertNull(auth.session(b));assertThrows(PlayerAuth.Unauthorized.class,()->auth.require(b));
    }
    @Test void passwordChangesAndRootResetsRevokeEverySession()throws Exception{
        var a=auth.register("alicia",PASSWORD,"alicia".strip().toLowerCase()+"@example.test","one");String first=auth.login("alicia",PASSWORD,"one"),second=auth.login("alicia",PASSWORD,"one");
        assertThrows(PlayerAuth.Unauthorized.class,()->auth.changePassword(a.id(),"Equivocada-123",PASSWORD));
        assertNotNull(auth.session(first));auth.changePassword(a.id(),PASSWORD,"Nueva-secreta-987");
        assertNull(auth.session(first));assertNull(auth.session(second));
        assertThrows(PlayerAuth.Unauthorized.class,()->auth.login("alicia",PASSWORD,"one"));
        String third=auth.login("alicia","Nueva-secreta-987","one");auth.resetPassword(a.id(),"Root-restablece-654");assertNull(auth.session(third));
        assertNotNull(auth.session(auth.login("alicia","Root-restablece-654","one")));
    }
    @Test void unknownUsersHaveTheSameFailureAndAttemptsAreLimitedAcrossAddresses()throws Exception{
        auth.register("alicia",PASSWORD,"alicia".strip().toLowerCase()+"@example.test","one");
        String known=assertThrows(PlayerAuth.Unauthorized.class,()->auth.login("alicia","Equivocada-123","one")).getMessage();
        assertEquals(known,assertThrows(PlayerAuth.Unauthorized.class,()->auth.login("desconocido","Equivocada-123","one")).getMessage());
        for(int i=1;i<8;i++){String address="address-"+i;assertThrows(PlayerAuth.Unauthorized.class,()->auth.login("ALICIA","Equivocada-123",address));}
        assertThrows(PlayerAuth.RateLimited.class,()->auth.login("alicia",PASSWORD,"different"));
        now.addAndGet(60000);assertNotNull(auth.session(auth.login("alicia",PASSWORD,"different")));
    }
    @Test void corruptedCredentialFileFailsWithoutResettingAccounts()throws Exception{
        Path path=dir.resolve("accounts.json");Files.writeString(path,"not json");
        assertThrows(Exception.class,()->new PlayerAuth(dir,json,true,12));assertEquals("not json",Files.readString(path));
    }
}
