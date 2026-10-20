package custom.fakeplayersai;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

/** Actual own358 STUN: reach the rear by ordinary movement BEFORE any strike. */
final class BluffRear
{
    private boolean moving;
    private int x, y, enemyX, enemyY, heading;
    private long moveAt;
    private long retryAt;
    static BuffInfo stun(Player bot, Player enemy)
    {
        if (bot == null || enemy == null || bot.getClient() != null || !enemy.isStunned()) return null;
        int cid = bot.getPlayerClass().getId();
        if (cid != 93 && cid != 101 && cid != 108) return null;
        for (BuffInfo info : enemy.getEffectList().getEffects())
        {
            if (info == null || info.isRemoved() || !info.isInUse() || info.getEffector() != bot
                || info.getEffected() != enemy || info.getSkill() == null || info.getSkill().getId() != 358
                || info.getEffects() == null || enemy.getEffectList().getBuffInfoByAbnormalType(info.getSkill().getAbnormalType()) != info) continue;
            for (var effect : info.getEffects())
                if (!effect.isInstant() && effect.getEffectType() == EffectType.STUN
                    && (effect.getEffectFlags() & EffectFlag.STUNNED.getMask()) != 0) return info;
        }
        return null;
    }
    static boolean targetStunned(Player bot)
    {
        return bot != null && bot.getTarget() != null && bot.getTarget().isPlayer()
            && stun(bot, bot.getTarget().asPlayer()) != null;
    }
    static boolean ready(Player bot,Player enemy)
    {
        return bot.calculateDistance2D(enemy)<=40 && Math.abs(bot.getZ()-enemy.getZ())<=100
            && TacticsPolicy.behind(enemy.getX(),enemy.getY(),enemy.getHeading(),bot.getX(),bot.getY());
    }
    static boolean skillAllowed(Player bot,Player enemy,org.l2jmobius.gameserver.mechanics.skill.Skill skill)
    {
        return stun(bot,enemy)==null || ready(bot,enemy) && (skill.getId()==531 || skill.isPhysical()&&BotTactics.directDamage(skill));
    }
    private boolean owns(Player bot)
    {
        return moving && bot.isMoving() && bot.getAI().getIntention() == Intention.MOVE_TO
            && Math.hypot((double)bot.getXdestination() - x, (double)bot.getYdestination() - y) < 5;
    }
    void stop(Player bot)
    {
        if (!bot.isCastingNow() && !bot.isCastingSimultaneouslyNow() && owns(bot)) bot.getAI().setIntentionIdle();
        moving = false; retryAt = 0;
    }
    boolean tick(Player bot, Player enemy, BuffInfo effect, long time, int[] center)
    {
        if (effect == null) { stop(bot); return false; }
        if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) return true;
        if (ready(bot, enemy))
        { stop(bot); return false; }
        if (bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) { stop(bot); return true; }
        // Native facing/location can change after Bluff. Never keep a stale route
        // forever if another intention or movement collision interrupted it.
        int turn=Math.abs(enemy.getHeading()-heading);turn=Math.min(turn,65536-turn);
        if (owns(bot) && Math.hypot(enemy.getX()-enemyX,enemy.getY()-enemyY)<10 && turn<2048 && time-moveAt<750) return true;
        if (owns(bot)) { stop(bot); }
        if (time < retryAt) return true;
        retryAt = time + 150;
        if (center == null || !GeoEngine.getInstance().canSeeTarget(bot, enemy) || Math.abs(bot.getZ() - enemy.getZ()) > 100) return true;
        double facing = enemy.getHeading() * (2 * Math.PI / 65536.0);
        double bearing = Math.atan2(bot.getY()-enemy.getY(),bot.getX()-enemy.getX());
        double rear=facing+Math.PI;
        double delta=Math.atan2(Math.sin(rear-bearing),Math.cos(rear-bearing));
        boolean around=Math.abs(delta)>Math.PI/3;
        // Circumvent the victim instead of drawing a straight chord through it.
        // Try both sides when the shortest arc is blocked by a wall/arena edge.
        double safe=Math.max(50,Math.min(85,bot.calculateDistance2D(enemy)));
        for (int direction : new int[]{delta<0?-1:1,delta<0?1:-1})
            for (int range : around ? new int[]{(int)safe,50,40} : new int[]{35,30,39})
            {
                double angle=around ? bearing+direction*Math.PI/3 : rear;
                int px=(int)Math.round(enemy.getX()+range*Math.cos(angle));
                int py=(int)Math.round(enemy.getY()+range*Math.sin(angle));
                double dx=px-bot.getX(),dy=py-bot.getY(),len=Math.hypot(dx,dy);
                if(len>160){px=bot.getX()+(int)Math.round(dx*160/len);py=bot.getY()+(int)Math.round(dy*160/len);}
                if (len<2 || Math.hypot((double)px-center[0],(double)py-center[1])>449
                    || !GeoEngine.getInstance().canMoveToTarget(bot.getX(),bot.getY(),bot.getZ(),px,py,bot.getZ(),bot.getInstanceId())) continue;
                bot.getAI().setIntentionMoveTo(new Location(px,py,bot.getZ()));
                x=px;y=py;moving=true;enemyX=enemy.getX();enemyY=enemy.getY();heading=enemy.getHeading();moveAt=time;
                if (owns(bot)) return true;
                moving=false;
            }
        return true; // no valid rear route: hold skills/autoattack, never fake a completed approach
    }
}
