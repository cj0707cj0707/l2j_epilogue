/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;
import org.l2jmobius.gameserver.entity.actor.instance.FortCommander;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Fear effect implementation.
 * @author littlecrow
 */
public class Fear extends AbstractEffect
{
    /** Destination and step limits in server coordinate units; never re-anchor on ticks. */
    static final int WANDER_RADIUS = 100;
    private static final int MAX_Z_CHANGE = 64;
    // Weak recipients; values never retain recipients/effector actors. All overlapping
    // Fear handlers share the first active origin, including different abnormal slots.
    private static final Map<Creature, Wander> WANDERS = new WeakHashMap<>();
    private record Source(Fear handler, int effectorId, int skillId) { }
    private static final class Wander
    {
        final int x, y, z, instance;
        final Map<Source, Integer> sources = new HashMap<>();
        volatile boolean retired;
        boolean ownsMove;
        int moveX, moveY, moveZ;
        Wander(Creature actor)
        {
            x = actor.getX(); y = actor.getY(); z = actor.getZ(); instance = actor.getInstanceId();
        }
    }

    private Source source(Creature effector, Skill skill)
    {
        return new Source(this, effector == null ? 0 : effector.getObjectId(), skill.getId());
    }

    private static boolean bounded(Creature actor)
    {
        return actor.isPlayer() || actor.isSummon();
    }

    private static Wander current(Creature actor)
    {
        synchronized (WANDERS) { return WANDERS.get(actor); }
    }

    private Wander begin(Creature effector, Creature actor, Skill skill)
    {
        final Source source = source(effector, skill);
        for (;;)
        {
            Wander state;
            synchronized (WANDERS)
            {
                state = WANDERS.get(actor);
                if (state == null || state.retired)
                {
                    state = new Wander(actor);
                    WANDERS.put(actor, state);
                }
            }
            synchronized (state)
            {
                if (state.retired) continue;
                state.sources.merge(source, 1, Integer::sum);
                return state;
            }
        }
    }

    /** No global registry lock is held while invoking actor/AI/geodata code. */
    private static void stopOwned(Creature actor, Wander state)
    {
        if (state.ownsMove && actor.hasAI() && actor.getInstanceId() == state.instance
            && actor.getAI().getIntention() == Intention.MOVE_TO
            && actor.getXdestination() == state.moveX && actor.getYdestination() == state.moveY
            && actor.getZdestination() == state.moveZ)
        {
            if (!actor.isCastingNow())
            {
                // notifyActionCancel itself aborts casts in this core.
                actor.getAI().notifyActionCancel();
                actor.getAI().setIntentionIdle();
            }
            actor.stopMove(null);
        }
        state.ownsMove = false;
    }

    static boolean inside(int x, int y, int originX, int originY)
    {
        return Math.hypot((double)x - originX, (double)y - originY) <= WANDER_RADIUS;
    }

    private static void wander(Creature actor, Wander state)
    {
        if (state.retired || !actor.hasAI() || !actor.isSpawned() || actor.isTeleporting() || actor.isAlikeDead()
            || actor.getInstanceId() != state.instance || Math.abs((long)actor.getZ() - state.z) > MAX_Z_CHANGE
            || !inside(actor.getX(), actor.getY(), state.x, state.y))
        {
            stopOwned(actor, state);
            return; // External teleport/knockback: never chase the old origin or silently re-anchor.
        }
        if (actor.isMovementDisabled() || actor.isAllSkillsDisabled() || actor.isCastingNow())
        {
            stopOwned(actor, state);
            return; // Root/stun/sleep/paralysis/real uninterruptible casts retain their restrictions.
        }
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 12; attempt++)
        {
            final double angle = random.nextDouble(Math.PI * 2);
            final double radius = 20 + random.nextDouble(WANDER_RADIUS - 21);
            final int x = state.x + (int)Math.round(Math.cos(angle) * radius);
            final int y = state.y + (int)Math.round(Math.sin(angle) * radius);
            // Limit individual segments too, not just the circle around the fixed origin.
            if (!inside(x, y, state.x, state.y) || !inside(x, y, actor.getX(), actor.getY())
                || Math.hypot((double)x - actor.getX(), (double)y - actor.getY()) < 10) continue;
            final Location point = GeoEngine.getInstance().getValidLocation(actor.getX(), actor.getY(), actor.getZ(), x, y, actor.getZ(), state.instance);
            if (point == null || !inside(point.getX(), point.getY(), state.x, state.y)
                || !inside(point.getX(), point.getY(), actor.getX(), actor.getY())
                || Math.abs((long)point.getZ() - state.z) > MAX_Z_CHANGE
                || Math.hypot((double)point.getX() - actor.getX(), (double)point.getY() - actor.getY()) < 10
                || !GeoEngine.getInstance().canMoveToTarget(actor.getX(), actor.getY(), actor.getZ(), point.getX(), point.getY(), point.getZ(), state.instance)) continue;
            if (!actor.isPet()) actor.setRunning(); // Same ordinary run speed as the core Fear path.
            actor.getAI().setIntentionMoveTo(point);
            state.moveX = point.getX(); state.moveY = point.getY(); state.moveZ = point.getZ();
            state.ownsMove = actor.getAI().getIntention() == Intention.MOVE_TO
                && actor.getXdestination() == state.moveX && actor.getYdestination() == state.moveY
                && actor.getZdestination() == state.moveZ;
            return;
        }
        stopOwned(actor, state); // No safe short step: keep fear/control, do not request a long fallback.
    }

	public Fear(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer() || effected.isSummon() || (effected.isAttackable() && //
			!((effected instanceof Defender) || (effected instanceof FortCommander) || //
				(effected instanceof SiegeFlag) || (effected.getTemplate().getRace() == Race.SIEGE_WEAPON)));
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.FEAR.getMask();
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.FEAR;
	}
	
	@Override
	public int getTicks()
	{
		return 5;
	}
	
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill)
	{
        if (!bounded(effected))
        {
            effected.getAI().notifyActionAfraid(effector, false); // NPC/monster behavior unchanged.
            return false;
        }
        final Wander state = current(effected);
        if (state != null)
        {
            synchronized (state)
            {
                if (!state.retired && state.sources.containsKey(source(effector, skill))) wander(effected, state);
            }
        }
        return false;
	}
	
    @Override
    public void onStart(Creature effector, Creature effected, Skill skill)
    {
        if (!bounded(effected))
        {
            if (effected.isCastingNow() && effected.canAbortCast()) effected.abortCast();
            effected.getAI().notifyActionAfraid(effector, true); // Legacy NPC path.
            return;
        }
        // Balance callbacks even if this application cannot move. Never create AI.
        final Wander state = begin(effector, effected, skill);
        synchronized (state)
        {
            if (state.retired || !state.sources.containsKey(source(effector, skill))) return;
            if (!effected.hasAI() || !effected.isSpawned() || effected.isTeleporting() || effected.isAlikeDead()
                || effected.getInstanceId() != state.instance) return;
            if (effected.isCastingNow() && effected.canAbortCast()) effected.abortCast();
            if (!effected.isCastingNow())
            {
                // Remove the pre-fear attack/follow/saved move, not the FEAR flag.
                effected.abortAttack();
                effected.getAI().notifyActionCancel();
                effected.getAI().setIntentionIdle();
            }
            effected.stopMove(null);
            if (effected.isPlayer()) effected.asPlayer().setQueuedSkill(null, false, false);
            wander(effected, state);
        }
    }

    @Override
    public void onExit(Creature effector, Creature effected, Skill skill)
    {
        if (!bounded(effected)) return;
        final Wander state = current(effected);
        if (state == null) return;
        synchronized (state)
        {
            final Source source = source(effector, skill);
            final Integer count = state.sources.get(source);
            if (count == null) return; // An unregistered tuple must not stop another fear.
            if (count > 1) state.sources.put(source, count - 1);
            else state.sources.remove(source);
            if (!state.sources.isEmpty()) return;
            state.retired = true;
            stopOwned(effected, state);
        }
        synchronized (WANDERS)
        {
            if (WANDERS.get(effected) == state) WANDERS.remove(effected);
        }
    }
}
