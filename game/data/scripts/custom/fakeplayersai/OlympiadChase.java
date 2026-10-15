/*
 * Olympiad-only pursuit. Reads the opponent's current server movement order,
 * predicts a bounded intercept and moves through the normal AI/geodata APIs.
 * No speed bonus, teleport, crowd-control bypass or client modification.
 */
package custom.fakeplayersai;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;

public final class OlympiadChase
{
	static final double MAX_LEAD_SECONDS = 1.25;
	static final double MAX_LEAD_DISTANCE = 300;
	private static final double REPATH_DISTANCE = 40;
	private static final long MIN_REPATH_NANOS = 250_000_000L;
	private static final Map<Integer, ChaseState> STATES = new HashMap<>();
	private static final Map<Integer, Progress> PROGRESS = new HashMap<>();
	private static final Map<Integer, Engagement> ENGAGED = new HashMap<>();
	// IDs only: no Player references retained after a match/despawn.
	private static final Map<Integer, Engagement> ENGINE_HOLDS = new HashMap<>();
	private static final Map<Integer, Integer> TRAINING_PAIRS = new HashMap<>();
	private static final long STUCK_NANOS = 1_500_000_000L;

	private record Engagement(int gameId, int enemyId)
	{
	}

	private record Progress(int x, int y, long since)
	{
	}


	private record ChaseState(int gameId, int enemyId, int x, int y, int z, long issuedAt)
	{
	}

	private OlympiadChase()
	{
	}

	/** Serialized with the 2-second combat driver, so both callers share one route owner. */
	public static synchronized void update(Player bot, Player enemy, int holdRange, boolean attackInRange, int arenaX, int arenaY, double arenaRadius)
	{
		update(bot, enemy, holdRange, attackInRange, arenaX, arenaY, arenaRadius, true);
	}

	/** The berserker phase driver owns its engines; shared pursuit must not restart them. */
	public static synchronized void update(Player bot, Player enemy, int holdRange, boolean attackInRange, int arenaX, int arenaY, double arenaRadius, boolean manageEngines)
	{
		updateInternal(bot, enemy, holdRange, attackInRange, arenaX, arenaY, arenaRadius, manageEngines, false);
	}

	public static synchronized void registerTrainingPair(Player bot, Player enemy)
	{
		if (bot.getClient() != null || bot.isInOlympiadMode() || enemy.isInOlympiadMode() || bot.getInstanceId() <= 0 || bot.getInstanceId() != enemy.getInstanceId())
		{
			throw new IllegalArgumentException("Invalid training pair");
		}
		forget(bot.getObjectId());
		TRAINING_PAIRS.put(bot.getObjectId(), enemy.getObjectId());
	}

	public static synchronized void unregisterTrainingPair(int botId)
	{
		TRAINING_PAIRS.remove(botId);
		forget(botId);
	}

	public static synchronized void updateTraining(Player bot, Player enemy, int holdRange, boolean attackInRange, int arenaX, int arenaY, double arenaRadius)
	{
		updateInternal(bot, enemy, holdRange, attackInRange, arenaX, arenaY, arenaRadius, false, true);
	}

	private static void updateInternal(Player bot, Player enemy, int holdRange, boolean attackInRange, int arenaX, int arenaY, double arenaRadius, boolean manageEngines, boolean training)
	{
        if (!training && !OlympiadCombatGate.ready(bot))
        {
            forget(bot.getObjectId());
            OlympiadCombatGate.hold(bot);
            return;
        }
		final int id = bot.getObjectId();
		final long now = System.nanoTime();
		if ((enemy == null) || (bot == enemy) || (training ? (!Integer.valueOf(enemy.getObjectId()).equals(TRAINING_PAIRS.get(id)) || bot.getClient() != null
				|| bot.isInOlympiadMode() || enemy.isInOlympiadMode() || bot.getInstanceId() <= 0)
				: (!bot.isInOlympiadMode() || !bot.isOlympiadStart() || !enemy.isInOlympiadMode() || !enemy.isOlympiadStart()
					|| bot.getOlympiadGameId() < 0 || bot.getOlympiadGameId() != enemy.getOlympiadGameId()))
			|| (bot.getInstanceId() != enemy.getInstanceId())
			|| bot.isAlikeDead() || enemy.isAlikeDead() || bot.isTeleporting() || enemy.isTeleporting()
			|| !enemy.isAutoAttackable(bot) || !inside(bot.getX(), bot.getY(), arenaX, arenaY, arenaRadius)
			|| !inside(enemy.getX(), enemy.getY(), arenaX, arenaY, arenaRadius))
		{
			forget(id);
			return;
		}

		ChaseState previous = STATES.get(id);
		if ((previous != null) && ((previous.gameId() != bot.getOlympiadGameId()) || (previous.enemyId() != enemy.getObjectId())))
		{
			forget(id);
			previous = null;
		}

		final Engagement held = ENGINE_HOLDS.get(id);
		if (!manageEngines || ((held != null) && (held.gameId() != bot.getOlympiadGameId() || held.enemyId() != enemy.getObjectId())))
		{
			// Drop the lease on a context change. Only a normal re-arm may start a new owner.
			ENGINE_HOLDS.remove(id);
		}
		final Engagement engaged = ENGAGED.get(id);
		if ((engaged != null) && (engaged.gameId() != bot.getOlympiadGameId() || engaged.enemyId() != enemy.getObjectId()))
		{
			ENGAGED.remove(id);
		}
		final boolean visible = GeoEngine.getInstance().canSeeTarget(bot, enemy);
		final double distanceToEnemy = bot.calculateDistance2D(enemy);
		if (visible && ((distanceToEnemy <= holdRange) || (attackInRange && ENGAGED.containsKey(id) && distanceToEnemy <= holdRange + 60)))
		{
			releaseEngines(bot);
			forget(id);
			ENGAGED.put(id, new Engagement(bot.getOlympiadGameId(), enemy.getObjectId())); // Range hysteresis: do not shuffle at 499/501 every tick.
			// Stop only OUR old move, never a retreat or the core's attack/cast approach.
			if (!busy(bot) && (previous != null) && (bot.getAI().getIntention() == Intention.MOVE_TO) && ownsMove(bot, previous))
			{
				if (attackInRange && (bot.getTarget() == enemy))
				{
					bot.getAI().setIntentionAttack(enemy);
				}
				else
				{
					bot.getAI().setIntentionIdle();
				}
			}
			return;
		}

		ENGAGED.remove(id);
		if (manageEngines && (ENGINE_HOLDS.containsKey(id) || bot.isAutoPlaying()))
		{
			ENGINE_HOLDS.put(id, new Engagement(bot.getOlympiadGameId(), enemy.getObjectId()));
			// Removing from the pool is necessary: AutoPlay does NOT test isAutoPlaying().
			// AutoUse remains registered for potions/items, but its offensive rotation tests it.
			AutoPlayTaskManager.getInstance().stopAutoPlay(bot);
			bot.setAutoPlaying(false);
			// Self-buffs are processed even with autoPlay=false. Do not schedule new casts
			// during pursuit; active effects and an already running real cast are untouched.
			bot.getAutoUseSettings().getAutoBuffs().clear();
		}
		if (busy(bot))
		{
			PROGRESS.remove(id); // A real cast/CC pause must never count as being stuck.
			return;
		}
		// CAST with !isCastingNow is PlayerAI's walk-into-range state, not a live cast.
		// Normal MOVE_TO replaces that follow order without aborting/forging a cast.

		// Move a little inside the engagement threshold, avoiding a stop just outside it.
		final int stopRange = visible ? Math.max(0, holdRange - 50) : 50;
		final boolean enemyMoving = enemy.isMoving() && !enemy.isMovementDisabled();
		final Location predicted = intercept(bot.getX(), bot.getY(), bot.getMoveSpeed(), enemy.getX(), enemy.getY(), enemy.getZ(),
			enemyMoving ? enemy.getXdestination() : enemy.getX(), enemyMoving ? enemy.getYdestination() : enemy.getY(),
			enemyMoving ? enemy.getMoveSpeed() : 0, stopRange);
		Location point = walkable(bot, predicted, arenaX, arenaY, arenaRadius) ? predicted : null;
		final Progress progress = PROGRESS.get(id);
		if ((progress == null) || Math.hypot((double) bot.getX() - progress.x(), (double) bot.getY() - progress.y()) >= 20)
		{
			PROGRESS.put(id, new Progress(bot.getX(), bot.getY(), now));
		}
		else if (bot.isMoving() && (previous != null) && ownsMove(bot, previous) && ((now - progress.since()) >= STUCK_NANOS))
		{
			// A short checked sidestep, not a teleport or a repeated run into the same wall.
			point = sideStep(bot, predicted, arenaX, arenaY, arenaRadius);
			PROGRESS.put(id, new Progress(bot.getX(), bot.getY(), now));
		}

		if (point == null)
		{
			// No shortcut through a wall: try the ordinary current-position approach.
			final Location direct = intercept(bot.getX(), bot.getY(), bot.getMoveSpeed(), enemy.getX(), enemy.getY(), enemy.getZ(),
				enemy.getX(), enemy.getY(), 0, stopRange);
			if (walkable(bot, direct, arenaX, arenaY, arenaRadius))
			{
				point = direct;
			}
		}
		if (point == null && !visible)
		{
			// Same range is not enough behind a pillar: try a short checked flank.
			point = sideStep(bot, predicted, arenaX, arenaY, arenaRadius);
		}
		if (point == null)
		{
			// Neither segment is safe. Stop only our route; leave normal pathfinding alone.
			releaseEngines(bot);
			forget(id);
			if (!busy(bot) && (previous != null) && (bot.getAI().getIntention() == Intention.MOVE_TO) && ownsMove(bot, previous))
			{
				bot.getAI().setIntentionIdle();
			}
			return;
		}

		if ((previous != null) && ((now - previous.issuedAt()) < MIN_REPATH_NANOS))
		{
			return;
		}
		final boolean changed = (previous == null) || (Math.hypot((double) point.getX() - previous.x(), (double) point.getY() - previous.y()) >= REPATH_DISTANCE)
			|| (Math.abs(point.getZ() - previous.z()) > 50);
		if (changed || !bot.isMoving() || (previous != null && !ownsMove(bot, previous)))
		{
			bot.getAI().setIntentionMoveTo(point);
			final ChaseState issued = new ChaseState(bot.getOlympiadGameId(), enemy.getObjectId(), point.getX(), point.getY(), point.getZ(), now);
			if ((bot.getAI().getIntention() == Intention.MOVE_TO) && ownsMove(bot, issued))
			{
				STATES.put(id, issued); // Never cache a refused/saved intention as a running route.
			}
			else
			{
				STATES.remove(id);
			}
		}
	}

	private static boolean busy(Player bot)
	{
		return bot.isCastingNow() || bot.isAttackingNow() || bot.isAllSkillsDisabled() || bot.isMovementDisabled() || bot.isOutOfControl();
	}

	public static synchronized boolean isPursuing(int objectId)
	{
		return ENGINE_HOLDS.containsKey(objectId);
	}

	/** Called by setup/repair too: those paths must not overwrite a pursuit lease. */
	public static synchronized void startAutoPlayUnlessPursuing(Player bot)
	{
		if (!ENGINE_HOLDS.containsKey(bot.getObjectId()) && !BotTactics.isOwned(bot.getObjectId())
            && (!bot.isInOlympiadMode() || OlympiadCombatGate.ready(bot)))
		{
			AutoPlayTaskManager.getInstance().startAutoPlay(bot);
		}
	}

	private static void releaseEngines(Player bot)
	{
		if ((ENGINE_HOLDS.remove(bot.getObjectId()) != null) && !BotTactics.isOwned(bot.getObjectId())
            && (!bot.isInOlympiadMode() || OlympiadCombatGate.ready(bot)))
		{
			AutoPlayTaskManager.getInstance().startAutoPlay(bot);
		}
	}

	/** Compact state for the existing 30-second olympiad log, not per-tick spam. */
	public static synchronized String describe(int objectId)
	{
		final ChaseState route = STATES.get(objectId);
		return "v2:held=" + ENGINE_HOLDS.containsKey(objectId) + ",point=" + (route == null ? "none" : route.x() + ":" + route.y());
	}

	private static Location sideStep(Player bot, Location toward, int arenaX, int arenaY, double radius)
	{
		final double dx = (double) toward.getX() - bot.getX();
		final double dy = (double) toward.getY() - bot.getY();
		final double distance = Math.hypot(dx, dy);
		if (distance < 1)
		{
			return null;
		}
		Location best = null;
		double bestDistance = Double.MAX_VALUE;
		for (double angle : new double[] { Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2 })
		{
			final double ux = dx / distance;
			final double uy = dy / distance;
			final int x = bot.getX() + (int) Math.round(160 * (ux * Math.cos(angle) - uy * Math.sin(angle)));
			final int y = bot.getY() + (int) Math.round(160 * (ux * Math.sin(angle) + uy * Math.cos(angle)));
			final Location candidate = new Location(x, y, bot.getZ());
			final double remaining = Math.hypot((double) toward.getX() - x, (double) toward.getY() - y);
			if (remaining <= distance + 30 && remaining < bestDistance && walkable(bot, candidate, arenaX, arenaY, radius))
			{
				best = candidate;
				bestDistance = remaining;
			}
		}
		return best;
	}

	private static boolean ownsMove(Player bot, ChaseState state)
	{
		return bot.isMoving() && (Math.hypot((double) bot.getXdestination() - state.x(), (double) bot.getYdestination() - state.y()) < REPATH_DISTANCE)
			&& (Math.abs(bot.getZdestination() - state.z()) <= 50);
	}

	private static boolean walkable(Player bot, Location point, int arenaX, int arenaY, double radius)
	{
		return inside(point.getX(), point.getY(), arenaX, arenaY, radius)
			&& GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), point.getX(), point.getY(), point.getZ(), bot.getInstanceId());
	}

	private static boolean inside(int x, int y, int centerX, int centerY, double radius)
	{
		return Math.hypot((double) x - centerX, (double) y - centerY) <= radius;
	}

	public static synchronized void forget(int objectId)
	{
		STATES.remove(objectId);
		PROGRESS.remove(objectId);
		ENGAGED.remove(objectId);
		ENGINE_HOLDS.remove(objectId); // No restart on death, teleport, despawn or invalid match.
	}

	/** Pure geometry, package-visible for repeatable tests without a GameServer/DB. */
	static Location intercept(double bx, double by, double botSpeed, double ex, double ey, int z, double destinationX, double destinationY, double enemySpeed, double stopRange)
	{
		final double dx = destinationX - ex;
		final double dy = destinationY - ey;
		final double remaining = Math.hypot(dx, dy);
		final double speed = Math.max(0, enemySpeed);
		double px = ex;
		double py = ey;
		if ((remaining > 1) && (speed > 0) && (botSpeed > 0))
		{
			final double vx = (dx / remaining) * speed;
			final double vy = (dy / remaining) * speed;
			final double rx = ex - bx;
			final double ry = ey - by;
			// |relativePosition + velocity*t| = botSpeed*t + stopRange.
			final double a = (speed * speed) - (botSpeed * botSpeed);
			final double b = 2 * ((rx * vx) + (ry * vy) - (botSpeed * stopRange));
			final double c = (rx * rx) + (ry * ry) - (stopRange * stopRange);
			double time = MAX_LEAD_SECONDS;
			if (c <= 0)
			{
				time = 0;
			}
			else if (Math.abs(a) < 0.000001)
			{
				if (b < -0.000001)
				{
					time = Math.min(time, -c / b);
				}
			}
			else
			{
				final double discriminant = (b * b) - (4 * a * c);
				if (discriminant >= 0)
				{
					final double root = Math.sqrt(discriminant);
					final double t1 = (-b - root) / (2 * a);
					final double t2 = (-b + root) / (2 * a);
					if (t1 >= 0)
					{
						time = Math.min(time, t1);
					}
					if (t2 >= 0)
					{
						time = Math.min(time, t2);
					}
				}
			}
			final double lead = Math.min(remaining, Math.min(MAX_LEAD_DISTANCE, speed * time));
			px += (dx / remaining) * lead;
			py += (dy / remaining) * lead;
		}
		final double distance = Math.hypot(px - bx, py - by);
		final double scale = (distance > stopRange) ? ((distance - stopRange) / distance) : 0;
		return new Location((int) Math.round(bx + ((px - bx) * scale)), (int) Math.round(by + ((py - by) * scale)), z);
	}
}
