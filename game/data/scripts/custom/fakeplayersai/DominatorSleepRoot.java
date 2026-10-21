package custom.fakeplayersai;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

/** One bounded root sequence per actual own Sleep; requests alone cannot start it. */
final class DominatorSleepRoot
{
    private BuffInfo sleep,root;
    private int attempts;
    private boolean done, retreat;
    final SleepRetreat movement=new SleepRetreat();
    void reset(Player bot) { movement.clear(bot); sleep=null; root=null; attempts=0; done=false; retreat=false; }
    void observe(Player bot,Player enemy)
    {
        BuffInfo live=SleepRetreat.ownSleep(bot,enemy);
        if(live!=null&&live!=sleep)
        {
            reset(bot);sleep=live;
            if(!bot.isCastingNow()&&!bot.isCastingSimultaneouslyNow()&&bot.isMoving())bot.getAI().setIntentionIdle();
        }
    }
    boolean active() { return sleep!=null&&!done; }
    boolean ownsSleep(Player enemy) { return sleep!=null&&enemy!=null&&enemy.isSleeping(); }
    boolean ownsRoot(Player bot,Player enemy) { return root!=null&&root==DominatorControlRetreat.control(bot,enemy); }
    void rooted(BuffInfo live) { root=live; }
    boolean retreating() { return retreat; }
    boolean rootAllowed() { return sleep==null||!done&&!retreat&&attempts<3; }
    void requested() { attempts++; }
    int attempts() { return attempts; }
    boolean retreat(Player bot,Player enemy,long time,int[] center)
    {
        retreat=true;
        if(bot.isMovementDisabled()||bot.isOutOfControl())return true;
        if(movement.tick(bot,enemy,time,center,sleep))return true;
        done=true;return false;
    }
}
