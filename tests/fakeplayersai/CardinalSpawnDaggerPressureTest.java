package custom.fakeplayersai;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.data.xml.SkillData;

/** spawnAll/missingRosterPresets inserted verbatim from production. Only creation is inert. */
public class CardinalSpawnDaggerPressureTest
{
    static int checks;
    static final Logger LOGGER=Logger.getLogger("isolated46");
    static final BotSettings SETTINGS=new BotSettings();
    static final Map<Integer,Player> BOTS=new LinkedHashMap<>();
    static final Map<Integer,Integer> BOT_CLASSES=new LinkedHashMap<>();
    static final Map<Integer,String> REGISTRY_NAMES=new LinkedHashMap<>();
    static String REGISTRY_PATH;
    static final Map<Integer,Integer> attempts=new HashMap<>();
    static final Map<String,Integer> owners=new HashMap<>();
    static final Set<Integer> fail=new HashSet<>();
    static int seq=50000;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static int getBotClassId(Player p){return p.getPlayerClass().getId();}
    static Player spawnBot(BotClassPreset p)
    {
        attempts.merge(p.getClassId(),1,Integer::sum);
        if(fail.contains(p.getClassId()))return null;
        try
        {
            String name=BotSpawner.classNickname(p);
            if(BotSpawner.staticClassNickname(p))name=BotSpawner.fixedClassNickname(name,0,n->owners.getOrDefault(n.toLowerCase(Locale.ROOT),-1));
            Player bot=ArenaOrbitTranceManaTest.actor(p.getClassId(),false,false);
            ArenaOrbitTranceManaTest.f(WorldObject.class,"_objectId").setInt(bot,seq++);
            ArenaOrbitTranceManaTest.f(WorldObject.class,"_name").set(bot,name);
            BOTS.put(bot.getObjectId(),bot);
            BOT_CLASSES.put(bot.getObjectId(),p.getClassId());REGISTRY_NAMES.put(bot.getObjectId(),name);
            if(BotSpawner.staticClassNickname(p))owners.put(name.toLowerCase(Locale.ROOT),bot.getObjectId());
            return bot;
        }
        catch(IllegalStateException expected){return null;}
        catch(Exception e){throw new RuntimeException(e);}
    }
    // INSERT_SPAWN_ALL
    // INSERT_MISSING
    // INSERT_SAVE
    static void reset(int count){BOTS.clear();BOT_CLASSES.clear();REGISTRY_NAMES.clear();attempts.clear();owners.clear();fail.clear();SETTINGS.setEnabled(true);SETTINGS.setBotCount(count);}
    static void spawnTests()
    {
        check(new BotSettings().getBotCount()==19,"default covers actual 19-class pool");
        check(BotPresets.getPresets().size()==19,"expected current pool");
        reset(19);owners.put("cardinal",1);
        check(spawnAll()==19&&BOTS.size()==19,"occupied Cardinal no longer drops class from pool");
        check(missingRosterPresets().isEmpty(),"one of every class, not a duplicate replacement");
        Player cardinal=BOTS.values().stream().filter(p->getBotClassId(p)==97).findFirst().orElseThrow();
        check(cardinal.getName().equals("Bishop"),"static class-appropriate fallback with no suffix");
        check(owners.get("cardinal")==1,"foreign primary owner preserved");
        check(spawnAll()==0,"repeated spawnAll idempotent at capacity");
        for(BotClassPreset p:BotPresets.getPresets())check(attempts.getOrDefault(p.getClassId(),0)==1,"no duplicate class attempts");
        // Recreate the old 18-class roster (Cardinal absent) and fill the missing class only.
        reset(19);for(BotClassPreset p:BotPresets.getPresets())if(p.getClassId()!=97)spawnBot(p);
        attempts.clear();check(spawnAll()==1&&BOTS.size()==19,"18 existing classes repaired by adding Cardinal");
        check(attempts.size()==1&&attempts.get(97)==1,"existing classes not duplicated to fill gap");
        reset(19);owners.put("cardinal",1);owners.put("bishop",2);
        check(spawnAll()==18&&BOTS.size()==18,"all fixed names occupied: refuse collision without masking error");
        check(missingRosterPresets().size()==1&&missingRosterPresets().get(0).getClassId()==97,"exact missing class visible");
        check(attempts.get(97)==1,"one attempt, not a 200-iteration failure loop");
        owners.remove("bishop");check(spawnAll()==1&&missingRosterPresets().isEmpty(),"retry recovers only missing slot after conflict resolved");
        reset(19);fail.add(97);check(spawnAll()==18,"generic Cardinal spawn failure leaves reserved gap");
        check(attempts.get(97)==1,"failure not replaced by arbitrary class");fail.clear();check(spawnAll()==1,"next request retries missing class");
        reset(18);check(spawnAll()==18&&BOTS.size()==18,"explicit lower limit respected, not silently overridden");
        reset(23);check(spawnAll()==23&&BOTS.size()==23,"extra slots still permitted after full coverage");
        check(missingRosterPresets().isEmpty(),"extra slots do not displace a class");
        SETTINGS.setEnabled(false);check(spawnAll()==0,"disabled spawning unchanged");
    }
    static void registry()throws Exception
    {
        var file=java.nio.file.Files.createTempFile("registry46-",".properties");REGISTRY_PATH=file.toString();
        try
        {
            reset(19);owners.put("cardinal",1);
            for(BotClassPreset p:BotPresets.getPresets())if(p.getClassId()!=97)spawnBot(p);
            spawnBot(BotPresets.getPresetByClassId(97)); // LAST new actor, already-correct fallback name
            Player cardinal=BOTS.values().stream().filter(p->getBotClassId(p)==97).findFirst().orElseThrow();
            saveRegistry();Properties read=new Properties();try(var input=new FileInputStream(REGISTRY_PATH)){read.load(input);}
            check(read.size()==19,"actual production serializer includes all19 actors");
            check(read.getProperty(""+cardinal.getObjectId()).equals("Bishop;97"),"last new bot's ID/name/class persists");
            BOTS.remove(cardinal.getObjectId());saveRegistry();read.clear();try(var input=new FileInputStream(REGISTRY_PATH)){read.load(input);}
            check(read.getProperty(""+cardinal.getObjectId()).equals("Bishop;97"),"despawn does not lose fixed fallback name");
        }
        finally{java.nio.file.Files.deleteIfExists(file);}
    }
    static void names()throws Exception
    {
        for(String base:List.of("Cardinal","Duelist"))
        {
            Map<String,Integer> used=new HashMap<>();var names=BotSpawner.fixedClassNames(base);
            check(names.size()==2,"bounded meaningful name choices");
            java.util.function.ToIntFunction<String> lookup=n->used.getOrDefault(n.toLowerCase(Locale.ROOT),-1);
            check(BotSpawner.fixedClassNickname(base,30,lookup).equals(base),"canonical preferred when free");
            used.put(base.toLowerCase(Locale.ROOT),1);
            check(BotSpawner.fixedClassNickname(base,30,lookup).equals(names.get(1)),"safe semantic fallback");
            used.put(names.get(1).toLowerCase(Locale.ROOT),30);
            for(int i=0;i<100;i++)check(BotSpawner.fixedClassNickname(base,30,lookup).equals(names.get(1)),"own ID never increments suffix");
            Player actor=ArenaOrbitTranceManaTest.actor(base.equals("Cardinal")?97:88,false,false);
            ArenaOrbitTranceManaTest.f(WorldObject.class,"_name").set(actor,names.get(1));
            check(!BotSpawner.ensureClassNickname(actor,BotPresets.getPresetByClassId(actor.getPlayerClass().getId())),"semantic fallback never renamed back; no DB call on restore");
            check(actor.getName().equals(names.get(1)),"restored fixed name remains");
            try{BotSpawner.fixedClassNickname(base,0,lookup);throw new AssertionError("occupied names must not collide");}catch(IllegalStateException expected){checks++;}
            check(used.get(base.toLowerCase(Locale.ROOT))==1&&used.get(names.get(1).toLowerCase(Locale.ROOT))==30,"owners untouched");
        }
    }
    static void dagger()throws Exception
    {
        Class<?> state=Class.forName("custom.fakeplayersai.BotTactics$State");
        Method step=BotTactics.class.getDeclaredMethod("step",Player.class,Player.class,BotClassPreset.class,state,long.class,double.class,boolean.class,double.class,boolean.class,boolean.class);step.setAccessible(true);
        for(BotClassPreset preset:BotPresets.getPresets())for(boolean unavailable:new boolean[]{false,true})
            check(TacticsPolicy.daggerStepAllowed(preset.getWeaponKind()==BotClassPreset.WeaponKind.DAGGER,unavailable)==(preset.getWeaponKind()!=BotClassPreset.WeaponKind.DAGGER||unavailable),"only armed daggers excluded from optional tactical movement");
        for(int cid:new int[]{93,101,108})for(boolean ranked:new boolean[]{false,true})
        {
            Player p=ArenaOrbitTranceManaTest.actor(cid,ranked,false),enemy=ArenaOrbitTranceManaTest.actor(94,ranked,true);
            BotClassPreset preset=BotPresets.getPresetByClassId(cid);
            for(double hp:new double[]{.1,.4,1})for(double distance:new double[]{1,40,70,120,169,200,349,500})for(int flags=0;flags<4;flags++)
                check(!(boolean)step.invoke(null,p,enemy,preset,null,1000L,distance,true,hp,(flags&1)!=0,(flags&2)!=0),"actual production step: armed dagger never retreats on cooldown or tries flank");
            for(int id:preset.getRotation())
            {
                var skill=SkillData.getInstance().getSkill(id,SkillData.getInstance().getMaxLevel(id));
                if(skill!=null)ArenaOrbitTranceManaTest.learn(p,skill);
            }
            check(BotTactics.tacticalApproachRange(p,enemy,preset)<=80,"real blow approach range remains close");
        }
    }
    public static void main(String[]args)throws Exception
    {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        ArenaOrbitTranceManaTest.f(LowManaAutoAttackTest.class,"unsafe").set(null,ArenaOrbitTranceManaTest.f(sun.misc.Unsafe.class,"theUnsafe").get(null));
        handlers.EffectMasterHandler.main(new String[0]);SkillData.getInstance();
        spawnTests();registry();names();dagger();
        System.out.println("PASS: "+checks+" production spawn-plan/name and real armed-dagger step/range checks; 19-class coverage, fixed Bishop fallback, no DB/world/client");
    }
}
