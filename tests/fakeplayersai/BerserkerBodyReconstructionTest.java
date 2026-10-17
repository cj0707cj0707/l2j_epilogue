package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Source-exact candidate construction + real native Skill/Player/HealPercent.
 * Inert actors and status/UI seam; no GameServer/DB/cast or scheduler startup. */
public final class BerserkerBodyReconstructionTest
{
    static int count;
    static void verify(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    static Map<Integer,Skill> currentPool(Player bot,Player enemy,BotClassPreset preset)
    {
        // INSERT_CURRENT_POOL
        return skills;
    }
    static Map<Integer,Skill> previousPool(Player bot,Player enemy,BotClassPreset preset)
    {
        // INSERT_PREVIOUS_POOL
        return skills;
    }
    static Object call(String name,Class<?>[] types,Object...args)throws Exception
    {Method m=BotTactics.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
    static OnCreatureSkillUse event(Player bot,Skill skill)
    {var e=new OnCreatureSkillUse();e.setCaster(bot);e.setSkill(skill);e.setTarget(bot);e.setTargets(List.of(bot));return e;}
    @SuppressWarnings("unchecked") static void checks()throws Exception
    {
        BotPresets.loadOverrides();BotClassPreset preset=BotPresets.getPresetByClassId(131);
        verify(preset.hasBuffsOverride()&&preset.hasRotationOverride(),"test really uses saved overrides");
        verify(Arrays.stream(preset.getBuffs()).noneMatch(id->id==833)&&Arrays.stream(preset.getRotation()).noneMatch(id->id==833),"reproduce persisted omission of833");
        Skill body=SkillData.getInstance().getSkill(833,1);verify(body!=null,"native Body Reconstruction exists");
        verify(body.getReuseDelay()==900000&&body.getHitTime()==1500,"native15min reuse and1.5s cast unchanged");
        verify(body.getMpConsume()==94&&body.getMpInitialConsume()==0,"native94MP not made free");
        verify(call("kind",new Class[]{Skill.class},body)==TacticsPolicy.Kind.HEAL,"native percent heal classified as self HEAL");
        verify((boolean)call("supportedTarget",new Class[]{Skill.class,boolean.class},body,true),"native SELF accepted");
        for(boolean ranked:new boolean[]{false,true})
        {
            Player b=actor(131,ranked,false),enemy=actor(94,ranked,true);
            f(Player.class,"_charges").set(b,new AtomicInteger());
            f(Creature.class,"_reuseTimeStampsSkills").set(b,new HashMap<>());
            Map<Integer,Long> disabled=new HashMap<>();f(Creature.class,"_disabledSkills").set(b,disabled);
            Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
            Set<Integer> training=(Set<Integer>)f(TrainingArena.class,"BOTS").get(null);
            if(ranked)roster.put(b.getObjectId(),b);else training.add(b.getObjectId());
            verify(!currentPool(b,enemy,preset).containsKey(833),"no unlearned skill granted");
            learn(b,body);
            verify(!previousPool(b,enemy,preset).containsKey(833),"baseline7a93669 reproduces missing heal despite known833");
            Map<Integer,Skill> pool=currentPool(b,enemy,preset);
            verify(pool.get(833)==body,"actual production pool now includes known833 with saved overrides");
            verify(pool.size()==1,"override untouched, unrelated unlearned skills not injected");
            for(double ratio:new double[]{.1,.3999,.4,.40001,.7,1})
            {
                hp(b,ratio*10000);mp(b,10000);
                verify(BotRecoveryRules.selfHealAllowed(b,body)==(ratio<=.4),"actual inclusive40% HP threshold in both modes");
                var result=BotRecoveryRules.guard(event(b,body));
                verify((result==null)==(ratio<=.4),"synchronous event threshold guard remains consistent");
                b.setInvul(true);verify(!BotRecoveryRules.selfHealAllowed(b,body),"selection forbidden under invulnerability");
                verify(BotRecoveryRules.guard(event(b,body)).terminate(),"native event veto under invulnerability");b.setInvul(false);
            }
            hp(b,4000);mp(b,93);verify(!(boolean)call("resources",new Class[]{Player.class,Skill.class},b,body),"insufficient native MP blocks cast");
            mp(b,94);verify((boolean)call("resources",new Class[]{Player.class,Skill.class},b,body),"exact native MP permits cast");
            verify(!b.isSkillDisabled(body),"native skill ready before cooldown");disabled.put(body.getReuseHashCode(),System.currentTimeMillis()+900000);
            verify(b.isSkillDisabled(body),"real cooldown prevents reuse");disabled.clear();
            var heal=body.getEffects(EffectScope.GENERAL).get(0);hp(b,4000);heal.onStart(b,b,body);
            verify(b.getCurrentHp()==10000,"actual native HealPercent effect restores full HP");
            hp(b,4000);b.setInvul(true);heal.onStart(b,b,body);verify(b.getCurrentHp()==4000,"actual effect cannot heal through invulnerability");b.setInvul(false);
            roster.remove(b.getObjectId());training.remove(b.getObjectId());
        }
        for(BotClassPreset other:BotPresets.getPresets())if(other.getClassId()!=131&&Arrays.stream(other.getBuffs()).noneMatch(id->id==833)&&Arrays.stream(other.getRotation()).noneMatch(id->id==833))
        {
            Player b=actor(other.getClassId(),false,false),enemy=actor(94,false,true);learn(b,body);
            verify(!currentPool(b,enemy,other).containsKey(833),"no new mandatory833 for other class"+other.getClassId());
        }
        var phase=new TacticsPolicy.TrainingBerserker();
        verify(phase.choose(List.of(new TacticsPolicy.Option(793,TacticsPolicy.Kind.DAMAGE,100),new TacticsPolicy.Option(833,TacticsPolicy.Kind.HEAL,300)),300,true,TacticsPolicy.Difficulty.STRONG,0,0)==833,"eligible emergency heal outranks ordinary training offense outside STUN");
    }
    public static void main(String[] args)throws Exception
    {
        f(LowManaAutoAttackTest.class,"unsafe").set(null,f(Unsafe.class,"theUnsafe").get(null));ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        handlers.EffectMasterHandler.main(new String[0]);checks();System.out.println("PASS: "+count+" persisted-override Body Reconstruction pool, baseline reproduction,40%/invul/MP/native reuse and actual full-HP effect checks; no live server");System.exit(0);
    }
}
