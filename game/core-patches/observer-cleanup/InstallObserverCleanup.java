import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;
import java.lang.classfile.*;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;

/** Java25 source launcher; stopped server only. Changes only the GM cleanup guard
 * in Player.leaveOlympiadObserverMode, never the observer entry or damage rules. */
public class InstallObserverCleanup
{
    static final String BASE = "olympiad-diversity-v1";
    static final String OLD_MARK = "META-INF/l2j-olympiad-diversity-v1.properties";
    static final String MARK = "META-INF/l2j-observer-cleanup-v1.properties";
    static final String PLAYER = "org/l2jmobius/gameserver/entity/actor/Player.class";
    static final String PLAYER_BASE = "f0022b223ece8287743efb89867559c39f9f4779d2af20b0b22d721fd2f1afc6";
    static String sha(byte[] b)throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    static byte[] entry(JarFile jar,String name)throws Exception
    {var e=jar.getJarEntry(name);if(e==null)throw new IOException("Missing entry: "+name);try(var in=jar.getInputStream(e)){return in.readAllBytes();}}
    static Properties properties(JarFile jar,String name)throws Exception
    {var p=new Properties();p.load(new ByteArrayInputStream(entry(jar,name)));return p;}
    static String payload(JarFile jar,Map<String,byte[]> replacements,String excluded)throws Exception
    {
        var digest=MessageDigest.getInstance("SHA-256");var names=new TreeSet<String>();jar.stream().forEach(e->names.add(e.getName()));names.addAll(replacements.keySet());names.remove(excluded);
        for(String name:names){byte[] key=name.getBytes(StandardCharsets.UTF_8),b=replacements.containsKey(name)?replacements.get(name):entry(jar,name);
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(key.length).array());digest.update(key);digest.update(java.nio.ByteBuffer.allocate(8).putLong(b.length).array());digest.update(b);}
        return HexFormat.of().formatHex(digest.digest());
    }
    static void sources(JarFile jar,Path patches)throws Exception
    {
        for(String[] spec:new String[][] {
            {"META-INF/l2j-debuff-no-refresh-v1.properties","debuff-no-refresh/DebuffCategoryGuard.java"},
            {"META-INF/l2j-arena-boundary-v1.properties","arena-boundary/ArenaBoundaryGuard.java"},
            {OLD_MARK,"olympiad-diversity/OlympiadDiversity.java"}})
            if(!sha(Files.readAllBytes(patches.resolve(spec[1]))).equals(properties(jar,spec[0]).getProperty("source")))
                throw new IOException("Prior helper source changed: "+spec[1]+"; restore matching backup before upgrade");
    }
    public static void main(String[] args)throws Exception
    {
        if(args.length!=2)throw new IllegalArgumentException("Usage: java InstallObserverCleanup.java CORE.jar PATCH_ROOT (server stopped)");
        Path core=Path.of(args[0]).toAbsolutePath(),patches=Path.of(args[1]).toAbsolutePath();
        String sourceHash=sha(Files.readAllBytes(patches.resolve("observer-cleanup/InstallObserverCleanup.java")));
        try(var channel=FileChannel.open(core.resolveSibling(".observer-cleanup-install.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock())
        {
            byte[] original=Files.readAllBytes(core),player;
            try(var jar=new JarFile(core.toFile()))
            {
                sources(jar,patches);
                if(jar.getJarEntry(MARK)!=null)
                {
                    var p=properties(jar,MARK);
                    if(!p.stringPropertyNames().equals(Set.of("base","input","source","playerBase","player","payload"))
                        ||!p.getProperty("input", "").matches("[0-9a-f]{64}")
                        ||!BASE.equals(p.getProperty("base"))||!sourceHash.equals(p.getProperty("source"))||!PLAYER_BASE.equals(p.getProperty("playerBase"))
                        ||!payload(jar,Map.of(),MARK).equals(p.getProperty("payload"))||!sha(entry(jar,PLAYER)).equals(p.getProperty("player")))
                        throw new IOException("Observer cleanup marker/hash mismatch; restore verified backup");
                    System.out.println("Olympiad observer cleanup v1 verified; all four patches intact.");return;
                }
                var old=properties(jar,OLD_MARK);
                if(!"arena-boundary-v1".equals(old.getProperty("base"))||!payload(jar,Map.of(),OLD_MARK).equals(old.getProperty("payload"))
                    ||!PLAYER_BASE.equals(sha(entry(jar,PLAYER))))throw new IOException("Unsupported or tampered diversity core / Player; install all three prior guards first");
                Map<String,byte[]> hierarchy=new HashMap<>();
                for(var e:jar.stream().filter(e->e.getName().endsWith(".class")).toList())hierarchy.put(e.getName(),entry(jar,e.getName()));
                player=patch(entry(jar,PLAYER),hierarchy);
            }
            Path temp=Files.createTempDirectory(core.getParent(),".observer-cleanup-build-");
            try
            {
                var props=new Properties();props.setProperty("base",BASE);props.setProperty("input",sha(original));props.setProperty("source",sourceHash);
                props.setProperty("playerBase",PLAYER_BASE);props.setProperty("player",sha(player));
                try(var jar=new JarFile(core.toFile())){props.setProperty("payload",payload(jar,Map.of(PLAYER,player),MARK));}
                var meta=new ByteArrayOutputStream();props.store(meta,"Olympiad observer cleanup v1");Path output=temp.resolve("GameServer.jar");
                try(var jar=new JarFile(core.toFile());var out=new JarOutputStream(Files.newOutputStream(output)))
                {
                    for(var e:jar.stream().toList())
                    {String name=e.getName();var copy=new JarEntry(name);copy.setTime(e.getTime());out.putNextEntry(copy);out.write(name.equals(PLAYER)?player:entry(jar,name));out.closeEntry();}
                    out.putNextEntry(new JarEntry(MARK));out.write(meta.toByteArray());out.closeEntry();
                }
                try(var before=new JarFile(core.toFile());var after=new JarFile(output.toFile()))
                {
                    for(var e:before.stream().toList())if(!e.getName().equals(PLAYER)&&!Arrays.equals(entry(before,e.getName()),entry(after,e.getName())))throw new IOException("Changed unrelated entry: "+e.getName());
                    if(!payload(after,Map.of(),MARK).equals(props.getProperty("payload")))throw new IOException("Output verification failed");
                }
                Path backup=core.resolveSibling(core.getFileName()+".pre-observer-cleanup-"+sha(original).substring(0,12));
                if(!Files.exists(backup))Files.copy(core,backup);
                if(!sha(Files.readAllBytes(backup)).equals(sha(original)))throw new IOException("Backup hash differs");
                try(var f=FileChannel.open(backup,StandardOpenOption.WRITE)){f.force(true);}try(var f=FileChannel.open(output,StandardOpenOption.WRITE)){f.force(true);}
                Files.move(output,core,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Installed Olympiad observer cleanup v1; verified backup: "+backup);
            }
            finally{try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
        }
    }
    static byte[] patch(byte[] original,Map<String,byte[]> hierarchy)
    {
        var resolver=ClassHierarchyResolver.ofResourceParsing((ClassDesc type)->
        {String d=type.descriptorString(),r=d.substring(1,d.length()-1)+".class";byte[] b=hierarchy.get(r);return b!=null?new ByteArrayInputStream(b):ClassLoader.getPlatformClassLoader().getResourceAsStream(r);});
        ClassFile cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));int[] counts={0,0};
        byte[] result=cf.transformClass(cf.parse(original),(builder,element)->
        {
            if(element instanceof MethodModel m&&m.methodName().equalsString("leaveOlympiadObserverMode")&&m.methodType().equalsString("()V"))
            {
                counts[0]++;
                builder.transformMethod(m,MethodTransform.transformingCode((code,e)->
                {
                    if(e instanceof InvokeInstruction call&&call.owner().asInternalName().equals("org/l2jmobius/gameserver/entity/actor/Player")
                        &&call.name().equalsString("isGM")&&call.type().equalsString("()Z"))
                    {
                        // Consume the original receiver and always take the existing cleanup path.
                        // No new hooks, timers, bypasses or changes to observer entry/teleport.
                        counts[1]++;code.pop().iconst_0();
                    }
                    else code.with(e);
                }));
            }
            else builder.with(element);
        });
        if(counts[0]!=1||counts[1]!=1)throw new IllegalStateException("Expected one native observer exit GM guard, got "+Arrays.toString(counts));
        return result;
    }
}
