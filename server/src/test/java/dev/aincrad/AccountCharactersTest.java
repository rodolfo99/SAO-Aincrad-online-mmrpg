package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountCharactersTest {
    @TempDir Path dir;ObjectMapper json=new ObjectMapper();PlayerStore store;GameWorld world;
    String owner=UUID.randomUUID().toString(),other=UUID.randomUUID().toString();
    @BeforeEach void setup()throws Exception{store=new PlayerStore(dir.resolve("players"),json);world=new GameWorld(WorldData.load(Path.of("../world/world.json"),json),store,(p,e)->{});}
    @AfterEach void close()throws Exception{store.close();}
    @Test void accountsOwnMultipleCharactersAndOtherAccountsCannotEnterThem()throws Exception{
        var first=world.joinAccount(owner,"","Primero",null);String id=(String)first.get("id");assertFalse(first.containsKey("token"));world.leave(id);
        var second=world.joinAccount(owner,"","Segundo",null);world.leave((String)second.get("id"));assertEquals(2,world.ownedCharacters(owner).size());assertTrue(world.ownedCharacters(other).isEmpty());
        assertThrows(IllegalArgumentException.class,()->world.joinAccount(other,id,"Intruso",null));
        var resumed=world.joinAccount(owner,id,"Ignorado",null);assertEquals(id,resumed.get("id"));assertEquals("Primero",world.online.get(id).name);
        assertThrows(IllegalArgumentException.class,()->world.joinAccount(owner,id,"Otra pestaña",null));
        assertFalse(json.writeValueAsString(world.snapshot(id)).contains("accountId"));
    }
    @Test void legacyClaimPreservesProgressAndInvalidatesLegacyAccessAcrossRestart()throws Exception{
        var legacy=world.join("Anterior","");String id=(String)legacy.get("id"),token=(String)legacy.get("token");var p=world.online.get(id);
        p.xp=750;p.col=234;p.potions=17;p.crafting.materials.put("iron",25);world.leave(id);
        // Missing accountId is exactly the pre-account disk format.
        var old=json.valueToTree(p.profile());((com.fasterxml.jackson.databind.node.ObjectNode)old).remove("accountId");Files.writeString(dir.resolve("players").resolve(id+".json"),json.writeValueAsString(old));
        world=new GameWorld(world.data,store,(who,e)->{});world.claimLegacy(owner,token);
        assertThrows(IllegalArgumentException.class,()->world.claimLegacy(other,token));
        assertThrows(IllegalArgumentException.class,()->world.join("Intruso",token));
        var restarted=new GameWorld(world.data,store,(who,e)->{});restarted.joinAccount(owner,id,"Ignorado",null);var loaded=restarted.online.get(id);
        assertEquals("Anterior",loaded.name);assertEquals(750,loaded.xp);assertEquals(234,loaded.col);assertEquals(17,loaded.potions);assertEquals(25,loaded.crafting.materials.get("iron"));
        assertTrue(Files.list(dir.resolve("backups")).anyMatch(f->f.getFileName().toString().startsWith("character-")));
    }
    @Test void assignmentRequiresOfflineCharacterAndRevokesOldOwnersAccess()throws Exception{
        String id=(String)world.joinAccount(owner,"","Viajero",null).get("id");
        assertThrows(IllegalArgumentException.class,()->world.assignAccount(id,other));world.leave(id);world.assignAccount(id,other);
        assertTrue(world.ownedCharacters(owner).isEmpty());assertEquals(1,world.ownedCharacters(other).size());
        assertThrows(IllegalArgumentException.class,()->world.joinAccount(owner,id,"Intruso",null));assertEquals(id,world.joinAccount(other,id,"Viajero",null).get("id"));
    }
}
