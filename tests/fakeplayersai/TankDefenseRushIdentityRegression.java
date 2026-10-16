package custom.fakeplayersai;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;
import static custom.fakeplayersai.TacticsPolicy.*;

/** Runs the actual production sequence and exact original-effect identity policy. */
public final class TankDefenseRushIdentityRegression
{
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static Option option(int id) { return new Option(id,Kind.DEFENSE,210); }
    private static BuffInfo effect(int id) throws Exception
    {
        StatSet set=new StatSet(); set.set("skill_id",id); set.set("level",1); set.set("operateType","A2");
        set.set("targetType","FRONT_AREA"); set.set("abnormalType","STUN"); set.set("abnormalTime",9);
        // Passive actual-library fixture only: the real constructor requires live
        // actors and starts the GameTime task. Do not boot actors/DB/timers here.
        Field unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe=(sun.misc.Unsafe)unsafeField.get(null);
        BuffInfo info=(BuffInfo)unsafe.allocateInstance(BuffInfo.class);
        Field effects=BuffInfo.class.getDeclaredField("_effects"); effects.setAccessible(true); effects.set(info,new java.util.ArrayList<>());
        Field skill=BuffInfo.class.getDeclaredField("_skill"); skill.setAccessible(true); skill.set(info,new Skill(set));
        Field time=BuffInfo.class.getDeclaredField("_abnormalTime"); time.setAccessible(true); time.setInt(info,9);
        return info;
    }
    public static void main(String[] args) throws Exception
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=java.nio.file.Path.of(args[0]).toFile();
        TankDefenseSequence tank=new TankDefenseSequence();
        List<Option> all=List.of(option(439),option(368),option(350),option(110));
        check(tank.choose(all,100000,false)==-1,"uninitialized clock never casts defense during prep");
        tank.reset(10000);
        for (long time:new long[]{10000,10001,12500,14999}) check(tank.choose(all,time,false)==-1,"no weak or strong defense before exact combat+5: "+time);
        check(tank.choose(all,15000,false)==110,"UD first exactly+5 regardless HP/CP/randomness");
        check(tank.choose(all,15000,true)==-1,"no second strong while original UD live");
        tank.observed(110);
        check(tank.choose(all,15001,true)==-1,"active UD not refreshed even if timer/reuse reset");
        check(tank.choose(all,15001,false)==368,"early cancel/dispel/steal immediately advances to Vengeance; no duration wait");
        tank.observed(368);
        check(tank.choose(all,15002,true)==-1,"active Vengeance blocks UD even if UD available again");
        check(tank.choose(List.of(option(350),option(110)),15002,false)==110,"next available strong wins before weak after Vengeance ends");
        check(tank.choose(List.of(option(439),option(350)),15002,false)==350,"weak physical pool fallback only when no strong is legal");
        check(tank.choose(List.of(option(368)),15002,false)==368,"learned/effective override only368 does not inject110");
        check(tank.choose(List.of(),15002,false)==-1,"no learned/resource/condition eligible defense never invented");
        tank.reset(20000);
        check(tank.choose(all,24999,false)==-1,"new pair resets clock rather than reusing prior match age");
        check(tank.choose(all,25000,false)==110,"new pair resets deterministic sequence");
        for (boolean active:new boolean[]{false,true}) for (long elapsed:new long[]{0,4999,5000,30000})
            check(tank.due(20000+elapsed)==(elapsed>=5000),"clock independent of nominal effect duration and active="+active);
        check(!meleeOpponent(true,false,true,false,40),"known disarmed bow still archer, not melee");
        check(!meleeOpponent(true,true,false,false,40),"known caster preset remains caster even with missing weapon");
        check(meleeOpponent(true,false,false,false,900),"known melee class remains melee despite temporary range change");
        check(!meleeOpponent(false,false,false,true,40),"unknown mage fallback not melee");
        check(meleeOpponent(false,false,false,false,40),"unknown melee uses bounded real range fallback");

        BuffInfo original=effect(793), replacement=effect(793), otherSkill=effect(494);
        Method applied=BotTactics.class.getDeclaredMethod("hasAppliedStun",BuffInfo.class); applied.setAccessible(true);
        check(!(boolean)applied.invoke(null,original),"empty/resisted793 info is NOT an actual applied stun");
        StatSet stunSet=new StatSet(); stunSet.set("name","Stun");
        original.addEffect(new handlers.skill.effects.Stun(null,null,stunSet,new StatSet()));
        check((boolean)applied.invoke(null,original),"real applied Stun handler has both STUNtype and STUNNEDflag");
        check(!(boolean)applied.invoke(null,replacement),"other empty793 same-template info cannot count as original stun");
        OriginalRushStun<BuffInfo> loop=new OriginalRushStun<>();
        check(!loop.active(original,true),"no loop without confirmed original effect capture");
        loop.begin(original);
        check(loop.active(original,true),"exact original live own793 retained");
        for (int casts=0;casts<8;casts++)
        {
            check(loop.active(original,true),"repeated real793 cast observes same original object, cycle="+casts);
            check(original.getAbnormalTime()==9,"loop never extends original abnormal time");
        }
        check(!loop.active(replacement,true),"same id/level/duration replacement is NOT the exact original");
        check(!loop.active(original,true),"replacement terminates anchor, does not silently restore/adopt old object");
        loop.begin(original); check(!loop.active(null,false),"expiry/cancel/steal ends loop immediately");
        loop.begin(original); check(!loop.active(original,false),"wrong effector/not in use/removed/not stunned signal ends loop");
        loop.begin(original); check(!loop.active(otherSkill,true),"other skill stun never continues original793 loop");
        loop.begin(original); check(loop.retreatAllowed(10000,45),"retreat starts on actual loop observation");
        check(loop.retreatAllowed(11999,100),"bounded two-second retreat grace");
        check(!loop.retreatAllowed(12000,100),"no unproductive perpetual retreat");
        check(loop.retreatAllowed(12001,220),"legal min-distance safety margin reached");
        check(loop.retreatAllowed(13000,45),"next charge may begin another bounded retreat while SAME effect remains");
        loop.clear(); check(!loop.active(original,true),"teardown clears effect identity");
        for (double distance:new double[]{1,40,100,199,219})
        {
            check(OriginalRushStun.stepLength(distance)<=160,"real movement waypoint cap");
            for (int i=0;i<5;i++)
            {
                double[] point=OriginalRushStun.retreatPoint(42745,188869,42745-distance,188869,i);
                check(point!=null && Math.hypot(point[0]-42745,point[1]-188869)<=160.0001,"each candidate is ordinary bounded step");
            }
        }
        // Actual arena starts are far apart; the old bot-spawn anchor cannot be
        // used after Rush. A legal point near the human must remain discoverable.
        double enemyX=42705,enemyY=188869,spawnX=44112,spawnY=188117;
        double total=Math.hypot(spawnX-enemyX,spawnY-enemyY);
        double landedX=enemyX+40*(spawnX-enemyX)/total,landedY=enemyY+40*(spawnY-enemyY)/total;
        boolean legal=false;
        for (int i=0;i<5;i++)
        {
            double[] point=OriginalRushStun.retreatPoint(landedX,landedY,enemyX,enemyY,i);
            if (TrainingArena.inside((int)Math.round(point[0]),(int)Math.round(point[1]))
                && Math.hypot(point[0]-enemyX,point[1]-enemyY)<=1600 && Math.hypot(point[0]-landedX,point[1]-landedY)<=650) legal=true;
        }
        check(total>1500 && legal,"actual1596-separated starts admit authoritative arena-bounded post-rush retreat");
        Field ref=OriginalRushStun.class.getDeclaredField("original");
        check(ref.getType()==java.lang.ref.WeakReference.class,"identity anchor cannot strongly retain actors through BuffInfo");
        check(!BotTactics.beginCombat(null,null),"combat marker never claims arbitrary human/null actors");
        int id=2147000002; BotTactics.setDifficulty(id,"strong");
        Field field=BotTactics.class.getDeclaredField("STATES"); field.setAccessible(true);
        Map<?,?> states=(Map<?,?>)field.get(null); Object state=states.get(id);
        Field loopField=state.getClass().getDeclaredField("rushLoop"); loopField.setAccessible(true);
        @SuppressWarnings("unchecked") OriginalRushStun<BuffInfo> owned=(OriginalRushStun<BuffInfo>)loopField.get(state); owned.begin(original);
        BotTactics.forget(id); check(!states.containsKey(id),"forget drops whole tank-clock/rush-identity owner state");
        BotTactics.setDifficulty(id,"strong");
        @SuppressWarnings("unchecked") OriginalRushStun<BuffInfo> fresh=(OriginalRushStun<BuffInfo>)loopField.get(states.get(id));
        check(!fresh.active(original,true),"new pair cannot reuse previous original stun reference"); BotTactics.forget(id);
        System.out.println("PASS: "+checks+" production tank sequence/original BuffInfo identity/bounds/lifecycle assertions");
    }
}
