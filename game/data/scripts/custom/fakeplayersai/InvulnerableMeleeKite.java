package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/** Arena-authorized caller only. No attacks/casts while a melee opponent is invulnerable. */
final class InvulnerableMeleeKite
{
    private WeakReference<Player> actor = new WeakReference<>(null);
    private boolean moving;
    private int x, y;
    private long nextAt;
    void reset() { stop(); nextAt = 0; }
    private boolean owns(Player bot)
    { return moving && bot.isMoving() && bot.getAI().getIntention() == Intention.MOVE_TO
        && Math.hypot((double)bot.getXdestination()-x,(double)bot.getYdestination()-y)<5; }
    void stop()
    {
        Player bot=actor.get();
        if(bot!=null && !bot.isCastingNow() && !bot.isCastingSimultaneouslyNow() && owns(bot)) bot.getAI().setIntentionIdle();
        actor.clear();moving=false;
    }
    boolean tick(Player bot,Player enemy,long time,boolean required)
    {
        if(!required) { stop();return false; }
        if(bot.getClient()!=null)return false;
        if(bot.isCastingNow()||bot.isCastingSimultaneouslyNow())return true;
        if(actor.get()!=bot){stop();actor=new WeakReference<>(bot);nextAt=0;}
        if(bot.isAttackingNow())bot.abortAttack();
        if(bot.getAI().getIntention()==Intention.ATTACK)bot.getAI().setIntentionIdle();
        if(bot.isMovementDisabled()||bot.isOutOfControl()||bot.isAllSkillsDisabled()){stop();return true;}
        if(owns(bot)||time<nextAt)return true;
        nextAt=time+400;
        int[] center=ArenaCombatRules.center(bot);
        if(center==null)return true;
        Location p=ArenaCombatRules.kitePoint(bot,enemy,120);
        if(p==null)return true;
        double dx=(double)p.getX()-center[0],dy=(double)p.getY()-center[1],r=Math.hypot(dx,dy);
        if(r>449)p=new Location(center[0]+(int)Math.round(dx*449/r),center[1]+(int)Math.round(dy*449/r),p.getZ());
        if(!GeoEngine.getInstance().canMoveToTarget(bot.getX(),bot.getY(),bot.getZ(),p.getX(),p.getY(),p.getZ(),bot.getInstanceId()))return true;
        bot.getAI().setIntentionMoveTo(p);x=p.getX();y=p.getY();moving=true;
        return true; // blocked terrain must not fall through to offensive/parent chase
    }
}
