package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.status.PlayerStatus;
import org.l2jmobius.gameserver.entity.actor.status.CreatureStatus;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Calculator;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncMul;
import org.l2jmobius.gameserver.network.GameClient;
import sun.misc.Unsafe;

/** Real core stats, real authority method, state teardown and shared policy; not a live match. */
public final class CardinalArenaParityTest
{
    private static Unsafe unsafe;
    private static int checks;
    private static final Class<?> STATE;
    static
    {
        try { STATE=Class.forName("custom.fakeplayersai.BotTactics$State"); }
        catch (ClassNotFoundException e) { throw new ExceptionInInitializerError(e); }
    }
    private static void check(boolean ok, String message) { checks++; if(!ok) throw new AssertionError(message); }
    private static void equal(double a,double b,String message) { check(Math.abs(a-b)<1e-8,message+": "+a+" != "+b); }
    private static Field field(Class<?> c,String name) throws Exception
    { Field f=c.getDeclaredField(name);f.setAccessible(true);return f; }
    private static Method method(String name,Class<?>...params) throws Exception
    { Method m=BotTactics.class.getDeclaredMethod(name,params);m.setAccessible(true);return m; }
    private static Player actor(int id,int classId,boolean ranked,boolean human) throws Exception
    {
        Player p=(Player)unsafe.allocateInstance(Player.class);
        PlayerTemplate t=(PlayerTemplate)unsafe.allocateInstance(PlayerTemplate.class);
        field(PlayerTemplate.class,"_playerClass").set(t,PlayerClass.getPlayerClass(classId));
        field(Creature.class,"_template").set(p,t);
        field(Creature.class,"_calculators").set(p,new Calculator[Stat.NUM_STATS]);
        field(Creature.class,"_stat").set(p,new PlayerStat(p));
        PlayerStatus status=(PlayerStatus)unsafe.allocateInstance(PlayerStatus.class);
        field(CreatureStatus.class,"_currentMp").setDouble(status,321);
        field(Creature.class,"_status").set(p,status);
        field(WorldObject.class,"_objectId").setInt(p,id);
        field(WorldObject.class,"_location").set(p,new Location(100,200,0,0,10));
        field(Player.class,"_isOnline").setBoolean(p,true);
        field(Player.class,"_inOlympiadMode").setBoolean(p,ranked);
        field(Player.class,"_olympiadStart").setBoolean(p,ranked);
        field(Player.class,"_olympiadGameId").setInt(p,ranked?5:-1);
        if(human) field(Player.class,"_client").set(p,unsafe.allocateInstance(GameClient.class));
        return p;
    }
    private static double regen(Player p) { return p.calcStat(Stat.REGENERATE_MP_RATE,20,null,null); }
    private static Object state(int id,long time) throws Exception { return method("state",int.class,long.class).invoke(null,id,time); }
    private static CardinalArenaSupport effect(Object state) throws Exception
    { return (CardinalArenaSupport)field(STATE,"cardinalSupport").get(state); }
    @SuppressWarnings("unchecked")
    private static Map<Integer,Object> states() throws Exception
    { return (Map<Integer,Object>)field(BotTactics.class,"STATES").get(null); }
    private static void clearStates() throws Exception
    { for(int id:new ArrayList<>(states().keySet())) BotTactics.forget(id); }
    private static void statsAndAuthority() throws Exception
    {
        @SuppressWarnings("unchecked") Map<Integer,Player> roster=(Map<Integer,Player>)field(BotManager.class,"BOTS").get(null);
        for(boolean ranked:new boolean[]{false,true})
        {
            Player bot=actor(100,97,ranked,false), enemy=actor(200,94,ranked,true), next=actor(300,94,ranked,true);
            Object st=state(bot.getObjectId(),1000);
            field(STATE,"trainingEnemy").setInt(st,enemy.getObjectId());
            field(STATE,"trainingInstance").setInt(st,10);
            if(ranked) roster.put(bot.getObjectId(),bot);
            Method valid=method("valid",Player.class,Player.class,boolean.class,STATE);
            check((boolean)valid.invoke(null,bot,enemy,ranked,st),"real authority accepts "+(ranked?"registered same-game pair":"authorized training pair"));
            Object existingBuff=new Object();
            enemy.addStatFunc(new FuncMul(Stat.REGENERATE_MP_RATE,0x30,existingBuff,1.5,null));
            equal(regen(enemy),30,"existing regen buff before suppression");
            equal(enemy.getCurrentMp(),321,"starting MP");
            CardinalArenaSupport support=effect(st);
            for(int i=0;i<300;i++)
            {
                support.bind(bot,enemy);
                equal(regen(enemy),3,"exact10% of real calculator including prior buffs, mode="+ranked);
                equal(enemy.getCurrentMp(),321,"no direct MP change");
                equal(enemy.calcStat(Stat.REGENERATE_HP_RATE,20,null,null),20,"HP regeneration unchanged");
                equal(regen(bot),20,"Cardinal's own regen unchanged");
            }
            support.bind(bot,next);
            equal(regen(enemy),30,"old target fully restored on pair change");
            equal(regen(next),2,"new opponent suppressed once");
            support.clear(); support.clear();
            equal(regen(next),20,"double release idempotent");
            support.bind(bot,enemy);
            BotTactics.forget(bot.getObjectId());
            equal(regen(enemy),30,"actual forget releases modifier but keeps pre-existing buff");
            st=state(bot.getObjectId(),2000);support=effect(st);support.bind(bot,enemy);
            // Real tick invalidation must release the same state, not just stop movement.
            check(!BotTactics.tick(bot,enemy,null,ranked),"null preset refuses control");
            equal(regen(enemy),30,"invalid tick teardown restores regen");
            st=state(bot.getObjectId(),3000);
            field(STATE,"trainingEnemy").setInt(st,enemy.getObjectId());field(STATE,"trainingInstance").setInt(st,10);
            field(Player.class,"_isOnline").setBoolean(enemy,false);
            check(!(boolean)valid.invoke(null,bot,enemy,ranked,st),"offline enemy rejected");
            field(Player.class,"_isOnline").setBoolean(enemy,true);
            if(ranked)
            {
                field(Player.class,"_olympiadStart").setBoolean(enemy,false);
                check(!(boolean)valid.invoke(null,bot,enemy,true,st),"countdown not combat");
                field(Player.class,"_olympiadStart").setBoolean(enemy,true);
                field(Player.class,"_olympiadGameId").setInt(enemy,6);
                check(!(boolean)valid.invoke(null,bot,enemy,true,st),"another match rejected");
            }
            else
            {
                field(STATE,"trainingEnemy").setInt(st,999);
                check(!(boolean)valid.invoke(null,bot,enemy,false,st),"another training opponent rejected");
            }
            roster.remove(bot.getObjectId());clearStates();
            support=new CardinalArenaSupport();
            support.bind(actor(400,97,ranked,true),enemy);equal(regen(enemy),30,"client-backed Cardinal never applies bot modifier");
            support.bind(actor(401,94,ranked,false),enemy);equal(regen(enemy),30,"another class never applies modifier");
            support.bind(bot,bot);equal(regen(bot),20,"no self suppression");
            support.bind(bot,enemy);support.bind(null,enemy);equal(regen(enemy),30,"invalid rebind cleans previous effect");
        }
    }
    private static void stateEviction() throws Exception
    {
        clearStates(); field(BotTactics.class,"nextSweep").setLong(null,0);
        Player bot=actor(100,97,false,false),enemy=actor(200,94,false,true);
        effect(state(100,1000)).bind(bot,enemy);
        state(101,30*60_000L+1001);
        equal(regen(enemy),20,"TTL sweep releases effect before removing state");
        clearStates();field(BotTactics.class,"nextSweep").setLong(null,0);
        effect(state(100,1000)).bind(bot,enemy);
        for(int id=101;id<=2148;id++)state(id,1000);
        equal(regen(enemy),20,"bounded eviction releases effect");
        check(states().size()==2048,"state count remains bounded");
        clearStates();
    }
    private static void policyAndActualSkills()
    {
        SkillData data=SkillData.getInstance();Skill trance=data.getSkill(1394,data.getMaxLevel(1394));
        Skill nuke=data.getSkill(1230,data.getMaxLevel(1230));Skill drain=data.getSkill(1398,data.getMaxLevel(1398));
        check(trance!=null && trance.hasEffectType(EffectType.SLEEP) && !trance.isBlockedInOlympiad(),"real Trance is an allowed Sleep skill");
        check(trance.getCastRange()==600&&nuke.getCastRange()==900&&drain.getCastRange()==900,"actual600-vs900 starvation fixture");
        equal(TacticsPolicy.cardinalApproachRange(97,900,trance.getCastRange()),600,"close enough for ready Trance instead of holding900");
        equal(TacticsPolicy.cardinalApproachRange(97,900,-1),900,"no forced short range while Trance illegal/on reuse");
        for(int id:new int[]{94,95,115,131})equal(TacticsPolicy.cardinalApproachRange(id,900,600),900,"other classes unchanged");
        var training=new TacticsPolicy.CardinalProminence();var ranked=new TacticsPolicy.CardinalProminence();
        var legal=List.of(new TacticsPolicy.Option(1394,TacticsPolicy.Kind.CONTROL,54),new TacticsPolicy.Option(1230,TacticsPolicy.Kind.DAMAGE,62));
        for(long t=1000;t<61000;t+=100)
        {
            int a=training.choose(97,legal,t),b=ranked.choose(97,legal,t);
            check(a==b,"identical shared nuke policy at same inputs/difficulty");
            if(a==1230){training.accepted(t);ranked.accepted(t);}
            check(TacticsPolicy.cardinalSleepException(97,1398)&&TacticsPolicy.cardinalSleepException(97,1230),"both modes keep narrow sleep exceptions");
        }
        for(boolean self:new boolean[]{false,true})for(boolean opponent:new boolean[]{false,true})
            check(TacticsPolicy.arenaMpFloorAllowed(self,opponent)==(!self&&!opponent),"no instant refill for either side of Cardinal-bot pair");
    }
    public static void main(String[] args) throws Exception
    {
        unsafe=(Unsafe)field(Unsafe.class,"theUnsafe").get(null);
        statsAndAuthority();stateEviction();
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        handlers.EffectMasterHandler.main(new String[0]);policyAndActualSkills();
        System.out.println("PASS: "+checks+" real-core regen, source/class guard, ranked/training authority, pair/invalid/end/TTL/eviction teardown,600-vs900 approach and shared10s policy checks; no live match");
    }
}
