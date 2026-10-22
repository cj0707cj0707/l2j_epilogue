package custom.fakeplayersai;

import java.util.*;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Actual script methods/native templates and guards; cast/movement recording, no live server. */
public class ArenaKiteRecovery97Test extends ArenaEntrySummon96Test
{
    static class QuietPetAI97 extends PetAI
    {
        QuietPetAI97(Summon pet){super(pet);}
        @Override public void setIntentionIdle(){_intention=Intention.IDLE;}
    }
    static void cancelWithPet()throws Exception
    {
        for(int cid:new int[]{94,103})for(boolean ranked:new boolean[]{false,true})
        for(boolean human:new boolean[]{false,true})for(int tank:new int[]{90,91})for(int id:new int[]{110,368,760})
        {
            var preset=BotPresets.getPresetByClassId(cid);Player e=actor(tank,ranked,human),p=armed96(preset,e,ranked);
            learn(p,nativeSkill(1056));register(p,e,ranked,human);Servitor pet=pet96(e,tank==90?14918:14835);
            try
            {
                defense(e,id);BotTactics.beginCombat(p,e);Object s=publicState(p);
                f(state,"nextDecision").setLong(s,Long.MAX_VALUE); // urgent Cancel cannot wait for the ordinary nuke clock
                targetEnemy=e;last=0;
                verify(BotTactics.tickSummonPriority(p,e,preset,ranked)&&last==1056&&p.getTarget()==e,
                    "live tank defense: Cancel outranks pet focus and normal decision latency cid="+cid+" buff="+id);
                verify(BotSpawner.tacticalSkillAllowed(p,e,p.getKnownSkill(1056),false),"shared candidate allows only the scoped Cancel exception");
                verify(BotRecoveryRules.guard(event96(p,e,1056))==null,"synchronous native guard admits anti-defense Cancel on owner");
                verify(BotRecoveryRules.guard(event96(p,e,cid==94?1230:1235))!=null,"ordinary owner damage remains blocked");
                int casts=traces.get(p).requests;
                verify(BotTactics.tickSummonPriority(p,e,preset,ranked)&&traces.get(p).requests==casts&&p.isCastingNow(),"in-flight Cancel never interrupted for pet");
                finish(p,e,s,clock()+7000);disable(p,1056);prompt(s);last=0;targetEnemy=pet;
                verify(BotTactics.tickSummonPriority(p,e,preset,ranked)&&last!=1056&&last!=0&&p.getTarget()==pet,
                    "Cancel native reuse: resume real pet damage, no idle wait");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void cancelProtection()throws Exception
    {
        for(int cid:new int[]{94,103})for(boolean ranked:new boolean[]{false,true})for(int gate:new int[]{0,1,2,3,4,5})
        {
            Player e=actor(91,ranked,true),p=armed96(BotPresets.getPresetByClassId(cid),e,ranked);
            learn(p,nativeSkill(1056));register(p,e,ranked,true);pet96(e,14835);defense(e,760);
            try
            {
                if(gate==0)e.setInvul(true);
                if(gate==1)mp(p,0);
                if(gate==2)disable(p,1056);
                if(gate==3)flags(p,EffectFlag.MUTED.getMask());
                if(gate==4)flags(p,EffectFlag.STUNNED.getMask());
                if(gate==5)at(p,e.getX()+900,e.getY());
                targetEnemy=e.getSummon();last=0;BotTactics.tickSummonPriority(p,e,BotPresets.getPresetByClassId(cid),ranked);
                verify(last!=1056,"anti-defense Cancel retains invul/MP/reuse/mute/CC/range gate="+gate);
                if(gate==0)verify(BotRecoveryRules.guard(event96(p,e,1056))!=null,"protected owner cannot be dispelled through pet exception");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void boundedFocus()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})
        {
            Player e=actor(91,ranked,human),p=armed96(BotPresets.getPresetByClassId(103),e,ranked);register(p,e,ranked,human);
            Servitor pet=pet96(e,14835);var focus=new SummonPriority();
            try
            {
                verify(focus.target(p,e,0)==pet&&focus.target(p,e,7999)==pet,"pet gets bounded first opportunity");
                verify(focus.target(p,e,8000)==null&&focus.target(p,e,10999)==null,"no real damage/closing: release pet for bounded owner opportunity");
                verify(focus.target(p,e,11000)==pet,"same living pet is retried; no permanent priority loss");
                f(org.l2jmobius.gameserver.entity.actor.status.CreatureStatus.class,"_currentHp").setDouble(pet.getStatus(),9990);
                verify(focus.target(p,e,18500)==pet&&focus.target(p,e,26000)==pet,"actual HP loss rearms progress, requests alone do not");
                verify(focus.target(p,e,26500)==null,"eventually release a non-damageable pet again");
                focus.reset();at(p,pet.getX()+400,pet.getY());verify(focus.target(p,e,0)==pet,"new approach starts observation");
                at(p,pet.getX()+300,pet.getY());verify(focus.target(p,e,7500)==pet&&focus.target(p,e,15000)==pet,"meaningful closing is progress even before first hit");
                verify(focus.target(p,e,15500)==null,"stationary unreachable distance cannot hold forever");
                at(p,e.getX()+30,e.getY());BotTactics.beginCombat(p,e);Object s=publicState(p);
                SummonPriority actual=(SummonPriority)f(state,"summonFocus").get(s);actual.target(p,e);actual.lease(false);
                f(SummonPriority.class,"progressAt").setLong(actual,clock()-8001);last=0;
                verify(!BotTactics.tickSummonPriority(p,e,BotPresets.getPresetByClassId(103),ranked)&&p.getTarget()==e,
                    "actual public hook releases lease/target after no-progress window");
                verify(!BotTactics.isOwned(p.getObjectId()),"owner driver authority restored after failed pet focus");
                verify(BotRecoveryRules.guard(event96(p,e,1235))==null,"ordinary legal owner nuke re-admitted during retry gap");
                e.setInvul(true);var attack=new OnCreatureAttack();attack.setAttacker(p);attack.setTarget(e);
                verify(BotRecoveryRules.guard(event96(p,e,1235))!=null&&BotRecoveryRules.bluffAttack(attack)!=null,
                    "fallback never bypasses actual owner invulnerability");
                f(Player.class,"_summon").set(e,null);verify(focus.target(p,e,20000)==null,"pet removal clears observation");
                var replacement=pet96(e,14835);verify(focus.target(p,e,20001)==replacement,"new current pet gets a new progress window");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void pantherPulse()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})
        {
            Player e=actor(103,ranked,true),p=armed96(BotPresets.getPresetByClassId(91),e,ranked);register(p,e,ranked,true);
            Servitor pet=pet96(p,14835);PetAI ai=new QuietPetAI97(pet);f(Creature.class,"_ai").set(pet,ai);var pulse=new SummonAttackPulse();
            try
            {
                f(WorldObject.class,"_isSpawned").setBoolean(e,true);p.setTarget(p);pulse.tick(p,e,1000);int requests=ai.requests;
                pulse.tick(p,e,1001);verify(ai.requests==requests,"active panther ATTACK keeps3s cadence");
                // Stop through the ordinary AI controller, not by editing production timers.
                pet.getAI().setIntentionIdle();pulse.tick(p,e,1100);
                verify(ai.requests==requests+1&&ai.requested==e&&p.getTarget()==p,"idle panther reattacks mage immediately; owner self target untouched");
                var attack=new OnCreatureAttack();attack.setAttacker(pet);attack.setTarget(e);
                verify(BotRecoveryRules.bluffAttack(attack)==null,"Hell Knight panther native attack on unprotected Mystic Muse is admitted");
                e.setInvul(true);pulse.tick(p,e,1101);verify(ai.requests==requests+1,"no panther attack request into invulnerability");
                verify(BotRecoveryRules.bluffAttack(attack)!=null,"summon invulnerability event guard retained");
            }
            finally{cleanup(p,e);}
        }
    }
    static void rankedKiteRush()throws Exception
    {
        Class<?> bs=Class.forName("custom.fakeplayersai.BotManager$BerserkerState");var constructor=bs.getDeclaredConstructor(int.class,int.class,int.class,int.class);constructor.setAccessible(true);
        var step=BotManager.class.getDeclaredMethod("driveBerserkerStep",Player.class,BotClassPreset.class,bs,Creature.class,long.class);step.setAccessible(true);
        for(int foe:new int[]{94,103,110,92})for(boolean staleCast:new boolean[]{false,true})
        {
            Player e=actor(foe,true,true),p=armed96(BotPresets.getPresetByClassId(131),e,true);register(p,e,true,true);
            try
            {
                float[] pool=new float[256];Arrays.fill(pool,10000);f(e.getTemplate().getClass(),"_baseCp").set(e.getTemplate(),pool);
                at(e,p.getX()+400,p.getY());MoveAI move=new MoveAI(e);f(Creature.class,"_ai").set(e,move);
                move.setIntentionMoveTo(new Location(e.getX()+300,e.getY(),e.getZ()));verify(RoleCombatPolicy.kited(p,e),"actual outward route recognized as kite");
                Object b=constructor.newInstance(p.getX(),p.getY(),p.getZ(),p.getOlympiadGameId());f(bs,"rushDone").setBoolean(b,true);f(bs,"enginesStopped").setBoolean(b,true);
                f(bs,"enumaDone").setBoolean(b,false);f(bs,"crushingDone").setBoolean(b,false);
                if(staleCast)f(org.l2jmobius.gameserver.ai.AbstractAI.class,"_intention").set(p.getAI(),Intention.CAST);
                p.setTarget(e);targetEnemy=e;last=0;
                verify((boolean)step.invoke(null,p,BotPresets.getPresetByClassId(131),b,e,10000L)&&last==793,
                    "kite rearms ready Rush before incomplete out-of-range melee burst/stale CAST foe="+foe);
                int requests=traces.get(p).requests;step.invoke(null,p,BotPresets.getPresetByClassId(131),b,e,10100L);
                verify(p.isCastingNow()&&traces.get(p).requests==requests,"never interrupt or duplicate in-flight Rush");
                // A new naturally opened gap must rearm again, not consume a once-per-match flag.
                p.setCastingNow(false);f(bs,"rushDone").setBoolean(b,true);((BerserkerCycle)f(bs,"movement").get(b)).pollRush(17000,false,400,p.getX(),p.getY());
                last=0;verify((boolean)step.invoke(null,p,BotPresets.getPresetByClassId(131),b,e,20000L)&&last==793,"continuing kite permits another native-ready Rush");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void rushSafety()throws Exception
    {
        Class<?> bs=Class.forName("custom.fakeplayersai.BotManager$BerserkerState");var constructor=bs.getDeclaredConstructor(int.class,int.class,int.class,int.class);constructor.setAccessible(true);
        var step=BotManager.class.getDeclaredMethod("driveBerserkerStep",Player.class,BotClassPreset.class,bs,Creature.class,long.class);step.setAccessible(true);
        for(int gate:new int[]{0,1,2,3})
        {
            Player e=owner(103,true),p=armed96(BotPresets.getPresetByClassId(131),e,true);register(p,e,true,true);
            try
            {
                at(e,p.getX()+400,p.getY());MoveAI move=new MoveAI(e);f(Creature.class,"_ai").set(e,move);move.setIntentionMoveTo(new Location(e.getX()+300,e.getY(),e.getZ()));
                Object b=constructor.newInstance(p.getX(),p.getY(),p.getZ(),p.getOlympiadGameId());f(bs,"rushDone").setBoolean(b,true);f(bs,"enginesStopped").setBoolean(b,true);
                if(gate==0)disable(p,793);if(gate==1)mp(p,0);if(gate==2)flags(p,EffectFlag.STUNNED.getMask());if(gate==3)p.setCastingNow(true);
                p.setTarget(e);targetEnemy=e;last=0;step.invoke(null,p,BotPresets.getPresetByClassId(131),b,e,10000L);
                verify(last!=793,"kite Rush never bypasses reuse/MP/CC or another real cast gate="+gate);
                if(gate==3)verify(p.isCastingNow(),"existing cast is preserved");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
        Player p=owner(131,true),e=owner(103,true);at(p,0,0);at(e,400,0);MoveAI move=new MoveAI(e);f(Creature.class,"_ai").set(e,move);
        verify(!RoleCombatPolicy.kited(p,e),"stationary foe is not invented kiting");move.setIntentionMoveTo(new Location(100,0,0));
        verify(!RoleCombatPolicy.kited(p,e),"movement toward Berserker is not outward kiting");
    }
    public static void main(String[] args){try
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
        handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("arena97-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        org.l2jmobius.gameserver.handler.TargetHandler.getInstance().registerHandler(new handlers.skill.targets.Party());
        if(args.length>1)
        {
            switch(args[1]){case "cancel" -> cancelWithPet();case "pulse" -> pantherPulse();case "rush" -> rankedKiteRush();default -> throw new IllegalArgumentException(args[1]);}
        }
        else {cancelWithPet();cancelProtection();boundedFocus();pantherPulse();rankedKiteRush();rushSafety();}
        System.out.println("PASS: "+checks+" rule97 Cancel/pet priority, bounded progress, idle panther and ranked kite-Rush checks; no live server");System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
