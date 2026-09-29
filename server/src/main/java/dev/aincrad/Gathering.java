package dev.aincrad;

import java.util.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Shared resource cooldowns and replayable harvest receipts prevent restart duplication. */
public final class Gathering {
 public record Resource(String id,String name,String professionId,String materialId,int minProfessionLevel,int amount,int experience,int respawnSeconds,String kind,String color){}
 public record Node(String id,String typeId,double x,double z){}
 public static class Rules {public boolean enabled=true;public double range=3,cooldownSeconds=1.5;public List<Resource> resources;}
 public record Receipt(long sequence,String playerId,String nodeId,String materialId,int amount,String professionId,int experience,long readyAt,long playerReadyAt){}
 public static Resource resource(Rules rules,String id){return rules.resources.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Recurso inexistente"));}
 public static void validate(Rules r,Crafting.Rules crafting){
  if(r==null||!Double.isFinite(r.range)||r.range<1.5||r.range>6||!Double.isFinite(r.cooldownSeconds)||r.cooldownSeconds<.5||r.cooldownSeconds>60||r.resources==null||r.resources.size()>100)throw new IllegalArgumentException("Reglas de recolección inválidas");
  Set<String> ids=new HashSet<>();for(var v:r.resources)if(v==null||!Crafting.id(v.id())||!ids.add(v.id())||!Crafting.name(v.name())||crafting.professions.stream().noneMatch(j->j.id().equals(v.professionId())&&Set.of("mine","forest").contains(j.stationRole()))||crafting.materials.stream().noneMatch(m->m.id().equals(v.materialId()))||v.minProfessionLevel()<1||v.minProfessionLevel()>crafting.maxProfessionLevel||v.amount()<1||v.amount()>100||v.experience()<0||v.experience()>1000||v.respawnSeconds()<1||v.respawnSeconds()>86400||!Set.of("rock","tree").contains(v.kind())||!WorldData.color(v.color()))throw new IllegalArgumentException("Tipo de recurso inválido");
 }
 public static final class Store {
  final Path directory;final ObjectMapper json;long sequence;final List<Receipt> receipts=new ArrayList<>();final Map<String,Long> ready=new HashMap<>();
  Store(Path data,ObjectMapper json)throws IOException{directory=data.resolve("harvest-journal");this.json=json;Files.createDirectories(directory);try(var files=Files.list(directory)){for(var f:files.filter(p->p.toString().endsWith(".json")).sorted().toList()){var r=json.readValue(f.toFile(),Receipt.class);receipts.add(r);sequence=Math.max(sequence,r.sequence());ready.put(r.nodeId(),r.readyAt());}}}
  Receipt commit(String playerId,Node node,Resource r,long now,Rules rules)throws IOException{
   var receipt=new Receipt(sequence+1,playerId,node.id(),r.materialId(),r.amount(),r.professionId(),r.experience(),now+r.respawnSeconds()*1000L,now+(long)(rules.cooldownSeconds*1000));var temp=directory.resolve("pending.partial");var file=directory.resolve(String.format(Locale.ROOT,"%012d.json",receipt.sequence()));
   try(var out=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){var bytes=ByteBuffer.wrap(json.writeValueAsBytes(receipt));while(bytes.hasRemaining())out.write(bytes);out.force(true);}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE);sequence++;ready.put(node.id(),receipt.readyAt());receipts.add(receipt);return receipt;
  }
 }
}
