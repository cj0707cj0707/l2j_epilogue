package custom.fakeplayersai;

import java.io.File;
import java.lang.classfile.ClassFile;
import java.lang.constant.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
import static custom.fakeplayersai.BerserkerSoulCleanseTest.*;

/** Actual production act/helper, native skill/flags/resources/reuse. Only target UI
 * and cast submission are recording seams; does not simulate native effect removal. */
public class DaggerEscapeShackleTest
{
    static int checks;
    static Method escape,drive,resolve;
    static Skill ordinary;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static boolean request(Player p,Skill s,boolean force,boolean shift)
    {
        Trace trace=traces.get(p);trace.requests++;trace.castTarget=p.getTarget();
        if(s==ordinary)
        {
            check(p.isRooted()&&p.getTarget()==p&&s.getId()==111,"ordinary defensive rotation remains usable under root");
            if(trace.accept)p.setCastingNow(true);
            return trace.accept;
        }
        check(s==skill&&!s.isMagic()&&s.getTargetType()==TargetType.SELF&&force&&shift,"native physical453 SELF submitted");
        check(p.isRooted()&&p.getTarget()==p,"root still present and self targeted");
        if(trace.accept)p.setCastingNow(true);
        return trace.accept;
    }
    static void recording453()throws Exception
    {
        ClassDesc player=ClassDesc.of(Player.class.getName()),world=ClassDesc.of(WorldObject.class.getName()),sk=ClassDesc.of(Skill.class.getName()),base=ClassDesc.of(BerserkerSoulCleanseTest.class.getName()),test=ClassDesc.of(DaggerEscapeShackleTest.class.getName());
        var bytes=ClassFile.of().build(ClassDesc.of("custom.fakeplayersai.EscapeRecordingPlayer"),c->c.withSuperclass(player)
            .withMethodBody("setTarget",MethodTypeDesc.of(ConstantDescs.CD_void,world),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).invokestatic(base,"target",MethodTypeDesc.of(ConstantDescs.CD_void,player,world)).return_())
            .withMethodBody("getTarget",MethodTypeDesc.of(world),ClassFile.ACC_PUBLIC,b->b.aload(0).invokestatic(base,"target",MethodTypeDesc.of(world,player)).areturn())
            .withMethodBody("useMagic",MethodTypeDesc.of(ConstantDescs.CD_boolean,sk,ConstantDescs.CD_boolean,ConstantDescs.CD_boolean),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).iload(2).iload(3).invokestatic(test,"request",MethodTypeDesc.of(ConstantDescs.CD_boolean,player,sk,ConstantDescs.CD_boolean,ConstantDescs.CD_boolean)).ireturn()));
        recording=MethodHandles.lookup().defineClass(bytes);
    }
    static boolean attempt453(Player p,Player enemy,Object s,long time)throws Exception
    {return (boolean)escape.invoke(null,p,enemy,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),s,time);}
    static void runTests()throws Exception
    {
        check(!skill.isMagic()&&skill.getTargetType()==TargetType.SELF,"native physical SELF definition");
        check(skill.getHitTime()==1000&&skill.getReuseDelay()==15000&&skill.getMpConsume()==60,"native1s cast15s reuse60MP");
        Method kind=BotTactics.class.getDeclaredMethod("kind",Skill.class);kind.setAccessible(true);
        check(kind.invoke(null,skill)==null,"never random tactical dispel");
        check(!BotSpawner.trainingPreparationSkillAllowed(skill),"not a preparation buff");
        int root=EffectFlag.ROOTED.getMask();
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{93,101,108})
        {
            Player p=bot(cid,ranked,false),enemy=actor(94,ranked,true);p.setTarget(enemy);hp(p,10000);
            check(!BotSpawner.tacticalSkillAllowed(p,enemy,skill,true),"general self selection blocked");
            Object s=fresh();
            check(!attempt453(p,enemy,s,1000)&&traces.get(p).requests==0,"no root means no cast");
            flags(p,root);f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            check((boolean)drive.invoke(null,p,enemy,BotPresets.getPresetByClassId(cid),s,1000L,false),"actual act priority before decision gate");
            check(traces.get(p).requests==1&&traces.get(p).castTarget==p,"urgent453 requested at fullHP");
            check(f(state,"pendingId").getInt(s)==453&&f(state,"pendingSelf").getBoolean(s),"shared pending tracks support cast");
            check(attempt453(p,enemy,s,1001)&&traces.get(p).requests==1,"no duplicate while pending");
            flags(p,0);check(attempt453(p,enemy,s,1002),"retain pending ownership if root expires midcast");
            p.setCastingNow(false);resolve.invoke(null,p,enemy,s,3000L);
            check(p.getTarget()==enemy&&f(state,"pendingId").getInt(s)==0,"restore enemy after real cast resolution");
            check(!attempt453(p,enemy,s,3001),"no re-cast after root ended");
            flags(p,root);
            for(double mana:new double[]{0,59,60,10000})
            {
                p.setCastingNow(false);mp(p,mana);int before=traces.get(p).requests;
                check(attempt453(p,enemy,fresh(),4000)==(mana>=60),"nativeMP availability boundary");
                check(traces.get(p).requests==before+(mana>=60?1:0),"no freeMP cast request");
            }
            mp(p,10000);p.setCastingNow(false);
            for(EffectFlag block:new EffectFlag[]{EffectFlag.PSYCHICAL_MUTED,EffectFlag.STUNNED,EffectFlag.SLEEP,EffectFlag.PARALYZED,EffectFlag.CONFUSED,EffectFlag.FEAR})
            {
                flags(p,root|block.getMask());int before=traces.get(p).requests;
                check(!attempt453(p,enemy,fresh(),5000)&&traces.get(p).requests==before,"native blocking control:"+block);
            }
            flags(p,root|EffectFlag.MUTED.getMask());check(attempt453(p,enemy,fresh(),5001),"magic silence does not block physical453");p.setCastingNow(false);
            flags(p,root);p.disableAllSkills();check(!attempt453(p,enemy,fresh(),5002),"allskills lock respected");p.enableAllSkills();
            @SuppressWarnings("unchecked") Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);
            disabled.put(skill.getReuseHashCode(),System.currentTimeMillis()+15000);s=fresh();
            int before=traces.get(p).requests;
            check(!attempt453(p,enemy,s,6000),"cooldown returns unhandled so rotation continues");
            f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            check((boolean)drive.invoke(null,p,enemy,BotPresets.getPresetByClassId(cid),s,6000L,false),"actual act falls through normal decision gate during cooldown");
            check(traces.get(p).requests==before&&f(state,"pendingId").getInt(s)==0,"waiting does not reserve a pending cast");
            // Exercise the actual normal selector, not only the helper's false return.
            ordinary=SkillData.getInstance().getSkill(111,1);learn(p,ordinary);hp(p,2000);
            float[] cp=new float[256];Arrays.fill(cp,10000);f(p.getTemplate().getClass(),"_baseCp").set(p.getTemplate(),cp);
            f(Creature.class,"_zones").set(enemy,new byte[64]);
            Object normal=fresh();f(state,"nextStep").setLong(normal,Long.MAX_VALUE);
            check((boolean)drive.invoke(null,p,enemy,BotPresets.getPresetByClassId(cid),normal,10000L,false),"actual normal rotation continues while453 cooling down");
            check(f(state,"pendingId").getInt(normal)==111&&traces.get(p).requests==before+1,"ordinary Ultimate Evasion submitted, not an idle wait");
            p.setCastingNow(false);hp(p,10000);ordinary=null;
            disabled.clear();check(attempt453(p,enemy,s,6001),"persistentroot uses453 as soon as native reuse ready");p.setCastingNow(false);
            s=fresh();pending(s,999);check(!attempt453(p,enemy,s,7000),"never steal another pending skill");
            p.setCastingNow(true);check(!attempt453(p,enemy,fresh(),7000),"never interrupt native cast");p.setCastingNow(false);
            p.setCastingSimultaneouslyNow(true);check(!attempt453(p,enemy,fresh(),7000),"never interrupt simultaneouscast");p.setCastingSimultaneouslyNow(false);
            traces.get(p).accept=false;s=fresh();before=traces.get(p).requests;
            check(!attempt453(p,enemy,s,8000)&&p.getTarget()==enemy,"rejection restores enemy and permits ordinaryrotation");
            check(f(state,"pendingId").getInt(s)==0,"rejection does not stall pending");
            check(!attempt453(p,enemy,s,8001)&&traces.get(p).requests==before+1,"bounded failure backoff avoids spam");
            traces.get(p).accept=true;check(attempt453(p,enemy,s,9000),"retry rejected request while root persists");
        }
        for(int cid:new int[]{88,92,94,131})
        {
            Player p=bot(cid,false,false),enemy=actor(94,false,true);flags(p,root);
            check(!attempt453(p,enemy,fresh(),1000),"not enabled for other classes:"+cid);
        }
        Player p=bot(93,false,true),enemy=actor(94,false,true);flags(p,root);
        check(!attempt453(p,enemy,fresh(),1000),"not controlling human dagger");
        p=bot(93,false,false);flags(p,root);
        check(!(boolean)escape.invoke(null,p,enemy,BotPresets.getPresetByClassId(101),fresh(),1000L),"preset/realclass mismatch denied");
        p.removeSkill(skill,false);check(!attempt453(p,enemy,fresh(),1000),"only learned453, no skill grant");
    }
    @SuppressWarnings("unchecked") static void arenaTickTests()throws Exception
    {
        Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{93,101,108})
        {
            Player p=bot(cid,ranked,false),enemy=actor(94,ranked,true);flags(p,EffectFlag.ROOTED.getMask());p.setTarget(enemy);
            if(ranked)roster.put(p.getObjectId(),p);
            else
            {
                check(!BotTactics.tick(p,enemy,BotPresets.getPresetByClassId(cid),false),"unauthorized training pair does not cast");
                check(BotTactics.initializeTraining(p,enemy),"explicitly authorize training pair");
            }
            check(BotTactics.tick(p,enemy,BotPresets.getPresetByClassId(cid),ranked),"actual public tick reaches root escape");
            check(BotTactics.isOwned(p.getObjectId())&&p.getTarget()==p&&traces.get(p).requests==1,"owned self cast through shared arena path");
            check(BotTactics.tick(p,enemy,BotPresets.getPresetByClassId(cid),ranked)&&traces.get(p).requests==1,"next public tick does not duplicate cast");
            BotTactics.forget(p.getObjectId());roster.remove(p.getObjectId());p.setCastingNow(false);
            if(ranked)f(Player.class,"_olympiadStart").setBoolean(p,false);
            check(!BotTactics.tick(p,enemy,BotPresets.getPresetByClassId(cid),ranked)&&traces.get(p).requests==1,"inactive match or ended training never casts");
            p.getAutoUseSettings().getAutoBuffs().add(453);
            BotSpawner.refreshAutoBuffs(p,BotPresets.getPresetByClassId(cid));
            check(!p.getAutoUseSettings().getAutoBuffs().contains(453),"refresh removes stale453 autobuff");
        }
    }
    public static void main(String[]args)throws Exception
    {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(453,1);
        recording453();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        escape=BotTactics.class.getDeclaredMethod("escapeShackle",Player.class,Player.class,BotClassPreset.class,state,long.class);escape.setAccessible(true);
        drive=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);drive.setAccessible(true);
        resolve=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);resolve.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("escape49-geodata-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;
        org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        runTests();arenaTickTests();System.out.println("PASS: "+checks+" real production Escape Shackle checks; cast/UI recording only, no live server");
    }
}
