package dev.aincrad;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
class LegacyWorldTest {
 @TempDir Path dir;
 @Test void shippedVersionOneWorldMigratesAndCanStartAllNewSpecialties()throws Exception{
  var json=new ObjectMapper();var file=dir.resolve("world.json");try(var in=getClass().getResourceAsStream("/world-v1.json")){Files.copy(in,file);}var w=WorldData.load(file,json);assertEquals(7,w.schemaRevision);assertEquals(8,w.characterOptions.specializations.size());assertEquals(33,w.characterOptions.skills.size());assertNull(w.floors.get(0).training);assertTrue(w.floors.get(0).resources.isEmpty());
  try(var store=new PlayerStore(dir.resolve("players"),json)){var game=new GameWorld(w,store,(id,e)->{});for(var s:w.characterOptions.specializations){var profile=game.join("Legado","",new WorldData.Appearance("male","calm","#342f32","#dfb18b",s.classId(),"human",s.id()));var p=game.online.get(profile.get("id"));assertNotNull(p.outfit());assertNotNull(p.weapons());}game.tick(.05);}
  var saved=json.readTree(file.toFile());assertEquals(7,saved.path("schemaRevision").asInt());try(var files=Files.list(dir)){assertEquals(1,files.filter(p->p.toString().endsWith(".bak")).count());}WorldData.load(file,json);try(var files=Files.list(dir)){assertEquals(1,files.filter(p->p.toString().endsWith(".bak")).count());}
 }
}
