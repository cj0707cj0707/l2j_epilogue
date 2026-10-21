package custom.fakeplayersai;

import java.util.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Actual act/poll/mandatory hooks/native XML and live effects. No live world/DB. */
public class ArenaClassRules94Test extends SleepDefense93Test
{
    static void cardinal94()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})
        for(int cid:new int[]{88,92,93,94,95,97,102,103,108,109,110,113,114,115,131,134})
        for(int clarity:new int[]{0,1397,3281,3282})
        {
            Player e=actor(cid,ranked,human),p=sleeper(e,97,ranked);clear(e,3282);
            if(clarity!=0)nativeSkill(clarity).applyEffects(e,e,true,60);
            Object s=arena(p,e);NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);appliedSleep(p,e,97);
            f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            verify(run(p,e,s,10000)&&last==0&&p.isMoving(),"non-tank: retreat starts before Mana Burn/hold");
            arrive93(p,ai);verify(run(p,e,s,11000)&&last==0&&p.isMoving(),"non-tank: complete center crossing");arrive93(p,ai);
            int moves=ai.requests;verify(run(p,e,s,12000),"owned retreat completion");
            if(clarity==3281||clarity==3282)
            {
                verify(last==0&&!p.isMoving(),"only active talisman Clarity keeps sleep HOLD");
                e.getEffectList().getBuffInfoBySkillId(clarity).setInUse(false);
                verify(run(p,e,s,12100)&&last==1398&&ai.requests==moves,"Clarity expiry: immediate Mana Burn, no repeated run");
            }
            else verify(last==1398&&!p.isMoving(),"no talisman or ordinary Clarity1397: immediate Mana Burn");
        }
    }
    static void roots94()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{88,90,91,93,99,106,108,113,114,131})
        for(int success:new int[]{0,1,2,3})
        {
            Player e=actor(cid,ranked,true),p=dom(e,ranked);Object s=arena(p,e);
            NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);appliedSleep(p,e,115);
            var cycle=(DominatorSleepRoot)f(state,"sleepRoot").get(s);
            int limit=success==0?3:success;
            for(int attempt=1;attempt<=limit;attempt++)
            {
                prompt(s);verify(run(p,e,s,10000+attempt*10000)&&last==1208&&!p.isMoving(),"landed own melee sleep: root before retreat attempt="+attempt);
                verify(cycle.attempts()==attempt,"exactly one accepted request per attempt");
                int casts=traces.get(p).requests;verify(run(p,e,s,10100+attempt*10000)&&traces.get(p).requests==casts,"no duplicate root during cast");
                finish(p,e,s,15000+attempt*10000);
            }
            if(success!=0)
            {
                // Actual native Root effect, not a request or merely sleeping.
                DominatorControlShadowTest.applied(p,e,1208);
            }
            prompt(s);verify(run(p,e,s,60000)&&last==0&&p.isMoving(),"root success or three failures: one retreat");
            int moves=ai.requests;verify(run(p,e,s,60100)&&last==0&&ai.requests==moves,"retreat owns whole native run");
            arrive93(p,ai);prompt(s);verify(run(p,e,s,62000)&&last>0&&last!=1208&&!p.isMoving(),"after retreat resume rotation, no root/second retreat cid="+cid+" success="+success+" last="+last);finish(p,e,s,67000);
            prompt(s);run(p,e,s,70000);verify(last!=1208&&cycle.attempts()==limit,"no fourth attempt under same sleep");
        }
        for(boolean ranked:new boolean[]{false,true})
        {
            Player e=actor(88,ranked,true),p=dom(e,ranked);Object s=arena(p,e);appliedSleep(p,e,115);
            disable(p,1208);verify(run(p,e,s,10000)&&last==0,"root reuse does not spend an attempt or wake target");
            var cycle=(DominatorSleepRoot)f(state,"sleepRoot").get(s);verify(cycle.attempts()==0,"reuse spent zero attempts");
            disabled(p).clear();flags(p,EffectFlag.MUTED.getMask());run(p,e,s,11000);verify(last==0&&cycle.attempts()==0,"mute spent zero attempts");flags(p,0);
            verify(run(p,e,s,12000)&&last==1208,"retry legal root as soon as available");finish(p,e,s,17000);
            clearSleep(e);appliedSleep(p,e,115);prompt(s);run(p,e,s,20000);verify(last==1208&&cycle.attempts()==1,"new actual sleep gets a fresh budget");
        }
    }
    static void mandatory94()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})
        {
            Player e=actor(88,ranked,true),p=dom(e,ranked);register(p,e,ranked,true);
            try
            {
                appliedSleep(p,e,115);Object s=publicState(p);
                // Mandatory hook owns root even with the optional tactical toggle disabled.
                last=0;targetEnemy=e;verify(BotTactics.tickRoleCombat(p,e,BotPresets.getPresetByClassId(115),ranked)&&last==1208,"public mandatory melee-Dominator hook starts root");
                s=publicState(p);var cycle=(DominatorSleepRoot)f(state,"sleepRoot").get(s);
                for(int attempt=1;attempt<=3;attempt++)
                {
                    if(attempt>1){prompt(s);verify(run(p,e,s,10000+attempt*10000)&&last==1208,"next native root request");}
                    verify(cycle.attempts()==attempt,"public state counts accepted root");
                    verify(BotRecoveryRules.guard(castEvent(p,e,1208))==null,"native admission allows current pending attempt including third");
                    finish(p,e,s,15000+attempt*10000);
                }
                verify(BotRecoveryRules.guard(castEvent(p,e,1208))!=null,"native admission blocks fourth root under same sleep");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void protectedTank94()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int shield:new int[]{110,368,760})
        {
            Player e=actor(90,ranked,true),p=dom(e,ranked);Object s=arena(p,e);defense(e,shield);
            NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);appliedSleep(p,e,115);
            for(int i=0;i<3;i++)
            {prompt(s);verify(run(p,e,s,10000+i*10000)&&last==1208,"protected tank: bounded root before retreat");finish(p,e,s,15000+i*10000);}
            verify(run(p,e,s,45000)&&p.isMoving()&&last==0,"protected tank: after three attempts one sleep retreat");arrive93(p,ai);
            int moves=ai.requests;prompt(s);verify(run(p,e,s,46000)&&last==0&&p.isMoving()&&ai.requests>moves,"completed phase resumes normal defensive kite, not legacy sleep HOLD");
        }
    }
    static void celestial94()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{88,90,92,94,97,115})for(int protection:new int[]{1,2,3})for(double health:new double[]{.3,.74,.91,1})
        {
            Player e=actor(cid,ranked,true),p=cardinal(e,ranked,1394,1398,1230,1218);hp(p,p.getMaxHp()*health);
            shield(p,protection);Object s=arena(p,e);f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            verify(run(p,e,s,10000)&&last==1394&&!p.isMoving(),"Celestial: Trance before healthy nuke and urgent heal, no decision delay");finish(p,e,s,15000);
            verify(run(p,e,s,16000)&&last==1394,"resisted sleep: immediate repeated legal Trance under live shield");finish(p,e,s,21000);
            appliedSleep(p,e,97);run(p,e,s,22000);verify(last==0&&!p.isMoving(),"landed sleep under shield: no damage or useless refresh");clearSleep(e);
            disable(p,1394);run(p,e,s,23000);verify(last==0&&!p.isMoving(),"Trance reuse: no unrelated filler under Celestial");disabled(p).clear();
            for(int id:new int[]{1418,5576}){var effect=p.getEffectList().getBuffInfoBySkillId(id);if(effect!=null)effect.setInUse(false);}p.setInvul(false);prompt(s);
            verify(run(p,e,s,30000)&&last==(health<.75?1218:health>.90?1230:1394),"shield ended: normal heal/nuke/Trance priority resumes");
        }
    }
    static void aurasAndBuffs94()throws Exception
    {
        var build=BotSpawner.class.getDeclaredMethod("buildOlympiadProfile",int[][].class);build.setAccessible(true);
        int[][] raw={{1268,4},{1204,2},{1040,3}};
        int[][] oly=(int[][])build.invoke(null,(Object)raw);
        verify(Arrays.stream(oly).noneMatch(b->b[0]==1268)&&oly.length==2&&raw.length==3,"Olympiad-only profile excludes Vampiric Rage; full profile untouched");
        for(int cid:new int[]{88,90,92,93,102,109,113,114,131,134,94,95,97,103,110,115})
        {
            Player e=actor(cid,false,true),p=dom(e,false);learn(p,nativeSkill(1283));
            boolean physical=RoleCombatPolicy.physical(e);BotSpawner.prepareSoulGuard(p,e);
            verify(CardinalSurvival.live(p,1283)==physical,"Soul Guard prepared only against physical classes cid="+cid);
            if(!physical)continue;
            BuffInfo guard=p.getEffectList().getBuffInfoBySkillId(1283);BotSpawner.prepareSoulGuard(p,e);
            verify(p.getEffectList().getBuffInfoBySkillId(1283)==guard,"active toggle never reapplied/reset during prep");
            verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1283),true),"shared selection cannot press active toggle off");
            guard.setInUse(false);Object s=arena(p,e);prompt(s);verify(run(p,e,s,10000)&&last==1283&&p.getTarget()==p,"missing aura: ordinary native self-toggle request even against archers");
        }
    }
    static void duelist94()throws Exception
    {
        Player p=owner(88,false),e=actor(92,false,true);foeReady(p,e);at(p,0,0);at(e,400,0);
        MoveAI ai=new MoveAI(e);f(Creature.class,"_ai").set(e,ai);
        for(double health:new double[]{.3999,.40,.4001,1})for(boolean moving:new boolean[]{false,true})
        {
            hp(e,e.getMaxHp()*health);
            if(moving)ai.setIntentionMoveTo(new org.l2jmobius.gameserver.entity.Location(700,0,0));else f(Creature.class,"_move").set(e,null);
            verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(5),false),"Double Sonic Slash blocked even from stale overrides");
            boolean allowed=moving&&health<.40;
            verify(BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(6),false)==allowed,"Blaster strict40 + outward kite conjunction");
            for(int id:new int[]{261,9,7})verify(BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(id),false),"Triple/Buster/Storm active candidates");
        }
        var preset=BotPresets.getPresetByClassId(88);
        for(int id:new int[]{261,9,7,6})verify(Arrays.stream(preset.getRotation()).anyMatch(x->x==id),"effective default/override rotation contains requested skill");
        verify(Arrays.stream(preset.getRotation()).noneMatch(x->x==5),"effective rotation excludes Double");
    }
    public static void main(String[]args){try
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
        handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("arena94-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        BotPresets.loadOverrides();cardinal94();roots94();mandatory94();protectedTank94();celestial94();aurasAndBuffs94();duelist94();
        System.out.println("PASS: "+checks+" rule94 Clarity/root-budget/Celestial/SoulGuard/Olympiad-buffs/Duelist checks; no live server");System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
