package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/** Movement only against an invulnerable ranged target: melee approaches and holds,
 * ranged attempts the existing arena orbit. Never attacks, teleports or edits effects. */
final class InvulnerableRangedTarget
{
    private WeakReference<Player> actor = new WeakReference<>(null);
    private boolean moving;
    private int x, y;
    private long nextAt;
    private boolean owns(Player bot)
    {
        return moving && bot.isMoving() && bot.getAI().getIntention() == Intention.MOVE_TO
            && Math.hypot((double)bot.getXdestination()-x,(double)bot.getYdestination()-y)<5;
    }
    void stop()
    {
        Player bot=actor.get();
        if(bot!=null && !bot.isCastingNow() && !bot.isCastingSimultaneouslyNow() && owns(bot))bot.getAI().setIntentionIdle();
        actor.clear();moving=false;nextAt=0;
    }
    boolean tick(Player bot,Player enemy,boolean melee,long time)
    {
        if(bot.getClient()!=null)return false;
        if(bot.isCastingNow()||bot.isCastingSimultaneouslyNow())return true;
        if(bot.isMovementDisabled()||bot.isOutOfControl()||bot.isAllSkillsDisabled()){stop();return true;}
        double reach=bot.getPhysicalAttackRange()+bot.getTemplate().getCollisionRadius()+enemy.getTemplate().getCollisionRadius();
        if(melee&&bot.calculateDistance3D(enemy)<=reach){stop();return true;}
        int[] center=ArenaCombatRules.center(bot);
        if(center==null){stop();return true;}
        if(melee&&Math.hypot((double)enemy.getX()-center[0],(double)enemy.getY()-center[1])>ArenaCombatRules.attackRadius(bot))
        {stop();return true;}
        Location point=melee?new Location(enemy.getX(),enemy.getY(),enemy.getZ()):ArenaCombatRules.kitePoint(bot,enemy,120);
        if(point==null){stop();return true;}
        if(!melee)
        {
            double dx=(double)point.getX()-center[0],dy=(double)point.getY()-center[1],radius=Math.hypot(dx,dy);
            if(radius>449)point=new Location(center[0]+(int)Math.round(dx*449/radius),center[1]+(int)Math.round(dy*449/radius),point.getZ());
        }
        if(actor.get()!=bot){stop();actor=new WeakReference<>(bot);}
        // Preserve our route while the target/waypoint has not materially changed.
        if(owns(bot)&&(!melee||Math.hypot((double)point.getX()-x,(double)point.getY()-y)<40))return true;
        if(time<nextAt)return true;
        nextAt=time+400;
        if(!GeoEngine.getInstance().canMoveToTarget(bot.getX(),bot.getY(),bot.getZ(),point.getX(),point.getY(),point.getZ(),bot.getInstanceId()))
        {stop();nextAt=time+400;return true;}
        bot.getAI().setIntentionMoveTo(point);x=point.getX();y=point.getY();moving=true;
        return true; // failed/blocked movement never delegates an attack to the parent
    }
}
