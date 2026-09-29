package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class PasswordRecoveryTest {
    @TempDir Path dir;
    ObjectMapper json=new ObjectMapper();AtomicLong now=new AtomicLong(1_800_000_000_000L);
    PlayerAuth auth;static final String PASSWORD="Original-prueba-123",NEXT="Restablecida-456";
    @BeforeEach void setup()throws Exception{auth=new PlayerAuth(dir,json,true,12,now::get);auth.register("alicia",PASSWORD,"Alicia@Example.test","one");}
    @Test void emailValidationUniquenessAndIpLiteralSender()throws Exception{
        assertEquals("noreply@[203.0.113.20]",PlayerAuth.email("noreply@[203.0.113.20]"));
        assertThrows(IllegalArgumentException.class,()->auth.register("beatriz",PASSWORD,"ALICIA@EXAMPLE.TEST","two"));
        for(String email:new String[]{"", "no-correo", "a@example.test\r\nBcc:b@example.test", "a@example.test,b@example.test", "Alias <a@example.test>"})assertThrows(IllegalArgumentException.class,()->PlayerAuth.email(email));
    }
    @Test void persistedHashedTokenIsSingleUseAndRevokesEverySession()throws Exception{
        String first=auth.login("alicia",PASSWORD,"one"),second=auth.login("alicia",PASSWORD,"two");
        var reset=auth.beginRecovery("alicia@example.test",30);assertEquals(43,reset.token().length());
        String disk=Files.readString(dir.resolve("accounts.json"));assertFalse(disk.contains(reset.token()));assertTrue(disk.contains(GameWorld.hash(reset.token())));
        assertThrows(IllegalArgumentException.class,()->auth.recoverPassword(reset.token(),"short","one"));assertNotNull(auth.session(first));
        assertEquals("alicia@example.test",auth.recoverPassword(reset.token(),NEXT,"one"));assertNull(auth.session(first));assertNull(auth.session(second));
        assertThrows(IllegalArgumentException.class,()->auth.recoverPassword(reset.token(),PASSWORD,"one"));
        assertThrows(PlayerAuth.Unauthorized.class,()->auth.login("alicia",PASSWORD,"one"));assertNotNull(auth.session(auth.login("alicia",NEXT,"one")));
        var restarted=new PlayerAuth(dir,json,true,12,now::get);assertThrows(IllegalArgumentException.class,()->restarted.recoverPassword(reset.token(),PASSWORD,"one"));
    }
    @Test void linksSurviveRestartExpireAndNewRequestsReplacePreviousLinks()throws Exception{
        assertNull(auth.beginRecovery("missing@example.test",30));var old=auth.beginRecovery("alicia@example.test",30);var fresh=auth.beginRecovery("alicia@example.test",30);
        assertThrows(IllegalArgumentException.class,()->auth.recoverPassword(old.token(),NEXT,"one"));
        var restarted=new PlayerAuth(dir,json,true,12,now::get);assertEquals("alicia@example.test",restarted.recoverPassword(fresh.token(),NEXT,"one"));
        var expired=restarted.beginRecovery("alicia@example.test",5);now.addAndGet(300_000);assertThrows(IllegalArgumentException.class,()->restarted.recoverPassword(expired.token(),PASSWORD,"one"));
        assertNotNull(restarted.session(restarted.login("alicia",NEXT,"one")));
    }
    @Test void concurrentRedemptionCanChangePasswordOnlyOnce()throws Exception{
        var reset=auth.beginRecovery("alicia@example.test",30);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try{Callable<Boolean> redeem=()->{start.await();try{auth.recoverPassword(reset.token(),NEXT,Thread.currentThread().getName());return true;}catch(IllegalArgumentException e){return false;}};
            var a=pool.submit(redeem);var b=pool.submit(redeem);start.countDown();assertNotEquals(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS));
        }finally{pool.shutdownNow();}
    }
    @Test void emailChangeAndRootResetInvalidatePendingLinks()throws Exception{
        String session=auth.login("alicia",PASSWORD,"one");String id=auth.require(session).accountId();var reset=auth.beginRecovery("alicia@example.test",30);
        assertThrows(PlayerAuth.Unauthorized.class,()->auth.changeEmail(id,"Incorrecta-789","new@example.test"));assertNotNull(auth.session(session));
        auth.changeEmail(id,PASSWORD,"new@example.test");assertNull(auth.session(session));assertNull(auth.beginRecovery("alicia@example.test",30));
        assertThrows(IllegalArgumentException.class,()->auth.recoverPassword(reset.token(),NEXT,"one"));
        var next=auth.beginRecovery("new@example.test",30);auth.resetPassword(id,NEXT);assertThrows(IllegalArgumentException.class,()->auth.recoverPassword(next.token(),PASSWORD,"one"));
    }
    @Test void oldAccountsWithoutEmailCanAddItAfterReauthentication()throws Exception{
        var data=json.readTree(dir.resolve("accounts.json").toFile());((com.fasterxml.jackson.databind.node.ObjectNode)data.get(0)).remove(java.util.List.of("email","resetHash","resetExpiresAt"));json.writeValue(dir.resolve("accounts.json").toFile(),data);
        var old=new PlayerAuth(dir,json,true,12,now::get);String session=old.login("alicia",PASSWORD,"one"),id=old.require(session).accountId();assertNull(old.account(id).email());
        old.changeEmail(id,PASSWORD,"recovered@example.test");assertNotNull(old.beginRecovery("recovered@example.test",30));
    }
    @Test void recoveryIsLimitedForUnknownAddressesToo(){
        for(int i=0;i<3;i++)auth.limitRecovery("missing@example.test","address-"+i);
        assertThrows(PlayerAuth.RateLimited.class,()->auth.limitRecovery("missing@example.test","different"));
        now.addAndGet(60_000);assertDoesNotThrow(()->auth.limitRecovery("missing@example.test","different"));
    }
    @Test void resetUrlUsesConfiguredOriginAndRejectsInjectedCredentialsOrPaths(){
        assertEquals("https://203.0.113.4",PasswordRecovery.validateUrl("https://203.0.113.4/"));assertEquals("http://192.168.1.10:8080",PasswordRecovery.validateUrl("http://192.168.1.10:8080"));
        for(String url:new String[]{"https://user:password@example.test", "https://example.test/evil", "https://example.test/?x=1", "javascript:alert(1)", "http://999.0.0.1"})assertThrows(IllegalArgumentException.class,()->PasswordRecovery.validateUrl(url));
    }
}
