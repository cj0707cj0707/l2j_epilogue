package custom.fakeplayersai;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static custom.fakeplayersai.TacticsPolicy.*;

/** Runs the exact production ordered-burst state and fallback policy, not a copied model. */
public final class TrainingBerserkerPressureRegression
{
    private static int checks;
    private static void check(boolean condition, String message)
    {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static Option option(int id) { return new Option(id, id==485 ? Kind.CONTROL : Kind.DAMAGE,62); }
    private static int choose(TrainingBerserker state, List<Option> legal, double distance, boolean primary)
    { return state.choose(legal,distance,primary,Difficulty.STRONG,0,0); }
    private static void confirmedRush(TrainingBerserker state)
    { state.resolved(793,true,400,45); }

    public static void main(String[] args) throws Exception
    {
        TrainingBerserker burst=new TrainingBerserker();
        List<Option> defaultLegal=new ArrayList<>(List.of(option(494),option(484),option(493),option(495),option(793),option(1435),option(501)));
        check(choose(burst,defaultLegal,400,true)==793,"real Rush Impact wins before other charges or ranged debuffs");
        for (int competitor:new int[]{494,484,493,495})
        {
            check(!burst.allowsSkill(competitor,400,true),"no competing charge with effective known793: "+competitor);
            check(burst.allowsSkill(competitor,400,false),"explicit no793 override retains requested charge: "+competitor);
        }
        check(!burst.allowsSkill(793,199.99,true),"real min distance200 is not bypassed");
        check(burst.allowsSkill(793,200,true),"exact min distance200");
        check(!burst.allowsSkill(485,150.01,true),"Disarm cannot exceed ranked cap150 even if presented as legal");
        check(burst.allowsSkill(485,150,true),"policy cap separate from real cast40+collision gate");
        check(!burst.allowsSkill(501,45,true),"Violent Temper never before actual rush");
        burst.resolved(793,false,400,45);
        check(!burst.allowsSkill(501,45,true),"accepted but unexecuted rush does not unlock finisher");
        burst.resolved(793,true,99,45);
        check(!burst.allowsSkill(501,45,true),"paid/cast-observed without real rush travel does not unlock finisher");
        burst.resolved(793,true,100,600);
        check(!burst.allowsSkill(501,45,true),"movement without approaching target fails landed check");
        confirmedRush(burst);
        check(burst.allowsSkill(501,45,true),"confirmed actual charge unlocks one finisher attempt");
        List<Option> melee=new ArrayList<>(List.of(option(1435),option(477),option(498),option(501),option(497),option(526),option(485)));
        // Each removed option represents real reuse/condition filtering on the next
        // tick. No arbitrary timer or fake resource replenishment makes it legal.
        for (int expected:new int[]{485,526,497,501,498,477,1435})
        {
            int selected=choose(burst,melee,45,true);
            check(selected==expected,"ordered post-rush burst/filler expected "+expected+" got "+selected);
            burst.resolved(selected,true,0,45);
            melee.removeIf(o -> o.id()==selected);
        }
        check(choose(burst,melee,45,true)==-1,"all real skills unavailable after burst");
        check(trainingBerserkerPressure(true,true,false,false,false,true,true,false),"armed cooldown gap yields pressure instead of generic idle/retreat");
        // The foe's stun is deliberately NOT a prohibition: only this bot's CC is.
        for (boolean foeStunned:new boolean[]{false,true})
            check(trainingBerserkerPressure(true,true,false,false,false,true,true,false),"post-rush pressure persists when foe stunned="+foeStunned);
        check(!burst.allowsSkill(501,45,true),"finisher attempted once even when reuse ends in2seconds");
        confirmedRush(burst);
        check(!burst.allowsSkill(501,45,true),"another rush does not reset once-per-battle finisher");
        burst.reset(); confirmedRush(burst); burst.resolved(501,false,0,45);
        check(burst.allowsSkill(501,45,true),"failed unstarted finisher request does not consume once flag");
        check(choose(burst,List.of(option(477)),45,false)==477,"override without793 never stalls or injects opener");
        check(choose(burst,List.of(option(494)),400,false)==494,"override-only charge remains usable without793");
        check(choose(burst,List.of(option(20000)),45,false)==20000,"nondefault effective override survives ordered selector");
        check(choose(burst,List.of(option(485),new Option(483,Kind.DEFENSE,160)),45,true)==483,"urgent real defense beats burst");
        check(choose(burst,List.of(option(526),new Option(482,Kind.BUFF,72)),45,true)==526,"ready usable melee attack beats optional self buff");
        check(choose(burst,List.of(option(477),option(1435)),45,true)==477,"conditional Contagion missing from legal set never blocks legal Dark Smash");
        check(choose(burst,List.of(option(1435)),400,true)==1435,"unavailable/out-of-range melee burst never blocks legal ranged filler");
        for (int mask=0;mask<256;mask++)
        {
            boolean authorized=(mask&1)!=0, armed=(mask&2)!=0, blocked=(mask&4)!=0, sleeping=(mask&8)!=0;
            boolean invul=(mask&16)!=0, visible=(mask&32)!=0, attackable=(mask&64)!=0, pending=(mask&128)!=0;
            boolean expected=authorized&&armed&&!blocked&&!sleeping&&!invul&&visible&&attackable&&!pending;
            check(trainingBerserkerPressure(authorized,armed,blocked,sleeping,invul,visible,attackable,pending)==expected,
                "exhaustive pressure safety gates "+mask);
        }
        check(effectiveRange(false,40,400,40,8)==48,"Enuma frontal affect400 is NOT legal melee casting range");
        check(effectiveRange(false,40,0,40,8)==48,"Disarm actual40+collision, not policy150 or ranged600");
        check(!basicAttackAllowed(true,false,false),"dagger auto prohibition unchanged");
        check(!basicAttackAllowed(false,false,true),"disarm/missing weapon prohibition unchanged");

        // Verify lifecycle wiring in actual BotTactics state, no DB/real actor needed.
        int id=2147000001; BotTactics.setDifficulty(id,"strong");
        Field statesField=BotTactics.class.getDeclaredField("STATES"); statesField.setAccessible(true);
        Map<?,?> states=(Map<?,?>)statesField.get(null); Object state=states.get(id);
        Field burstField=state.getClass().getDeclaredField("trainingBurst"); burstField.setAccessible(true);
        TrainingBerserker real=(TrainingBerserker)burstField.get(state); confirmedRush(real); real.resolved(501,true,0,45);
        check(!real.allowsSkill(501,45,true),"production state holds consumed finisher");
        BotTactics.forget(id); check(!states.containsKey(id),"teardown removes training burst with existing bounded owner state");
        BotTactics.setDifficulty(id,"strong"); TrainingBerserker next=(TrainingBerserker)burstField.get(states.get(id));
        check(!next.allowsSkill(501,45,true),"new fight requires new real rush"); confirmedRush(next);
        check(next.allowsSkill(501,45,true),"new fight finisher is not permanently consumed"); BotTactics.forget(id);
        check(!BotTactics.tick(null,null,null,false),"no actor/no authorization never claims training ownership");
        System.out.println("PASS: "+checks+" actual production training berserker burst/pressure/lifecycle assertions");
    }
}
