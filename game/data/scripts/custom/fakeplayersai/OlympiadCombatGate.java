package custom.fakeplayersai;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

/** Server-state gate, not a timer or a substitute for door collision/geodata.
 * Call only for bots; no dependency on bot registration or tactical ownership.
 * Readiness is a conservative snapshot, not a transaction with the core thread. */
public final class OlympiadCombatGate
{
	private static final Logger LOGGER = Logger.getLogger(OlympiadCombatGate.class.getName());
	private static final AtomicBoolean WARNED = new AtomicBoolean();
	private static final Map<Integer, WeakReference<Object>> CLAIMS = new HashMap<>();
	private static final Map<Integer, WeakReference<Object>> SUMMON_CLAIMS = new HashMap<>();
	private static final Access ACCESS = access();

	private record Access(Field stadiums, Method doorIds, Field startTime, Field stadiumId, Field one, Field two, Method aborted) { }
	private record Match(Object game, Player one, Player two, int id) { }

	private OlympiadCombatGate() { }

	private static void warn(Throwable error)
	{
		if (WARNED.compareAndSet(false, true))
		{
			LOGGER.log(Level.WARNING, "FakePlayersAI: Olympiad gate read/hold failed; readiness stays closed", error);
		}
	}

	private static Access access()
	{
		try
		{
			final ClassLoader loader = Olympiad.class.getClassLoader();
			final Class<?> manager = Class.forName("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager", false, loader);
			final Class<?> stadium = Class.forName("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadStadium", false, loader);
			final Field stadiums = manager.getDeclaredField("STADIUMS");
			final Method doorIds = stadium.getDeclaredMethod("getDoorID");
			final Class<?> game = Class.forName("org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGame", false, loader);
			final Field startTime = game.getDeclaredField("_startTime");
			final Field stadiumId = game.getDeclaredField("_stadiumID");
			final Field one = game.getDeclaredField("_playerOne");
			final Field two = game.getDeclaredField("_playerTwo");
			final Method aborted = game.getDeclaredMethod("isAborted");
			if (!Modifier.isStatic(stadiums.getModifiers()) || !stadiums.getType().isArray()
				|| stadiums.getType().getComponentType() != stadium || doorIds.getReturnType() != int[].class
				|| startTime.getType() != long.class || stadiumId.getType() != int.class
				|| one.getType() != Player.class || two.getType() != Player.class || aborted.getReturnType() != boolean.class)
			{
				throw new NoSuchFieldException("Unsupported Olympiad core descriptor");
			}
			stadiumId.setAccessible(true);
			one.setAccessible(true);
			two.setAccessible(true);
			aborted.setAccessible(true);
			stadiums.setAccessible(true);
			doorIds.setAccessible(true); // Declaring stadium class is package-private in the actual jar.
			startTime.setAccessible(true);
			return new Access(stadiums, doorIds, startTime, stadiumId, one, two, aborted);
		}
		catch (ReflectiveOperationException | RuntimeException | LinkageError error)
		{
			warn(error);
			return null;
		}
	}

	private static Match match(Player bot) throws ReflectiveOperationException
	{
		if (ACCESS == null || bot == null || !bot.isOnline() || !bot.isInOlympiadMode() || bot.isAlikeDead() || bot.isTeleporting()) return null;
		final int id = bot.getOlympiadGameId();
		if (id < 0) return null;
		final Object game = Olympiad.getInstance().getOlympiadGames().get(id);
		if (game == null || Boolean.TRUE.equals(ACCESS.aborted.invoke(game)) || ACCESS.stadiumId.getInt(game) != id) return null;
		final Player one = (Player) ACCESS.one.get(game);
		final Player two = (Player) ACCESS.two.get(game);
		if (one == null || two == null || one == two || (one != bot && two != bot)) return null;
		if (!participant(one, id, bot.getInstanceId()) || !participant(two, id, bot.getInstanceId())) return null;
		return new Match(game, one, two, id);
	}

	private static boolean participant(Player player, int game, int instance)
	{
		return player.isOnline() && player.isInOlympiadMode() && !player.isAlikeDead() && !player.isTeleporting()
			&& player.getOlympiadGameId() == game && player.getInstanceId() == instance;
	}

	private static boolean current(Match match) throws ReflectiveOperationException
	{
		return Olympiad.getInstance().getOlympiadGames().get(match.id) == match.game && !Boolean.TRUE.equals(ACCESS.aborted.invoke(match.game))
			&& ACCESS.one.get(match.game) == match.one && ACCESS.two.get(match.game) == match.two
			&& participant(match.one, match.id, match.one.getInstanceId())
			&& participant(match.two, match.id, match.one.getInstanceId());
	}

	/** Both official participants started AND both actual server doors open. Unknown means false. */
	public static boolean ready(Player bot)
	{
		try
		{
			final Match match = match(bot);
			if (match == null || ACCESS == null || !match.one.isOlympiadStart() || !match.two.isOlympiadStart()
				|| ACCESS.startTime.getLong(match.game) <= 0) return false;
			final Object[] stadiums = (Object[]) ACCESS.stadiums.get(null);
			if (stadiums == null || match.id < 0 || match.id >= stadiums.length) return false;
			final Object stadium = stadiums[match.id];
			if (stadium == null) return false;
			final int[] ids = (int[]) ACCESS.doorIds.invoke(stadium);
			if (ids == null || ids.length != 2 || ids[0] <= 0 || ids[1] <= 0 || ids[0] == ids[1]) return false;
			final Door one = DoorData.getInstance().getDoor(ids[0]);
			final Door two = DoorData.getInstance().getDoor(ids[1]);
			return one != null && two != null && one.isOpen() && two.isOpen() && current(match)
				&& match.one.isOlympiadStart() && match.two.isOlympiadStart();
		}
		catch (ReflectiveOperationException | RuntimeException | LinkageError error)
		{
			warn(error);
			return false;
		}
	}

	/** Synchronous PRE_START_HOOKS only. No timer fallback, and no claim after a start attempt.
	 * Weak references avoid retaining an ended game's players; an active mapped game stays live. */
	public static synchronized boolean claimPreStart(Player bot)
	{
		return claim(bot, CLAIMS);
	}

	/** Separate claim: summon preparation must not consume the existing opener claim. */
	public static synchronized boolean claimSummonPreparation(Player bot)
	{
		return claim(bot, SUMMON_CLAIMS);
	}

	private static boolean claim(Player bot, Map<Integer, WeakReference<Object>> claims)
	{
		try
		{
			final Match match = match(bot);
			if (match == null || ACCESS == null || match.one.isOlympiadStart() || match.two.isOlympiadStart()
				|| ACCESS.startTime.getLong(match.game) != 0 || !current(match)) return false;
			final WeakReference<Object> previous = claims.get(bot.getObjectId());
			if (previous != null && previous.get() == match.game) return false;
			claims.put(bot.getObjectId(), new WeakReference<>(match.game));
			return true;
		}
		catch (ReflectiveOperationException | RuntimeException | LinkageError error)
		{
			warn(error);
			return false;
		}
	}

	public static synchronized void forget(int objectId)
	{
		CLAIMS.remove(objectId);
		SUMMON_CLAIMS.remove(objectId);
	}

	private static void attempt(Runnable action)
	{
		try { action.run(); }
		catch (RuntimeException | LinkageError error) { warn(error); }
	}

	private static void holdActor(Creature actor)
	{
		attempt(() -> actor.getAI().notifyActionCancel()); // PlayerAI clears saved post-cast intentions here.
		attempt(actor::abortAttack);
		attempt(actor::abortCast);
		attempt(() -> actor.getAI().setIntentionIdle());
		attempt(() -> actor.stopMove(null));
		attempt(() -> actor.setTarget(null));
	}

	/** Does not forget one-shot state and never re-arms engines. Safe to repeat during countdown/end. */
	public static void hold(Player bot)
	{
		if (bot == null) return;
		attempt(() -> AutoPlayTaskManager.getInstance().stopAutoPlay(bot));
		attempt(() -> AutoUseTaskManager.getInstance().stopAutoUseTask(bot));
		attempt(() -> bot.setQueuedSkill(null, false, false));
		holdActor(bot);
		attempt(() ->
		{
			final Summon summon = bot.getSummon();
			if (summon != null)
			{
				attempt(() -> summon.setFollowStatus(false));
				holdActor(summon);
			}
		});
	}
}
