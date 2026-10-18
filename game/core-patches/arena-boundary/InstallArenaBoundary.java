import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;
import java.lang.classfile.*;
import java.lang.constant.*;
import javax.tools.ToolProvider;

/** Java 25 source launcher. Patches only a hash-pinned stopped core, atomically, with a verified backup. */
public class InstallArenaBoundary
{
    static final String BASE = "debuff-no-refresh-v1";
    static final String OLD_MARK = "META-INF/l2j-debuff-no-refresh-v1.properties";
    static final String CREATURE = "org/l2jmobius/gameserver/entity/actor/Creature.class";
    static final String EFFECT = "org/l2jmobius/gameserver/entity/WorldObject.class";
    static final String GUARD = "org/l2jmobius/gameserver/mechanics/skill/ArenaBoundaryGuard.class";
    static final String RECORD = "org/l2jmobius/gameserver/mechanics/skill/ArenaBoundaryGuard$Boundary.class";
    static final String MARK = "META-INF/l2j-arena-boundary-v1.properties";
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
    public static void main(String[] args) throws Exception
    {
        if(args.length!=2) throw new IllegalArgumentException("Usage: java InstallArenaBoundary.java CORE.jar ArenaBoundaryGuard.java (server must be stopped)");
        Path core=Path.of(args[0]).toAbsolutePath(), source=Path.of(args[1]).toAbsolutePath();
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
        boolean diversity;
        try(var probe=new JarFile(core.toFile())){diversity=probe.getJarEntry("META-INF/l2j-olympiad-diversity-v1.properties")!=null;}
        if(diversity)
        {
            Path folder=source.getParent().getParent().resolve("olympiad-diversity");
            int result=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"--class-path",".",
                folder.resolve("InstallOlympiadDiversity.java").toString(),core.toString(),folder.resolve("OlympiadDiversity.java").toString()).inheritIO().start().waitFor();
            if(result!=0)throw new IOException("Combined diversity/arena/debuff verification failed: "+result);
            return;
        }

        // A separate lock file serializes concurrent launcher attempts; never silently launch an unpatched core.
        try(var lockChannel=FileChannel.open(core.resolveSibling(".arena-boundary-install.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            var lock=lockChannel.lock())
        {
            byte[] original=Files.readAllBytes(core); String sourceHash=sha(Files.readAllBytes(source));
            try(var jar=new JarFile(core.toFile()))
            {
                if(jar.getJarEntry(MARK)!=null)
                {
                    Properties p=new Properties();p.load(new ByteArrayInputStream(entry(jar,MARK)));
                    if(!BASE.equals(p.getProperty("base")) || !sourceHash.equals(p.getProperty("source"))
                        || !payload(jar,Map.of()).equals(p.getProperty("payload"))
                        || !sha(entry(jar,CREATURE)).equals(p.getProperty("creature")) || !sha(entry(jar,EFFECT)).equals(p.getProperty("effect")) || !sha(entry(jar,GUARD)).equals(p.getProperty("guard")))
                        throw new IOException("Patch marker/hash mismatch. Restore the verified backup before changing patch version.");
                    System.out.println("Arena boundary guard v1 verified; already installed.");return;
                }
            }
            try(var jar=new JarFile(core.toFile()))
            {
                if(jar.getJarEntry(OLD_MARK)==null)throw new IOException("Install/verify debuff category guard first");
                Properties old=new Properties();old.load(new ByteArrayInputStream(entry(jar,OLD_MARK)));
                if(!"8f27196ca9d74b82bbe9ad0d12d70da0196fa4e0d1bf23595267b724c79a23dd".equals(old.getProperty("base"))
                    ||!oldPayload(jar).equals(old.getProperty("payload")))throw new IOException("Unsupported or tampered debuff core");
            }
            if(ToolProvider.getSystemJavaCompiler()==null) throw new IOException("Java 25 JDK required (compiler unavailable)");
            Path temp=Files.createTempDirectory(core.getParent(),".arena-boundary-build-");
            try
            {
                byte[] patched, creature;
                // Parse hierarchy bytes without loading server classes into the application
                // classloader (which would pin the replacement target on Windows).
                Map<String,byte[]> hierarchy=new HashMap<>();
                try(var jar=new JarFile(core.toFile()))
                {
                    for(var e:jar.stream().filter(e->e.getName().endsWith(".class")).toList())
                        hierarchy.put(e.getName(),entry(jar,e.getName()));
                    patched=patch(entry(jar,EFFECT),hierarchy,false);
                    creature=patch(entry(jar,CREATURE),hierarchy,true);
                }
                Path target=temp.resolve(EFFECT);Files.createDirectories(target.getParent());Files.write(target,patched);
                Path ct=temp.resolve(CREATURE);Files.createDirectories(ct.getParent());Files.write(ct,creature);
                // Compile against the transformed coordinate wrappers,
                // then the exact original core. No reflection in the runtime path.
                var classpath=new ArrayList<Path>();classpath.add(temp);classpath.add(core);
                try(var files=Files.list(core.getParent()))
                {files.filter(p->p.toString().endsWith(".jar")&&!p.equals(core)).sorted().forEach(classpath::add);}
                // Explicitly close javac's archive/file caches BEFORE atomic replacement.
                var compiler=ToolProvider.getSystemJavaCompiler();
                try(var files=compiler.getStandardFileManager(null,Locale.ROOT,java.nio.charset.StandardCharsets.UTF_8))
                {
                    files.setLocationFromPaths(javax.tools.StandardLocation.CLASS_PATH,classpath);
                    var units=files.getJavaFileObjectsFromPaths(List.of(source));
                    if(!compiler.getTask(new PrintWriter(System.err,true),files,null,
                        List.of("-encoding","UTF-8","-d",temp.toString()),null,units).call())
                        throw new IOException("Arena boundary guard compilation failed (see diagnostics above)");
                }
                try(var generated=Files.walk(temp)){if(generated.filter(p->p.toString().endsWith(".class")).count()!=4)throw new IOException("Unexpected compiler-generated helper classes");}
                byte[] helper=Files.readAllBytes(temp.resolve(GUARD)), record=Files.readAllBytes(temp.resolve(RECORD));
                Properties props=new Properties(); props.setProperty("base",BASE);props.setProperty("source",sourceHash);
                props.setProperty("input",sha(original));props.setProperty("creature",sha(creature));
                props.setProperty("effect",sha(patched));props.setProperty("guard",sha(helper));
                try(var jar=new JarFile(core.toFile())){props.setProperty("payload",payload(jar,Map.of(EFFECT,patched,CREATURE,creature,GUARD,helper,RECORD,record)));}
                ByteArrayOutputStream meta=new ByteArrayOutputStream();props.store(meta,"Arena boundary guard v1");
                Path output=temp.resolve("GameServer.jar");
                try(var jar=new JarFile(core.toFile()); var out=new JarOutputStream(Files.newOutputStream(output)))
                {
                    var entries=jar.entries();
                    while(entries.hasMoreElements())
                    {
                        JarEntry e=entries.nextElement();String name=e.getName();
                        if(name.equals(GUARD)||name.equals(MARK)) throw new IOException("Unexpected patch entry");
                        JarEntry copy=new JarEntry(name);copy.setTime(e.getTime());out.putNextEntry(copy);
                        out.write(name.equals(EFFECT)?patched:name.equals(CREATURE)?creature:entry(jar,name));out.closeEntry();
                    }
                    for(var item:Map.of(GUARD,helper,RECORD,record,MARK,meta.toByteArray()).entrySet())
                    {out.putNextEntry(new JarEntry(item.getKey()));out.write(item.getValue());out.closeEntry();}
                }
                // Verify every untouched entry; no decompile/recompile of unrelated core methods.
                try(var before=new JarFile(core.toFile());var after=new JarFile(output.toFile()))
                {
                    var entries=before.entries();while(entries.hasMoreElements())
                    {String name=entries.nextElement().getName();if(!name.equals(EFFECT)&&!name.equals(CREATURE)&&!Arrays.equals(entry(before,name),entry(after,name))) throw new IOException("Changed unrelated entry: "+name);}
                    if(!Arrays.equals(creature,entry(after,CREATURE))||!Arrays.equals(patched,entry(after,EFFECT))||!Arrays.equals(helper,entry(after,GUARD)))throw new IOException("Arena boundary output verification failed");
                }
                Path backup=core.resolveSibling(core.getFileName()+".pre-arena-boundary-"+sha(original).substring(0,12));
                if(!Files.exists(backup)) Files.copy(core,backup);
                if(!sha(Files.readAllBytes(backup)).equals(sha(original)))throw new IOException("Backup exists but hash differs");
                try(var f=FileChannel.open(backup,StandardOpenOption.WRITE)){f.force(true);}
                try(var f=FileChannel.open(output,StandardOpenOption.WRITE)){f.force(true);}
                // No non-atomic fallback. A failure leaves the original core intact.
                Files.move(output,core,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Installed arena boundary guard v1; verified backup: "+backup);
            }
            finally {try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
        }
    }
    static String oldPayload(JarFile jar) throws Exception
    {
        var digest=MessageDigest.getInstance("SHA-256");var names=new TreeSet<String>();jar.stream().forEach(e->names.add(e.getName()));names.remove(OLD_MARK);
        for(String name:names){byte[] key=name.getBytes(java.nio.charset.StandardCharsets.UTF_8),bytes=entry(jar,name);
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(key.length).array());digest.update(key);digest.update(java.nio.ByteBuffer.allocate(8).putLong(bytes.length).array());digest.update(bytes);}
        return HexFormat.of().formatHex(digest.digest());
    }
    static byte[] patch(byte[] original, Map<String,byte[]> hierarchy, boolean creature)
    {
        var resolver=ClassHierarchyResolver.ofResourceParsing((ClassDesc type)->
        {
            String descriptor=type.descriptorString();String resource=descriptor.substring(1,descriptor.length()-1)+".class";
            byte[] bytes=hierarchy.get(resource);
            return bytes!=null?new ByteArrayInputStream(bytes):ClassLoader.getPlatformClassLoader().getResourceAsStream(resource);
        });
        ClassFile cf=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));int[] calls={0};
        ClassDesc owner=ClassDesc.of(creature?"org.l2jmobius.gameserver.entity.actor.Creature":"org.l2jmobius.gameserver.entity.WorldObject");
        ClassDesc world=ClassDesc.of("org.l2jmobius.gameserver.entity.WorldObject"),helper=ClassDesc.of("org.l2jmobius.gameserver.mechanics.skill.ArenaBoundaryGuard");
        byte[] result=cf.transformClass(cf.parse(original),(builder,element)->
        {
            if(element instanceof MethodModel method)
            {
                String name=method.methodName().stringValue();
                boolean location=!creature&&name.equals("setLocation")&&method.methodType().stringValue().equals("(Lorg/l2jmobius/gameserver/entity/Location;)V");
                if(location||name.equals(creature?"moveToLocation":"setXYZ")&&method.methodType().stringValue().equals(creature?"(IIII)V":"(III)V"))
                {
                    calls[0]++;int flags=method.flags().flagsMask();String bridge="boundaryOriginal"+Character.toUpperCase(name.charAt(0))+name.substring(1);
                    builder.withMethod(bridge,method.methodTypeSymbol(),flags,mb->{for(MethodElement e:method)if(!(e instanceof AccessFlags))mb.with(e);});
                    builder.withMethod(name,method.methodTypeSymbol(),flags,mb->
                    {
                        for(MethodElement e:method)if(!(e instanceof AccessFlags)&&!(e instanceof CodeModel))mb.with(e);
                        mb.withCode(cb->
                        {
                            if(location)
                            {
                                ClassDesc loc=ClassDesc.of("org.l2jmobius.gameserver.entity.Location");
                                cb.aload(0).aload(0).aload(1).invokestatic(helper,"location",MethodTypeDesc.of(loc,world,loc))
                                    .invokevirtual(owner,bridge,method.methodTypeSymbol()).return_();return;
                            }
                            int local=creature?5:4;
                            cb.aload(0).iload(1).iload(2).invokestatic(helper,"clamp",MethodTypeDesc.of(ConstantDescs.CD_int.arrayType(),world,ConstantDescs.CD_int,ConstantDescs.CD_int)).astore(local);
                            cb.aload(0).aload(local).iconst_0().iaload().aload(local).iconst_1().iaload().iload(3);
                            if(creature)cb.iload(4);
                            cb.invokevirtual(owner,bridge,method.methodTypeSymbol()).return_();
                        });
                    });return;
                }
            }
            builder.with(element);
        });
        if(calls[0]!=(creature?1:2))throw new IllegalStateException("Unexpected boundary method count: "+calls[0]);
        if(!cf.verify(result).isEmpty())throw new IllegalStateException("Boundary class verification failed: "+cf.verify(result));
        return result;
    }
}
