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

public final class OlympiadChase
{
	static final double MAX_LEAD_SECONDS = 1.25;
	static final double MAX_LEAD_DISTANCE = 300;
	private static final double REPATH_DISTANCE = 40;
	private static final long MIN_REPATH_NANOS = 250_000_000L;
	private static final Map<Integer, ChaseState> STATES = new HashMap<>();

	private record ChaseState(int gameId, int enemyId, int x, int y, int z, long issuedAt)
	{
	}

	private OlympiadChase()
	{
	}

	/** Serialized with the 2-second combat driver, so both callers share one route owner. */
	public static synchronized void update(Player bot, Player enemy, int holdRange, boolean attackInRange, int arenaX, int arenaY, double arenaRadius)
	{
		final int id = bot.getObjectId();
		if ((enemy == null) || (bot == enemy) || !bot.isInOlympiadMode() || !bot.isOlympiadStart()
			|| !enemy.isInOlympiadMode() || !enemy.isOlympiadStart() || (bot.getOlympiadGameId() < 0)
			|| (bot.getOlympiadGameId() != enemy.getOlympiadGameId()) || (bot.getInstanceId() != enemy.getInstanceId())
			|| bot.isAlikeDead() || enemy.isAlikeDead() || bot.isTeleporting() || enemy.isTeleporting()
			|| !enemy.isAutoAttackable(bot) || !inside(bot.getX(), bot.getY(), arenaX, arenaY, arenaRadius)
			|| !inside(enemy.getX(), enemy.getY(), arenaX, arenaY, arenaRadius))
		{
			STATES.remove(id);
			return;
		}

		// Never replace a live/queued cast, fear/confusion movement, root, stun, etc.
		if (bot.isCastingNow() || bot.isAttackingNow() || bot.isAllSkillsDisabled() || (bot.getAI().getIntention() == Intention.CAST) || bot.isMovementDisabled() || bot.isOutOfControl())
		{
			return;
		}
		ChaseState previous = STATES.get(id);
		if ((previous != null) && ((previous.gameId() != bot.getOlympiadGameId()) || (previous.enemyId() != enemy.getObjectId())))
		{
			STATES.remove(id);
			previous = null;
		}

		if (bot.calculateDistance2D(enemy) <= holdRange)
		{
			STATES.remove(id);
			// Stop only OUR old move, never a retreat or the core's attack/cast approach.
			if ((previous != null) && (bot.getAI().getIntention() == Intention.MOVE_TO) && ownsMove(bot, previous))
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

		// Move a little inside the engagement threshold, avoiding a stop just outside it.
		final int stopRange = Math.max(0, holdRange - 50);
		final boolean enemyMoving = enemy.isMoving() && !enemy.isMovementDisabled();
		final Location predicted = intercept(bot.getX(), bot.getY(), bot.getMoveSpeed(), enemy.getX(), enemy.getY(), enemy.getZ(),
			enemyMoving ? enemy.getXdestination() : enemy.getX(), enemyMoving ? enemy.getYdestination() : enemy.getY(),
			enemyMoving ? enemy.getMoveSpeed() : 0, stopRange);
		Location point = walkable(bot, predicted, arenaX, arenaY, arenaRadius) ? predicted : null;
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
		if (point == null)
		{
			// Neither segment is safe. Stop only our route; leave normal pathfinding alone.
			STATES.remove(id);
			if ((previous != null) && (bot.getAI().getIntention() == Intention.MOVE_TO) && ownsMove(bot, previous))
			{
				bot.getAI().setIntentionIdle();
			}
			return;
		}

		final long now = System.nanoTime();
		if ((previous != null) && ((now - previous.issuedAt()) < MIN_REPATH_NANOS))
		{
			return;
		}
		final boolean changed = (previous == null) || (Math.hypot((double) point.getX() - previous.x(), (double) point.getY() - previous.y()) >= REPATH_DISTANCE)
			|| (Math.abs(point.getZ() - previous.z()) > 50);
		if (changed || !bot.isMoving() || (previous != null && !ownsMove(bot, previous)))
		{
			bot.getAI().setIntentionMoveTo(point);
			STATES.put(id, new ChaseState(bot.getOlympiadGameId(), enemy.getObjectId(), point.getX(), point.getY(), point.getZ(), now));
		}
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
