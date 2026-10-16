package custom.fakeplayersai;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.util.StatSet;
import static custom.fakeplayersai.TacticsPolicy.*;

/** Production policies + real skill/effect/target classes, without a running server. */
public final class DominatorCpTitanPressureRegression
{
    private static int checks;
    private static void check(boolean ok,String message) { checks++; if(!ok) throw new AssertionError(message); }
    private static Option heal(int id) { return new Option(id,Kind.HEAL,145); }
    private static Object call(String name,Class<?>[] args,Object... values) throws Exception
    { Method m=BotTactics.class.getDeclaredMethod(name,args);m.setAccessible(true);return m.invoke(null,values); }
    private static Skill skill(int id,String target,String operate,String abnormal) throws Exception
    {
        StatSet set=new StatSet();set.set("skill_id",id);set.set("level",1);set.set("targetType",target);set.set("operateType",operate);
        if(abnormal!=null) {set.set("abnormalType",abnormal);set.set("abnormalTime",30);}
        return new Skill(set);
    }
    private static AbstractEffect effect(String name) throws Exception
    {
        StatSet set=new StatSet();set.set("name",name);StatSet params=new StatSet();params.set("power",687);
        return (AbstractEffect)Class.forName("handlers.skill.effects."+name).getConstructor(Condition.class,Condition.class,StatSet.class,StatSet.class).newInstance(null,null,set,params);
    }
    public static void main(String[] args) throws Exception
    {
        for(double cp:new double[]{0,.01,.34,.49,.499999}) check(dominatorCpNeeded(cp),"CP below50 eligible");
        for(double cp:new double[]{.50,.500001,.65,1,-1,Double.NaN,Double.POSITIVE_INFINITY}) check(!dominatorCpNeeded(cp),"CP at/above50 or invalid not healed");
        List<Option> both=List.of(heal(1306),new Option(1245,Kind.DAMAGE,62),heal(1305),new Option(1097,Kind.CONTROL,54));
        DominatorCpWeave weave=new DominatorCpWeave();
        check(weave.choose(both,.49)==1305,"Honor preferred despite order/randomness");
        weave.resolved(1305,false,false);check(weave.choose(both,.49)==1305,"rejected request does not consume weave attack turn");
        for(int cycle=0;cycle<5;cycle++)
        {
            check(weave.choose(both,.49)==1305,"ready legal CP between attacks cycle"+cycle);
            weave.resolved(1305,true,false);
            check(weave.choose(both,.25)==1245,"real CP cast must yield a ready DAMAGE before another CP cast");
            weave.resolved(1245,false,true);check(weave.choose(both,.25)==1245,"failed damage request never fakes weave progress");
            weave.resolved(1245,true,true);
        }
        weave.resolved(1305,true,false);
        check(weave.choose(List.of(heal(1306),new Option(1097,Kind.CONTROL,54)),.25)==1097,"available legal curse/control can provide between-CP offense");
        check(weave.choose(List.of(heal(1306)),.25)==1306,"no offensive option does not permanently lock legal recovery");
        check(weave.choose(List.of(),.25)==-1,"no learned/MP/reuse legal support never injected");
        weave.reset(); check(weave.choose(List.of(heal(1306)),.49)==1306,"Ritual real fallback when Honor unavailable/override removed");
        check(weave.choose(both,.50)==-1,"at50 ordinary offense is not displaced by CP healing");
        weave.resolved(1306,true,false);check(weave.choose(both,.80)==1245,"no stale highCP recovery loop after a cast");
        weave.reset();check(weave.choose(both,.49)==1305,"new pair resets weaving debt");
        check(weave.choose(List.of(heal(1305),new Option(1218,Kind.HEAL,160)),.10)==1218,"preserve genuine life-saving HP heal");
        for(int id:new int[]{1305,1306})
        {
            Skill cp=skill(id,id==1305?"PARTY_CLAN":"ONE","A1",null);AbstractEffect e=effect("CpHeal");cp.addEffect(EffectScope.GENERAL,e);
            check(e.getEffectType()==EffectType.CPHEAL&&e.isInstant(),"actual CP handler not guessed support flag");
            check(call("kind",new Class[]{Skill.class},cp)==Kind.HEAL,"actual instant CpHeal classified self HEAL");
            check((boolean)call("supportedTarget",new Class[]{Skill.class,boolean.class},cp,true),"actual Honor/Ritual selftarget accepted");
            check(!(boolean)call("self",new Class[]{Kind.class},Kind.DAMAGE),"offense is not retargeted as support");
        }
        Skill victory=skill(1414,"PARTY_CLAN","A2","MULTI_BUFF");victory.addEffect(EffectScope.GENERAL,effect("Buff"));victory.addEffect(EffectScope.GENERAL,effect("CpHealPercent"));
        check(call("kind",new Class[]{Skill.class},victory)==Kind.BUFF,"real Victory compound buff is not instant CP recovery choice");
        check((boolean)call("supportedTarget",new Class[]{Skill.class,boolean.class},victory,true),"real Victory selfrecast target afterCancel supported");
        check(!(boolean)call("supportedTarget",new Class[]{Skill.class,boolean.class},skill(20001,"PARTY_CLAN","A1",null),true),"no broad unrelated clan target bypass");
        // Actual target handlers: inert allocated Player, no constructor, DB, AI, timers,
        // effect application or CP/stat writes. onlyFirst PartyClan includes self directly.
        Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        Player self=(Player)unsafe.allocateInstance(Player.class);
        Skill honor=skill(1305,"PARTY_CLAN","A1",null),ritual=skill(1306,"ONE","A1",null);
        check(new handlers.skill.targets.PartyClan().getTargetList(honor,self,true,self).get(0)==self,"real PARTY_CLAN always includes caster even solo/clanless");
        check(new handlers.skill.targets.One().getTargetList(ritual,self,true,self).get(0)==self,"real ONE allows positive Ritual targeted self");
        Object enemy=new Object(), ownSummon=new Object();
        check(selfOnlyTargets(self,List.of(self)),"solo actual target identity list remains self-only");
        check(!selfOnlyTargets(self,List.of(self,enemy)),"same-clan enemy candidate prevents Honor/Victory area application");
        check(!selfOnlyTargets(self,List.of(enemy,self)),"same-party enemy candidate rejected irrespective of target order");
        check(!selfOnlyTargets(self,List.of(self,ownSummon)),"strict SELF contract does not silently permit any additional target");
        check(!selfOnlyTargets(self,null)&&!selfOnlyTargets(self,List.of()),"missing/empty actual target list cannot be treated self-safe");
        check(!selfOnlyTargets(self,java.util.Arrays.asList(self,null)),"invalid target member rejected");
        check(!selfOnlyTargets(null,List.of(self)),"missing caster identity rejected");
        String identity=new String("bot");check(!selfOnlyTargets(identity,List.of(new String("bot"))),"identity guard not equals/name coincidence");
        weave.reset();
        List<Option> clanCandidates=new java.util.ArrayList<>();
        if(selfOnlyTargets(self,List.of(self,enemy))) clanCandidates.add(heal(1305));
        clanCandidates.add(heal(1306));
        check(weave.choose(clanCandidates,.25)==1306,"ordinary unsafe area target exclusion falls back to legal self Ritual");
        check(selfOnlyTargets(self,new handlers.skill.targets.PartyClan().getTargetList(honor,self,true,self)),"real targethandler singleton passes identity seam");
        for(int id:new int[]{368,110,760,20002})
        {
            Skill defense=skill(id,id==368?"AURA":"SELF","A2","PD_UP_SPECIAL");
            defense.addEffect(id==368?EffectScope.SELF:EffectScope.GENERAL,effect(id==760?"Buff":"ImmobileBuff"));
            boolean classified=(boolean)call("immobilizingSupport",new Class[]{Skill.class},defense);
            check(classified==(id!=368&&id!=760),"explicit368 exception precedes actual GENERAL/SELF immobile scan: "+id);
            check(defense.getEffects(id==368?EffectScope.SELF:EffectScope.GENERAL).get(0).getName().equals(id==760?"Buff":"ImmobileBuff"),"AI exception never edits actual effect template");
        }
        for(boolean ranked:new boolean[]{false,true}) for(int mask=0;mask<128;mask++)
        {
            boolean armed=(mask&1)!=0,blocked=(mask&2)!=0,sleeping=(mask&4)!=0,invul=(mask&8)!=0,los=(mask&16)!=0,attackable=(mask&32)!=0,pending=(mask&64)!=0;
            check(titanPursuit(113,armed,blocked,sleeping,invul,attackable,pending)==(armed&&!blocked&&!sleeping&&!invul&&attackable&&!pending),"ranked="+ranked+" los="+los+" Titan MOVEMENT-only pursuit guards not bypassed");
        }
        for(int id:new int[]{88,89,90,91,92,99,102,103,106,108,109,110,113,114,115,131})
        {
            check(titanRecoveryAllowed(id,false)==(id!=113),"only Titan cooldown recovery changed, class"+id);
            check(titanRecoveryAllowed(id,true),"disarm/noWeapon recovery retained class"+id);
            check(titanPursuit(id,true,false,false,false,true,false)==(id==113),"no other class receives Titan auto fallback class"+id);
        }
        for(boolean ranked:new boolean[]{false,true})
        {
            boolean generic=recoveryStepAllowed(false,false,false,100,false);
            check(generic,"actual generic root cause exists, not unique toTitan");
            check(!(tankRecoveryAllowed(false,false)&&titanRecoveryAllowed(113,false)&&generic),"ranked="+ranked+" armedTitan cooldown-only step suppressed");
            check(titanPursuit(113,true,false,false,false,true,false),"ranked="+ranked+" legal cooldown auto pressure allowed");
            check(titanRecoveryAllowed(113,true)&&recoveryStepAllowed(true,false,false,100,false),"ranked="+ranked+" real disarm recovery still possible");
            for(int distance:new int[]{30,600})
            {
                check(titanPursuit(113,true,false,false,false,true,false),"ranked="+ranked+" near/far blind distance="+distance+" yields movement-only pursuit");
                check(!allowed(Kind.DAMAGE,true,true,false,false,false,true,true),"ranked="+ranked+" blind pursuit never grants actual damage cast");
                check(!allowed(Kind.CONTROL,true,true,false,false,false,true,true),"ranked="+ranked+" blind pursuit never grants actual control cast");
            }
        }
        for(Kind kind:new Kind[]{Kind.HEAL,Kind.DAMAGE}) for(int mask=0;mask<32;mask++)
        {
            boolean ready=(mask&1)!=0,range=(mask&2)!=0,visible=(mask&4)!=0,active=(mask&8)!=0,resource=(mask&16)!=0;
            check(allowed(kind,ready,range,visible,active,false,resource,true)==(ready&&range&&!active&&resource&&(kind==Kind.HEAL||visible)),"weave cannot bypass real readiness/range/MP/active/LOS option gates");
        }
        int botId=2147000004;BotTactics.setDifficulty(botId,"strong");Field sf=BotTactics.class.getDeclaredField("STATES");sf.setAccessible(true);
        Map<?,?> states=(Map<?,?>)sf.get(null);Object state=states.get(botId);Field wf=state.getClass().getDeclaredField("dominatorCp");wf.setAccessible(true);
        DominatorCpWeave production=(DominatorCpWeave)wf.get(state);production.resolved(1305,true,false);check(production.choose(both,.25)==1245,"actual owner state holds weave progress");
        BotTactics.forget(botId);BotTactics.setDifficulty(botId,"strong");check(((DominatorCpWeave)wf.get(states.get(botId))).choose(both,.25)==1305,"teardown removes all prior CP weave state");BotTactics.forget(botId);
        System.out.println("PASS: "+checks+" production CP weave, actual handlers/targets, Titan pressure and Vengeance exception assertions");
    }
}
