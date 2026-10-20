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

/** Java25 stopped-core patch: ordered pair reservations, manager wait pulses and scoped countdown bypass. */
public class InstallOrderedOlympiad
{
    static final String BASE = "olympiad-battle-timer-v1";
    static final String OLD_MARK = "META-INF/l2j-olympiad-battle-timer-v1.properties";
    static final String MARK = "META-INF/l2j-ordered-olympiad-v1.properties";
    static final String TASK = "org/l2jmobius/gameserver/mechanics/olympiad/OlympiadGameTask.class";
    static final String MANAGER="org/l2jmobius/gameserver/mechanics/olympiad/OlympiadManager.class";
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
        String observer=properties(jar,"META-INF/l2j-observer-cleanup-v1.properties").getProperty("source","");
        if(!observer.equals(LEGACY_OBSERVER_SOURCE)&&!observer.equals(sha(Files.readAllBytes(patches.resolve("observer-cleanup/InstallObserverCleanup.java")))))
            throw new IOException("Unsupported observer installer source");
    }
    public static void main(String[] args)throws Exception
    {
        if(args.length!=2)throw new IllegalArgumentException("Usage: java InstallOrderedOlympiad.java CORE.jar PATCH_ROOT (server stopped)");
        Path core=Path.of(args[0]).toAbsolutePath(),patches=Path.of(args[1]).toAbsolutePath();
        String sourceHash=sha(Files.readAllBytes(patches.resolve("ordered-olympiad/InstallOrderedOlympiad.java")));
        try(var channel=FileChannel.open(core.resolveSibling(".ordered-olympiad-install.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock())
        {
            byte[] original=Files.readAllBytes(core);
            Map<String,byte[]> replacements=new HashMap<>();
            try(var jar=new JarFile(core.toFile()))
            {
                sources(jar,patches);
                if(jar.getJarEntry(MARK)!=null)
                {
                    var p=properties(jar,MARK);
                    if(!BASE.equals(p.getProperty("base"))||!sourceHash.equals(p.getProperty("source"))
                        ||!sha(Files.readAllBytes(patches.resolve("ordered-olympiad/OrderedOlympiad.java"))).equals(p.getProperty("helperSource"))
                        ||!sha(Files.readAllBytes(patches.resolve("olympiad-battle-timer/InstallOlympiadBattleTimer.java"))).equals(p.getProperty("timerSource"))
                        ||!payload(jar,Map.of(),MARK).equals(p.getProperty("payload")))
                        throw new IOException("Ordered Olympiad marker/source/payload mismatch");
                    System.out.println("Ordered Olympiad v1 verified; all six patches intact.");return;
                }
                var old=properties(jar,OLD_MARK);
                String timerSource=old.getProperty("source","");
                if(!"observer-cleanup-v1".equals(old.getProperty("base"))||!payload(jar,Map.of(),OLD_MARK).equals(old.getProperty("payload"))
                    ||!(timerSource.equals("5636523cd6b07cf0502ba3b276417ae5d29da9f1cbb6efabdd3c9456dce147a1")||timerSource.equals(sha(Files.readAllBytes(patches.resolve("olympiad-battle-timer/InstallOlympiadBattleTimer.java"))))))
                    throw new IOException("Unsupported or tampered five-patch core; install all five prior guards first");
                Map<String,byte[]> hierarchy=new HashMap<>();
                for(var e:jar.stream().filter(e->e.getName().endsWith(".class")).toList())hierarchy.put(e.getName(),entry(jar,e.getName()));
                replacements.put(MANAGER,patch(entry(jar,MANAGER),hierarchy,true));
                replacements.put(TASK,patch(entry(jar,TASK),hierarchy,false));
            }
            Path temp=Files.createTempDirectory(core.getParent(),".ordered-olympiad-build-");
            try
            {
                for(var item:replacements.entrySet()){Path target=temp.resolve(item.getKey());Files.createDirectories(target.getParent());Files.write(target,item.getValue());}
                var classpath=new ArrayList<Path>();classpath.add(temp);classpath.add(core);
                try(var paths=Files.list(core.getParent())){paths.filter(p->p.toString().endsWith(".jar")&&!p.equals(core)).sorted().forEach(classpath::add);}
                var compiler=javax.tools.ToolProvider.getSystemJavaCompiler();
                if(compiler==null)throw new IOException("Java25 JDK required");
                try(var files=compiler.getStandardFileManager(null,Locale.ROOT,StandardCharsets.UTF_8))
                {
                    files.setLocationFromPaths(javax.tools.StandardLocation.CLASS_PATH,classpath);
                    if(!compiler.getTask(new PrintWriter(System.err,true),files,null,List.of("-encoding","UTF-8","-d",temp.toString()),null,
                        files.getJavaFileObjectsFromPaths(List.of(patches.resolve("ordered-olympiad/OrderedOlympiad.java")))).call())throw new IOException("Ordered helper compilation failed");
                }
                try(var generated=Files.walk(temp))
                {for(Path file:generated.filter(f->f.toString().endsWith(".class")).toList())replacements.put(temp.relativize(file).toString().replace('\\','/'),Files.readAllBytes(file));}
                var props=new Properties();props.setProperty("base",BASE);props.setProperty("input",sha(original));props.setProperty("source",sourceHash);
                props.setProperty("helperSource",sha(Files.readAllBytes(patches.resolve("ordered-olympiad/OrderedOlympiad.java"))));
                props.setProperty("timerSource",sha(Files.readAllBytes(patches.resolve("olympiad-battle-timer/InstallOlympiadBattleTimer.java"))));
                try(var jar=new JarFile(core.toFile())){props.setProperty("payload",payload(jar,replacements,MARK));}
                var meta=new ByteArrayOutputStream();props.store(meta,"Ordered Olympiad v1");Path output=temp.resolve("GameServer.jar");
                try(var jar=new JarFile(core.toFile());var out=new JarOutputStream(Files.newOutputStream(output)))
                {
                    for(var e:jar.stream().toList())
                    {String name=e.getName();var copy=new JarEntry(name);copy.setTime(e.getTime());out.putNextEntry(copy);out.write(replacements.containsKey(name)?replacements.get(name):entry(jar,name));out.closeEntry();}
                    for(var item:replacements.entrySet())if(jar.getJarEntry(item.getKey())==null){out.putNextEntry(new JarEntry(item.getKey()));out.write(item.getValue());out.closeEntry();}
                    out.putNextEntry(new JarEntry(MARK));out.write(meta.toByteArray());out.closeEntry();
                }
                try(var before=new JarFile(core.toFile());var after=new JarFile(output.toFile()))
                {
                    for(var e:before.stream().toList())if(!replacements.containsKey(e.getName())&&!Arrays.equals(entry(before,e.getName()),entry(after,e.getName())))throw new IOException("Changed unrelated entry: "+e.getName());
                    if(!payload(after,Map.of(),MARK).equals(props.getProperty("payload")))throw new IOException("Output verification failed");
                }
                Path backup=core.resolveSibling(core.getFileName()+".pre-ordered-olympiad-"+sha(original).substring(0,12));
                if(!Files.exists(backup))Files.copy(core,backup);
                if(!sha(Files.readAllBytes(backup)).equals(sha(original)))throw new IOException("Backup hash differs");
                try(var f=FileChannel.open(backup,StandardOpenOption.WRITE)){f.force(true);}try(var f=FileChannel.open(output,StandardOpenOption.WRITE)){f.force(true);}
                Files.move(output,core,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Installed Ordered Olympiad v1; verified backup: "+backup);
            }
            finally{try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
        }
    }
    static byte[] patch(byte[] original,Map<String,byte[]> hierarchy,boolean manager)
    {
        var resolver=ClassHierarchyResolver.ofResourceParsing((ClassDesc type)->
        {String d=type.descriptorString(),r=d.substring(1,d.length()-1)+".class";byte[] b=hierarchy.get(r);return b!=null?new ByteArrayInputStream(b):ClassLoader.getPlatformClassLoader().getResourceAsStream(r);});
        ClassFile cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));int[] counts={0,0};
        var owner=ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager");
        var game=ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame");
        var helper=ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OrderedOlympiad");
        byte[] result=cf.transformClass(cf.parse(original),(builder,element)->
        {
            if(element instanceof MethodModel m&&m.methodName().equalsString(manager?"run":"teleportCountdown"))
            {
                builder.transformMethod(m,MethodTransform.transformingCode(((CodeTransform)(code,e)->
                {
                    if(manager)
                    {
                        if(e instanceof java.lang.classfile.instruction.InvokeInstruction i&&i.name().equalsString("wait")&&i.type().equalsString("(J)V"))
                        {counts[1]++;code.invokestatic(helper,"await",java.lang.constant.MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_void,owner,java.lang.constant.ConstantDescs.CD_long));return;}
                        code.with(e);
                        if(e instanceof java.lang.classfile.instruction.StoreInstruction i&&i.slot()==1&&i.opcode()==Opcode.ASTORE_1&&counts[0]==0)
                        {counts[0]++;code.aload(0).aload(1).invokestatic(helper,"attach",java.lang.constant.MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_void,owner,ClassDesc.of("java.util.Map")));}
                    }
                    else code.with(e);
                }).andThen(manager?CodeTransform.ACCEPT_ALL:CodeTransform.ofStateful(()->new CodeTransform(){
                    public void atStart(CodeBuilder code)
                    {counts[0]++;var normal=code.newLabel();code.aload(0).getfield(ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask"),"_game",game)
                        .invokestatic(helper,"ordered",java.lang.constant.MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_boolean,game)).ifeq(normal).iconst_1().ireturn().labelBinding(normal);}
                    public void accept(CodeBuilder b,CodeElement e){b.with(e);}
                }))));
            }
            else builder.with(element);
        });
        if(counts[0]!=1||(manager&&counts[1]!=4))throw new IllegalStateException("Unexpected native layout "+Arrays.toString(counts));
        return result;
    }
}
