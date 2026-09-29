package dev.aincrad;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
class NpcAiTest {
 @TempDir Path dir;ObjectMapper json=new ObjectMapper();HttpServer mock;NpcAi ai;AtomicReference<JsonNode> payload=new AtomicReference<>();AtomicInteger status=new AtomicInteger(200),delay=new AtomicInteger();ExecutorService httpWorkers;
 @BeforeEach void setup()throws Exception{mock=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);httpWorkers=Executors.newCachedThreadPool();mock.setExecutor(httpWorkers);mock.createContext("/api/chat",exchange->{try{payload.set(json.readTree(exchange.getRequestBody()));Thread.sleep(delay.get());byte[] data="{\"model\":\"test-model\",\"created_at\":\"2026-09-28T00:00:00Z\",\"message\":{\"role\":\"assistant\",\"content\":\"Soy Lyra. Sigue el sendero hacia el norte.\"},\"done\":true,\"done_reason\":\"stop\",\"prompt_eval_count\":10,\"eval_count\":9}".getBytes();exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(status.get(),data.length);exchange.getResponseBody().write(data);}catch(Exception ignored){}finally{exchange.close();}});mock.start();ai=new NpcAi(dir,json);}
 @AfterEach void close(){ai.close();mock.stop(0);httpWorkers.shutdownNow();}
 NpcAi.Config config(){var c=new NpcAi.Config();c.baseUrl="http://127.0.0.1:"+mock.getAddress().getPort();c.model="test-model";c.enabled=true;c.cooldownSeconds=1;return c;}
 @Test void actualSpringAiSendsPersonaModelAndOptionsToOllama()throws Exception{var c=config();var r=ai.test(c,"lyra","¿Dónde está el portal?");assertFalse(r.fallback(),r.reason());assertTrue(r.text().contains("Lyra"));assertEquals("test-model",payload.get().path("model").asText());assertEquals(180,payload.get().path("options").path("num_predict").asInt());assertTrue(payload.get().path("messages").get(0).path("content").asText().contains("No tienes herramientas"));assertEquals("user",payload.get().path("messages").get(1).path("role").asText());}
 @Test void unavailableProviderUsesConfiguredFallback()throws Exception{status.set(503);var c=config();c.bindings.get(0).fallback="Vuelve más tarde, viajero.";var r=ai.test(c,"lyra","Hola");assertTrue(r.fallback());assertEquals("Vuelve más tarde, viajero.",r.text());}
 @Test void timeoutIsBoundedAndDoesNotStopSimulationThread()throws Exception{delay.set(1800);var c=config();c.timeoutSeconds=1;ai.save(c,WorldData.load(Path.of("../world/world.json"),json));var future=new CompletableFuture<NpcAi.Reply>();long start=System.nanoTime();ai.ask("p1","lyra","pradera","Hola",future::complete);assertTrue((System.nanoTime()-start)/1e6<250);var reply=future.get(4,TimeUnit.SECONDS);assertTrue(reply.fallback());assertTrue((System.nanoTime()-start)/1e9<3.5);}
 @Test void settingsPersistAndUnknownBindingIsRejected()throws Exception{var c=config();c.temperature=.9;ai.save(c,WorldData.load(Path.of("../world/world.json"),json));ai.close();ai=new NpcAi(dir,json);assertEquals(.9,ai.config().temperature);c.bindings.get(0).npcId="fake";assertThrows(IllegalArgumentException.class,()->ai.save(c,WorldData.load(Path.of("../world/world.json"),json)));}
 @Test void invalidUrlsLimitsAndDuplicateBindingsAreRejected(){var c=config();c.baseUrl="file:///etc/passwd";assertThrows(IllegalArgumentException.class,()->NpcAi.validate(c));c.baseUrl="http://localhost:11434";c.maxConcurrent=500;assertThrows(IllegalArgumentException.class,()->NpcAi.validate(c));}
}
