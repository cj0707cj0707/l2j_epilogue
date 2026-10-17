package custom.fakeplayersai;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/** Actual native Player/Skill + shipped effect deny path. Never constructs a native NPC. */
public class OlympiadSummonNativeGuardTest
{
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception
    {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        handlers.EffectMasterHandler.main(new String[0]);SkillData.getInstance();
        ArenaOrbitTranceManaTest.f(LowManaAutoAttackTest.class,"unsafe").set(null,ArenaOrbitTranceManaTest.f(sun.misc.Unsafe.class,"theUnsafe").get(null));
        Map<Integer,Player> roster=(Map<Integer,Player>)ArenaOrbitTranceManaTest.f(BotManager.class,"BOTS").get(null);
        Method permit=OlympiadSummonRules.class.getDeclaredMethod("withPermit",Player.class,Skill.class,Runnable.class);permit.setAccessible(true);
        StatSet set=new StatSet(),params=new StatSet();set.set("name","Summon");params.set("npcId",0);
        var effect=new handlers.skill.effects.Summon(null,null,set,params);
        for(int cid:new int[]{90,91,95,97,88})
        {
            Player p=ArenaOrbitTranceManaTest.actor(cid,true,false),other=ArenaOrbitTranceManaTest.actor(cid,true,false);
            roster.put(p.getObjectId(),p);roster.put(other.getObjectId(),other);
            Skill skill=SkillData.getInstance().getSkill(283,1),wrong=SkillData.getInstance().getSkill(912,1);
            check(skill!=null&&wrong!=null,"real summon templates");
            for(int i=0;i<100;i++)
            {
                effect.onStart(p,p,skill);check(p.getSummon()==null,"effect denied BEFORE native NPC/template lookup");
                permit.invoke(null,p,skill,(Runnable)()->{
                    check(!OlympiadSummonRules.allowSummon(other,skill),"actor bound");
                    check(!OlympiadSummonRules.allowSummon(p,wrong),"skill bound");
                    AtomicReference<Throwable> error=new AtomicReference<>();
                    Thread t=new Thread(()->{try{check(!OlympiadSummonRules.allowSummon(p,skill),"thread bound");}catch(Throwable e){error.set(e);}});
                    t.start();try{t.join();}catch(InterruptedException e){throw new RuntimeException(e);}
                    if(error.get()!=null)throw new AssertionError(error.get());
                    check(OlympiadSummonRules.allowSummon(p,skill),"one authorized effect");
                    check(!OlympiadSummonRules.allowSummon(p,skill),"permit consumed before creation");
                });
                check(!OlympiadSummonRules.allowSummon(p,skill),"finally revoked");
            }
            try{permit.invoke(null,p,skill,(Runnable)()->{throw new IllegalStateException("injected");});throw new AssertionError("expected");}
            catch(InvocationTargetException expected){check(expected.getCause() instanceof IllegalStateException,"exception propagated");}
            check(!OlympiadSummonRules.allowSummon(p,skill),"exception cleanup");
            Player human=ArenaOrbitTranceManaTest.actor(cid,true,true);roster.put(human.getObjectId(),human);
            check(OlympiadSummonRules.allowSummon(human,skill),"client-backed human untouched even if roster ID");
            Player training=ArenaOrbitTranceManaTest.actor(cid,false,false);
            check(OlympiadSummonRules.allowSummon(training,skill),"training unchanged");
            roster.clear();check(OlympiadSummonRules.allowSummon(p,skill),"unmanaged actor unchanged");
        }
        System.out.println("PASS: "+checks+" real-core Player/Skill/effect guard and one-use permit checks; no native NPC/DB/client spawn");
    }
}
