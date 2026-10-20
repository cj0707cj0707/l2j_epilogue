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
    private int x, y;
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
        if (bot.calculateDistance2D(enemy) <= 40 && TacticsPolicy.behind(enemy.getX(), enemy.getY(), enemy.getHeading(), bot.getX(), bot.getY()))
        { stop(bot); return false; }
        if (bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) { stop(bot); return true; }
        if (owns(bot) || time < retryAt) return true;
        retryAt = time + 500;
        if (center == null || !GeoEngine.getInstance().canSeeTarget(bot, enemy) || Math.abs(bot.getZ() - enemy.getZ()) > 100) return true;
        double heading = enemy.getHeading() * (2 * Math.PI / 65536.0);
        for (double offset : new double[]{0, .35, -.35, .7, -.7})
            for (int range : new int[]{35, 30, 40})
            {
                double gx = enemy.getX() - range * Math.cos(heading + offset);
                double gy = enemy.getY() - range * Math.sin(heading + offset);
                if (Math.hypot(gx - center[0], gy - center[1]) > 449) continue;
                double dx = gx - bot.getX(), dy = gy - bot.getY(), len = Math.hypot(dx, dy);
                double scale = len > 160 ? 160 / len : 1;
                int px = (int)Math.round(bot.getX() + dx * scale), py = (int)Math.round(bot.getY() + dy * scale);
                if (len < 2 || Math.hypot((double)px - center[0], (double)py - center[1]) > 450
                    || !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), px, py, bot.getZ(), bot.getInstanceId())) continue;
                bot.getAI().setIntentionMoveTo(new Location(px, py, bot.getZ()));
                x = px; y = py; moving = true;
                if (owns(bot)) return true;
                moving = false;
            }
        return true; // no valid rear route: hold skills/autoattack, never fake a completed approach
    }
}
