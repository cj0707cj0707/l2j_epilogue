package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/** One ordinary retreat per actually applied own sleep. Arena authority belongs to BotTactics. */
final class SleepRetreat
{
    // One-unit rounding margin INSIDE the existing450 kite circle, not the460 core wall.
    static final double RADIUS = 449;
    private WeakReference<BuffInfo> sleep = new WeakReference<>(null);
    private boolean finished, moving;
    private int moveX, moveY;
    private long retryAt;

    void reset() { sleep.clear(); finished = false; moving = false; retryAt = 0; }

    static BuffInfo ownSleep(Player bot, Player enemy)
    {
        int cid = bot.getPlayerClass().getId();
        if (bot.getClient() != null || (cid != 97 && cid != 115) || !enemy.isSleeping()) return null;
        for (BuffInfo info : enemy.getEffectList().getEffects())
        {
            if (info == null || info.isRemoved() || !info.isInUse() || info.getEffector() != bot
                || info.getEffected() != enemy || info.getSkill() == null || info.getEffects() == null
                || enemy.getEffectList().getBuffInfoByAbnormalType(info.getSkill().getAbnormalType()) != info) continue;
            for (var effect : info.getEffects())
                if (!effect.isInstant() && effect.getEffectType() == EffectType.SLEEP
                    && (effect.getEffectFlags() & EffectFlag.SLEEP.getMask()) != 0) return info;
        }
        return null;
    }

    static double distance(Player bot)
    {
        if (bot.getPlayerClass().getId() == 115) return 300;
        Skill manaBurn = bot.getKnownSkill(1398);
        return manaBurn == null ? 0 : Math.max(0, bot.getMagicalAttackRange(manaBurn));
    }

    /** Nearest point at the requested separation, or the farthest feasible boundary.
     * The segment between any two inside points stays inside the convex kite disk. */
    static Location goal(int x, int y, int z, int ex, int ey, int cx, int cy, double requested)
    {
        if (!Double.isFinite(requested) || requested <= 0) return null;
        double dx = (double) ex - cx, dy = (double) ey - cy, d = Math.hypot(dx, dy);
        double separation = Math.max(Math.max(0, d - RADIUS), Math.min(Math.max(0, requested - 1), d + RADIUS));
        double angle = Math.hypot((double)x - ex, (double)y - ey) < 1
            ? (d < 1 ? 0 : Math.atan2(-dy, -dx)) : Math.atan2((double)y - ey, (double)x - ex);
        double tx = ex + separation * Math.cos(angle), ty = ey + separation * Math.sin(angle);
        if (Math.hypot(tx - cx, ty - cy) > RADIUS && d > 0 && separation > 0)
        {
            double limit = Math.clamp((RADIUS * RADIUS - d * d - separation * separation) / (2 * d * separation), -1, 1);
            double base = Math.atan2(dy, dx), offset = Math.acos(limit);
            double ax = ex + separation * Math.cos(base + offset), ay = ey + separation * Math.sin(base + offset);
            double bx = ex + separation * Math.cos(base - offset), by = ey + separation * Math.sin(base - offset);
            if (Math.hypot(ax - x, ay - y) <= Math.hypot(bx - x, by - y)) { tx = ax; ty = ay; }
            else { tx = bx; ty = by; }
        }
        return new Location((int)Math.round(tx), (int)Math.round(ty), z);
    }

    private boolean ownsMove(Player bot)
    {
        return moving && bot.getAI().getIntention() == Intention.MOVE_TO && bot.isMoving()
            && Math.hypot((double)bot.getXdestination() - moveX, (double)bot.getYdestination() - moveY) < 5;
    }
    private void stopOwnedMove(Player bot)
    {
        if (ownsMove(bot)) bot.getAI().setIntentionIdle();
        moving = false;
    }

    boolean tick(Player bot, Player enemy, long time, int[] center)
    {
        BuffInfo effect = ownSleep(bot, enemy);
        if (effect == null) { stopOwnedMove(bot); reset(); return false; }
        if (effect != sleep.get())
        {
            stopOwnedMove(bot); reset(); sleep = new WeakReference<>(effect);
        }
        if (center == null || finished) return false;
        if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || bot.isOutOfControl()) return false;
        if (bot.isMovementDisabled()) { stopOwnedMove(bot); return false; }
        Location end = goal(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), center[0], center[1], distance(bot));
        if (end == null) return false;
        double need = Math.hypot((double)end.getX() - enemy.getX(), (double)end.getY() - enemy.getY());
        if (Math.hypot((double)bot.getX() - center[0], (double)bot.getY() - center[1]) <= 450
            && bot.calculateDistance2D(enemy) >= need - 2)
        {
            stopOwnedMove(bot); finished = true; return false;
        }
        if (ownsMove(bot)) return true; // own the complete run, not just a200ms micro-step
        if (time < retryAt) return false;
        retryAt = time + 500; // reserve before geodata/AI; blocked routes never spam per-tick
        for (Location p : new Location[]{end, new Location(center[0], center[1], bot.getZ())})
        {
            if (Math.hypot((double)p.getX() - bot.getX(), (double)p.getY() - bot.getY()) < 5
                || Math.hypot((double)p.getX() - center[0], (double)p.getY() - center[1]) > 450
                || !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), p.getX(), p.getY(), p.getZ(), bot.getInstanceId())) continue;
            bot.getAI().setIntentionMoveTo(p);
            moveX = p.getX(); moveY = p.getY(); moving = true;
            if (ownsMove(bot)) return true;
            moving = false;
        }
        return false; // safe ordinary rotation if terrain/AI/root prevents the retreat
    }
}
