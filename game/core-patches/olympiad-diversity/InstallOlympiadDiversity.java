import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;
import java.lang.classfile.*;
import java.lang.constant.*;
import javax.tools.ToolProvider;

/** Stopped Java25 core only. Adds two small method wrappers; keeps original methods intact. */
public class InstallOlympiadDiversity
{
    static final String BASE="arena-boundary-v1";
    static final String OLD_MARK="META-INF/l2j-arena-boundary-v1.properties";
    static final String DEBUFF_MARK="META-INF/l2j-debuff-no-refresh-v1.properties";
    static final String MARK="META-INF/l2j-olympiad-diversity-v1.properties";
    static final String MANAGER="org/l2jmobius/gameserver/mechanics/olympiad/OlympiadManager.class";
    static final String GUARD="org/l2jmobius/gameserver/mechanics/olympiad/OlympiadDiversity.class";
    static final String HISTORY="org/l2jmobius/gameserver/mechanics/olympiad/OlympiadDiversity$History.class";
    static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    static byte[] entry(JarFile jar, String name) throws Exception
    { JarEntry e=jar.getJarEntry(name); if(e==null) throw new IOException("Missing entry: "+name); try(var in=jar.getInputStream(e)){return in.readAllBytes();} }
    static String payload(JarFile jar, Map<String,byte[]> replacements) throws Exception
    {
        var digest=MessageDigest.getInstance("SHA-256");var names=new TreeSet<String>();jar.stream().forEach(e->names.add(e.getName()));names.addAll(replacements.keySet());names.remove(MARK);
        for(String name:names){byte[] key=name.getBytes(java.nio.charset.StandardCharsets.UTF_8);byte[] bytes=replacements.containsKey(name)?replacements.get(name):entry(jar,name);
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(key.length).array());digest.update(key);digest.update(java.nio.ByteBuffer.allocate(8).putLong(bytes.length).array());digest.update(bytes);}
        return HexFormat.of().formatHex(digest.digest());
    }

    static Properties properties(JarFile jar,String name)throws Exception
    {var p=new Properties();p.load(new ByteArrayInputStream(entry(jar,name)));return p;}
    static void sources(JarFile jar,Path source)throws Exception
    {
        Path patches=source.getParent().getParent();
        for(String[] item:new String[][]{{DEBUFF_MARK,"debuff-no-refresh/DebuffCategoryGuard.java"},{OLD_MARK,"arena-boundary/ArenaBoundaryGuard.java"}})
            if(!sha(Files.readAllBytes(patches.resolve(item[1]))).equals(properties(jar,item[0]).getProperty("source")))
                throw new IOException("Prior helper source changed: "+item[1]+"; restore matching backup before upgrade");
    }
    public static void main(String[] args)throws Exception
    {
        if(args.length!=2)throw new IllegalArgumentException("Usage: java InstallOlympiadDiversity.java CORE.jar OlympiadDiversity.java (server stopped)");
        Path core=Path.of(args[0]).toAbsolutePath(),source=Path.of(args[1]).toAbsolutePath();
        boolean battleTimer;
        try(var probe=new JarFile(core.toFile())){battleTimer=probe.getJarEntry("META-INF/l2j-olympiad-battle-timer-v1.properties")!=null;}
        if(battleTimer)
        {
            Path root=source.getParent().getParent();
            int result=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"--class-path",".",
                root.resolve("olympiad-battle-timer/InstallOlympiadBattleTimer.java").toString(),core.toString(),root.toString()).inheritIO().start().waitFor();
            if(result!=0)throw new IOException("Combined five-patch verification failed: "+result);
            return;
        }
        boolean observerCleanup;
        try(var probe=new JarFile(core.toFile())){observerCleanup=probe.getJarEntry("META-INF/l2j-observer-cleanup-v1.properties")!=null;}
        if(observerCleanup)
        {
            Path patches=source.getParent().getParent();
            int result=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"--class-path",".",
                patches.resolve("observer-cleanup/InstallObserverCleanup.java").toString(),core.toString(),patches.toString()).inheritIO().start().waitFor();
            if(result!=0)throw new IOException("Combined observer/diversity/arena/debuff verification failed: "+result);
            return;
        }
        try(var channel=FileChannel.open(core.resolveSibling(".olympiad-diversity-install.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock())
        {
            byte[] original=Files.readAllBytes(core);String sourceHash=sha(Files.readAllBytes(source));
            try(var jar=new JarFile(core.toFile()))
            {
                sources(jar,source);
                if(jar.getJarEntry(MARK)!=null)
                {
                    var p=properties(jar,MARK);
                    if(!BASE.equals(p.getProperty("base"))||!sourceHash.equals(p.getProperty("source"))
                        ||!payload(jar,Map.of()).equals(p.getProperty("payload"))||!sha(entry(jar,MANAGER)).equals(p.getProperty("manager"))
                        ||!sha(entry(jar,GUARD)).equals(p.getProperty("guard")))throw new IOException("Diversity patch marker/hash mismatch; restore verified backup");
                    System.out.println("Olympiad diversity v1 verified; already installed.");return;
                }
                var old=properties(jar,OLD_MARK);
                if(!"debuff-no-refresh-v1".equals(old.getProperty("base"))||!priorPayload(jar).equals(old.getProperty("payload")))
                    throw new IOException("Unsupported or tampered arena/debuff core; install both guards first");
            }
            if(ToolProvider.getSystemJavaCompiler()==null)throw new IOException("Java25 JDK required");
            Path temp=Files.createTempDirectory(core.getParent(),".olympiad-diversity-build-");
            try
            {
                Map<String,byte[]> hierarchy=new HashMap<>();byte[] manager;
                try(var jar=new JarFile(core.toFile()))
                {
                    for(var e:jar.stream().filter(e->e.getName().endsWith(".class")).toList())hierarchy.put(e.getName(),entry(jar,e.getName()));
                    manager=patch(entry(jar,MANAGER),hierarchy);
                }
                Path target=temp.resolve(MANAGER);Files.createDirectories(target.getParent());Files.write(target,manager);
                var classpath=new ArrayList<Path>();classpath.add(temp);classpath.add(core);
                try(var paths=Files.list(core.getParent())){paths.filter(p->p.toString().endsWith(".jar")&&!p.equals(core)).sorted().forEach(classpath::add);}
                var compiler=ToolProvider.getSystemJavaCompiler();
                try(var files=compiler.getStandardFileManager(null,Locale.ROOT,java.nio.charset.StandardCharsets.UTF_8))
                {
                    files.setLocationFromPaths(javax.tools.StandardLocation.CLASS_PATH,classpath);
                    if(!compiler.getTask(new PrintWriter(System.err,true),files,null,List.of("-encoding","UTF-8","-d",temp.toString()),null,files.getJavaFileObjectsFromPaths(List.of(source))).call())
                        throw new IOException("Diversity helper compilation failed");
                }
                try(var generated=Files.walk(temp)){if(generated.filter(p->p.toString().endsWith(".class")).count()!=3)throw new IOException("Unexpected generated classes");}
                var replacements=Map.of(MANAGER,manager,GUARD,Files.readAllBytes(temp.resolve(GUARD)),HISTORY,Files.readAllBytes(temp.resolve(HISTORY)));
                var props=new Properties();props.setProperty("base",BASE);props.setProperty("input",sha(original));props.setProperty("source",sourceHash);
                props.setProperty("manager",sha(manager));props.setProperty("guard",sha(replacements.get(GUARD)));
                try(var jar=new JarFile(core.toFile())){props.setProperty("payload",payload(jar,replacements));}
                var meta=new ByteArrayOutputStream();props.store(meta,"Olympiad diversity v1");Path output=temp.resolve("GameServer.jar");
                try(var jar=new JarFile(core.toFile());var out=new JarOutputStream(Files.newOutputStream(output)))
                {
                    for(var e:jar.stream().toList())
                    {
                        String name=e.getName();if(name.equals(MARK)||name.equals(GUARD)||name.equals(HISTORY))throw new IOException("Unexpected existing diversity entry");
                        var copy=new JarEntry(name);copy.setTime(e.getTime());out.putNextEntry(copy);out.write(replacements.containsKey(name)?replacements.get(name):entry(jar,name));out.closeEntry();
                    }
                    for(String name:List.of(GUARD,HISTORY)){out.putNextEntry(new JarEntry(name));out.write(replacements.get(name));out.closeEntry();}
                    out.putNextEntry(new JarEntry(MARK));out.write(meta.toByteArray());out.closeEntry();
                }
                try(var before=new JarFile(core.toFile());var after=new JarFile(output.toFile()))
                {
                    for(var e:before.stream().toList())if(!e.getName().equals(MANAGER)&&!Arrays.equals(entry(before,e.getName()),entry(after,e.getName())))throw new IOException("Changed unrelated entry: "+e.getName());
                    if(!payload(after,Map.of()).equals(props.getProperty("payload")))throw new IOException("Output payload mismatch");
                }
                Path backup=core.resolveSibling(core.getFileName()+".pre-olympiad-diversity-"+sha(original).substring(0,12));
                if(!Files.exists(backup))Files.copy(core,backup);
                if(!sha(Files.readAllBytes(backup)).equals(sha(original)))throw new IOException("Backup hash differs");
                try(var f=FileChannel.open(backup,StandardOpenOption.WRITE)){f.force(true);}try(var f=FileChannel.open(output,StandardOpenOption.WRITE)){f.force(true);}
                Files.move(output,core,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Installed Olympiad diversity v1; verified backup: "+backup);
            }
            finally{try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
        }
    }
    static String priorPayload(JarFile jar)throws Exception
    {
        var digest=MessageDigest.getInstance("SHA-256");var names=new TreeSet<String>();jar.stream().forEach(e->names.add(e.getName()));names.remove(OLD_MARK);
        for(String name:names){byte[] key=name.getBytes(java.nio.charset.StandardCharsets.UTF_8),bytes=entry(jar,name);digest.update(java.nio.ByteBuffer.allocate(4).putInt(key.length).array());digest.update(key);digest.update(java.nio.ByteBuffer.allocate(8).putLong(bytes.length).array());digest.update(bytes);}
        return HexFormat.of().formatHex(digest.digest());
    }
    static byte[] patch(byte[] original,Map<String,byte[]> hierarchy)
    {
        var resolver=ClassHierarchyResolver.ofResourceParsing((ClassDesc type)->
        {String descriptor=type.descriptorString(),resource=descriptor.substring(1,descriptor.length()-1)+".class";byte[] bytes=hierarchy.get(resource);return bytes!=null?new ByteArrayInputStream(bytes):ClassLoader.getPlatformClassLoader().getResourceAsStream(resource);});
        ClassFile cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));int[] count={0};
        ClassDesc owner=ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager"),helper=ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadDiversity"),list=ClassDesc.of("java.util.List");
        byte[] result=cf.transformClass(cf.parse(original),(builder,element)->
        {
            if(element instanceof MethodModel method)
            {
                String name=method.methodName().stringValue();boolean next=name.equals("nextOpponents")&&method.methodType().stringValue().equals("(Ljava/util/List;)Ljava/util/List;");
                boolean ready=name.equals("existNextOpponents")&&method.methodType().stringValue().equals("(Ljava/util/List;)Z");
                if(next||ready)
                {
                    count[0]++;int flags=method.flags().flagsMask();String bridge="diversityOriginal"+Character.toUpperCase(name.charAt(0))+name.substring(1);
                    builder.withMethod(bridge,method.methodTypeSymbol(),flags,mb->{for(MethodElement e:method)if(!(e instanceof AccessFlags))mb.with(e);});
                    builder.withMethod(name,method.methodTypeSymbol(),flags,mb->
                    {
                        for(MethodElement e:method)if(!(e instanceof AccessFlags)&&!(e instanceof CodeModel))mb.with(e);
                        mb.withCode(cb->
                        {
                            if(next){cb.aload(0).aload(1).invokestatic(helper,"next",MethodTypeDesc.of(list,owner,list)).areturn();return;}
                            var denied=cb.newLabel();cb.aload(0).aload(1).invokevirtual(owner,bridge,method.methodTypeSymbol()).ifeq(denied)
                                .aload(1).invokestatic(helper,"ready",MethodTypeDesc.of(ConstantDescs.CD_boolean,list)).ireturn().labelBinding(denied).iconst_0().ireturn();
                        });
                    });return;
                }
            }
            builder.with(element);
        });
        if(count[0]!=2||!cf.verify(result).isEmpty())throw new IOExceptionUnchecked("Unexpected methods or invalid diversity wrapper");
        return result;
    }
    private static final class IOExceptionUnchecked extends RuntimeException {IOExceptionUnchecked(String s){super(s);}}
}
