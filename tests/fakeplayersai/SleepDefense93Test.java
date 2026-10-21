package custom.fakeplayersai;

import java.util.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Rule93: actual act, real native skills/effects and movement AI; no live world/DB. */
public class SleepDefense93Test extends RoleCombat88Test
{
    static Player sleeper(Player e,int cid,boolean ranked)throws Exception
    {
        Player p=cid==97?cardinal(e,ranked,1394,1398,1230,1218):dom(e,ranked);
        hp(p,10000);cp(p,10000);return p;
    }
    static void arrive93(Player p,NativeMoveAI ai)throws Exception
    {at(p,ai.requested.getX(),ai.requested.getY());f(Creature.class,"_move").set(p,null);}
    static void branches()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})
        for(int cid:new int[]{97,115})for(int tank:new int[]{90,91,99,106})
        for(int protection:new int[]{0,110,368,760})for(boolean clarity:new boolean[]{false,true})
        {
            Player e=actor(tank,ranked,human),p=sleeper(e,cid,ranked);
            if(!clarity)clear(e,3282);
            Object s=arena(p,e);NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);
            BuffInfo b=protection==0?null:defense(e,protection);appliedSleep(p,e,cid);
            f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            verify(run(p,e,s,10000)&&last==0&&p.isMoving(),"landed sleep: retreat first, no attack before movement cid="+cid);
            int moves=ai.requests;
            verify(run(p,e,s,10100)&&last==0&&ai.requests==moves,"complete retreat owns movement, not cast/orbit spam cid="+cid+" tank="+tank+" protection="+protection+" clarity="+clarity+" last="+last+" moves="+moves+" requests="+ai.requests+" moving="+p.isMoving());
            arrive93(p,ai);
            if(cid==97)
            {
                verify(run(p,e,s,11000)&&last==0&&p.isMoving(),"Cardinal continues from center to opposite endpoint");
                arrive93(p,ai);
            }
            moves=ai.requests;
            int wake=cid==97?1398:1245;
            verify(run(p,e,s,12000),"completed retreat remains owned by tactics");
            if(b!=null)
            {
                verify(last==0&&!p.isMoving()&&ai.requests==moves,"active110/368/760: HOLD, no wake damage");
                verify(run(p,e,s,40000)&&last==0&&ai.requests==moves,"protected sleep is not broken by old5s timeout");
                b.setInUse(false);
                verify(run(p,e,s,40100)&&last==wake&&!p.isMoving()&&ai.requests==moves,"defense expiry: wake immediately, do not repeat retreat cid="+cid);
            }
            else verify(last==wake&&!p.isMoving()&&ai.requests==moves,"unprotected: immediate Mana Burn/Steal after retreat, <5s, bypass decision latency cid="+cid+" got="+last);
            int casts=traces.get(p).requests;
            verify(run(p,e,s,40200)&&traces.get(p).requests==casts&&p.isCastingNow(),"never interrupt/duplicate accepted native cast");
            verify(SleepRetreat.wakeSkillAllowed(p,e,nativeSkill(wake)),"narrow wake candidate admitted");
            verify(!SleepRetreat.wakeSkillAllowed(p,e,nativeSkill(cid==97?1394:1097)),"sleep itself is never an exception");
        }
    }
    static void nativeSafety()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{97,115})for(int gate=0;gate<6;gate++)
        {
            Player e=actor(90,ranked,true),p=sleeper(e,cid,ranked);clear(e,3282);
            Object s=arena(p,e);int wake=cid==97?1398:1245;appliedSleep(p,e,cid);
            // Native ROOT forbids the retreat but permits a stationary legal spell.
            flags(p,EffectFlag.ROOTED.getMask());
            if(gate==1)disable(p,wake);
            if(gate==2)flags(p,EffectFlag.ROOTED.getMask()|EffectFlag.MUTED.getMask());
            if(gate==3)flags(p,EffectFlag.STUNNED.getMask());
            if(gate==4)e.setInvul(true);
            if(gate==5&&cid==115)mp(p,0);
            verify(run(p,e,s,10000),"native gate stays tactically owned");
            if(gate==0||gate==5&&cid==97)verify(last==wake&&!p.isMoving(),"root-only: legal stationary wake, no movement bypass");
            else verify(last!=wake&&!p.isMoving(),"reuse/mute/hardCC/invul/MP cannot be bypassed gate="+gate);
        }
        for(boolean ranked:new boolean[]{false,true})
        {
            Player e=actor(90,ranked,true),p=sleeper(e,97,ranked);clear(e,3282);register(p,e,ranked,true);
            try
            {
                appliedSleep(p,e,97);
                verify(BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1398),false),"shared candidate permits Mana Burn under own unprotected sleep");
                verify(BotRecoveryRules.guard(castEvent(p,e,1398))==null,"synchronous native cast guard permits same exception");
                verify(BotRecoveryRules.guard(castEvent(p,e,1394))!=null,"native guard still forbids sleep refresh");
                BuffInfo d=defense(e,368);
                verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1398),false)&&BotRecoveryRules.guard(castEvent(p,e,1398))!=null,"both admission layers preserve protected sleep");
                d.setInUse(false);e.setInvul(true);
                verify(!SleepRetreat.wakeAllowed(p,e),"real invulnerability wins even without defense");e.setInvul(false);
                clearSleep(e);verify(!SleepRetreat.wakeAllowed(p,e),"accepted/resisted sleep is not a landed effect");
                appliedSleep(actor(97,ranked,true),e,97);verify(!SleepRetreat.wakeAllowed(p,e),"foreign sleep cannot invent own landing");
            }
            finally{cleanup(p,e);}
            Player mage=actor(94,ranked,true),d=dom(mage,ranked);appliedSleep(d,mage,115);
            verify(!SleepRetreat.wakeAllowed(d,mage),"Dominator ranged Silence cycle88/89 unchanged");
            Player human=actor(97,ranked,true);appliedSleep(human,e,97);
            verify(!SleepRetreat.wakeAllowed(human,e),"human actors unchanged");
        }
    }
    static boolean sleepHook(Player p,Player e,boolean ranked)
    {
        last=0;targetEnemy=e;
        return p.getPlayerClass().getId()==97
            ? BotTactics.tickCardinalControl(p,e,BotPresets.getPresetByClassId(97),ranked)
            : BotTactics.tickInvulnerableMelee(p,e,BotPresets.getPresetByClassId(115),ranked);
    }
    static void publicTransitions()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{97,115})
        for(boolean protectedAtStart:new boolean[]{false,true})
        {
            Player e=actor(90,ranked,true),p=sleeper(e,cid,ranked);clear(e,3282);
            register(p,e,ranked,true);
            try
            {
                int[] c=ArenaCombatRules.center(p);at(p,c[0]+80,c[1]);at(e,c[0],c[1]);
                NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);
                BuffInfo d=protectedAtStart?defense(e,110):null;appliedSleep(p,e,cid);
                verify(sleepHook(p,e,ranked)&&last==0&&p.isMoving(),"mandatory hook owns own sleep even without defense or optional TacticalAI");
                int moves=ai.requests;if(d!=null)d.setInUse(false);
                verify(sleepHook(p,e,ranked)&&last==0&&p.isMoving()&&ai.requests==moves,"defense expiry mid-retreat preserves same run, no duplicate retreat");
                arrive93(p,ai);
                if(cid==97)
                {
                    // Movement retry throttle uses the monotonic public clock.
                    Thread.sleep(510);
                    verify(sleepHook(p,e,ranked)&&last==0&&p.isMoving(),"mandatory Cardinal continues opposite leg");arrive93(p,ai);
                }
                moves=ai.requests;Object s=publicState(p);f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
                verify(sleepHook(p,e,ranked)&&last==(cid==97?1398:1245)&&ai.requests==moves&&!p.isMoving(),"mandatory hook immediately wakes unprotected tank after same retreat");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    public static void main(String[] args){try
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
        handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("sleep93-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        BotPresets.loadOverrides();branches();nativeSafety();publicTransitions();
        System.out.println("PASS: "+checks+" rule93 conditional sleep HOLD/immediate wake/native admission checks; no live server");System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
