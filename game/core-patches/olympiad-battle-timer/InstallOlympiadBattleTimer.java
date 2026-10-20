import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;
import java.lang.classfile.*;
import java.lang.classfile.Opcode;
import java.lang.constant.ClassDesc;

/** Java25 stopped-core patch: remove the native signed-short battle counter narrowing. */
public class InstallOlympiadBattleTimer
{
    static final String BASE = "observer-cleanup-v1";
    static final String OLD_MARK = "META-INF/l2j-observer-cleanup-v1.properties";
    static final String MARK = "META-INF/l2j-olympiad-battle-timer-v1.properties";
    static final String TASK = "org/l2jmobius/gameserver/mechanics/olympiad/OlympiadGameTask.class";
    static final String TASK_BASE = "f1bd2e0e8eedc9b39621c622059b2908b09479356ac1d8f2061a9cd073ee7250";
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
    static final String LEGACY_OBSERVER_SOURCE = "7d48e6b2377d04d0ee705f71665855151eb630e128dd4df22ac23468109bc426";
    static void sources(JarFile jar,Path patches)throws Exception
    {
        for(String[] spec:new String[][] {
            {"META-INF/l2j-debuff-no-refresh-v1.properties","debuff-no-refresh/DebuffCategoryGuard.java"},
            {"META-INF/l2j-arena-boundary-v1.properties","arena-boundary/ArenaBoundaryGuard.java"},
            {"META-INF/l2j-olympiad-diversity-v1.properties","olympiad-diversity/OlympiadDiversity.java"}})
            if(!sha(Files.readAllBytes(patches.resolve(spec[1]))).equals(properties(jar,spec[0]).getProperty("source")))
                throw new IOException("Prior helper source changed: "+spec[1]);
        String observer=properties(jar,OLD_MARK).getProperty("source","");
        if(!observer.equals(LEGACY_OBSERVER_SOURCE)&&!observer.equals(sha(Files.readAllBytes(patches.resolve("observer-cleanup/InstallObserverCleanup.java")))))
            throw new IOException("Unsupported observer installer source");
    }
    public static void main(String[] args)throws Exception
    {
        if(args.length!=2)throw new IllegalArgumentException("Usage: java InstallOlympiadBattleTimer.java CORE.jar PATCH_ROOT (server stopped)");
        Path core=Path.of(args[0]).toAbsolutePath(),patches=Path.of(args[1]).toAbsolutePath();
        boolean ordered;
        try(var probe=new JarFile(core.toFile())){ordered=probe.getJarEntry("META-INF/l2j-ordered-olympiad-v1.properties")!=null;}
        if(ordered)
        {
            int result=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"--class-path",".",
                patches.resolve("ordered-olympiad/InstallOrderedOlympiad.java").toString(),core.toString(),patches.toString()).inheritIO().start().waitFor();
            if(result!=0)throw new IOException("Combined six-patch verification failed: "+result);
            return;
        }
        String sourceHash=sha(Files.readAllBytes(patches.resolve("olympiad-battle-timer/InstallOlympiadBattleTimer.java")));
        try(var channel=FileChannel.open(core.resolveSibling(".olympiad-battle-timer-install.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock())
        {
            byte[] original=Files.readAllBytes(core),task;
            try(var jar=new JarFile(core.toFile()))
            {
                sources(jar,patches);
                if(jar.getJarEntry(MARK)!=null)
                {
                    var p=properties(jar,MARK);
                    if(!p.stringPropertyNames().equals(Set.of("base","input","source","taskBase","task","observerSource","payload"))
                        ||!p.getProperty("input", "").matches("[0-9a-f]{64}")
                        ||!BASE.equals(p.getProperty("base"))||!(sourceHash.equals(p.getProperty("source"))||"5636523cd6b07cf0502ba3b276417ae5d29da9f1cbb6efabdd3c9456dce147a1".equals(p.getProperty("source")))||!TASK_BASE.equals(p.getProperty("taskBase"))
                        ||!sha(Files.readAllBytes(patches.resolve("observer-cleanup/InstallObserverCleanup.java"))).equals(p.getProperty("observerSource"))
                        ||!payload(jar,Map.of(),MARK).equals(p.getProperty("payload"))||!sha(entry(jar,TASK)).equals(p.getProperty("task")))
                        throw new IOException("Battle timer marker/hash mismatch; restore verified backup");
                    System.out.println("Olympiad battle timer v1 verified; all five patches intact.");return;
                }
                var old=properties(jar,OLD_MARK);
                if(!"olympiad-diversity-v1".equals(old.getProperty("base"))||!payload(jar,Map.of(),OLD_MARK).equals(old.getProperty("payload"))
                    ||!TASK_BASE.equals(sha(entry(jar,TASK))))throw new IOException("Unsupported or tampered observer core / task; install all four prior guards first");
                Map<String,byte[]> hierarchy=new HashMap<>();
                for(var e:jar.stream().filter(e->e.getName().endsWith(".class")).toList())hierarchy.put(e.getName(),entry(jar,e.getName()));
                task=patch(entry(jar,TASK),hierarchy);
            }
            Path temp=Files.createTempDirectory(core.getParent(),".olympiad-battle-timer-build-");
            try
            {
                var props=new Properties();props.setProperty("base",BASE);props.setProperty("input",sha(original));props.setProperty("source",sourceHash);
                props.setProperty("observerSource",sha(Files.readAllBytes(patches.resolve("observer-cleanup/InstallObserverCleanup.java"))));
                props.setProperty("taskBase",TASK_BASE);props.setProperty("task",sha(task));
                try(var jar=new JarFile(core.toFile())){props.setProperty("payload",payload(jar,Map.of(TASK,task),MARK));}
                var meta=new ByteArrayOutputStream();props.store(meta,"Olympiad battle timer v1");Path output=temp.resolve("GameServer.jar");
                try(var jar=new JarFile(core.toFile());var out=new JarOutputStream(Files.newOutputStream(output)))
                {
                    for(var e:jar.stream().toList())
                    {String name=e.getName();var copy=new JarEntry(name);copy.setTime(e.getTime());out.putNextEntry(copy);out.write(name.equals(TASK)?task:entry(jar,name));out.closeEntry();}
                    out.putNextEntry(new JarEntry(MARK));out.write(meta.toByteArray());out.closeEntry();
                }
                try(var before=new JarFile(core.toFile());var after=new JarFile(output.toFile()))
                {
                    for(var e:before.stream().toList())if(!e.getName().equals(TASK)&&!Arrays.equals(entry(before,e.getName()),entry(after,e.getName())))throw new IOException("Changed unrelated entry: "+e.getName());
                    if(!payload(after,Map.of(),MARK).equals(props.getProperty("payload")))throw new IOException("Output verification failed");
                }
                Path backup=core.resolveSibling(core.getFileName()+".pre-olympiad-battle-timer-"+sha(original).substring(0,12));
                if(!Files.exists(backup))Files.copy(core,backup);
                if(!sha(Files.readAllBytes(backup)).equals(sha(original)))throw new IOException("Backup hash differs");
                try(var f=FileChannel.open(backup,StandardOpenOption.WRITE)){f.force(true);}try(var f=FileChannel.open(output,StandardOpenOption.WRITE)){f.force(true);}
                Files.move(output,core,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Installed Olympiad battle timer v1; verified backup: "+backup);
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
            if(element instanceof MethodModel m&&m.methodName().equalsString("runGame")&&m.methodType().equalsString("()Z"))
            {
                counts[0]++;
                builder.transformMethod(m,MethodTransform.transformingCode((code,e)->
                {
                    if(e instanceof Instruction i&&i.opcode()==Opcode.I2S) { counts[1]++;code.nop(); }
                    else code.with(e);
                }));
            }
            else builder.with(element);
        });
        if(counts[0]!=1||counts[1]!=1)throw new IllegalStateException("Expected one native runGame short narrowing, got "+Arrays.toString(counts));
        return result;
    }
}
