package custom.fakeplayersai;

import java.io.File;
import java.lang.classfile.ClassFile;
import java.lang.constant.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.ai.PlayerAI;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Actual production emergency method, real core actors/skill flags/resources/conditions.
 * Only useMagic and UI target access are recording seams; no DB/client/cast scheduler. */
public class BerserkerSoulCleanseTest
{
    static int checks;
    static Unsafe unsafe;
    static Class<?> recording,state;
    static Method cleanse,act,poll;
    static Skill skill;
    static final Map<Player,Trace> traces=new IdentityHashMap<>();
    static class Trace {WorldObject target,castTarget;int requests;boolean accept=true;}
    static void verify(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void target(Player p,WorldObject t){traces.get(p).target=t;}
    public static WorldObject target(Player p){return traces.get(p).target;}
    public static boolean request(Player p,Skill s,boolean force,boolean shift)
    {
        Trace trace=traces.get(p);trace.requests++;trace.castTarget=p.getTarget();
        verify(s==skill&&s.isMagic()&&force&&shift,"real native magical1510 requested force+shift");
        verify(p.getTarget()==p,"self selected before useMagic, not opponent");
        if(trace.accept)p.setCastingNow(true);
        return trace.accept;
    }
    // Player constructors are private. A constructor-free test subtype with three
    // recording overrides is defined through the standard Java25 classfile API.
    // All state/query/condition/MP/reuse/effect code remains inherited real core code.
    static void recordingType()throws Exception
    {
        ClassDesc player=ClassDesc.of("org.l2jmobius.gameserver.entity.actor.Player"),world=ClassDesc.of("org.l2jmobius.gameserver.entity.WorldObject"),sk=ClassDesc.of("org.l2jmobius.gameserver.mechanics.skill.Skill"),test=ClassDesc.of(BerserkerSoulCleanseTest.class.getName());
        var bytes=ClassFile.of().build(ClassDesc.of("custom.fakeplayersai.CleanseRecordingPlayer"),c->c.withSuperclass(player)
            .withMethodBody("setTarget",MethodTypeDesc.of(ConstantDescs.CD_void,world),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).invokestatic(test,"target",MethodTypeDesc.of(ConstantDescs.CD_void,player,world)).return_())
            .withMethodBody("getTarget",MethodTypeDesc.of(world),ClassFile.ACC_PUBLIC,b->b.aload(0).invokestatic(test,"target",MethodTypeDesc.of(world,player)).areturn())
            .withMethodBody("useMagic",MethodTypeDesc.of(ConstantDescs.CD_boolean,sk,ConstantDescs.CD_boolean,ConstantDescs.CD_boolean),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).iload(2).iload(3).invokestatic(test,"request",MethodTypeDesc.of(ConstantDescs.CD_boolean,player,sk,ConstantDescs.CD_boolean,ConstantDescs.CD_boolean)).ireturn()));
        recording=MethodHandles.lookup().defineClass(bytes);
    }
    static Player bot(int cid,boolean ranked,boolean human)throws Exception
    {
        Player original=actor(cid,ranked,human),p=(Player)unsafe.allocateInstance(recording);
        for(Class<?> c=Player.class;c!=Object.class;c=c.getSuperclass())for(Field field:c.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);field.set(p,field.get(original));}
        traces.put(p,new Trace());p.getTemplate().setRace(Race.KAMAEL);
        Field access=f(Player.class,"_accessLevel");access.set(p,unsafe.allocateInstance(access.getType()));
        f(Player.class,"_charges").set(p,new AtomicInteger());
        f(Player.class,"_autoPlaying").set(p,new AtomicBoolean());
        f(Player.class,"_autoUseSettings").set(p,new AutoUseSettingsHolder());
        f(Creature.class,"_reuseTimeStampsSkills").set(p,new HashMap<>());
        f(Creature.class,"_disabledSkills").set(p,new HashMap<>());
        f(Creature.class,"_ai").set(p,new PlayerAI(p));
        learn(p,skill);mp(p,10000);
        return p;
    }
    static void flags(Player p,int flags)throws Exception{f(EffectList.class,"_effectFlags").setInt(p.getEffectList(),flags);}
    static Object fresh()throws Exception{var c=state.getDeclaredConstructor();c.setAccessible(true);return c.newInstance();}
    static boolean attempt(Player p,Player enemy,Object s,long time)throws Exception
    {return (boolean)cleanse.invoke(null,p,enemy,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),s,time);}
    static void pending(Object s,int id)throws Exception{f(state,"pendingId").setInt(s,id);}
    static void tests()throws Exception
    {
        verify(skill.isMagic()&&skill.getTargetType()==org.l2jmobius.gameserver.mechanics.skill.targets.TargetType.ONE,"XML magical ONE unchanged");
        verify(skill.getHitTime()==2000&&skill.getReuseDelay()==15000&&skill.getMpConsume()==25&&skill.getMpInitialConsume()==7,"native2s cast15s reuse25+7MP unchanged");
        Method kind=BotTactics.class.getDeclaredMethod("kind",Skill.class);kind.setAccessible(true);verify(kind.invoke(null,skill)==null,"never an offensive dispel option");
        int root=EffectFlag.ROOTED.getMask(),physical=EffectFlag.PSYCHICAL_MUTED.getMask();
        for(boolean ranked:new boolean[]{false,true})for(int trigger:new int[]{root,physical,root|physical})for(int foeControl:new int[]{0,EffectFlag.STUNNED.getMask(),EffectFlag.SLEEP.getMask()})
        {
            Player p=bot(131,ranked,false),enemy=actor(94,ranked,true);p.setTarget(enemy);flags(p,trigger);flags(enemy,foeControl);hp(p,10000);
            verify(p.isRooted()||p.isPhysicalMuted(),"actual native flags trigger");
            verify(!p.isAllSkillsDisabled()&&!p.isOutOfControl(),"root/physical mute do not prohibit magic");
            Object s=fresh();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            // Actual act, not just the helper: urgent cleanse precedes reaction and Rush evaluation.
            if(!ranked){f(state,"trainingEnemy").setInt(s,enemy.getObjectId());f(state,"trainingInstance").setInt(s,p.getInstanceId());}
            verify((boolean)act.invoke(null,p,enemy,BotPresets.getPresetByClassId(131),s,1000L,false),"urgent self cleanse owns actual act tick");
            verify(traces.get(p).requests==1&&traces.get(p).castTarget==p&&p.getTarget()==p,"self target preserved during accepted cast at100%HP");
            verify(f(state,"pendingId").getInt(s)==1510&&f(state,"pendingSelf").getBoolean(s)&&!f(state,"pendingDebuff").getBoolean(s),"normal pending tracks self support");
            verify(attempt(p,enemy,s,1001)&&traces.get(p).requests==1,"no duplicate request during native cast");
            flags(p,0);verify(attempt(p,enemy,s,1002),"retain self ownership even if trigger disappears during cast");
            p.setCastingNow(false);poll.invoke(null,p,enemy,s,3000L);
            verify(p.getTarget()==enemy&&f(state,"pendingId").getInt(s)==0,"after completed cast normal pending restores opponent");
            verify(!attempt(p,enemy,s,3001),"no cleanse without trigger");
            // Native resource boundary, silence and real reuse; no free costs/reset.
            for(double mana:new double[]{0,31,32,100})
            {
                p.setCastingNow(false);s=fresh();flags(p,trigger);mp(p,mana);int before=traces.get(p).requests;
                boolean accepted=attempt(p,enemy,s,4000);
                verify(accepted==(mana>=32)&&traces.get(p).requests==before+(mana>=32?1:0),"native32MP boundary");
            }
            mp(p,10000);p.setCastingNow(false);
            for(EffectFlag block:new EffectFlag[]{EffectFlag.MUTED,EffectFlag.STUNNED,EffectFlag.SLEEP,EffectFlag.PARALYZED,EffectFlag.CONFUSED,EffectFlag.FEAR})
            {
                s=fresh();flags(p,trigger|block.getMask());int before=traces.get(p).requests;
                verify(!attempt(p,enemy,s,5000)&&traces.get(p).requests==before,"native blocking control/magic silence respected:"+block);
            }
            flags(p,trigger);p.disableAllSkills();verify(!attempt(p,enemy,fresh(),5000),"all-skills lock preserved");p.enableAllSkills();
            @SuppressWarnings("unchecked") Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);
            disabled.put(skill.getReuseHashCode(),System.currentTimeMillis()+15000);verify(!attempt(p,enemy,fresh(),5000),"real reuse blocks cast");disabled.clear();
            p.setCastingNow(true);verify(!attempt(p,enemy,fresh(),5000),"never cancel another real cast");p.setCastingNow(false);
            p.setCastingSimultaneouslyNow(true);verify(!attempt(p,enemy,fresh(),5000),"simultaneous cast preserved");p.setCastingSimultaneouslyNow(false);
            s=fresh();pending(s,793);verify(!attempt(p,enemy,s,5000),"drain unresolved Rush attempt first");
            traces.get(p).accept=false;s=fresh();int before=traces.get(p).requests;
            verify(!attempt(p,enemy,s,6000)&&p.getTarget()==enemy,"rejected cast restores enemy");
            verify(!attempt(p,enemy,s,6499)&&traces.get(p).requests==before+1,"normal failure backoff prevents request spam");
            traces.get(p).accept=true;verify(attempt(p,enemy,s,6500)&&traces.get(p).requests==before+2,"retry after native request refusal backoff");
            p.setCastingNow(false);
        }
        for(boolean ranked:new boolean[]{false,true})
        {
            Player p=bot(131,ranked,false),enemy=actor(94,ranked,true);
            for(int flags:new int[]{0,EffectFlag.DISARMED.getMask(),EffectFlag.PSYCHICAL_ATTACK_MUTED.getMask()}){flags(p,flags);verify(!attempt(p,enemy,fresh(),1000),"disarm/autoattack mute alone is not physical-skill mute");}
            flags(p,root);@SuppressWarnings("unchecked") Map<Integer,Skill> known=(Map<Integer,Skill>)f(Creature.class,"_skills").get(p);known.remove(1510);verify(!attempt(p,enemy,fresh(),1000),"unknown skill not invented");
            for(int cid:new int[]{88,93,97,95,115}){Player other=bot(cid,ranked,false);flags(other,root);verify(!attempt(other,enemy,fresh(),1000),"other class untouched:"+cid);}
            Player human=bot(131,ranked,true);flags(human,root);verify(!attempt(human,enemy,fresh(),1000),"human not commandeered");
        }
    }
    @SuppressWarnings("unchecked") static void leaseTests()throws Exception
    {
        Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
        Map<Integer,Object> states=(Map<Integer,Object>)f(BotTactics.class,"STATES").get(null);
        for(boolean ranked:new boolean[]{false,true})
        {
            Player p=bot(131,ranked,false),enemy=actor(94,ranked,true);
            if(ranked)roster.put(p.getObjectId(),p);else verify(BotTactics.initializeTraining(p,enemy),"isolated training explicitly authorized");
            flags(p,EffectFlag.ROOTED.getMask());p.setTarget(enemy);
            verify(BotTactics.tickSoulCleanse(p,enemy,BotPresets.getPresetByClassId(131),ranked),"public validated emergency hook accepts root");
            verify(BotTactics.isOwned(p.getObjectId())&&p.getTarget()==p,"short lease protects self from generic parent retarget");
            verify(BotTactics.tickSoulCleanse(p,enemy,BotPresets.getPresetByClassId(131),ranked)&&traces.get(p).requests==1,"pending hook holds without repeated cast");
            flags(p,0);p.setCastingNow(false);
            verify(!BotTactics.tickSoulCleanse(p,enemy,BotPresets.getPresetByClassId(131),ranked),"completed cleanse returns control to parent");
            verify(!BotTactics.isOwned(p.getObjectId())&&p.getTarget()==enemy,"temporary lease released and opponent restored");
            flags(p,EffectFlag.PSYCHICAL_MUTED.getMask());
            verify(BotTactics.tickSoulCleanse(p,enemy,BotPresets.getPresetByClassId(131),ranked),"second learned trigger uses same native hook");
            // Context reset releases only the temporary cleanse lease.
            p.setCastingNow(false);flags(p,0);BotTactics.forget(p.getObjectId());
            verify(!BotTactics.isOwned(p.getObjectId())&&!states.containsKey(p.getObjectId()),"normal teardown removes lease/pending state");
            roster.remove(p.getObjectId());
            if(ranked)
            {
                f(Player.class,"_olympiadStart").setBoolean(p,false);int before=traces.get(p).requests;flags(p,EffectFlag.ROOTED.getMask());
                verify(!BotTactics.tickSoulCleanse(p,enemy,BotPresets.getPresetByClassId(131),true)&&traces.get(p).requests==before,"countdown/inactive match never casts");
            }
            else verify(!BotTactics.tickSoulCleanse(p,enemy,BotPresets.getPresetByClassId(131),false),"training after teardown never casts");
        }
    }
    public static void main(String[]args)throws Exception
    {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);
        recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        cleanse=BotTactics.class.getDeclaredMethod("soulCleanse",Player.class,Player.class,BotClassPreset.class,state,long.class);cleanse.setAccessible(true);
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("cleanse48-geodata-");
        org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;
        org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        tests();leaseTests();System.out.println("PASS: "+checks+" real production Soul Cleanse checks: root/physical mute, self-target, immediate priority, nativeMP/reuse/silence, pending and rejection; recording cast/UI only, no live server");
    }
}
