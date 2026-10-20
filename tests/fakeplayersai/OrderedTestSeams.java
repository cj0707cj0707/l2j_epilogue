import java.lang.classfile.*;import java.lang.constant.*;import java.nio.file.*;import java.util.jar.*;
/** Test-only singleton bootstrap and native combat task recording; no production patch. */
class OrderedTestSeams
{
    public static void main(String[] args)throws Exception
    {
        var test=ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OrderedOlympiadTest");
        try(var jar=new JarFile(args[0]))
        {
            for(String name:new String[]{"Olympiad","OlympiadManager","OlympiadGameTask"})
            {
                String path="org/l2jmobius/gameserver/mechanics/olympiad/"+name+".class";
                var cf=ClassFile.of();byte[] bytes=cf.transformClass(cf.parse(jar.getInputStream(jar.getJarEntry(path)).readAllBytes()),(c,e)->{
                    if(e instanceof MethodModel m&&name.equals("Olympiad")&&m.methodName().equalsString("addSpectator"))
                    {c.withMethodBody("addSpectator",m.methodTypeSymbol(),m.flags().flagsMask(),b->b.iload(0).aload(1).iload(2).invokestatic(test,"observe",m.methodTypeSymbol()).return_());return;}
                    if(e instanceof MethodModel m&&m.methodName().equalsString(name.equals("OlympiadGameTask")?"run":"getInstance"))
                        c.withMethodBody(m.methodName().stringValue(),m.methodTypeSymbol(),m.flags().flagsMask(),b->{
                            if(name.equals("OlympiadGameTask"))b.aload(0).invokestatic(test,"combat",MethodTypeDesc.of(ConstantDescs.CD_void,ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask"))).return_();
                            else b.getstatic(test,name.equals("Olympiad")?"service":"manager",ClassDesc.of("org.l2jmobius.gameserver.mechanics.olympiad."+name)).areturn();
                        });else c.with(e);
                });Path out=Path.of(args[1]).resolve(path);Files.createDirectories(out.getParent());Files.write(out,bytes);
            }
        }
    }
}
