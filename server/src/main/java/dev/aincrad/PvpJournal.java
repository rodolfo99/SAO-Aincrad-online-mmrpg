package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.*;

/** Atomic kill receipts: a crash between two profile writes cannot erase the penalty. */
public final class PvpJournal {
    public record Receipt(long sequence,String kind,String attacker,String victim,boolean murder,long at,Map<String,Pvp.State> states){}
    private final Path directory;private final ObjectMapper json;private long sequence;
    public PvpJournal(Path data,ObjectMapper json)throws IOException{this.directory=data.resolve("pvp-journal");this.json=json;Files.createDirectories(directory);for(var r:receipts())sequence=Math.max(sequence,r.sequence());}
    public List<Receipt> receipts()throws IOException{List<Receipt> values=new ArrayList<>();try(var files=Files.list(directory)){for(Path file:files.filter(f->f.toString().endsWith(".json")).sorted().toList())values.add(json.readValue(file.toFile(),Receipt.class));}return values;}
    public Receipt commit(String kind,String attacker,String victim,boolean murder,long at,Map<String,Pvp.State> states)throws IOException{
        long next=sequence+1;Map<String,Pvp.State> copies=new LinkedHashMap<>();states.forEach((id,state)->{var s=state.copy();s.lastEvent=next;copies.put(id,s);});var r=new Receipt(next,kind,attacker,victim,murder,at,copies);
        Path file=directory.resolve(String.format(Locale.ROOT,"%012d.json",next)),temp=directory.resolve("pending.partial");
        try(var out=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){var bytes=ByteBuffer.wrap(json.writerWithDefaultPrettyPrinter().writeValueAsBytes(r));while(bytes.hasRemaining())out.write(bytes);out.force(true);}Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE);sequence=next;return r;
    }
}
