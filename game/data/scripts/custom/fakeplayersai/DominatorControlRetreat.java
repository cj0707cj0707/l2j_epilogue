package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

/** Own live root / Hammer Crush stun on melee, never a successful request alone. */
final class DominatorControlRetreat
{
    private WeakReference<BuffInfo> effect = new WeakReference<>(null);
    private boolean finished, moving;
    private int moveX, moveY;
    private long retryAt;
    void reset() { effect.clear(); finished = false; moving = false; retryAt = 0; }

    static BuffInfo control(Player bot, Player enemy)
    {
        if (bot.getClient() != null || bot.getPlayerClass().getId() != 115) return null;
        for (BuffInfo info : enemy.getEffectList().getEffects())
        {
            if (info == null || info.isRemoved() || !info.isInUse() || info.getEffector() != bot
                || info.getEffected() != enemy || info.getSkill() == null || info.getEffects() == null
                || enemy.getEffectList().getBuffInfoByAbnormalType(info.getSkill().getAbnormalType()) != info) continue;
            for (var e : info.getEffects())
                if (!e.isInstant() && (enemy.isRooted() && e.getEffectType() == EffectType.ROOT && (e.getEffectFlags() & EffectFlag.ROOTED.getMask()) != 0
                    || enemy.isStunned() && info.getSkill().getId() == 260 && e.getEffectType() == EffectType.STUN && (e.getEffectFlags() & EffectFlag.STUNNED.getMask()) != 0)) return info;
        }
        return null;
    }
    /** About200 from enemy on the centerward ray; center-coincident target uses current side. */
    static Location goal(int x, int y, int z, int ex, int ey, int cx, int cy)
    {
        double dx = (double)cx - ex, dy = (double)cy - ey, length = Math.hypot(dx, dy);
        if (length < 1) { dx = (double)x - ex; dy = (double)y - ey; length = Math.hypot(dx, dy); }
        if (length < 1) { dx = 1; dy = 0; length = 1; }
        double tx = ex + dx / length * 199, ty = ey + dy / length * 199;
        double r = Math.hypot(tx - cx, ty - cy);
        if (r > 449) { tx = cx + (tx - cx) / r * 449; ty = cy + (ty - cy) / r * 449; }
        return new Location((int)Math.round(tx), (int)Math.round(ty), z);
    }
    private boolean owns(Player bot)
    {
        return moving && bot.isMoving() && bot.getAI().getIntention() == Intention.MOVE_TO
            && Math.hypot((double)bot.getXdestination() - moveX, (double)bot.getYdestination() - moveY) < 5;
    }
    private void stop(Player bot) { if (owns(bot)) bot.getAI().setIntentionIdle(); moving = false; }
    /** Returns true only while owning an ordinary accepted run. Restriction remains even if blocked. */
    boolean tick(Player bot, BuffInfo live, long time, int[] center)
    {
        if (live == null) { stop(bot); reset(); return false; }
        if (effect.get() != live) { stop(bot); reset(); effect = new WeakReference<>(live); }
        if (finished || center == null || bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || bot.isOutOfControl()) return false;
        if (bot.isMovementDisabled()) { stop(bot); return false; }
        Player enemy = live.getEffected().asPlayer();
        Location end = goal(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), center[0], center[1]);
        double inward = ((double)bot.getX() - enemy.getX()) * ((double)center[0] - enemy.getX())
            + ((double)bot.getY() - enemy.getY()) * ((double)center[1] - enemy.getY());
        if (bot.calculateDistance2D(end) < 4 || bot.calculateDistance2D(enemy) >= 198 && inward >= 0
            && Math.hypot((double)bot.getX() - center[0], (double)bot.getY() - center[1]) <= 450)
        { stop(bot); finished = true; return false; }
        if (owns(bot)) return true;
        if (time < retryAt) return false;
        retryAt = time + 500;
        if (!GeoEngine.getInstance().canMoveToTarget(bot.getX(),bot.getY(),bot.getZ(),end.getX(),end.getY(),end.getZ(),bot.getInstanceId())) return false;
        bot.getAI().setIntentionMoveTo(end); moveX = end.getX(); moveY = end.getY(); moving = true;
        if (owns(bot)) return true;
        moving = false; return false;
    }
}
