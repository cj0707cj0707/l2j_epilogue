package custom.fakeplayersai;

import java.util.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Real act/poll/public hooks/skills; cast, movement, networking and DB recording seams only. */
public class RoleCombat88Test extends InvulnerableTargetBehaviorTest
{
    static Player dom(Player e,boolean ranked)throws Exception
    {
        Player p=owner(115,ranked);foeReady(p,e);quiet(p);arena(p,e);cp(p,10000);
        for(int id:new int[]{1245,1097,1246,1367,1092,1208,260,1306})learn(p,nativeSkill(id));
        return p;
    }
    static void appliedSeal(Player p,Player e)throws Exception
    {
        var props=new org.l2jmobius.gameserver.util.StatSet();props.set("name","Mute");
        BuffInfo mute=CpMeleeRushRecoveryTest.info(p,e,nativeSkill(1246),new handlers.skill.effects.Mute(null,null,props,new org.l2jmobius.gameserver.util.StatSet()));
        e.getEffectList().getDebuffs().add(mute);
        @SuppressWarnings("unchecked") var slots=(Map<AbnormalType,BuffInfo>)f(org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList.class,"_stackedEffects").get(e.getEffectList());
        slots.put(nativeSkill(1246).getAbnormalType(),mute);
        flags(e,EffectFlag.SLEEP.getMask()|EffectFlag.MUTED.getMask());
    }
    static void bows()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{92,102,109,134})
        {
            Player e=actor(cid,ranked,human),p=dom(e,ranked);Object s=arena(p,e);cp(p,1);
            for(int i=0;i<12;i++)
            {
                prompt(s);verify(run(p,e,s,10000+i*10000)&&last==1245,"bow/crossbow: only Steal Essence, never sleep/curse/CP even after many drains");finish(p,e,s,15000+i*10000);
            }
            verify(BotTactics.tacticalApproachRange(p,e,BotPresets.getPresetByClassId(115))==900,"bow approach uses actual900u Steal, not600u Sleep");
            for(int id:new int[]{1097,1246,1367,1092,1208,260,1306})verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(id),id==1306),"shared bow candidate rejects unrelated skill");
            e.setInvul(true);prompt(s);verify(run(p,e,s,200000)&&last==0,"invulnerability rule87 still suppresses all bow offense");
        }
    }
    static void mageCycle()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{94,95,97,103,110,115})
        {
            Player e=actor(cid,ranked,human),p=dom(e,ranked);Object s=arena(p,e);cp(p,1000);
            for(int i=0;i<4;i++)
            {prompt(s);verify(run(p,e,s,10000+i*10000)&&last==1245,"mage: four actual drains before occasional Sleep, no awake CP/curse");finish(p,e,s,15000+i*10000);}
            prompt(s);verify(run(p,e,s,60000)&&last==1097,"four completed drains admit learned ready Sleep regardless old rotation");
            int requests=traces.get(p).requests;run(p,e,s,60100);verify(traces.get(p).requests==requests,"never duplicate real Sleep cast");finish(p,e,s,65000);
            prompt(s);verify(run(p,e,s,70000)&&last==1245,"resisted/nonapplied Sleep returns to Steal; no imaginary CP/curse phase");finish(p,e,s,75000);
            appliedSleep(p,e,115);prompt(s);verify(run(p,e,s,80000)&&last==1306&&p.getTarget()==p,"actual own sleep: legal CP recovery first, correct self target");finish(p,e,s,85000);
            // Recording casts do not apply CP effects; model the real native effect result explicitly.
            cp(p,10000);prompt(s);verify(run(p,e,s,90000)&&last==1246,"full CP: Seal of Silence, not Steal/fear/other debuffs or a forced wake after5s");finish(p,e,s,95000);
            prompt(s);verify(run(p,e,s,100000)&&last==1246,"resisted Silence retries while actual Sleep remains; no two-attempt budget");finish(p,e,s,105000);
            appliedSeal(p,e);
            for(int id:new int[]{1097,1246,1306,1367})verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(id),id==1306),"active own Seal admits only Steal, never support/control id="+id);
            verify(BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1245),false),"active own Seal admits Steal even before its first hit wakes Sleep");
            prompt(s);verify(run(p,e,s,110000)&&last==1245,"landed own Silence immediately switches to Steal-only kill pressure while Seal remains");finish(p,e,s,115000);
            clearSleep(e);prompt(s);verify(run(p,e,s,120000)&&last==1245,"Seal expiry/wake returns to the ordinary Steal burst");finish(p,e,s,125000);
            disable(p,1245);prompt(s);verify(run(p,e,s,130000)&&last==0,"unavailable Steal does not fall back to unrelated curses or early Sleep");
            flags(p,EffectFlag.MUTED.getMask());prompt(s);verify(run(p,e,s,140000)&&last==0,"native magic silence not bypassed");flags(p,0);
        }
    }
    static void chargeFighters()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{88,114})for(int foe:new int[]{88,92,94,131})
        {
            Player e=actor(foe,ranked,human),p=owner(cid,ranked);foeReady(p,e);quiet(p);Object s=arena(p,e);at(e,p.getX()+400,p.getY());
            int rage=cid==88?345:346,main=cid==88?7:54;
            learn(p,nativeSkill(rage));learn(p,nativeSkill(main));
            var inv=new NativeDebuffEpicTest.TestInventory(p);
            f(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.class,"_paperdollListeners").set(inv,new ArrayList<>());
            f(Player.class,"_inventory").set(p,inv);
            var stones=new NativeDebuffEpicTest.TestItem(5589);stones.setCount(100);inv.own(stones);
            var weapon=new NativeDebuffEpicTest.TestItem(BotPresets.getWeaponId(BotPresets.getPresetByClassId(cid),true));
            inv.own(weapon);inv.setPaperdollItem(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.PAPERDOLL_RHAND,weapon);
            f(Player.class,"_charges").set(p,new java.util.concurrent.atomic.AtomicInteger(7));
            verify(BotTactics.strictMeleePressure(p,e,BotPresets.getPresetByClassId(cid)),"all opponents: charge fighters are melee, not just against mages");
            verify(BotTactics.tacticalApproachRange(p,e,BotPresets.getPresetByClassId(cid))<=60,"Rage600 never turns pursuit into ranged hold");
            verify(!run(p,e,s,10000)&&last==0,"stationary distant enemy: no ranged fight, yield melee pursuit");
            f(Player.class,"_charges").set(p,new java.util.concurrent.atomic.AtomicInteger(4));
            MoveAI move=new MoveAI(e);f(Creature.class,"_ai").set(e,move);move.setIntentionMoveTo(new org.l2jmobius.gameserver.entity.Location(e.getX()+300,e.getY(),e.getZ()));
            verify(RoleCombatPolicy.kited(p,e),"observed outward movement is actual kite");
            prompt(s);boolean fired=run(p,e,s,20000);verify(fired&&last!=0&&last!=rage,"kited: native-legal real ranged attack before weak Rage when available cid="+cid+" foe="+foe+" ranked="+ranked+" last="+last+" result="+fired+" native="+p.getKnownSkill(main).checkCondition(p,e,false)+" target="+p.getKnownSkill(main).getTargetType()+" charges="+p.getCharges());finish(p,e,s,25000);
            prompt(s);verify(!run(p,e,s,25100)&&last==0,"after ranged cast: real pursuit window, no continuous stationary ranged spam");
            // Rage is only a last-resort charge builder, never usable at full charges.
            disable(p,main);at(e,p.getX()+10,p.getY());f(Creature.class,"_move").set(e,null);prompt(s);
            verify(run(p,e,s,30000)&&last==rage,"close, charges missing and main unavailable: Rage rebuilds charges last");finish(p,e,s,35000);
            f(Player.class,"_charges").set(p,new java.util.concurrent.atomic.AtomicInteger(7));prompt(s);run(p,e,s,40000);verify(last!=rage,"full charges: weak Rage is not a damage filler");
            move.setIntentionMoveTo(new org.l2jmobius.gameserver.entity.Location(p.getX()-300,p.getY(),p.getZ()));verify(!RoleCombatPolicy.kited(p,e),"approaching movement is not kite");
        }
    }
    static void mirrors()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int self:new int[]{90,91,106})for(int cid:new int[]{90,91,99,106})for(int defense:new int[]{110,368,760})
        {
            Player e=actor(cid,ranked,human),p=owner(self,ranked);foeReady(p,e);quiet(p);arena(p,e);learn(p,nativeSkill(defense));at(e,p.getX()+500,p.getY());
            verify(!BotSpawner.tankMirrorDefenseAllowed(p,e,nativeSkill(defense)),"tank mirror: no strong defense before physical contact");
            verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(defense),true),"same veto in shared tactical/override selection");
            at(e,p.getX()+10,p.getY());verify(BotSpawner.tankMirrorDefenseAllowed(p,e,nativeSkill(defense)),"actual contact enables defense eligibility, not a guaranteed cast or reuse bypass");
            at(e,p.getX()+500,p.getY());Player mage=actor(94,ranked,human);verify(BotSpawner.tankMirrorDefenseAllowed(p,mage,nativeSkill(defense)),"nonmirror defense policy not changed by contact gate");
        }
    }
    static boolean role(Player p,Player e,boolean ranked)
    {last=0;targetEnemy=e;return BotTactics.tickRoleCombat(p,e,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),ranked);}
    static void publicHooks()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})
        {
            Player e=actor(94,ranked,human),p=dom(e,ranked);register(p,e,ranked,human);
            try
            {
                verify(role(p,e,ranked),"public role hook owns the valid pair");
                Object s=publicState(p);if(last!=1245){prompt(s);verify(role(p,e,ranked)&&last==1245,"public mandatory hook casts Steal after native reaction without optional TacticalAI");}
                int requests=traces.get(p).requests;
                verify(role(p,e,ranked)&&traces.get(p).requests==requests,"public hook retains actual cast, no duplicate");finish(p,e,s,clock()+5000);
                verify(BotRecoveryRules.guard(castEvent(p,e,1367))!=null,"native admission also rejects legacy curse");
                var basic=new org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack();basic.setAttacker(p);basic.setTarget(e);
                verify(BotRecoveryRules.bluffAttack(basic)!=null,"native basic attack cannot bypass only-Steal role");
                appliedSleep(p,e,115);cp(p,10000);int[] center=ArenaCombatRules.center(p);at(p,center[0],center[1]);at(e,center[0]+400,center[1]);prompt(s);
                verify(role(p,e,ranked)&&last==0&&p.isMoving(),"real sleep: approach native Silence200, no out-of-range cast or retreat");
                at(p,e.getX()-100,e.getY());f(Creature.class,"_move").set(p,null);prompt(s);
                verify(role(p,e,ranked)&&last==1246,"public hook casts Silence only after ordinary approach");
                verify(BotRecoveryRules.guard(castEvent(p,p,1246))==null,"AURA primary self still allows legal Silence against own sleeping mage");
                finish(p,e,s,clock()+5000);
                appliedSeal(p,e);prompt(s);
                verify(role(p,e,ranked)&&last==1245,"confirmed own Seal immediately wakes with Steal instead of recast/support");
                finish(p,e,s,clock()+5000);clearSleep(e);at(p,center[0]-350,center[1]);at(e,center[0]+350,center[1]);prompt(s);
                var cycle=(RoleCombatPolicy.DominatorCycle)f(state,"rangedCycle").get(s);cycle.observe(false);for(int i=0;i<4;i++)cycle.completed(1245,true);f(state,"nextStep").setLong(s,0);
                verify(role(p,e,ranked)&&last==0&&p.isMoving(),"due ready Sleep approaches600 instead of holding at Steal900");
            }finally{p.setCastingNow(false);cleanup(p,e);}
            for(int cid:new int[]{88,114})
            {
                e=actor(92,ranked,human);p=owner(cid,ranked);foeReady(p,e);quiet(p);arena(p,e);int[] cc=ArenaCombatRules.center(p);at(p,cc[0],cc[1]);at(e,cc[0]+400,cc[1]);register(p,e,ranked,human);
                f(Player.class,"_charges").set(p,new java.util.concurrent.atomic.AtomicInteger(7));learn(p,nativeSkill(cid==88?345:346));
                try
                {
                    verify(role(p,e,ranked)&&last==0&&p.isMoving(),"mandatory charge melee driver actually chases stationary ranged foe");
                    verify(BotRecoveryRules.guard(castEvent(p,e,cid==88?345:346))!=null,"native guard rejects Rage at full charges");
                    flags(p,EffectFlag.ROOTED.getMask());f(Creature.class,"_move").set(p,null);prompt(publicState(p));
                    verify(role(p,e,ranked)&&last==0&&!p.isMoving(),"root does not permit movement or remote charge spam");
                }finally{cleanup(p,e);}
            }
            e=actor(90,ranked,human);p=owner(91,ranked);foeReady(p,e);quiet(p);arena(p,e);at(e,p.getX()+500,p.getY());register(p,e,ranked,human);
            try
            {
                // Establish actual pair before a self-targeted native event.
                BotTactics.tickInvulnerableMelee(p,e,BotPresets.getPresetByClassId(91),ranked);p.setTarget(p);
                for(int id:new int[]{110,368,760})verify(BotRecoveryRules.guard(castEvent(p,p,id))!=null,"native self-target cannot conceal distant tank opponent");
                at(e,p.getX()+10,p.getY());for(int id:new int[]{110,368,760})verify(BotRecoveryRules.guard(castEvent(p,p,id))==null,"native mirror defense admission opens only at contact");
            }finally{cleanup(p,e);}
            Player humanActor=actor(115,ranked,true);verify(!BotTactics.tickRoleCombat(humanActor,e,BotPresets.getPresetByClassId(115),ranked),"new role hook never owns a human");
        }
    }
    public static void main(String[]args){try
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
        handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("rules88-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        BotPresets.loadOverrides();bows();mageCycle();chargeFighters();mirrors();publicHooks();
        System.out.println("PASS: "+checks+" structure89 Silence-to-Steal burst, ranged Dominator cycle, charge melee priorities and tank-mirror contact checks; no live server");System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
