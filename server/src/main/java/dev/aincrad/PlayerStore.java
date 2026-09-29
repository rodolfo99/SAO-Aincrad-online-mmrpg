package dev.aincrad;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.*;

/** Profiles never use a display name as an authentication key. Writes are forced then atomically renamed. */
public class PlayerStore implements AutoCloseable {
    public record Profile(String id,String tokenHash,String name,int floor,double x,double z,int hp,int xp,int col,int potions,int kills,boolean quest,boolean reward,boolean unlocked,int weapon,WorldData.Appearance appearance,Map<String,Integer> attributeRanks,Map<String,Integer> talentRanks,Pvp.State pvp,String weaponSetId,Crafting.State crafting,Skills.State skills,String accountId) {}
    private final Path directory; private final ObjectMapper json;
    private final FileChannel lockChannel; private final java.nio.channels.FileLock lock;
    public PlayerStore(Path directory,ObjectMapper json) throws IOException {
        this.directory=directory;this.json=json;Files.createDirectories(directory);
        lockChannel=FileChannel.open(directory.resolve(".server.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        lock=lockChannel.tryLock();if(lock==null)throw new IOException("Otro servidor utiliza DATA_DIR");
    }
    public List<Profile> load() throws IOException {
        List<Profile> list=new ArrayList<>();
        try(var paths=Files.list(directory)){for(Path p:paths.filter(p->p.toString().endsWith(".json")).toList())list.add(json.readValue(p.toFile(),Profile.class));}
        return list; // Corrupt data fails startup loudly; no silent reset.
    }
    public Gathering.Store gathering()throws IOException{return new Gathering.Store(directory.getParent(),json);}
    public PvpJournal journal()throws IOException{return new PvpJournal(directory.getParent(),json);}
    public void save(Profile p) throws IOException {
        if(!p.id().matches("[a-f0-9-]{36}"))throw new IOException("ID inválido");
        Path target=directory.resolve(p.id()+".json"),temp=directory.resolve(p.id()+".partial");
        try(FileChannel out=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){
            ByteBuffer bytes=ByteBuffer.wrap(json.writerWithDefaultPrettyPrinter().writeValueAsBytes(p));while(bytes.hasRemaining())out.write(bytes);out.force(true);
        }
        Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    public void backup(Profile p)throws IOException{Path dir=directory.getParent().resolve("backups");Files.createDirectories(dir);Path file=dir.resolve("character-"+p.id()+"-"+System.currentTimeMillis()+"-"+UUID.randomUUID()+".json");Files.write(file,json.writerWithDefaultPrettyPrinter().writeValueAsBytes(p),StandardOpenOption.CREATE_NEW);}
    public void close() throws IOException {lock.release();lockChannel.close();}
}
