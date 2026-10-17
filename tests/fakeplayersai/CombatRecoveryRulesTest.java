package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.status.CreatureStatus;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.mechanics.events.*;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.util.StatSet;
import sun.misc.Unsafe;

/** Real Skill/Player/effect/dispatcher tests. Inert fixture, no live DB/world. */
public final class CombatRecoveryRulesTest
{
    private static int checks;
    private static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void eq(double a,double b,String why){check(Math.abs(a-b)<1e-8,why);}
    private static Player actor(int cid,boolean ranked,boolean human)throws Exception
    {
        Method m=LowManaAutoAttackTest.class.getDeclaredMethod("actor",int.class,boolean.class,boolean.class);m.setAccessible(true);
        Player p=(Player)m.invoke(null,cid,ranked,human);
        float[] hp=new float[256];Arrays.fill(hp,10000);
        field(PlayerTemplate.class,"_baseHp").set(p.getTemplate(),hp);
        field(PlayerStat.class,"_oldMaxHp").setInt(p.getStat(),10000);
        field(CreatureStatus.class,"_creature").set(p.getStatus(),p);
        health(p,5000);return p;
    }
    private static void health(Player p,double hp)throws Exception{field(CreatureStatus.class,"_currentHp").setDouble(p.getStatus(),hp);}
    private static StatSet named(String name){StatSet s=new StatSet();s.set("name",name);return s;}
    private static Skill skill(int id){return SkillData.getInstance().getSkill(id,SkillData.getInstance().getMaxLevel(id));}
    private static OnCreatureSkillUse event(Player p,Skill s,Player target)
    {
        OnCreatureSkillUse e=new OnCreatureSkillUse();e.setCaster(p);e.setSkill(s);e.setTarget(target);e.setTargets(List.of(target));return e;
    }
    @SuppressWarnings("unchecked")
    private static void nativeEffectsAndEvents()throws Exception
    {
        Map<Integer,Player> roster=(Map<Integer,Player>)field(BotManager.class,"BOTS").get(null);
        Set<Integer> training=(Set<Integer>)field(TrainingArena.class,"BOTS").get(null);
        Skill heal=skill(1218),full=skill(833);
        StatSet params=new StatSet();params.set("power",100);
        var percent=new handlers.skill.effects.HealPercent(null,null,named("HealPercent"),params);
        var hot=new handlers.skill.effects.HealOverTime(null,null,named("HealOverTime"),params);
        var cp=new handlers.skill.effects.CpHeal(null,null,named("CpHeal"),params);
        var cpp=new handlers.skill.effects.CpHealPercent(null,null,named("CpHealPercent"),params);
        var cphot=new handlers.skill.effects.CpHealOverTime(null,null,named("CpHealOverTime"),params);
        var byLevel=new handlers.skill.effects.HpByLevel(null,null,named("HpByLevel"),params);
        BotRecoveryRules.init();BotRecoveryRules.init();
        for(BotClassPreset preset:BotPresets.getPresets())for(boolean ranked:new boolean[]{false,true})
        {
            Player p=actor(preset.getClassId(),ranked,false),human=actor(preset.getClassId(),ranked,true);
            if(ranked)roster.put(p.getObjectId(),p);else training.add(p.getObjectId());
            p.setInvul(true);human.setInvul(true);
            check(BotRecoveryRules.blockedSelfHeal(p,p),"every managed class/mode blocked");
            check(!BotRecoveryRules.blockedSelfHeal(human,human),"human excluded");
            check(!BotRecoveryRules.blockedSelfHeal(human,p),"external healing not redefined");
            check(BotRecoveryRules.guard(event(p,heal,p)).terminate(),"synchronous ordinary heal veto");
            check(BotRecoveryRules.guard(event(p,full,p)).terminate(),"synchronous percent heal veto");
            check(BotRecoveryRules.guard(event(human,heal,human))==null,"human cast untouched");
            var result=EventDispatcher.getInstance().notifyEvent(event(p,heal,p),Containers.Global(),TerminateReturn.class);
            check(result!=null&&result.terminate(),"actual global listener veto, idempotent init");
            percent.onStart(p,p,full);hot.onActionTime(p,p,heal);cp.onStart(p,p,heal);
            cpp.onStart(p,p,heal);cphot.onActionTime(p,p,heal);byLevel.onStart(p,p,heal);
            eq(p.getCurrentHp(),5000,"no percent/HoT/by-level HP restore under invulnerability");
            eq(p.getCurrentCp(),0,"no flat/percent/HoT CP restore under invulnerability");
            p.setInvul(false);health(p,3000);
            check(BotRecoveryRules.guard(event(p,heal,p))==null,"heal unlocked after invulnerability ends");
            if(ranked)roster.remove(p.getObjectId());else training.remove(p.getObjectId());
        }
    }
    @SuppressWarnings("unchecked")
    private static void sacrificeAndThresholds()throws Exception
    {
        Player phoenix=actor(90,false,false);Skill original=SkillData.getInstance().getSkill(69,130);
        Map<Integer,Skill> known=(Map<Integer,Skill>)field(Creature.class,"_skills").get(phoenix);known.put(69,original);
        BotRecoveryRules.applyPhoenixSacrifice(phoenix);Skill copy=phoenix.getKnownSkill(69);
        check(copy!=original&&copy.getLevel()==130,"private +30 Power template");
        check(copy.getReuseDelay()==0&&original.getReuseDelay()==2000,"bot-only no reuse, shared XML template unchanged");
        check(copy.getHpConsume()==original.getHpConsume()&&copy.getHitTime()==original.getHitTime(),"real HP cost/cast time unchanged");
        check(copy.getTargetType()==original.getTargetType()&&copy.isMagic()==original.isMagic(),"native target and magic classification");
        Method kind=BotTactics.class.getDeclaredMethod("kind",Skill.class);kind.setAccessible(true);
        check(kind.invoke(null,copy)==TacticsPolicy.Kind.HEAL,"copied Sacrifice is a real tactical self-heal");
        Method target=BotTactics.class.getDeclaredMethod("supportedTarget",Skill.class,boolean.class);target.setAccessible(true);
        check((boolean)target.invoke(null,copy,true),"native ONE target accepts the bot itself");
        eq(field(handlers.skill.effects.Heal.class,"_power").getDouble(copy.getEffects(EffectScope.GENERAL).get(0)),3002,"actual +30 Power heal");
        BotRecoveryRules.applyPhoenixSacrifice(phoenix);check(phoenix.getKnownSkill(69)==copy,"idempotent grant");
        for(double hp:new double[]{0,.1,.3999,.4,.40001,.69999,.7,.70001,1})
        {
            check(TacticsPolicy.recoveryHealAllowed(90,69,hp)==(hp<.7),"Sacrifice strict70% boundary");
            check(TacticsPolicy.recoveryHealAllowed(131,833,hp)==(hp<=.4),"Body Reconstruction inclusive40% boundary");
            health(phoenix,hp*10000);check(BotRecoveryRules.selfHealAllowed(phoenix,copy)==(hp<.7),"actual Phoenix decision boundary");
        }
        Map<Integer,Player> roster=(Map<Integer,Player>)field(BotManager.class,"BOTS").get(null);roster.put(phoenix.getObjectId(),phoenix);
        health(phoenix,6000);Player enemy=actor(94,false,true);
        check(BotRecoveryRules.guard(event(phoenix,copy,enemy)).terminate(),"Sacrifice never heals opponent");
        check(BotRecoveryRules.guard(event(phoenix,copy,phoenix))==null,"self heal below70% allowed");
        health(phoenix,7000);check(BotRecoveryRules.guard(event(phoenix,copy,phoenix)).terminate(),"legacy auto-use threshold also guarded");
        roster.remove(phoenix.getObjectId());
    }
    private static void policies()
    {
        var armor=new TacticsPolicy.Option(760,TacticsPolicy.Kind.DEFENSE,230);
        var vengeance=new TacticsPolicy.Option(368,TacticsPolicy.Kind.DEFENSE,234);
        var ud=new TacticsPolicy.Option(110,TacticsPolicy.Kind.DEFENSE,230);
        for(int i=0;i<100;i++)
        {
            var s=new TacticsPolicy.TankDefenseSequence();s.reset(1000);
            check(s.choose(List.of(vengeance,armor),5999,false,true)==-1,"no protection before5s");
            check(s.choose(List.of(vengeance,armor),6000,false,true)==760,"mage first Anti Magic Armor regardless list/score");
            s.observed(760);check(s.choose(List.of(armor,vengeance),7000,false,true)==368,"then Vengeance");
            check(s.choose(List.of(armor,vengeance),7000,true,true)==-1,"no overlapping strong defense");
            check(s.choose(List.of(vengeance),7000,false,true)==368,"unavailable armor is not a forced illegal cast");
            s.reset(1000);check(s.choose(List.of(armor,vengeance,ud),6000,false,false)==368,"melee order preserved");
        }
        for(boolean melee:new boolean[]{false,true})for(boolean controlled:new boolean[]{false,true})
            for(double distance:new double[]{0,40,99,100,144,200,449,450,900})for(int actions:new int[]{0,1,2})
                check(TacticsPolicy.soultakerKite(melee,distance,controlled,actions)==
                    (melee&&!controlled&&distance>0&&distance<450&&(actions>=1||distance<100)),"bounded Soultaker attack/kite policy");
    }
    public static void main(String[] args)throws Exception
    {
        field(LowManaAutoAttackTest.class,"unsafe").set(null,field(Unsafe.class,"theUnsafe").get(null));
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        handlers.EffectMasterHandler.main(new String[0]);
        nativeEffectsAndEvents();sacrificeAndThresholds();policies();
        System.out.println("PASS: "+checks+" real effect/event/all19-class guards, bot-only Power Sacrifice,40/70% thresholds, magic-defense order and Soultaker kite checks; no live match");
    }
}