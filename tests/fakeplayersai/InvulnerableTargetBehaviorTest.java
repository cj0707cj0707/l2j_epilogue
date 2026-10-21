package custom.fakeplayersai;

import java.util.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Structure87: actual public hooks, native skills/effects/admission; only world/DB/cast/movement recording seams. */
public class InvulnerableTargetBehaviorTest extends TankRootStationary81Test
{
    @SuppressWarnings("unchecked") static void register(Player p,Player e,boolean ranked,boolean human)throws Exception
    {
        var bots=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
        if(ranked)bots.put(p.getObjectId(),p);
        else {((Set<Integer>)f(TrainingArena.class,"BOTS").get(null)).add(p.getObjectId());BotTactics.initializeTraining(p,e);}
        if(!human)bots.put(e.getObjectId(),e);
    }
    @SuppressWarnings("unchecked") static void cleanup(Player p,Player e)throws Exception
    {
        BotTactics.forget(p.getObjectId());var bots=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
        bots.remove(p.getObjectId());bots.remove(e.getObjectId());((Set<Integer>)f(TrainingArena.class,"BOTS").get(null)).remove(p.getObjectId());
    }
    static boolean phase(Player p,Player e,boolean ranked)
    {last=0;targetEnemy=e;return BotTactics.tickInvulnerableTarget(p,e,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),ranked);}
    static boolean melee(BotClassPreset preset)
    {return !preset.isCaster()&&preset.getWeaponKind()!=BotClassPreset.WeaponKind.BOW&&preset.getWeaponKind()!=BotClassPreset.WeaponKind.CROSSBOW;}
    static void movementAndGuards()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})
        for(var preset:BotPresets.getPresets())for(var ranged:BotPresets.getPresets())
        {
            if(melee(ranged))continue;
            Player p=owner(preset.getClassId(),ranked),e=actor(ranged.getClassId(),ranked,human);foeReady(p,e);quiet(p);
            int[] c=ArenaCombatRules.center(p);at(e,c[0],c[1]);at(p,c[0]+350,c[1]);e.setInvul(true);
            MoveAI ai=new MoveAI(p);f(Creature.class,"_ai").set(p,ai);register(p,e,ranked,human);
            try
            {
                verify(phase(p,e,ranked)&&last==0&&ai.requests==1,"mandatory invulnerable-ranged phase owns all19 classes, both modes and opponent kinds");
                if(melee(preset))verify(ai.requested.getX()==e.getX()&&ai.requested.getY()==e.getY(),"melee approaches target by ordinary movement, never an attack intention");
                else verify(Math.hypot(ai.requested.getX()-c[0],ai.requested.getY()-c[1])<=450,"ranged attempts existing bounded orbit, not a fictitious600-distance policy");
                int moves=ai.requests;verify(phase(p,e,ranked)&&ai.requests==moves,"owned movement is not requeued every tick");
                if(melee(preset))
                {
                    at(p,e.getX()+10,e.getY());verify(phase(p,e,ranked)&&!p.isMoving()&&last==0,"melee reaches physical range and holds until immunity ends");
                }
                var attack=new org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack();attack.setAttacker(p);attack.setTarget(e);
                verify(BotRecoveryRules.bluffAttack(attack)!=null,"native managed-arena autoattack veto during immunity");
                verify(BotRecoveryRules.guard(castEvent(p,e,1230))!=null,"native offensive cast admission veto");
                verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1230),false),"shared legacy/tactical candidate veto");
                var self=castEvent(p,p,1218);verify((BotRecoveryRules.guard(self)==null)==(preset.getClassId()!=115),"rule88 Dominator ranged restriction, otherwise invulnerability does not block legal self support");
                p.setCastingNow(true);verify(phase(p,e,ranked)&&p.isCastingNow()&&ai.requests==moves,"real cast is not interrupted/duplicated");
                e.setInvul(false);verify(phase(p,e,ranked)&&p.isCastingNow(),"expiry during a real cast retains route handle for later cleanup");p.setCastingNow(false);
                verify(!phase(p,e,ranked)&&!p.isMoving()&&!BotTactics.isOwned(p.getObjectId()),"expiry releases old route and previous parent authority");
                verify((BotRecoveryRules.bluffAttack(attack)==null&&BotRecoveryRules.guard(castEvent(p,e,1230))==null)==(preset.getClassId()!=115),"immunity gate reopens; rule88 Dominator still only uses Steal Essence against ranged");
            }
            finally{cleanup(p,e);}
        }
    }
    static void directAndGates()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})
        {
            Player e=actor(94,ranked,true),p=owner(94,ranked);foeReady(p,e);quiet(p);Object s=arena(p,e);learn(p,nativeSkill(1230));e.setInvul(true);
            verify(run(p,e,s,10000)&&last==0&&p.isMoving(),"actual act independently blocks the previously reproduced Prominence leak and kites");
            e.setInvul(false);prompt(s);verify(run(p,e,s,20000)&&last==1230,"actual ordinary nuke resumes when immunity ends");p.setCastingNow(false);
            e=actor(94,ranked,true);p=owner(97,ranked);foeReady(p,e);quiet(p);s=arena(p,e);e.setInvul(true);proc(p);
            verify(run(p,e,s,10000)&&last==0&&p.isMoving(),"explicit ranged-vs-invulnerable-ranged rule also applies to Cardinal under own barrier");
            e=actor(94,ranked,true);p=owner(88,ranked);foeReady(p,e);quiet(p);s=arena(p,e);e.setInvul(true);flags(p,EffectFlag.ROOTED.getMask());
            verify(run(p,e,s,10000)&&last==0&&!p.isMoving(),"root forbids approach but never permits offense");
            Player human=actor(94,ranked,true);verify(BotRecoveryRules.guard(castEvent(human,e,1230))==null,"human casters remain outside bot-only native admission rules");
            verify(!BotTactics.tickInvulnerableTarget(human,e,BotPresets.getPresetByClassId(94),ranked),"public hook never commandeers a human");
        }
    }
    static void rootedDominator()throws Exception
    {
        var preset=BotPresets.getPresetByClassId(115);int[] saved=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);
        try
        {
            preset.setRotationOverride(new int[]{1245,1306});
            for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{90,91,99,106})for(int id:new int[]{110,368,760})
            {
                Player e=actor(cid,ranked,human),p=owner(115,ranked);foeReady(p,e);quiet(p);arena(p,e);defense(e,id);
                for(int learned:new int[]{1097,1245,1306})learn(p,nativeSkill(learned));cp(p,10000);root(e,p);appliedSleep(p,e,115);register(p,e,ranked,human);
                try
                {
                    verify(publicTank(p,e,ranked)&&last==0&&!p.isMoving(),"asleep protected tank remains HOLD under opponent root");Object s=publicState(p);clearSleep(e);
                    f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
                    verify(publicTank(p,e,ranked)&&last==1097&&!p.isMoving()&&p.getTarget()==e,"wake: ready learned sleep bypasses old override and generic decision latency while rooted");
                    int requests=traces.get(p).requests;verify(publicTank(p,e,ranked)&&traces.get(p).requests==requests,"no duplicate during real sleep cast");
                }
                finally{p.setCastingNow(false);cleanup(p,e);}
            }
            // Native cooldown/mute/MP still veto the priority; no free spell/forced waiting.
            for(int gate:new int[]{0,1,2})
            {
                Player e=actor(90,false,true),p=owner(115,false);foeReady(p,e);quiet(p);Object s=arena(p,e);defense(e,110);learn(p,nativeSkill(1097));learn(p,nativeSkill(1245));root(e,p);
                if(gate==0)disable(p,1097);else if(gate==1)flags(p,EffectFlag.ROOTED.getMask()|EffectFlag.MUTED.getMask());else mp(p,0);
                verify(run(p,e,s,10000)&&last!=1097,"rooted sleep retains native reuse/silence/resource gates");p.setCastingNow(false);
            }
        }
        finally{preset.setRotationOverride(saved);}
    }
    static void rootBeforeBluff()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{93,101,108})
        {
            Player e=actor(90,ranked,human),p=owner(cid,ranked);foeReady(p,e);quiet(p);Object s=arena(p,e);learn(p,nativeSkill(453));root(e,p);ClarityBluffPhaseTest.bluff(p,e);register(p,e,ranked,human);
            try
            {
                f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
                verify(run(p,e,s,10000)&&last==453&&p.getTarget()==p&&!p.isMoving(),"native root escape wins over own Bluff and future reaction timer for all3 daggers");
                verify(BotRecoveryRules.guard(castEvent(p,p,453))==null,"native self cast is admitted during own Bluff");
                int requests=traces.get(p).requests;verify(run(p,e,s,10100)&&traces.get(p).requests==requests,"no duplicate root cleanse while casting");
                finish(p,e,s,15000);disable(p,453);prompt(s);
                verify(run(p,e,s,20000)&&last!=453&&!p.isMoving(),"unavailable cleanse does not force reuse/movement; Bluff restrictions still hold");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void summonImmunity()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})
        {
            Player p=owner(95,ranked),e=actor(94,ranked,true);foeReady(p,e);quiet(p);
            f(org.l2jmobius.gameserver.entity.WorldObject.class,"_isSpawned").setBoolean(e,true);
            PetAI[] holder=new PetAI[1];var summon=pet(p,holder);
            PetAI ai=new PetAI(summon){@Override public void setIntentionIdle(){_intention=org.l2jmobius.gameserver.ai.Intention.IDLE;}};
            f(Creature.class,"_ai").set(summon,ai);SummonAttackPulse pulse=new SummonAttackPulse();
            pulse.tick(p,e,1000);verify(ai.requests==1,"existing summon starts native attack while target is vulnerable");
            e.setInvul(true);pulse.tick(p,e,1001);
            verify(ai.requests==1&&ai.getIntention()==org.l2jmobius.gameserver.ai.Intention.IDLE,"immunity stops existing summon attack before its3s cadence");
            pulse.tick(p,e,4000);verify(ai.requests==1,"summon does not receive attack commands throughout immunity");
            e.setInvul(false);pulse.tick(p,e,4001);verify(ai.requests==2&&ai.requested==e,"summon resumes legal attack after immunity");
        }
    }
    public static void main(String[]args){try
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
        handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("rules87-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        BotPresets.loadOverrides();movementAndGuards();directAndGates();rootedDominator();rootBeforeBluff();summonImmunity();
        System.out.println("PASS: "+checks+" structure87 ranged-immunity movement/native guards, rooted Dominator wake sleep and root-before-Bluff checks; no live server");System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
