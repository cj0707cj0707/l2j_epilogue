package custom.fakeplayersai;

import java.lang.reflect.Field;
import java.util.Map;
import static custom.fakeplayersai.TacticsPolicy.*;

/** Exercises the production original-lease cycle, including actual core CHARGE overlap. */
public final class CyclicTrainingRushRegression
{
    private static int checks;
    private static void check(boolean condition,String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static double retreatToRange(OriginalRushStun<Object> loop,Object original,long now,double landing,double previousX,double previousY)
    {
        double enemyX=42705,enemyY=188869, x=enemyX+landing,y=enemyY;
        for (int leg=0;leg<4 && Math.hypot(x-enemyX,y-enemyY)<220;leg++)
        {
            check(loop.active(original,true),"same original lease throughout ordinary retreat");
            check(loop.retreatAllowed(now+leg*500,Math.hypot(x-enemyX,y-enemyY)),"bounded movement budget for each new landing");
            double[] selected=null;
            for (int attempt=0;attempt<8;attempt++)
            {
                double[] point=OriginalRushStun.retreatPoint(x,y,enemyX,enemyY,attempt,previousX,previousY);
                check(point!=null,"defined direction even at exact target overlap");
                check(Math.hypot(point[0]-x,point[1]-y)<=160.0001,"ordinary waypoint never exceeds160");
                if (TrainingArena.inside((int)Math.round(point[0]),(int)Math.round(point[1]))
                    && Math.hypot(point[0]-enemyX,point[1]-enemyY)<=1600
                    && Math.hypot(point[0]-enemyX,point[1]-enemyY)<=650)
                { selected=point; break; }
            }
            check(selected!=null,"legal arena point exists near actual human start after core zero landing");
            x=selected[0]; y=selected[1];
        }
        double distance=Math.hypot(x-enemyX,y-enemyY);
        check(distance>=220 && distance<=600,"retreat reaches actual legal793 window");
        return distance;
    }
    public static void main(String[] args) throws Exception
    {
        Object original=new Object(), replacement=new Object();
        OriginalRushStun<Object> loop=new OriginalRushStun<>(); loop.begin(original);
        TrainingBerserker burst=new TrainingBerserker();
        long now=10000;
        for (int cycle=0;cycle<5;cycle++)
        {
            // Core beginCast really sets XYZ exactly to targetXYZ, distance0,
            // not the formerly assumed retail melee40 landing.
            double distance=retreatToRange(loop,original,now,0,1407,-752);
            check(burst.allowsSkill(793,distance,true),"normal burst state has no one-shot restriction on loop793");
            check(loop.retreatAllowed(now+1000,distance),"ready distance phase");
            check(loop.retreatAllowed(now+5000,distance),"real cooldown/resource wait does not consume movement budget");
            check(loop.active(original,true),"waiting cannot replace original lease");
            loop.loopRequestAccepted();
            check(loop.retreatAllowed(now+7000,0),"actual cast time not charged to retreat budget");
            check(loop.resolvedLoopLanding(original,true,true,distance,0),"resolved real zero-distance landing rearms cycle "+cycle);
            burst.resolved(793,true,distance,0);
            check(loop.active(original,true),"resolved landing retains SAME original effect rather than begin/reanchor");
            now+=8000;
        }
        check(retreatToRange(loop,original,now,40,1407,-752)>=220,"nonoverlap40 landing remains compatible");
        // Slow movement and blocked geodata pause, never abandon the live lease.
        loop.retryRetreat(now);
        check(!loop.retreatAllowed(now+749,0),"blocked path retry paced750ms");
        check(loop.active(original,true),"no-path hold retains exact original identity");
        check(loop.retreatAllowed(now+750,0),"blocked path may retry when ordinary movement becomes legal");
        check(!loop.retreatAllowed(now+2750,80),"slow incomplete retreat stops after bounded2seconds");
        check(loop.active(original,true),"unproductive movement budget cannot clear original lease");
        check(!loop.retreatAllowed(now+3499,80),"budget pause is paced not unbounded walking");
        check(loop.retreatAllowed(now+3500,80),"fresh bounded movement budget after pause");
        loop.loopRequestAccepted();
        check(!loop.resolvedLoopLanding(original,true,false,260,0),"failed request cannot masquerade as successful landing");
        check(loop.active(original,true),"failed request does not discard still-live original");
        loop.loopRequestAccepted();
        check(!loop.resolvedLoopLanding(original,true,true,99,0),"insufficient real displacement is not a completed charge");
        loop.loopRequestAccepted();
        check(!loop.resolvedLoopLanding(replacement,true,true,260,0),"same793 replacement ends original cycle");
        check(!loop.active(original,true),"old effect cannot resurrect after replacement");
        loop.begin(original); loop.loopRequestAccepted();
        check(!loop.active(null,false),"original expiry/dispel ends cycle during pending cast safely");
        check(!loop.resolvedLoopLanding(original,true,true,260,0),"late completion cannot restart expired original");
        loop.begin(replacement);
        check(loop.active(replacement,true),"a NEW normal own793 stun starts fresh identity");
        check(retreatToRange(loop,replacement,now+10000,0,-200,0)>=220,"fresh original has fresh cycle geometry/budget");
        // Full fan includes inward travel at an arena edge even if fallback points outward.
        boolean inward=false;
        for (int attempt=0;attempt<8;attempt++)
        {
            double[] point=OriginalRushStun.retreatPoint(42410,188493,42410,188493,attempt,-1,0);
            if (TrainingArena.inside((int)Math.round(point[0]),(int)Math.round(point[1]))) inward=true;
        }
        check(inward,"full bounded candidate fan finds inward zero-distance retreat at authorized edge");
        check(OriginalRushStun.retreatPoint(0,0,0,0,0)!=null,"default geometry also never returnsnull at zero");
        check(OriginalRushStun.retreatPoint(0,0,0,0,8)==null,"finite eight-candidate movement fan");
        for (boolean ready:new boolean[]{false,true}) for (boolean resource:new boolean[]{false,true})
            for (boolean visible:new boolean[]{false,true}) for (boolean sleeping:new boolean[]{false,true})
                check(allowed(Kind.CONTROL,ready,true,visible,false,sleeping,resource,true)==(ready&&resource&&visible&&!sleeping),"cyclic mode cannot bypass normal legal793 gates");
        int id=2147000003; BotTactics.setDifficulty(id,"strong");
        Field statesField=BotTactics.class.getDeclaredField("STATES"); statesField.setAccessible(true);
        Map<?,?> states=(Map<?,?>)statesField.get(null); Object state=states.get(id);
        Field leaseField=state.getClass().getDeclaredField("rushLoop"); leaseField.setAccessible(true);
        @SuppressWarnings("unchecked") OriginalRushStun<Object> production=(OriginalRushStun<Object>)leaseField.get(state);
        production.begin(original); production.loopRequestAccepted();
        check(production.resolvedLoopLanding(original,true,true,260,0),"actual owner state uses same cycle seam");
        BotTactics.forget(id); BotTactics.setDifficulty(id,"strong");
        @SuppressWarnings("unchecked") OriginalRushStun<Object> fresh=(OriginalRushStun<Object>)leaseField.get(states.get(id));
        check(!fresh.active(original,true),"forget/new pair removes original and pending-cycle state"); BotTactics.forget(id);
        System.out.println("PASS: "+checks+" production cyclic zero/40 landing, retry, identity, legality and lifecycle assertions");
    }
}
