package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.mechanics.effects.*;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.util.StatSet;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Actual skills, persisted overrides, production predicates and native effect lists.
 * Inert actors/status seam only; never starts a GameServer, DB, cast or movement task. */
public class CpMeleeRushRecoveryTest
{
    static int count;
    static void verify(boolean ok,String msg){count++;if(!ok)throw new AssertionError(msg);}
    static Unsafe unsafe;
    static void set(Object obj,Class<?> owner,String name,Object value)throws Exception{f(owner,name).set(obj,value);}
    static BuffInfo info(Player caster,Player target,Skill skill,AbstractEffect effect)throws Exception
    {
        BuffInfo b=(BuffInfo)unsafe.allocateInstance(BuffInfo.class);
        set(b,BuffInfo.class,"_effector",caster);set(b,BuffInfo.class,"_effected",target);set(b,BuffInfo.class,"_skill",skill);
        set(b,BuffInfo.class,"_effects",new ArrayList<>(List.of(effect)));b.setInUse(true);b.setFinishType(SkillFinishType.NORMAL);return b;
    }
    static void cpAndMelee()throws Exception
    {
        BotPresets.loadOverrides();BotClassPreset dom=BotPresets.getPresetByClassId(115);
        verify(dom.hasRotationOverride(),"repository persisted override is loaded, not a default-only test");
        verify(Arrays.stream(dom.getRotation()).noneMatch(i->i==1305||i==1306),"reproduce CP skills absent from override");
        for(boolean ranked:new boolean[]{false,true})
        {
            Player d=actor(115,ranked,false),enemy=actor(94,ranked,true);
            set(d,Player.class,"_charges",new java.util.concurrent.atomic.AtomicInteger());
            verify(BotSpawner.getTacticalCpSupport(d,enemy).length==0,"no unlearned CP skills injected");
            for(int id:new int[]{1305,1306})learn(d,SkillData.getInstance().getSkill(id,SkillData.getInstance().getMaxLevel(id)));
            verify(Arrays.equals(BotSpawner.getTacticalCpSupport(d,enemy),new int[]{1305,1306}),"both known CP skills survive real persisted override in both modes");
            for(int id:new int[]{1305,1306})
            {
                Skill skill=d.getKnownSkill(id);
                verify(skill.hasEffectType(EffectType.CPHEAL),"native CP heal effect");
                verify(BotRecoveryRules.selfHealAllowed(d,skill),"ordinary known CP heal eligible");
                d.setInvul(true);verify(!BotRecoveryRules.selfHealAllowed(d,skill),"CP selfheal forbidden under invulnerability");d.setInvul(false);
                Method resource=BotTactics.class.getDeclaredMethod("resources",Player.class,Skill.class);resource.setAccessible(true);
                mp(d,0);verify(!(boolean)resource.invoke(null,d,skill),"CP support does not grant free MP");mp(d,10000);
                verify((boolean)resource.invoke(null,d,skill),"native CP cost payable");
            }
            Player wrong=actor(88,ranked,false);learn(wrong,d.getKnownSkill(1305));verify(BotSpawner.getTacticalCpSupport(wrong,enemy).length==0,"other classes not granted support");
            for(int cid:new int[]{88,114})for(int mage:new int[]{94,95,97,103,110,115})
            {
                Player b=actor(cid,ranked,false),m=actor(mage,ranked,true);BotClassPreset preset=BotPresets.getPresetByClassId(cid);
                verify(BotTactics.strictMeleePressure(b,m,preset),"charge fighter recognizes mage "+mage);
                for(int mana:new int[]{0,10000})
                {
                    mp(b,mana);verify(BotTactics.tacticalApproachRange(b,m,preset)<=40,"real pursuit stays melee, regardless MP/readiness");
                    for(int id:preset.getRotation()){Skill skill=SkillData.getInstance().getSkill(id,SkillData.getInstance().getMaxLevel(id));if(skill!=null)learn(b,skill);}
                    verify(BotTactics.tacticalApproachRange(b,m,preset)<=40,"ready ranged charges do not enlarge melee envelope");
                }
                verify(!TacticsPolicy.chargeMeleeRecoveryAllowed(true,false),"armed fighter does not backpedal just for cooldown");
                verify(TacticsPolicy.chargeMeleeRecoveryAllowed(true,true),"real disarm exception retained");
                verify(BotTactics.strictMeleePressure(b,actor(131,ranked,true),preset),"rule88 melee pursuit also applies against physical opponents");
            }
            for(int cid:new int[]{90,92,95,113,131})verify(!BotTactics.strictMeleePressure(actor(cid,ranked,false),enemy,BotPresets.getPresetByClassId(cid)),"other class policy unchanged");
        }
    }
    static void liveStun()throws Exception
    {
        Player bot=actor(131,true,false),enemy=actor(94,true,true),other=actor(90,true,false);
        StatSet props=new StatSet();props.set("name","Stun");AbstractEffect stun=new handlers.skill.effects.Stun(null,null,props,new StatSet());
        Skill rush=SkillData.getInstance().getSkill(793,1);EffectList list=enemy.getEffectList();
        @SuppressWarnings("unchecked") Map<AbnormalType,BuffInfo> slots=(Map<AbnormalType,BuffInfo>)f(EffectList.class,"_stackedEffects").get(list);
        for(int id:new int[]{793,92})
        {
            Skill skill=SkillData.getInstance().getSkill(id,1);BuffInfo b=info(other,enemy,skill,stun);
            list.getDebuffs().clear();list.getDebuffs().add(b);slots.clear();slots.put(skill.getAbnormalType(),b);f(EffectList.class,"_effectFlags").setInt(list,EffectFlag.STUNNED.getMask());
            verify(RushStunSupport.stun(enemy)==b,"any genuinely applied STUN owns Rush-only phase, not only our own793");
            b.setInUse(false);verify(RushStunSupport.stun(enemy)==null,"inactive stun rejected");b.setInUse(true);
            b.setFinishType(SkillFinishType.REMOVED);verify(RushStunSupport.stun(enemy)==null,"removed stun rejected");b.setFinishType(SkillFinishType.NORMAL);
            slots.clear();verify(RushStunSupport.stun(enemy)==null,"not current abnormal slot rejected");slots.put(skill.getAbnormalType(),b);
            f(EffectList.class,"_effectFlags").setInt(list,EffectFlag.SLEEP.getMask());verify(RushStunSupport.stun(enemy)==null,"sleep is not STUN");
            f(EffectList.class,"_effectFlags").setInt(list,EffectFlag.STUNNED.getMask());
            BuffInfo next=info(bot,enemy,rush,stun);list.getDebuffs().clear();list.getDebuffs().add(next);slots.clear();slots.put(rush.getAbnormalType(),next);
            verify(RushStunSupport.stun(enemy)==next,"native replacement effect is followed, no guessed duration");
        }
        var loop=new TacticsPolicy.OriginalRushStun<Object>();Object a=new Object(),b=new Object();
        verify(loop.follow(a,true)&&loop.follow(b,true),"training follows current live effect replacement");verify(!loop.follow(null,false),"training releases on actual stun expiry");
        BerserkerCycle cycle=new BerserkerCycle();verify(cycle.canRetreat(0,0),"native CHARGE zero-distance landing can retreat");
        cycle.beginRetreat(0,0);verify(!cycle.retreatUnproductive(1,0),"zero distance no longer instantly cancels retreat");
        verify(cycle.retreatUnproductive(1500,0),"blocked retreat still bounded");
        for(int angle=0;angle<360;angle+=3)for(int radius:new int[]{0,100,400,450,459})
        {
            int x=(int)Math.round(radius*Math.cos(Math.toRadians(angle))),y=(int)Math.round(radius*Math.sin(Math.toRadians(angle)));
            List<Location> points=RushStunSupport.retreat(x,y,7,x,y,0,0,450);
            verify(!points.isEmpty(),"zero distance retreat available at arena edge "+angle+"/"+radius);
            for(Location p:points){verify(Math.hypot(p.getX(),p.getY())<=450,"entire destination inside460 guard");verify(Math.hypot(p.getX()-x,p.getY()-y)>=220,"native Rush minimum reached without clipped shortfall");verify(p.getZ()==7,"retreat preserves Z");}
        }
    }
    public static void main(String[] args)throws Exception
    {
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;handlers.EffectMasterHandler.main(new String[0]);
        cpAndMelee();liveStun();System.out.println("PASS: "+count+" actual-JAR persisted-override CP, melee approach, live STUN and boundary retreat checks; no live match");System.exit(0);
    }
}
