/*
 * Fake Players AI - central bot manager: startup spawning, maintenance ticker,
 * olympiad registration, respawn and area control.
 */
package custom.fakeplayersai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncMul;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * @author FakePlayersAI
 */
public class BotManager
{
	private static final Logger LOGGER = Logger.getLogger(BotManager.class.getName());
	
	/** Arena centers of the olympiad stadia (mirror of the core OlympiadManager.STADIUMS coordinates). */
	private static final int[][] OLYMPIAD_STADIUM_CENTERS =
	{
		{ -88000, -252637, -3331 }, { -83760, -252637, -3331 }, { -79600, -252637, -3331 }, { -75648, -252637, -3331 },
		{ -88000, -249762, -3331 }, { -83760, -249762, -3331 }, { -79600, -249762, -3331 }, { -75648, -249762, -3331 },
		{ -88000, -246951, -3331 }, { -83760, -246951, -3331 }, { -79600, -246951, -3331 }, { -75648, -246951, -3331 },
		{ -88000, -244290, -3331 }, { -83760, -244290, -3331 }, { -79600, -244290, -3331 }, { -75648, -244290, -3331 },
		{ -88000, -241490, -3331 }, { -83760, -241490, -3331 }, { -79600, -241490, -3331 }, { -75648, -241490, -3331 },
		{ -88000, -238825, -3331 }, { -83760, -238825, -3331 },
	};
	
	public static final String REGISTRY_PATH = "data/scripts/custom/fakeplayersai/registry.properties";

	// ------------------------------------------------------------------
	// Olympiad login points + arena containment + dead match escape
	// (admin rule / bug reports of 04.10.2026)
	// ------------------------------------------------------------------

	/**
	 * Olympiad points of EVERY character and administrator at EVERY game entry
	 * (admin rule of 04.10.2026): on each login the noble's olympiad points are set
	 * to this value regardless of what the previous session left. Bots do not log in
	 * (clientless spawn) - they receive the same value at every server start through
	 * OlympiadResetPoints (kept equal to this constant).
	 */
	public static final int OLYMPIAD_LOGIN_POINTS = 200;

	/** Maximum 2D distance between an active match participant and its stadium center (the port positions are +-1200, the arena fits well inside). */
	private static final double OLYMPIAD_ARENA_MAX_DISTANCE = 2000;

	/**
	 * A bot whose match opponent stays missing/dead for this long is released from a
	 * DEAD match (the game task thread died - its cleanup never runs; a LIVE match
	 * always resolves a dead opponent within seconds through haveWinner).
	 */
	private static final long OLYMPIAD_DEAD_MATCH_RELEASE_MS = 60000;

	/**
	 * A bot continuously inside a STARTED match for longer than the battle period
	 * (OlympiadBattle = 360 s) plus a cleanup margin is released: a live game task
	 * always ends the match by then, anything longer is a dead task (frozen bots
	 * fighting forever or idling in the arena).
	 */
	private static final long OLYMPIAD_MAX_ACTIVE_MATCH_MS = 420000;

	/**
	 * objectId -> {firstSeenActiveMatchTs, opponentMissingSinceTs (0 = opponent present)}
	 * of the dead match escape watch. Entries exist only while the bot is inside a
	 * STARTED olympiad match and are removed on the first tick without it.
	 */
	private static final Map<Integer, long[]> OLY_MATCH_WATCH = new ConcurrentHashMap<>();

	/**
	 * Typed consumer for the GLOBAL ON_PLAYER_LOGIN registration. The listener
	 * contract of the core is Consumer<? extends IBaseEvent>, and a raw method
	 * reference does not typecheck against the captured wildcard, so the typed
	 * reference is captured in this variable first (same pattern as the core
	 * PremiumManager login listener).
	 */
	private static final Consumer<OnPlayerLogin> OLYMPIAD_LOGIN_LISTENER = BotManager::onPlayerLogin;

	// Charge window for the sonic/force charge classes (admin rules of 16.09.2026):
	// the charge REQUIREMENTS of the Duelist/Grand Khavatari skills are back in the
	// datapack (chargeConsume + Charges conditions), the charge builder skills are part
	// of the class ROTATIONS (Sonic Focus 8 for the Duelist, Focused Force 50 for the
	// Grand Khavatari), and the combat driver keeps the charge LEVEL of those bots inside
	// the window: below CHARGE_MIN it is refilled up to CHARGE_MAX instantly (the same
	// style as the olympiad buff set - the core auto-buff engine never fires under a
	// continuous auto-attack), above CHARGE_MAX it is trimmed back (a rotation builder
	// cast at the ceiling or a Maximum Focus/Sonic Focus from a //bot buff set would
	// otherwise sit at the retail cap of 8). Between the 2 s ticks the level may dip
	// lower while the rotation spends charges on attacks - exactly like a real player.
	private static final int SONIC_FOCUS_ID = 8; // Sonic Focus (Duelist line, +1 charge per cast)
	private static final int FOCUS_FORCE_ID = 50; // Focused Force (Grand Khavatari line, +1 charge per cast)
	private static final int CHARGE_MIN = 4; // admin rule: keep the charge level at 4 or above
	private static final int CHARGE_MAX = 7; // admin rule: keep the charge level at 7 or below

	// Chase-the-runner: melee bots re-path to the player's CURRENT coordinates on
	// every combat tick while the distance is above CHASE_RANGE, so a fleeing
	// player is run down as fast as possible (the auto skill rotation keeps
	// firing while closing). The move point is re-issued whenever the player has
	// moved on by more than CHASE_REPATH_DISTANCE from the last issued point.
	private static final int CHASE_RANGE = 150; // within melee/short-cast range
	private static final int CHASE_REPATH_DISTANCE = 100; // player moved this far -> re-path
	/**
	 * Casters inside an active olympiad match stop closing in once the enemy is within
	 * this distance (most caster nukes have a 600-900 cast range): from here the skill
	 * rotation fights and the attack assert engages only a fully stopped caster.
	 */
	private static final int CASTER_HOLD_RANGE = 500;
	/** objectId -> last issued chase point {x, y}. */
	private static final Map<Integer, int[]> CHASE_POINTS = new ConcurrentHashMap<>();

	/** objectId -> bot */
	private static final Map<Integer, Player> BOTS = new ConcurrentHashMap<>();
	/** objectId -> classId */
	private static final Map<Integer, Integer> BOT_CLASSES = new ConcurrentHashMap<>();
	/** objectId -> bot name (from the registry file, for logging before bots are in the world). */
	private static final Map<Integer, String> REGISTRY_NAMES = new ConcurrentHashMap<>();
	/** objectId -> death timestamp */
	private static final Map<Integer, Long> DEATH_TIMES = new ConcurrentHashMap<>();
	/** objectId(bot) -> objectId(enemy) the class opening curse was already applied to (see combatTick). */
	private static final Map<Integer, Integer> OPEN_CURSE_TARGETS = new ConcurrentHashMap<>();
	/**
	 * objectId -> NON-BOT olympiad match participant (real players). Registered by the
	 * pre-start hook (buff parity: the player receives the same buff treatment as the bot
	 * of the same class) and maintained by the combat tick while the match lasts; the entry
	 * is dropped as soon as the player leaves the match or the world.
	 */
	private static final Map<Integer, Player> OLYMPIAD_PARTICIPANTS = new ConcurrentHashMap<>();

	// ------------------------------------------------------------------
	// Olympiad kiting (bow bots) and the opening-curse interval
	// ------------------------------------------------------------------

	/** A kiting bot closes in only to this distance (same idea as the caster hold range). */
	private static final int KITE_HOLD_RANGE = 500;
	/** Below this distance a kiting bot retreats (melee reach + a safety margin). */
	private static final int KITE_MIN_RANGE = 250;
	/** Length of one retreat step. */
	private static final int KITE_STEP = 300;
	/**
	 * When a kiting bot gets this close to its OWN arena spawn point, the retreat direction
	 * flips toward the ENEMY spawn point (the kite crosses the arena instead of backing the
	 * bot into the wall/corner behind its spawn - the admin request: never get cornered,
	 * never run into a wall; wall safety is additionally enforced by geodata checks).
	 */
	private static final int KITE_FLIP_DISTANCE = 450;
	/** objectId -> arena spawn point {x, y, z, gameId} of an olympiad participant (bots and real players; recorded by the pre-start hook). */
	private static final Map<Integer, int[]> OLY_SPAWN_POINTS = new ConcurrentHashMap<>();

	/** Actual Olympiad stays, including countdown/end; claims survive until true leave. */
	private static final Set<Integer> OLY_GATE_STAYS = ConcurrentHashMap.newKeySet();

	/** objectId(bot) -> timestamp of the last applied class opening curse (anti-spam interval, see combatTick). */
	private static final Map<Integer, Long> CURSE_LAST_USE = new ConcurrentHashMap<>();
	/** Minimum interval (ms) between two opening-curse applications by the same bot: the curse must not become a free spammable debuff when the target keeps being re-acquired (Shadow Step target cancels etc.). */
	private static final long OPEN_CURSE_MIN_INTERVAL_MS = 2500;

	/**
	 * objectId(bot) whose skill ROTATION is paused because the olympiad kiting bot is
	 * pressed (dist below KITE_MIN_RANGE): the running cast finishes (never aborted),
	 * no NEW rotation cast starts, the bot walks one geodata-checked retreat step and
	 * the rotation resumes - either on the next tick when the distance has opened, or
	 * immediately when there is no safe retreat point (stand and fight). Purely
	 * tick-driven: no background timers, nothing to leak.
	 */
	private static final Set<Integer> KITE_USE_STOPPED = ConcurrentHashMap.newKeySet();

	// ------------------------------------------------------------------
	// Dagger bots (fixed damage-skill success chance) and combat titles (HP+CP percent)
	// ------------------------------------------------------------------

	/**
	 * The dagger classes of the roster: Adventurer (93), Wind Rider (101), Ghost Hunter
	 * (108). The skill effect handlers (handlers.skill.effects) give every bot of these
	 * classes a FIXED success chance on all damage-dealing skills - the calcSuccess roll
	 * is replaced entirely by isDaggerBotSkillSuccess() (skill evasion, blow chance and
	 * the Backstab behind requirement are not evaluated for them); real players keep the
	 * retail success rates. See isDaggerBot() and DAGGER_SKILL_SUCCESS_PERCENT.
	 */
	private static final int[] DAGGER_CLASS_IDS =
	{
		93, // Adventurer
		101, // Wind Rider
		108, // Ghost Hunter
	};

	/**
	 * The fixed success chance (%) of every damage-dealing skill cast by a dagger bot
	 * (admin rule: 70%, was 100%). The roll in isDaggerBotSkillSuccess() replaces the
	 * retail checks entirely for dagger bots - a failed roll is a genuine failure (no
	 * fall-through to the retail formula, which would push the rate back above 70%).
	 */
	public static final int DAGGER_SKILL_SUCCESS_PERCENT = 70;

	// ------------------------------------------------------------------
	// Tank class identity for the existing defense rule (not a probability override).
	// ------------------------------------------------------------------
	private static final int[] TANK_CLASS_IDS = {90, 91, 106};

	// ------------------------------------------------------------------
	// Tank bots (defense reduced by 50%)
	// ------------------------------------------------------------------

	/**
	 * The defense reduction of the tank bots (admin rule of 16.09.2026): the P.Def and
	 * M.Def of every tank bot are multiplied by this factor. Implemented as two MUL stat
	 * functions (Stat.POWER_DEFENCE / Stat.MAGIC_DEFENCE) owned by TANK_DEFENSE_FUNC_OWNER
	 * - the same mechanism the core uses for skill effects (MaxHp/MaxCp handlers) - added
	 * on bot registration and removed on despawn (and on class changes: non-tank bots are
	 * cleaned up on every registration, so a //bot class override stays correct).
	 */
	public static final double TANK_DEFENSE_MULTIPLIER = 0.5;

	/**
	 * The owner token of the tank defense reduction functions (removeStatsOwner key). A
	 * dedicated instance: it must never collide with any other stat function owner.
	 */
	private static final Object TANK_DEFENSE_FUNC_OWNER = new Object();

	/** objectId -> last combat title text shown above the bot (skips redundant broadcasts). */
	private static final Map<Integer, String> BOT_TITLES = new ConcurrentHashMap<>();



	// ------------------------------------------------------------------
	// Berserker (Doombringer, class 131) olympiad duel driver
	// ------------------------------------------------------------------

	/** The berserker class of the olympiad roster (Kamael berserker line). */
	private static final int CLASS_DOOMBRINGER = 131;

	/** The cycle opener: rushes to the enemy and stuns it (no reuse delay). */
	private static final int BERSERKER_RUSH_IMPACT_ID = 793;
	/** Burst skill 1: disarms the enemy for 5 seconds (8 s reuse). */
	private static final int BERSERKER_DISARM_ID = 485;
	/** Burst skill 2: the big frontal strike (75 s reuse - cast only when recharged). */
	private static final int BERSERKER_ENUMA_ELISH_ID = 526;
	/** Burst skill 3: the big single blow (7 s reuse - cast only when recharged). */
	private static final int BERSERKER_CRUSHING_PAIN_ID = 497;
	/**
	 * Burst finisher debuff (admin rules): Violent Temper may be used ONCE per battle
	 * and only AFTER a Rush Impact went off in that battle (2 s reuse - otherwise the
	 * rotation engine would spam it, which is why it is driver-managed now).
	 */
	private static final int BERSERKER_VIOLENT_TEMPER_ID = 501;
	/** Defensive self-buff, used only vs non-mage opponents when CP dropped below 70% (900 s reuse). */
	private static final int BERSERKER_SWORD_SHIELD_ID = 483;

	/** Mass Disarm (485) is cast only when the enemy is this close (admin rule: "in melee"). */
	private static final int BERSERKER_DISARM_MAX_DISTANCE = 150;

	/**
	 * Rush Impact requires its target to be at least this far away (datapack condition
	 * "target mindistance=200"): a short bounded retreat can open this distance. The driver
	 * approaches normally first, and requests a stationary cast only within the skill range.
	 */
	private static final int BERSERKER_RUSH_MIN_DISTANCE = 200;

	/** Sword Shield is cast only while the bot's CP percent is below this value. */
	private static final int BERSERKER_SWORD_SHIELD_CP_PERCENT = 70;

	/**
	 * Skills removed from the LIVE auto rotation while the berserker fights an olympiad
	 * match (the preset/default rotation is untouched): the burst/opener skills are
	 * cast by the driver itself (order and timing), and the other rush/charge skills of
	 * the class (Rush, Storm Assault, Shoulder Charge, Blade Rush - all with the same
	 * min-target-distance condition) would fire on their own during the approach or the
	 * retreat and break the Rush-Impact-driven cycle. Violent Temper (501) is driver
	 * managed too (once per battle, after a rush). What stays in the live rotation:
	 * Contagion (498), Dark Smash (477), Death Mark (1435).
	 */
	private static final int[] BERSERKER_MANAGED_SKILLS =
	{
		BERSERKER_RUSH_IMPACT_ID,
		BERSERKER_DISARM_ID,
		BERSERKER_ENUMA_ELISH_ID,
		BERSERKER_CRUSHING_PAIN_ID,
		BERSERKER_VIOLENT_TEMPER_ID,
		484, // Rush
		493, // Storm Assault
		494, // Shoulder Charge
		1510, // Soul Cleanse: emergency self-targeted magic, never the live offensive auto pool.
		495, // Blade Rush
	};

	/** objectId -> berserker duel state (exists only while the bot is inside an olympiad match). */
	private static final Map<Integer, BerserkerState> BERSERKER_STATES = new ConcurrentHashMap<>();

	/** Cumulative count of olympiad opponent force-retargets (observability of the summon-mis-targeting fix). */
	private static long _oppRetargetTotal = 0;

	/**
	 * Per-match state of a berserker bot on the olympiad: the remembered arena spawn
	 * point plus the phase flags of the current rush cycle.
	 */
	private static final class BerserkerState
	{
		final int gameId;
		final int spawnX;
		final int spawnY;
		final int spawnZ;

		/** Cycle phase: true after Rush Impact was initiated, until the retreat begins again. */
		boolean rushDone = false;
		/** Burst bookkeeping: Disarm / Enuma Elish / Crushing Pain cast this cycle. */
		boolean disarmDone = false;
		boolean enumaDone = false;
		boolean crushingDone = false;
		/** Admin rule: Violent Temper ONCE per battle (NOT reset per cycle), only after a rush. */
		boolean violentDone = false;
		/** True while the driver deliberately stopped both auto engines (the distance-breaking run). */
		boolean enginesStopped = false;
		/** True while a distance-breaking run is in progress (for the log lines). */
		boolean retreating = false;
		/** Rush cycle counter (logs). */
		int cycle = 0;
		final BerserkerCycle movement = new BerserkerCycle();
		long nextDecisionAt;
		final RushStunLoop rushStun = new RushStunLoop();
		boolean stunLoopMoving;
		volatile boolean stunLoopReleasing;
		long stunLoopMoveAt;
		double stunLoopMoveDistance;

		BerserkerState(int x, int y, int z, int game)
		{
			spawnX = x;
			spawnY = y;
			spawnZ = z;
			gameId = game;
		}
	}

	/** Pure identity policy. Only a new applied effect observed after our own cast can
	 * start a loop; replacements never silently extend the original stun window. */
	private static final class RushStunLoop
	{
		static final int NONE = 0, ACTIVE = 1, ENDED = 2;
		volatile Object original;
		private Object target;
		private Object before;
		private boolean watching;
		private long requestedAt;

		void requested(Object enemy, Object previous, long now)
		{
			if (original != null) return; // Repeat damage does not change the tracked original.
			target = enemy;
			before = previous;
			watching = true;
			requestedAt = now;
		}

		int observe(Object enemy, Object applied, long now)
		{
			if (original != null)
			{
				if (target == enemy && applied != null) { original = applied; return ACTIVE; }
				original = null;
				watching = false;
				target = before = null;
				return ENDED;
			}
			if (applied != null) { target = enemy; original = applied; watching = false; before = null; return ACTIVE; }
			if (!watching) return NONE;
			if (target != enemy || now < requestedAt || now - requestedAt > BerserkerCycle.RUSH_TIMEOUT_MS)
			{
				watching = false;
				target = before = null;
				return NONE;
			}
			if (applied != null && applied != before)
			{
				original = applied;
				watching = false;
				before = null;
				return ACTIVE;
			}
			return NONE;
		}
	}

	
	private static BotSettings SETTINGS = new BotSettings();
	private static boolean _initialized = false;
	private static int _tickerCycle = 0;
	private static int _combatCycle = 0;
	/**
	 * Coliseum combat pause (//bot button): when set, the combat driver stops assigning
	 * targets and asserting attack intentions for WORLD bots (olympiad matches in progress
	 * are never paused) and the auto combat engines of world bots are stopped, so every
	 * coliseum fight freezes. Resumed by //bot resume.
	 */
	private static volatile boolean _combatPaused = false;
	
	public static void init()
	{
		TacticalSettings.init();
		BotRecoveryRules.init();
		CombatAnalytics.init();

		if (_initialized)
		{
			return;
		}
		_initialized = true;
		
		OlympiadOpponentNotice.init(); // Player-facing arrival notice also works with bots disabled.
		SETTINGS.load();
		BotPresets.loadOverrides();
		
		if (!SETTINGS.isEnabled())
		{
			LOGGER.info("FakePlayersAI: disabled by config.");
			return;
		}
		
		// Maintenance ticker.
		ThreadPool.scheduleAtFixedRate(BotManager::ticker, SETTINGS.getSpawnDelaySeconds() * 1000L, 10000L);
		
		// Combat driver: assigns targets and asserts attack intentions at a fast pace.
		ThreadPool.scheduleAtFixedRate(BotManager::combatTick, SETTINGS.getSpawnDelaySeconds() * 1000L, 2000L);
		
		// Fast revive pass (every second): a dead bot is brought back RespawnSeconds after
		// its death (default 1 s) instead of waiting for the 10-second maintenance ticker,
		// so bots never lie dead in the coliseum for long.
		ThreadPool.scheduleAtFixedRate(BotManager::reviveTick, SETTINGS.getSpawnDelaySeconds() * 1000L, 1000L);

		// Fast retarget pass (every second): a bot that lost its target (target-canceling
		// effects like Shadow Step / Trick / Switch, the target dying, engine cleanups, ...)
		// takes the enemy back into target within a second instead of waiting for the
		// 2-second combat tick.
		ThreadPool.scheduleAtFixedRate(BotManager::retargetTick, SETTINGS.getSpawnDelaySeconds() * 1000L, 1000L);
		
		// Olympiad-only movement prediction: react to opponent turns/stops at 250 ms,
		// without changing the pace of buffs, rotations, registration or world combat.
		ThreadPool.scheduleAtFixedRate(BotManager::olympiadChaseTick, SETTINGS.getSpawnDelaySeconds() * 1000L, 100L);

		// Initial spawn at server start.
		if (SETTINGS.isSpawnOnStart())
		{
			ThreadPool.schedule(BotManager::spawnAll, (SETTINGS.getSpawnDelaySeconds() + 5) * 1000L);
		}

		// Olympiad points reset at every server start: every known bot begins the
		// session with a full points reserve (OlympiadResetPoints). The core loads
		// the nobles map from olympiad_nobles before scripts run (GameServer init
		// order puts the Olympiad section ahead of Scripts), so the reset can land
		// right here, before the first registration cycle of the new session.
		resetOlympiadPoints();

		// Olympiad pre-start hook: the core fires it for BOTH match participants at
		// countdown 2 (nominally two seconds before battle). Everyone gets the spawn point record, the
		// Chant of Victory strip and the olympiad buff set/profile; bots additionally get
		// the summon support and the immediate one-shot self-enhancement burst (Final
		// Secret, the dagger evasions, Wind Riding / Exciting Adventure, the caster auras
		// - BOTS ONLY) synchronously at the core countdown-2 hook, before combat is armed.
		Olympiad.PRE_START_HOOKS.add(BotManager::olympiadPreStart);

		// Olympiad points on every game entry (admin rule of 04.10.2026): every
		// character and administrator that enters the game world gets exactly
		// OLYMPIAD_LOGIN_POINTS (200) olympiad points, whatever the previous session
		// left in the nobles map. Registered as a GLOBAL ON_PLAYER_LOGIN listener -
		// the core dispatches it in Player.enterWorld for every real login. Bots never
		// log in (clientless spawn): their points get the same value at every server
		// start through OlympiadResetPoints (kept equal to OLYMPIAD_LOGIN_POINTS).
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGIN, OLYMPIAD_LOGIN_LISTENER, BotManager.class));

		// Olympiad dead-participant revive hook: when a match participant is dead right
		// before the match starts (a bot killed in the coliseum after its registration),
		// the core normally cancels the match ("Your opponent does not meet the
		// requirements..."). This hook revives the dead bot instead, so the match proceeds.
		Olympiad.DEAD_PARTICIPANT_REVIVE_HOOKS.add(BotManager::reviveDeadBotForOlympiad);

		// Catch-up pass of the same reset for bots that were missing from the nobles
		// map above (characters created but never registered yet: the core enters
		// them with the default points on their first registration). Runs once, when
		// the initial spawn and the first registration cycles are done but no match
		// of this session has finished yet - see olympiadPointsCatchUp().
		ThreadPool.schedule(BotManager::olympiadPointsCatchUp, (SETTINGS.getSpawnDelaySeconds() + 70) * 1000L);

		LOGGER.info("FakePlayersAI: initialized, " + SETTINGS.getBotCount() + " bots scheduled; predictive-chase v2; Olympiad tactical suite v3 (class skills/precombat gate/training results).");
	}

	// ------------------------------------------------------------------
	// Olympiad points reset (every server start)
	// ------------------------------------------------------------------

	/**
	 * ON_PLAYER_LOGIN listener (global, registered in init()): sets the olympiad points
	 * of every entering character/administrator to OLYMPIAD_LOGIN_POINTS (200) - the
	 * admin rule of 04.10.2026 ("при каждом входе в игру очки олимпиады равны 200").
	 * Works for real players and admins alike; characters without a nobles-map entry
	 * are untouched by the core (the lookup simply has no entry for them). Idempotent.
	 * @param event the login event
	 */
	private static void onPlayerLogin(OnPlayerLogin event)
	{
		try
		{
			final Player player = event.getPlayer();
			if ((player == null) || !player.isNoble())
			{
				return;
			}

			final Olympiad olympiad = Olympiad.getInstance();
			if (olympiad == null)
			{
				return;
			}

			final int points = olympiad.getNoblePoints(player.getObjectId());
			if (points != OLYMPIAD_LOGIN_POINTS)
			{
				olympiad.setNoblePoints(player.getObjectId(), OLYMPIAD_LOGIN_POINTS);
				LOGGER.info("FakePlayersAI: oly-login " + player.getName() + ": olympiad points " + points + " -> " + OLYMPIAD_LOGIN_POINTS + " (game entry rule).");
			}
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: olympiad login points handler failed: " + e);
		}
	}

	/** Core PRE_START_HOOKS fires at countdown 2 (nominal T-2, not a wall-clock promise).
	 * Apply base/profile first, then one-shot bot boosts without re-arming either engine.
	 * Human participants retain existing base/profile parity, not bot-only boosts. */
	private static void olympiadPreStart(Player player)
	{
		try
		{
			if (player == null)
			{
				return;
			}

			OlympiadOpponentNotice.announce(player); // Deduplicated fallback if arrival raced the native pair.
			final boolean bot = isBot(player);
			final BotClassPreset preset = BotPresets.getPresetByClassId(bot ? getBotClassId(player) : player.getPlayerClass().getId());
			final boolean caster = (preset != null) ? preset.isCaster() : player.isMageClass();

			// Arena spawn point of EVERY participant (bots and real players alike): the
			// olympiad kiting (bow bots / Dominator) flips the retreat direction toward the
			// enemy spawn point, which is looked up by the opponent's objectId here.
			OLY_SPAWN_POINTS.put(player.getObjectId(), new int[]
			{
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getOlympiadGameId()
			});

			if (bot && (preset != null))
			{
				OlympiadCombatGate.hold(player);
				OLY_GATE_STAYS.add(player.getObjectId());

				// Berserker (Doombringer): the arena appearance point is remembered right at the
				// pre-start moment (the bot is already ported and stands still until the fight
				// starts) - it retains this point as match context, but retreats only a short local step
				// when breaking distance for the next Rush Impact (see driveBerserker). Both auto engines are ALSO stopped here:
				// the battle has to open with Rush Impact (admin rule "the rotation starts with
				// the rush") - no autoattack and no rotation filler may fire before it; the
				// driver re-arms the engines right after the rush lands (armBerserkerCombat).
				if (preset.getClassId() == CLASS_DOOMBRINGER)
				{
					final BerserkerState preStartState = new BerserkerState(player.getX(), player.getY(), player.getZ(), player.getOlympiadGameId());
					preStartState.enginesStopped = true;
					BERSERKER_STATES.put(player.getObjectId(), preStartState);
					AutoPlayTaskManager.getInstance().stopAutoPlay(player);
					AutoUseTaskManager.getInstance().stopAutoUseTask(player);
					LOGGER.info("FakePlayersAI: berserker " + player.getName() + ": olympiad spawn point recorded (" + player.getX() + ", " + player.getY() + "), game " + player.getOlympiadGameId() + "; engines stopped until the opening Rush Impact.");
				}
			}

			// Buff parity with the bot of the same class: every participant (bots and real
			// players alike) receives the basic olympiad buff set and the olympiad buff
			// profile (all applied instantly).
			//
			// Chant of Victory ban (admin rule): NO olympiad participant (bot or real player)
			// may enter the fight with a Chant of Victory family buff (CoV analogs included).
			// The core wiped all effects at match creation (OlympiadGame.removals()), but a
			// self-cast during the countdown is still possible - strip it right before the
			// pre-start buffs (the olympiad profile applied below contains no family buffs).
			BotSpawner.stripChantBuffs(player);
			BotSpawner.applyOlympiadBuffs(player);
			BotSpawner.maintainFullBuffs(player, caster);
			if (bot && preset != null && OlympiadCombatGate.claimPreStart(player))
			{
				OlympiadSummonRules.prepare(player, preset);
				BotSpawner.maintainOlympiadSummon(player, preset, null);
				BotSpawner.applyPreCombatOpeners(player, preset);
			}

			if (bot) OlympiadCombatGate.hold(player); // Also hold a newly prepared servitor.

			if (!bot)
			{
				OLYMPIAD_PARTICIPANTS.put(player.getObjectId(), player);
			}
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: olympiad pre-start hook failed: " + e);
		}
	}

	/**
	 * Resets the olympiad points of every known bot (registry) to the configured
	 * value (OlympiadResetPoints, aligned with OLYMPIAD_LOGIN_POINTS = 200 - the
	 * game entry rule of 04.10.2026; bots never log in, their "entry" is the server
	 * start). Called once per server start at script init; the reset lands in the
	 * core nobles map, the database is updated by the core itself as usual
	 * (saveNobleData on weekly change / shutdown). Bots absent from the nobles
	 * map (never registered characters) are skipped here - olympiadPointsCatchUp()
	 * picks them up after their first registration.
	 */
	private static void resetOlympiadPoints()
	{
		try
		{
			final Olympiad olympiad = Olympiad.getInstance();
			if (olympiad == null)
			{
				LOGGER.warning("FakePlayersAI: olympiad points reset skipped: core olympiad is not available.");
				return;
			}

			final int resetPoints = SETTINGS.getOlympiadResetPoints();
			int adjusted = 0;
			for (Map.Entry<Integer, Integer> entry : BOT_CLASSES.entrySet())
			{
				final int objectId = entry.getKey();
				final int points = olympiad.getNoblePoints(objectId);
				if (points != resetPoints)
				{
					olympiad.setNoblePoints(objectId, resetPoints);
					LOGGER.info("FakePlayersAI: oly-reset " + REGISTRY_NAMES.getOrDefault(objectId, "#" + objectId) + ": " + points + " -> " + resetPoints + ".");
					adjusted++;
				}
			}
			LOGGER.info("FakePlayersAI: olympiad points reset to " + resetPoints + ": " + adjusted + " of " + BOT_CLASSES.size() + " registered bots adjusted.");
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: olympiad points reset failed: " + e);
		}
	}

	/**
	 * Catch-up pass of the startup points reset. Bots absent from the nobles map
	 * at script init (freshly created characters that have never registered) enter
	 * it with the core default points on their first registration; this pass lifts
	 * such idle bots to the reset value as well. Runs once, when the initial spawn
	 * and the first registration cycles are done but no match of this session has
	 * finished yet (registration needs SpawnDelaySeconds + ~30s, a finished match
	 * needs ~100s more), so it can never undo a match result. Bots inside an
	 * active match are skipped: they entered it with proper points.
	 */
	private static void olympiadPointsCatchUp()
	{
		try
		{
			final Olympiad olympiad = Olympiad.getInstance();
			if (olympiad == null)
			{
				return;
			}

			final int resetPoints = SETTINGS.getOlympiadResetPoints();
			int adjusted = 0;
			for (Player bot : BOTS.values())
			{
				if (!bot.isNoble() || bot.isInOlympiadMode() || olympiad.playerInStadia(bot))
				{
					continue;
				}
				final int points = olympiad.getNoblePoints(bot.getObjectId());
				if (points != resetPoints)
				{
					olympiad.setNoblePoints(bot.getObjectId(), resetPoints);
					LOGGER.info("FakePlayersAI: oly-reset " + bot.getName() + ": " + points + " -> " + resetPoints + " (catch-up).");
					adjusted++;
				}
			}
			if (adjusted > 0)
			{
				LOGGER.info("FakePlayersAI: olympiad points catch-up: " + adjusted + " bots lifted to " + resetPoints + ".");
			}
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: olympiad points catch-up failed: " + e);
		}
	}
	
	private static void ticker()
	{
		try
		{
			_tickerCycle++;
			
			// Keep only alive/known bots.
			BOTS.values().removeIf(bot -> { if (bot.isOnline()) return false; OlympiadRegistration.clear(bot.getObjectId()); return true; });
			final Olympiad olympiad = Olympiad.getInstance();
			final boolean compPeriod = olympiad != null && olympiad.inCompPeriod();
			
			// 1) Olympiad registration: bots register for the non-class based games
			// as soon as the competition period opens (immediately at server start when possible).
			if (SETTINGS.isAutoRegisterOlympiad() && compPeriod && ((_tickerCycle % 3) == 0))
			{
				// Keep bots eligible: the core refuses to register a non-classed noble
				// with less than 5 olympiad points, and once enough bots drain below that,
				// registrations fall under OlympiadNonClassedParticipants and no new games
				// are created at all. Top points back up for every idle noble bot.
				for (Player bot : BOTS.values())
				{
					if (bot.isNoble() && !bot.isInOlympiadMode() && !olympiad.playerInStadia(bot))
					{
						final int points = olympiad.getNoblePoints(bot.getObjectId());
						if (points < SETTINGS.getOlympiadMinPoints())
						{
							olympiad.setNoblePoints(bot.getObjectId(), SETTINGS.getOlympiadMinPoints());
							LOGGER.info("FakePlayersAI: oly-points " + bot.getName() + ": " + points + " -> " + SETTINGS.getOlympiadMinPoints() + ".");
						}
					}
				}
				
				final int registered = OlympiadRegistration.register(olympiad, BOTS.values(), SETTINGS.getOlympiadMinPoints());
				LOGGER.info("FakePlayersAI: olympiad randomized-register: in-world=" + BOTS.size() + ", registered=" + registered + ".");
			}
			
			// 1b) Combat status (one line per minute): dead bots and total pvp/pk kills.
			if ((_tickerCycle % 6) == 0)
			{
				int dead = 0;
				long kills = 0;
				for (Player bot : BOTS.values())
				{
					if (bot.isAlikeDead())
					{
						dead++;
					}
					kills += bot.getPvpKills() + bot.getPkKills();
				}
				LOGGER.info("FakePlayersAI: combat status: in-world=" + BOTS.size() + ", dead=" + dead + ", kills=" + kills + ".");
			}
			
			// 2) Per-bot maintenance.
			for (Player bot : BOTS.values())
			{
				// Clientless bots never send the Appearing packet after a teleport, so
				// the core keeps them in the "teleporting" state forever (invulnerable,
				// invisible to their own AI). Finish the teleport for them right away.
				// (The TeleportWatchdogTimeout config is the fallback for this.)
				if (bot.isTeleporting() && (bot.getClient() == null))
				{
					bot.onTeleported();
				}
				
				final boolean inOlympiad = bot.isInOlympiadMode() || olympiad.playerInStadia(bot);
				
				if (OrderedBattles.waitOutsideMatch(bot)) continue;

				// Dead bots are handled by the dedicated 1-second reviveTick() pass.
				if (bot.isAlikeDead())
				{
					continue;
				}
				DEATH_TIMES.remove(bot.getObjectId());
				
				// Re-arm the auto combat engines: Player.onTeleported() stops them on every
				// teleport (including the olympiad arena port and the port back home).
				// Skipped while the coliseum combat is paused (//bot pause). A berserker bot
				// inside an olympiad match is skipped as well - its duel driver owns the
				// engines there (it stops them on purpose for the distance-breaking runs);
				// the fast combat tick repairs them when needed.
				if (!_combatPaused && (!bot.isAutoPlaying() || bot.getAutoUseSettings().getAutoSkills().isEmpty())
					&& !BERSERKER_STATES.containsKey(bot.getObjectId()) && !OlympiadChase.isPursuing(bot.getObjectId()) && !BotTactics.isOwned(bot.getObjectId()))
				{
					final BotClassPreset preset = BotPresets.getPresetByClassId(BOT_CLASSES.getOrDefault(bot.getObjectId(), bot.getPlayerClass().getId()));
					if (preset != null)
					{
						BotSpawner.applyAutoCombat(bot, preset);
					}
				}
				
				if (inOlympiad)
				{
					// DEBUG: olympiad movement check (30s).
					if ((_tickerCycle % 3) == 0)
					{
						final WorldObject t = bot.getTarget();
						LOGGER.info("FakePlayersAI: oly-dbg " + bot.getName() + ": tgt=" + (t != null ? t.getName() : "null") + ", distToTgt=" + (t != null ? (int) bot.calculateDistance2D(t) : -1) + ", intent=" + bot.getAI().getIntention() + ", mv=" + bot.isMoving() + ", start=" + bot.isOlympiadStart() + ", gameId=" + bot.getOlympiadGameId() + ", pos=" + bot.getX() + "," + bot.getY() + ", chase=" + OlympiadChase.describe(bot.getObjectId()) + ", autoPlay=" + bot.isAutoPlaying() + ", cast=" + bot.isCastingNow() + ", " + BotTactics.describe(bot.getObjectId()));
					}
					continue;
				}
				
				// Return to the combat area if dragged away (peace zone, town, ...).
				if (SETTINGS.isTeleportHome() && !bot.isInsideZone(ZoneId.PVP) && !bot.isTeleporting())
				{
					final int[] point = randomSpawnPoint();
					bot.teleToLocation(point[0], point[1], point[2]);
					continue;
				}
				
				// Out of olympiad: keep bots in shape, but only outside of combat
				// (healing mid-fight would make arena fights endless).
				final boolean inCombat = AttackStanceTaskManager.getInstance().hasAttackStanceTask(bot);
				if (!inCombat)
				{
					if (SETTINGS.isHealBots())
					{
						bot.setCurrentHp(bot.getMaxHp());
						bot.setCurrentCp(bot.getMaxCp());
					}
					// Periodic idle refill is now Cardinal-only; everyone else regenerates natively.
					if (SETTINGS.isRestoreMp() && bot.getPlayerClass().getId() == 97)
					{
						bot.setCurrentMp(bot.getMaxMp());
					}
					if (SETTINGS.isClearKarma() && (bot.getKarma() > 0))
					{
						bot.setKarma(0);
						bot.broadcastUserInfo();
					}
				}
				if (SETTINGS.isFixLevel() && (bot.getLevel() < 85))
				{
					final long exp = ExperienceData.getInstance().getExpForLevel(85);
					if (bot.getExp() < exp)
					{
						bot.addExpAndSp(exp - bot.getExp(), 0);
					}
				}

				// The out-of-combat heal above changed HP/CP: keep the combat title current.
				updateCombatTitle(bot);
				
				// DEBUG: detailed per-bot state once per minute (console truncates long lines).
				if ((_tickerCycle % 6) == 1)
				{
					final WorldObject t = bot.getTarget();
					double minDist = 99999;
					for (Player other : BOTS.values())
					{
						if ((other != bot) && !other.isAlikeDead())
						{
							minDist = Math.min(minDist, bot.calculateDistance2D(other));
						}
					}
					LOGGER.info("FakePlayersAI: dbg " + bot.getName() + ": tgt=" + (t != null ? t.getName() : "null") + ", intent=" + bot.getAI().getIntention() + ", autoPlay=" + bot.isAutoPlaying() + ", pvp=" + bot.isInsideZone(ZoneId.PVP));
					LOGGER.info("FakePlayersAI: dbg " + bot.getName() + ": dist=" + (int) minDist + ", mv=" + bot.isMoving() + ", cast=" + bot.isCastingNow() + ", atk=" + bot.isAttackingNow() + ", hp=" + (int) bot.getCurrentHp());
				}
				
				// Safety: make sure the combat engines are running (never while paused).
				if (!_combatPaused && ((_tickerCycle % 6) == 0))
				{
					final BotClassPreset preset = BotPresets.getPresetByClassId(BOT_CLASSES.getOrDefault(bot.getObjectId(), bot.getPlayerClass().getId()));
					if (preset != null)
					{
						BotSpawner.applyAutoCombat(bot, preset);
					}
				}
			}
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: ticker error: " + e);
		}
	}
	
	private static void reviveBot(Player bot)
	{
		try
		{
			bot.doRevive();
			bot.setCurrentHp(bot.getMaxHp());
			bot.setCurrentMp(bot.getMaxMp());
			bot.setCurrentCp(bot.getMaxCp());
			final int[] point = randomSpawnPoint();
			bot.teleToLocation(point[0], point[1], point[2]);
			bot.broadcastUserInfo();
			// Coliseum auto-buff on EVERY resurrection: the full warrior/mage profile is
			// applied right after the revive (missing buffs only - the pass is idempotent).
			final BotClassPreset preset = BotPresets.getPresetByClassId(BOT_CLASSES.getOrDefault(bot.getObjectId(), bot.getPlayerClass().getId()));
			if (preset != null)
			{
				BotSpawner.refreshAutoBuffs(bot, preset);
				BotSpawner.maintainFullBuffs(bot, preset);
			}
			// The revive healed the bot to full: the combat title is "100%" right away.
			updateCombatTitle(bot);
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: revive of " + bot.getName() + " failed: " + e);
		}
	}
	
	/**
	 * Fast revive pass (every second): brings dead bots back RespawnSeconds after their
	 * death (default 1 second). Bots inside an active olympiad match are left alone - the
	 * match system owns their life state.
	 */
	private static void reviveTick()
	{
		try
		{
			final Olympiad olympiad = Olympiad.getInstance();
			for (Player bot : BOTS.values())
			{
				if (!bot.isAlikeDead() || bot.isInOlympiadMode() || ((olympiad != null) && olympiad.playerInStadia(bot)))
				{
					continue;
				}
				final long deathTime = DEATH_TIMES.computeIfAbsent(bot.getObjectId(), id -> System.currentTimeMillis());
				if ((System.currentTimeMillis() - deathTime) > (SETTINGS.getRespawnSeconds() * 1000L))
				{
					DEATH_TIMES.remove(bot.getObjectId());
					reviveBot(bot);
				}
			}
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: revive tick error: " + e);
		}
	}
	
	/**
	 * Olympiad dead-participant revive hook (Olympiad.DEAD_PARTICIPANT_REVIVE_HOOKS): the
	 * core consults it when a match participant is dead right before the match starts.
	 * Dead BOT participants are revived (full HP/MP/CP) so the match proceeds instead of
	 * being cancelled with "Your opponent does not meet the requirements to do battle".
	 * Real players keep the retail behavior (the match is cancelled as usual).
	 * @param participant the dead match participant
	 * @return true when the participant is a bot and was revived
	 */
	public static boolean reviveDeadBotForOlympiad(Player participant)
	{
		if (!isBot(participant) || !participant.isDead())
		{
			return false;
		}
		try
		{
			participant.doRevive();
			participant.setCurrentHp(participant.getMaxHp());
			participant.setCurrentMp(participant.getMaxMp());
			participant.setCurrentCp(participant.getMaxCp());
			DEATH_TIMES.remove(participant.getObjectId());
			updateCombatTitle(participant);
			LOGGER.info("FakePlayersAI: oly-revive " + participant.getName() + ": dead bot revived for its olympiad match.");
			return true;
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: olympiad revive of " + participant.getName() + " failed: " + e);
			return false;
		}
	}

	/**
	 * Releases a bot from a DEAD olympiad match (the core game task thread died before
	 * its cleanup - see the dead match escape in the combat tick): the olympiad flags
	 * are cleared, the bot is healed and teleported back to the bot area (the Coliseum)
	 * and the next combat tick re-arms its combat engines as a world bot. A dead bot is
	 * handled by the revive tick afterwards (the olympiad skip no longer applies once
	 * the flag is off). The match itself is beyond saving - its task thread is gone and
	 * with it the points bookkeeping; releasing the bot is the only way to return it to
	 * play (the leaked stadium of the dead game stays unusable until the next server
	 * restart - a core-side issue, reported to the admin in the session notes).
	 * @param bot the stuck bot
	 * @param reason the human-readable release reason (for the log)
	 */
	private static void releaseBotFromDeadMatch(Player bot, String reason)
	{
		try
		{
			bot.setInOlympiadMode(false);
			bot.setOlympiadStart(false);
			bot.setOlympiadSide(-1);
			bot.setOlympiadGameId(-1);
			OLY_SPAWN_POINTS.remove(bot.getObjectId());
			OPEN_CURSE_TARGETS.remove(bot.getObjectId());
			CHASE_POINTS.remove(bot.getObjectId());
			bot.setTarget(null);
			if (!bot.isAlikeDead())
			{
				bot.setCurrentHp(bot.getMaxHp());
				bot.setCurrentCp(bot.getMaxCp());
				bot.setCurrentMp(bot.getMaxMp());
				bot.getAI().setIntentionIdle();
			}
			if (SETTINGS.isTeleportHome() && !bot.isTeleporting())
			{
				final int[] point = randomSpawnPoint();
				bot.teleToLocation(point[0], point[1], point[2]);
			}
			LOGGER.info("FakePlayersAI: oly-release " + bot.getName() + ": released from a dead olympiad match (" + reason + "); teleported home, engines re-armed by the combat tick.");
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: olympiad dead match release of " + bot.getName() + " failed: " + e);
		}
	}
	
	// ------------------------------------------------------------------
	// Coliseum combat pause (//bot buttons)
	// ------------------------------------------------------------------
	
	/**
	 * Pauses the coliseum combat: the combat driver stops assigning targets and asserting
	 * attack intentions for world bots, every world bot's auto combat engines are stopped
	 * and its current combat intention is dropped (fights freeze). Bots inside an active
	 * olympiad match are NOT paused. Also revives every dead bot (except olympiad
	 * participants) as requested by the admin button.
	 * @return the number of dead bots revived by this call
	 */
	public static synchronized int pauseCombat()
	{
		_combatPaused = true;
		int revived = 0;
		final Olympiad olympiad = Olympiad.getInstance();
		for (Player bot : BOTS.values())
		{
			final boolean inOlympiadMatch = bot.isInOlympiadMode() || ((olympiad != null) && olympiad.playerInStadia(bot));
			if (bot.isAlikeDead() && !inOlympiadMatch)
			{
				reviveBot(bot);
				revived++;
			}
			if (inOlympiadMatch)
			{
				continue; // never pause an active olympiad match
			}
			BotSpawner.stopAutoCombat(bot);
			CHASE_POINTS.remove(bot.getObjectId());
			OPEN_CURSE_TARGETS.remove(bot.getObjectId());
			bot.setTarget(null);
			if (!bot.isAlikeDead())
			{
				bot.getAI().setIntentionIdle();
			}
		}
		LOGGER.info("FakePlayersAI: coliseum combat PAUSED by admin (revived " + revived + " dead bot(s)).");
		return revived;
	}
	
	/**
	 * Resumes the coliseum combat after a pause: re-arms the auto combat engines of every
	 * alive world bot; the combat driver starts assigning targets again on its next tick.
	 */
	public static synchronized void resumeCombat()
	{
		_combatPaused = false;
		final Olympiad olympiad = Olympiad.getInstance();
		for (Player bot : BOTS.values())
		{
			if (bot.isAlikeDead() || bot.isInOlympiadMode() || ((olympiad != null) && olympiad.playerInStadia(bot)))
			{
				continue;
			}
			final BotClassPreset preset = BotPresets.getPresetByClassId(BOT_CLASSES.getOrDefault(bot.getObjectId(), bot.getPlayerClass().getId()));
			if (preset != null)
			{
				BotSpawner.applyAutoCombat(bot, preset);
			}
		}
		LOGGER.info("FakePlayersAI: coliseum combat RESUMED by admin.");
	}
	
	public static boolean isCombatPaused()
	{
		return _combatPaused;
	}
	
	/**
	 * Requests olympiad registration through the SAME randomized queue as automatic passes.
	 * Newly idle bots wait5..20s; repeated button presses do not reroll or bypass the pause.
	 * Existing bot min-points eligibility and active/assigned-match guards are preserved.
	 * @return the number of bots registered, or -1 when the competition period is closed
	 */
	public static synchronized int forceOlympiadRegister()
	{
		final Olympiad olympiad = Olympiad.getInstance();
		if ((olympiad == null) || !olympiad.inCompPeriod())
		{
			return -1;
		}
		// Admin requests use the same shuffle/delay gate; no parallel bypass of pending matches.
		final int registered = OlympiadRegistration.request(olympiad, BOTS.values(), SETTINGS.getOlympiadMinPoints());
		LOGGER.info("FakePlayersAI: olympiad force-register by admin: " + registered + " bot(s) registered.");
		return registered;
	}
	
	/**
	 * Despawns every bot that is not inside an active olympiad match and schedules the
	 * regular startup spawn (spawnAll - the same path the server start uses) 5 seconds
	 * later, so the bots come back fresh (//bot button).
	 * @return the number of bots despawned
	 */
	public static synchronized int despawnAllForRestart()
	{
		int despawned = 0;
		final Olympiad olympiad = Olympiad.getInstance();
		for (Player bot : new ArrayList<>(BOTS.values()))
		{
			if (bot.isInOlympiadMode() || ((olympiad != null) && olympiad.playerInStadia(bot)))
			{
				continue; // never break an active olympiad match
			}
			despawnBot(bot);
			despawned++;
		}
		DEATH_TIMES.clear();
		ThreadPool.schedule(BotManager::spawnAll, 5000);
		LOGGER.info("FakePlayersAI: admin restart of bots: " + despawned + " despawned, spawn scheduled in 5 seconds.");
		return despawned;
	}
	
	/**
	 * Fast combat driver (every 2 seconds): assigns targets to idle bots and
	 * asserts attack intentions. The core auto play engine keeps cleaning up
	 * dead targets and rotating the skill list; the attack branch of that engine
	 * refuses "busy" targets, which deadlocks mutual bot fights, so the attack
	 * intention is driven here directly.
	 */
	private static void combatTick()
	{
		try
		{
			final Olympiad olympiad = Olympiad.getInstance();
			int withTarget = 0;
			int skippedTeleport = 0;
			int oppRetarget = 0;

			// Every combat tick checks the SAME randomized due-times as maintenance/admin.
			// Bots wait5..20s after becoming idle, rather than instantly reforming the same pair.
			if (SETTINGS.isAutoRegisterOlympiad() && (olympiad != null))
			{
				final int registeredNow = OlympiadRegistration.register(olympiad, BOTS.values(), SETTINGS.getOlympiadMinPoints());
				if (registeredNow > 0) LOGGER.info("FakePlayersAI: olympiad randomized fast-register: " + registeredNow + " bot(s).");
			}

			for (Player bot : BOTS.values())
			{
				// Clientless teleports need this completion before readiness can become true.
				// onTeleported stops engines; no combat decision happens before the gate.
				if (bot.isTeleporting() && bot.getClient() == null) bot.onTeleported();
				if (OrderedBattles.waitOutsideMatch(bot)) continue;
				if (holdOlympiadIfNotReady(bot))
				{
					watchOlympiadMatch(bot, true);
					continue;
				}
				if (bot.isAlikeDead())
				{
					continue;
				}

				// Combat title above the bot's head: the combined HP+CP percent,
				// refreshed on every combat tick (2 seconds).
				updateCombatTitle(bot);

				if (bot.isTeleporting())
				{
					// Finish clientless bot teleports immediately; real (online) players
					// are skipped until their teleport completes as usual.
					if (bot.getClient() == null)
					{
						bot.onTeleported();
					}
					else
					{
						skippedTeleport++;
						continue;
					}
				}
				
				final boolean inOlympiad = bot.isInOlympiadMode() || olympiad.playerInStadia(bot);

				// Fallback: make sure the bot's arena spawn point of the CURRENT match is
				// recorded (the pre-start hook normally does it for every participant; a
				// missed hook would leave the olympiad kiter without its flip reference).
				if (inOlympiad && bot.isOlympiadStart())
				{
					final int[] recorded = OLY_SPAWN_POINTS.get(bot.getObjectId());
					if ((recorded == null) || (recorded[3] != bot.getOlympiadGameId()))
					{
						OLY_SPAWN_POINTS.put(bot.getObjectId(), new int[]
						{
							bot.getX(),
							bot.getY(),
							bot.getZ(),
							bot.getOlympiadGameId()
						});
					}
				}

				if (watchOlympiadMatch(bot, inOlympiad)) continue;

				// Arena containment (bug report of 04.10.2026): a bot inside an olympiad
				// match must never stand in the open world. When the core's arena port
				// misses a participant (a bot mid-teleport from the previous match's
				// return), or a stale port-back yanks the bot out of the arena mid-match,
				// the combat drivers happily walk BOTH bots towards each other ACROSS THE
				// WORLD (a Storm Screamer and a Duelist were observed marching 137 km to
				// each other for 37 minutes while their match never ended). The
				// containment returns the bot to its stadium port position immediately.
				final int olyGameId = bot.getOlympiadGameId();
				if (inOlympiad && (olyGameId >= 0) && (olyGameId < OLYMPIAD_STADIUM_CENTERS.length) && !bot.isTeleporting())
				{
					final int[] stadium = OLYMPIAD_STADIUM_CENTERS[olyGameId];
					final double distToArena = bot.calculateDistance2D(stadium[0], stadium[1], stadium[2]);
					if (distToArena > OLYMPIAD_ARENA_MAX_DISTANCE)
					{
						// The participant's port position (mirrors the core:
						// side 1 -> +1200, side 2 -> -1200); unknown side -> center.
						final int side = bot.getOlympiadSide();
						final int portX = stadium[0] + ((side == 1) ? 1200 : (side == 2) ? -1200 : 0);
						bot.teleToLocation(portX, stadium[1], stadium[2]);
						LOGGER.info("FakePlayersAI: oly-contain " + bot.getName() + ": bot was " + (int) distToArena + " away from its stadium, returned to the arena (game " + olyGameId + ").");
						continue;
					}
				}

				// Coliseum combat pause: world bots are kept completely idle - their engines
				// are (re)stopped on every tick, so a bot returning from an olympiad match
				// with re-armed engines freezes again instead of resuming world fights.
				// Bots inside an olympiad match keep fighting.
				if (_combatPaused && !inOlympiad)
				{
					if (bot.isAutoPlaying())
					{
						BotSpawner.stopAutoCombat(bot);
						CHASE_POINTS.remove(bot.getObjectId());
						OPEN_CURSE_TARGETS.remove(bot.getObjectId());
						bot.setTarget(null);
						if (!bot.isAlikeDead())
						{
							bot.getAI().setIntentionIdle();
						}
					}
					continue;
				}
				
				// Coliseum: bots must never stay locked by disabling crowd control. A
				// stun/sleep/root/paralysis/fear/mute chain leaves them standing idle
				// (they stop attacking AND casting), so every such effect is removed
				// right here on each combat tick. Olympiad matches keep their CC intact.
				if (!inOlympiad)
				{
					final EffectList effects = bot.getEffectList();
					effects.stopEffects(EffectType.STUN);
					effects.stopEffects(EffectType.SLEEP);
					effects.stopEffects(EffectType.PARALYZE);
					effects.stopEffects(EffectType.ROOT);
					effects.stopEffects(EffectType.MUTE);
					effects.stopEffects(EffectType.FEAR);
				}
				else
				{
					// Olympiad ban (admin rule): NO participant of an olympiad match may
					// hold a Chant of Victory family buff (CoV analogs included). The core
					// wiped all effects at match entry and the pre-start hook stripped the
					// countdown casts, but a mid-fight self-cast is still possible - remove
					// the family effects again on every tick, and keep the skills out of the
					// bot's live auto lists (rotation + auto buffs) so the engines (and any
					// //bot override) cannot cast them on the arena - CoV family AND
					// dances/songs (admin rule of 04.10.2026). The preset itself is
					// untouched: outside olympiad the full sets work as before.
					BotSpawner.stripChantBuffs(bot);
					BotSpawner.trimOlympiadBannedSkillsFromAutoLists(bot);
				}
				
				// onTeleported() stops both auto engines on every teleport - re-arm them.
				// A rotation emptied by the core (AutoUseTaskManager drops unknown skills
				// from the list) is re-filled here as well, including inside olympiad
				// matches, where the maintenance ticker skips its own re-arm block.
				final BotClassPreset preset = BotPresets.getPresetByClassId(BOT_CLASSES.getOrDefault(bot.getObjectId(), bot.getPlayerClass().getId()));
				// Berserker (Doombringer) duel driver: active while the bot fights an olympiad
				// match. Inside the match the driver owns the auto engines (it stops them on
				// purpose for the distance-breaking runs) and keeps the live rotation trimmed
				// (its skills are cast by the driver itself), so the generic re-arm must not
				// run for it. The duel state lives for the WHOLE olympiad stay (countdown
				// included): it is created by the pre-start hook (or here as the fallback)
				// and dropped with the full combat setup restored only when the bot actually
				// leaves the olympiad. (The old "active fight only" lifetime dropped and
				// re-created the state one second before the battle: a bogus "match is over"
				// restore right between the pre-start and the gates opening.)
				final boolean berserkerFight = inOlympiad && bot.isOlympiadStart() && (preset != null) && (preset.getClassId() == CLASS_DOOMBRINGER);
				updateBerserkerState(bot, preset, inOlympiad && (preset != null) && (preset.getClassId() == CLASS_DOOMBRINGER));

				// Kiting safety net: a pressed kiting bot whose rotation was paused for the
				// retreat (KITE_USE_STOPPED) gets it resumed right here whenever it is no
				// longer in a kiting situation (match over, back in the world, not a kiter
				// preset anymore). Also covers the rotation stop surviving an engine restart
				// of the generic re-arm above.
				if (KITE_USE_STOPPED.contains(bot.getObjectId())
					&& ((preset == null) || !(inOlympiad && bot.isOlympiadStart() && isKiterPreset(preset))))
				{
					KITE_USE_STOPPED.remove(bot.getObjectId());
					if (preset != null)
					{
						BotSpawner.applyAutoCombat(bot, preset);
					}
				}
				if (preset != null)
				{
					final BerserkerState berserkerState = BERSERKER_STATES.get(bot.getObjectId());
					if (berserkerFight && (berserkerState != null))
					{
						// Only an unexpected engine stop (e.g. a teleport) is repaired inside the
						// match, and always with the driver-trimmed rotation; the deliberate
						// engine stop of a distance-breaking run is left alone.
						if (!bot.isAutoPlaying() && !berserkerState.enginesStopped)
						{
							armBerserkerCombat(bot, preset, berserkerState);
						}
					}
					else if (!OlympiadChase.isPursuing(bot.getObjectId()) && !BotTactics.isOwned(bot.getObjectId()) && (!bot.isAutoPlaying() || bot.getAutoUseSettings().getAutoSkills().isEmpty()))
					{
						BotSpawner.applyAutoCombat(bot, preset);
					}
					
					// Keep the auto-buff list in sync with the "already applied" state:
					// personal (self-targeted) buffs currently active on the bot are held
					// out of the list, so the core auto-buff engine never casts a buff the
					// bot already has (its canCastBuff refresh window covers buffs with
					// 3 or fewer seconds left). Buffs that have ended are restored here
					// every tick, so the bot re-casts them shortly after they drop.
					//
					// Admin rules: BEFORE the olympiad gates open the bot buffs nothing at
					// all (refreshAutoBuffs clears the list during the countdown), the buff
					// profile is (re)applied by the pre-start hook 2 seconds before the
					// battle and not maintained during the countdown. Coliseum rule: NO
					// buffing while the bot is in an active world fight - the full buff
					// profile and self-buffs are applied out of combat (in particular on
					// every resurrection) and never mid-fight.
					final boolean inCombat = AttackStanceTaskManager.getInstance().hasAttackStanceTask(bot);
					if (inOlympiad || !inCombat)
					{
						BotSpawner.refreshAutoBuffs(bot, preset);
						// No profile maintenance before the gates open (the pre-start hook
						// applies the full set 2 seconds before the battle).
						if (!inOlympiad || bot.isOlympiadStart())
						{
							BotSpawner.maintainFullBuffs(bot, preset);
						}
					}
					
					// Full warrior/mage buff profile + consumables: both S-grade shots active
					// and stocked (the core clears auto shots on olympiad entry), Spirit Ore
					// stock capped at 1000. Consumables are kept up everywhere - Coliseum
					// PvP, waiting for the olympiad and olympiad matches alike.
					BotSpawner.maintainConsumables(bot, preset);
					
					// Native MP regeneration for every class; Cardinal refill belongs to BotTactics.

				}

				// Charge window (see the constants above): bots of the charge classes keep
				// their sonic/force charges between CHARGE_MIN and CHARGE_MAX. Applied
				// instantly, the same way as the olympiad buff set; bots that know no
				// charge builder are skipped (they have no charge skills at all).
				if ((bot.getKnownSkill(SONIC_FOCUS_ID) != null) || (bot.getKnownSkill(FOCUS_FORCE_ID) != null))
				{
					final int charges = bot.getCharges();
					if (charges < CHARGE_MIN)
					{
						// Refill up to the window ceiling (max cap = CHARGE_MAX as well).
						bot.increaseCharges(CHARGE_MAX - charges, CHARGE_MAX);
					}
					else if (charges > CHARGE_MAX)
					{
						// Trim back into the window (a builder cast at the ceiling or a
						// Maximum Focus/Sonic Focus would otherwise sit at the retail 8).
						bot.decreaseCharges(charges - CHARGE_MAX);
					}
				}
				
				// Olympiad: the basic buff set is applied by the pre-start hook (2 seconds
				// BEFORE the gates open, both participants - bots and real players alike).
				// Once the gates are open no self-buff longer than 2 minutes may be applied
				// (admin rule), and every buff of the set lasts far longer - so the old
				// "apply once at battle start" pass was removed with its per-match marker.

				// Match the training caller: an owned driver keeps its target, including self-casts.
				if (inOlympiad && bot.isOlympiadStart() && BotTactics.isOwned(bot.getObjectId()))
				{
					BotSpawner.maintainOlympiadSummon(bot, preset, getOlympiadOpponent(bot));
					continue;
				}

				WorldObject target = bot.getTarget();

				// Re-assign when missing or dead.
				if ((target == null) || !target.isCreature() || target.asCreature().isAlikeDead())
				{
					final Player enemy = findTargetFor(bot, inOlympiad);
					if (enemy != null)
					{
						bot.setTarget(enemy);
						target = enemy;
					}
				}

				// Olympiad (active match): the ONLY valid target is the match opponent. The
				// core auto-play engine re-targets on its own and happily grabs the nearest
				// "playable" - which on the arena is the opponent's constantly re-created
				// servitor (maintainOlympiadSummon): bots spent whole matches shooting the
				// summon while its owner killed them. The opponent is forced back as the
				// target within one tick whenever the current one is anything else.
				if (inOlympiad && bot.isOlympiadStart())
				{
					final Player opponent = getOlympiadOpponent(bot);
					if ((opponent != null) && !opponent.isAlikeDead() && (target != opponent))
					{
						bot.setTarget(opponent);
						target = opponent;
						oppRetarget++;
					}
				}
				_oppRetargetTotal += oppRetarget;
				if (target == null)
				{
					continue;
				}
				withTarget++;

				if (inOlympiad && target.isPlayer() && BotTactics.tickBluffRear(bot, target.asPlayer(), preset, true)) continue;
				if (inOlympiad && target.isPlayer() && BotTactics.tickCardinalControl(bot, target.asPlayer(), preset, true)) continue;
                if (inOlympiad && target.isPlayer() && BotTactics.tickMageCancel(bot, target.asPlayer(), preset, true)) continue;
				if (inOlympiad && target.isPlayer() && BotTactics.tickInvulnerableMelee(bot, target.asPlayer(), preset, true)) continue;

				// Opening curse: mage bots open the fight with the resist-lowering
				// curse of their class (Archmage: Surrender to Fire, Mystic Muse:
				// Surrender to Water, Storm Screamer: Surrender to Wind; the necromancer:
				// Curse Gloom). Applied once per enemy, right when the bot acquires a
				// valid attackable target (world PvP and olympiad matches alike). The
				// marker resets whenever the target is gone or not attackable yet, so
				// the opener always fires at the very start of the next fight.
				// Anti-spam: a minimum interval between two applications (a target kept
				// being re-acquired - e.g. by Shadow Step target cancels - turned the
				// opener into an instant free debuff spammed every 1-2 seconds) plus the
				// skill reuse gate inside applyOpeningCurse itself.
				if ((preset != null) && target.isCreature() && !target.asCreature().isAlikeDead() && target.asCreature().isAutoAttackable(bot))
				{
					final Integer cursed = OPEN_CURSE_TARGETS.get(bot.getObjectId());
					if ((cursed == null) || (cursed.intValue() != target.getObjectId()))
					{
						final long now = System.currentTimeMillis();
						final Long lastUse = CURSE_LAST_USE.get(bot.getObjectId());
						if ((lastUse == null) || ((now - lastUse) >= OPEN_CURSE_MIN_INTERVAL_MS))
						{
							if (BotSpawner.applyOpeningCurse(bot, preset, target.asCreature()))
							{
								OPEN_CURSE_TARGETS.put(bot.getObjectId(), target.getObjectId());
								CURSE_LAST_USE.put(bot.getObjectId(), now);
							}
						}
					}
				}
				else
				{
					OPEN_CURSE_TARGETS.remove(bot.getObjectId());
				}

				// Berserker (Doombringer) olympiad duel driver: rush cycle (Rush Impact ->
				// Disarm -> Enuma Elish -> Crushing Pain -> run to the spawn point until the
				// distance allows the next Rush Impact) plus the Sword Shield rule. When the
				// driver handled this tick, the generic chase/attack-assert logic below is
				// skipped - the driver owns movement and intentions during its phases.
				final BerserkerState berserker = berserkerFight ? BERSERKER_STATES.get(bot.getObjectId()) : null;
				if ((berserker != null) && target.isCreature() && !target.asCreature().isAlikeDead() && target.asCreature().isAutoAttackable(bot)
					&& driveBerserker(bot, preset, berserker, target.asCreature()))
				{
					continue;
				}

				// Assert the attack intention (unless casting or the target is not yet attackable, e.g. olympiad countdown).
				final Intention intention = bot.getAI().getIntention();

				// Closing the distance: a fleeing player must be caught up with. Melee bots
				// continuously re-path to the target's CURRENT coordinates (world PvP and
				// olympiad alike) - the point is re-issued whenever the player has moved on
				// from the previously issued one or the bot is standing still, so the bot
				// never walks to a stale position. While the distance is being closed the
				// world auto skill rotation keeps firing; a casting bot is never interrupted.
				// Olympiad pursuit instead leases movement and pauses NEW rotation casts.
				// Active olympiad matches use the shared predictive driver instead; casters
				// and bows keep their hold ranges, melee cuts toward the bounded intercept.
				boolean closingIn = false;
				if (!target.asCreature().isAlikeDead() && (!bot.isCastingNow() || (inOlympiad && bot.isOlympiadStart() && isKiterPreset(preset))) && target.asCreature().isAutoAttackable(bot))
				{
					final boolean caster = (preset != null) && preset.isCaster();
					final double distToTarget = bot.calculateDistance2D(target);
					final boolean olympiadFight = inOlympiad && bot.isOlympiadStart();
					final int offenseRange = olympiadFight && target.isPlayer()
						? BotTactics.tacticalApproachRange(bot, target.asPlayer(), preset) : CHASE_RANGE;
					// Olympiad kiting (admin request), SMOOTH variant: bow bots must not let a
					// melee opponent pin them down. Close in only to KITE_HOLD_RANGE; when
					// pressed below KITE_MIN_RANGE, pause the skill ROTATION (the running cast
					// is never aborted, no NEW rotation cast starts) and walk one geodata-checked
					// retreat step between casts - away from the enemy, flipped toward the ENEMY
					// spawn point when the retreat comes near the OWN spawn point (never cornered
					// at the wall). Once the distance opens past KITE_MIN_RANGE the rotation
					// resumes and the bot shoots while the enemy closes in again. With no safe
					// retreat point the bot stands and fights. World (coliseum) behavior is
					// unchanged. The Dominator is NOT a kiter anymore (he fought the kite loop
					// instead of the enemy): he fights like every other caster below.
					final boolean kiter = olympiadFight && isKiterPreset(preset);
					if (kiter)
					{
						if (distToTarget < KITE_MIN_RANGE)
						{
							// Pressed by a melee opponent.
							closingIn = true;
							if (!KITE_USE_STOPPED.contains(bot.getObjectId()))
							{
								// First tick of the pressing: stop NEW rotation casts; the
								// running cast finishes and opens the retreat window.
								KITE_USE_STOPPED.add(bot.getObjectId());
								AutoUseTaskManager.getInstance().stopAutoUseTask(bot);
								LOGGER.info("FakePlayersAI: kite " + bot.getName() + ": pressed at dist " + (int) distToTarget + ", rotation paused for the retreat.");
							}
							if (!bot.isCastingNow())
							{
								final int[] ownSpawn = OLY_SPAWN_POINTS.get(bot.getObjectId());
								final Player enemyPlayer = target.isPlayer() ? target.asPlayer() : null;
								final int[] enemySpawn = (enemyPlayer != null) ? OLY_SPAWN_POINTS.get(enemyPlayer.getObjectId()) : null;
								final Location kiteTo = kitePoint(bot, target.asCreature(), ownSpawn, enemySpawn);
								if (kiteTo == null)
								{
									// No safe retreat point (wall / arena edge / enemy behind
									// every candidate): stand and fight, rotation back on.
									KITE_USE_STOPPED.remove(bot.getObjectId());
									BotSpawner.applyAutoCombat(bot, preset);
									closingIn = false;
								}
								else if (!bot.isMoving())
								{
									bot.getAI().setIntentionMoveTo(kiteTo);
								}
							}
							CHASE_POINTS.remove(bot.getObjectId());
						}
						else
						{
							// Outside the pressed zone: make sure the rotation is running.
							if (KITE_USE_STOPPED.remove(bot.getObjectId()))
							{
								BotSpawner.applyAutoCombat(bot, preset);
							}
							if (distToTarget > offenseRange)
							{
								// Shared with the fast tick: predicted intercept, stop at bow range.
								moveForOlympiadChase(bot, KITE_HOLD_RANGE, false);
								closingIn = true;
							}
							else
							{
								// Inside the fight window (KITE_MIN_RANGE..KITE_HOLD_RANGE): stand and shoot.
								CHASE_POINTS.remove(bot.getObjectId());
							}
						}
					}
					else if (caster)
					{
						if (olympiadFight && (distToTarget > offenseRange))
						{
							// Shared prediction prevents the 2-second driver from overwriting
							// the fast intercept with an obsolete direct approach.
							moveForOlympiadChase(bot, CASTER_HOLD_RANGE, false);
							closingIn = true;
						}
						else if (olympiadFight && bot.isMoving() && (intention != Intention.ATTACK))
						{
							// The enemy is inside the hold range while the bot is still walking to
							// an older (farther) point: stop the stale run and fight from here.
							// A purposeful ATTACK approach is not interrupted - the AI stops itself
							// at the strike range.
							CHASE_POINTS.remove(bot.getObjectId());
							bot.getAI().setIntentionIdle();
							closingIn = true;
						}
						else
						{
							// World casters (and a caster standing inside the hold range): the core
							// auto play engine keeps the caster at distance - no chasing. While far
							// away the attack intention is not asserted either (old behavior): the
							// rotation fires from where the caster stands.
							if (!olympiadFight && (distToTarget > CHASE_RANGE) && !bot.isMoving())
							{
								closingIn = true;
							}
							CHASE_POINTS.remove(bot.getObjectId());
						}
					}
					else if (olympiadFight && ((distToTarget > offenseRange) || ((isTankBot(bot) || getBotClassId(bot) == 113) && !GeoEngine.getInstance().canSeeTarget(bot, target))))
					{
						moveForOlympiadChase(bot, CHASE_RANGE, true);
						closingIn = true;
					}
					else if (!olympiadFight && (distToTarget > CHASE_RANGE))
					{
						// Melee: chase the player's actual position, regardless of the
						// current intention (including ATTACK - closing the distance has
						// priority, the skills fire as soon as they come into range).
						final int[] last = CHASE_POINTS.get(bot.getObjectId());
						final boolean targetMoved = (last == null) || ((Math.abs(last[0] - target.getX()) + Math.abs(last[1] - target.getY())) > CHASE_REPATH_DISTANCE);
						final boolean issue = targetMoved || !bot.isMoving();
						if (issue)
						{
							bot.getAI().setIntentionMoveTo(new Location(target.getX(), target.getY(), target.getZ()));
							CHASE_POINTS.put(bot.getObjectId(), new int[]
							{
								target.getX(),
								target.getY()
							});
							closingIn = true;
						}
						else if (!bot.isMoving())
						{
							// Walking an already issued point - do not override with ATTACK.
							closingIn = true;
						}
					}
					else
					{
						// Within strike range again - clear the chase state.
						CHASE_POINTS.remove(bot.getObjectId());
					}
				}

				// Admin rule: mage bots (Archmage / Soultaker / Mystic Muse / Storm Screamer /
				// Dominator - all "casters" in the presets) must NEVER auto-attack: in a fight
				// they only cast their magic strikes and curses. Any lingering ATTACK intention
				// (e.g. picked up from the AI after a cast) is killed here, and the attack
				// assert below is skipped for casters.
				if (!BotTactics.allowsBasicAttack(bot, preset) && !bot.isCastingNow() && (bot.getAI().getIntention() == Intention.ATTACK))
				{
					bot.abortAttack();
					bot.getAI().setIntentionIdle();
				}

				if (!closingIn && (!inOlympiad || (!isTankBot(bot) && getBotClassId(bot) != 113) || GeoEngine.getInstance().canSeeTarget(bot, target)) && !OlympiadChase.isPursuing(bot.getObjectId()) && BotTactics.allowsBasicAttack(bot, preset) && (!inOlympiad || OlympiadCombatGate.ready(bot)) && (intention != Intention.ATTACK) && (intention != Intention.CAST) && !bot.isMoving() && target.asCreature().isAutoAttackable(bot))
				{
					bot.getAI().setIntentionAttack(target);
				}

				// Tank bots (admin rules): at most ONE defensive self-buff active at a time,
				// and its group follows the opponent's class (mage -> magic defense, warrior
				// -> physical defense). Vengeance is pool-managed and never cast by the
				// rotation engine. Runs on every combat tick; see refreshTankDefenses.
				if ((preset != null) && BotManager.isTankBot(bot))
				{
					BotSpawner.refreshTankDefenses(bot, preset, target);
				}

				// Summoner classes on the olympiad (Soultaker / Hell Knight / Phoenix Knight):
				// only support the pre-start summon (never replace it after death). Transfer Pain,
				// beast soulshot stock, and attacks are maintained for a surviving servitor at the
				// match opponent. Runs on every combat tick of an active match.
				if (inOlympiad && bot.isOlympiadStart())
				{
					BotSpawner.maintainOlympiadSummon(bot, preset, target);
				}
			}

			// Non-bot olympiad participants (real players): keep the olympiad buff profile
			// maintained for the whole match - the same treatment the bots get (a buff wiped
			// mid-match, e.g. by Cancellation, is re-applied within 2 seconds). Entries are
			// dropped when the player leaves the match or the world.
			if (!OLYMPIAD_PARTICIPANTS.isEmpty())
			{
				for (Map.Entry<Integer, Player> entry : OLYMPIAD_PARTICIPANTS.entrySet())
				{
					final Player participant = entry.getValue();
					if (BOTS.containsKey(entry.getKey()) || !participant.isOnline()
						|| (!participant.isInOlympiadMode() && ((olympiad == null) || !olympiad.playerInStadia(participant))))
					{
						OLYMPIAD_PARTICIPANTS.remove(entry.getKey());
						OLY_SPAWN_POINTS.remove(entry.getKey());
						continue;
					}

					final BotClassPreset preset = BotPresets.getPresetByClassId(participant.getPlayerClass().getId());
					// Chant of Victory ban for REAL participants (same rule as for the bots):
					// strip a family buff the player may have (self-)cast during the countdown
					// or mid-fight, before the (family-free) olympiad profile is re-maintained.
					BotSpawner.stripChantBuffs(participant);
					BotSpawner.maintainFullBuffs(participant, preset != null ? preset.isCaster() : participant.isMageClass());
				}
			}
			if ((_combatCycle % 15) == 0)
			{
				LOGGER.info("FakePlayersAI: combatTick: bots=" + BOTS.size() + ", withTarget=" + withTarget + ", skippedTeleport=" + skippedTeleport + ", opp-retarget=" + oppRetarget + " (total " + _oppRetargetTotal + ").");
			}
			_combatCycle++;
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: combatTick error: " + e);
		}
	}

	/**
	 * Fast retarget pass (every second): a bot whose target is gone (canceled by effects
	 * like Shadow Step / Trick / Switch, dead, or no longer a creature) takes the enemy
	 * back into target within a second - the 2-second combat tick is too slow for that.
	 * A valid target that the bot is merely standing idle on (aborted attack/cast) also
	 * resumes the fight within the same second: non-casters get the attack intention
	 * re-asserted (the AI walks into strike range and swings), a casting or moving bot is
	 * never interrupted. Casters only receive the target - the rotation engine fires at it
	 * and the combat tick owns their closing in. Paused world bots and the berserker duel
	 * driver (which owns its bot's targeting) are skipped.
	 */
	private static void retargetTick()
	{
		try
		{
			final Olympiad olympiad = Olympiad.getInstance();
			for (Player bot : BOTS.values())
			{
				if (OrderedBattles.waitOutsideMatch(bot)) continue;
				if (holdOlympiadIfNotReady(bot)) continue;
				if (bot.isAlikeDead() || bot.isTeleporting())
				{
					BotTactics.forget(bot.getObjectId());
					continue;
				}
				final boolean inOlympiad = bot.isInOlympiadMode() || ((olympiad != null) && olympiad.playerInStadia(bot));
				if ((_combatPaused && !inOlympiad) || BERSERKER_STATES.containsKey(bot.getObjectId()))
				{
					continue;
				}

				if (BotTactics.isOwned(bot.getObjectId()))
				{
					continue; // Includes self-target casts and deliberate sleep holds.
				}
				final BotClassPreset preset = BotPresets.getPresetByClassId(getBotClassId(bot));
				if (!inOlympiad) BotTactics.maintainWorldManaFallback(bot, preset);
				final WorldObject target = bot.getTarget();
				if ((target == null) || !target.isCreature() || target.asCreature().isAlikeDead())
				{
					final Player enemy = findTargetFor(bot, inOlympiad);
					if (enemy == null)
					{
						continue;
					}
					bot.setTarget(enemy);
				}

				// Olympiad (active match): the auto-play engine may have swapped the target
				// for the opponent's servitor (the nearest "playable" on the arena) between
				// the combat ticks - force the match opponent back here too, within a second.
				if (inOlympiad && bot.isOlympiadStart())
				{
					final Player opponent = getOlympiadOpponent(bot);
					if ((opponent != null) && !opponent.isAlikeDead() && (bot.getTarget() != opponent))
					{
						bot.setTarget(opponent);
					}
				}

				// The bot has a target again: resume the fight immediately when it is doing
				// nothing (this is what makes the recovery feel instant - target AND attack).
				final boolean caster = (preset != null) && preset.isCaster();
				final WorldObject currentTarget = bot.getTarget();
				if (BotTactics.allowsBasicAttack(bot, preset) && (!inOlympiad || OlympiadCombatGate.ready(bot)) && !OlympiadChase.isPursuing(bot.getObjectId()) && !bot.isCastingNow() && !bot.isCastingSimultaneouslyNow() && !bot.isMoving() && (currentTarget != null) && currentTarget.isCreature()
					&& !currentTarget.asCreature().isAlikeDead() && currentTarget.asCreature().isAutoAttackable(bot))
				{
					final Intention intention = bot.getAI().getIntention();
					if ((intention != Intention.ATTACK) && (intention != Intention.CAST || BotTactics.needsManaAutoAttack(bot, preset)))
					{
						bot.getAI().setIntentionAttack(currentTarget.asCreature());
					}
				}
			}
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: retarget tick error: " + e);
		}
	}

	/** Fast tactical/movement pass; never drives human players or actors outside a live match. */
	private static void olympiadChaseTick()
	{
		for (Player bot : BOTS.values())
		{
			try
			{
				if (OrderedBattles.waitOutsideMatch(bot)) continue;
				if (holdOlympiadIfNotReady(bot)) continue;
				if (!bot.isInOlympiadMode() || !bot.isOlympiadStart() || bot.isAlikeDead() || bot.isTeleporting())
				{
					BotTactics.forget(bot.getObjectId());
					OlympiadChase.forget(bot.getObjectId());
					continue;
				}
				final BotClassPreset preset = BotPresets.getPresetByClassId(getBotClassId(bot));
				final Player activeOpponent = getOlympiadOpponent(bot);
				if (activeOpponent != null && ArenaCombatRules.update(bot, activeOpponent, preset)) { OlympiadCombatGate.hold(bot); continue; }
				BotTactics.tickSummonAttack(bot, activeOpponent, true);
				if (BotTactics.tickBluffRear(bot, activeOpponent, preset, true)) continue;
				if (BotTactics.tickCardinalControl(bot, activeOpponent, preset, true)) continue;
                if (BotTactics.tickMageCancel(bot, activeOpponent, preset, true)) continue;
				if (BotTactics.tickInvulnerableMelee(bot, activeOpponent, preset, true)) continue;
				final BerserkerState state = BERSERKER_STATES.get(bot.getObjectId());
				if (state != null)
				{
					final Player enemy = getOlympiadOpponent(bot);
					final int game = bot.getOlympiadGameId();
					if ((enemy != null) && (state.gameId == game) && enemy.isInOlympiadMode() && enemy.isOlympiadStart()
						&& !enemy.isAlikeDead() && !enemy.isTeleporting() && (enemy.getOlympiadGameId() == game)
						&& (enemy.getInstanceId() == bot.getInstanceId()) && enemy.isAutoAttackable(bot)
						&& (game >= 0) && (game < OLYMPIAD_STADIUM_CENTERS.length))
					{
						final int[] center = OLYMPIAD_STADIUM_CENTERS[game];
						if ((bot.calculateDistance2D(center[0], center[1], center[2]) <= OLYMPIAD_ARENA_MAX_DISTANCE)
							&& (enemy.calculateDistance2D(center[0], center[1], center[2]) <= OLYMPIAD_ARENA_MAX_DISTANCE))
						{
							if (!bot.isCastingNow() && !bot.isCastingSimultaneouslyNow() && !BotTactics.isOwned(bot.getObjectId()))
							{
								bot.setTarget(enemy);
							}
							if (!driveBerserker(bot, preset, state, enemy))
							{
								moveForOlympiadChase(bot, CHASE_RANGE, true);
							}
						}
					}
					continue;
				}
				final Player opponent = getOlympiadOpponent(bot);
				if (opponent != null) CombatAnalytics.watch(bot, opponent);
				// Continue servicing a mandatory phase owner after its live effect expires.
				if ((TacticalSettings.isEnabled() || BotTactics.isOwned(bot.getObjectId()) || CardinalSurvival.passiveTrance(bot, opponent) || CardinalSurvival.ownCelestial(bot) || CardinalSurvival.active(bot, opponent) || BluffRear.stun(bot, opponent) != null || TacticsPolicy.tankClass(preset.getClassId())) && (opponent != null))
				{
					if (!BotTactics.isOwned(bot.getObjectId()) && !bot.isCastingNow() && !bot.isCastingSimultaneouslyNow()) bot.setTarget(opponent);
					if (BotTactics.tick(bot, opponent, preset, true)) continue;
				}
				final boolean ranged = ((preset != null) && preset.isCaster()) || isKiterPreset(preset);
				moveForOlympiadChase(bot, ranged ? CASTER_HOLD_RANGE : CHASE_RANGE, !ranged);
				if (OlympiadCombatGate.ready(bot) && BotTactics.isOwned(bot.getObjectId()) && BotTactics.allowsBasicAttack(bot, preset) && opponent != null
					&& !bot.isAttackingNow() && !bot.isCastingNow() && !bot.isMovementDisabled() && !bot.isOutOfControl()
					&& !bot.isAllSkillsDisabled() && !bot.isMoving() && !opponent.isSleeping()
					&& bot.calculateDistance2D(opponent) <= BotTactics.tacticalApproachRange(bot, opponent, preset)
					&& opponent.isAutoAttackable(bot) && GeoEngine.getInstance().canSeeTarget(bot, opponent))
				{
					bot.setTarget(opponent);
					bot.getAI().setIntentionAttack(opponent);
				}
			}
			catch (Throwable e)
			{
				// One invalid participant must not stop pursuit for every other match.
				LOGGER.warning("FakePlayersAI: olympiad chase " + bot.getObjectId() + ": " + e);
			}
		}
	}

	private static void moveForOlympiadChase(Player bot, int holdRange, boolean attackInRange)
	{
		if (!OlympiadCombatGate.ready(bot))
		{
			OlympiadCombatGate.hold(bot);
			return;
		}
		final int gameId = bot.getOlympiadGameId();
		if ((gameId < 0) || (gameId >= OLYMPIAD_STADIUM_CENTERS.length))
		{
			OlympiadChase.forget(bot.getObjectId());
			return;
		}
		final Player enemy = getOlympiadOpponent(bot);
		final BotClassPreset preset = BotPresets.getPresetByClassId(getBotClassId(bot));
		final int reach = BotTactics.tacticalApproachRange(bot, enemy, preset);
		// Preserve explicit rush-positioning windows; ordinary approaches use real offense reach.
		final int approach = attackInRange || preset == null || preset.getClassId() != CLASS_DOOMBRINGER
			? Math.max(0, reach - 10) : Math.max(0, Math.min(holdRange, reach) - 10);
		final int[] center = OLYMPIAD_STADIUM_CENTERS[gameId];
		// Tanks and Titan must reach their real melee envelope. Chase's +60 attack
		// hysteresis can otherwise retain ENGAGED without a live route or strike.
		final boolean closeControlTank = preset != null && (preset.getClassId() == 90 || preset.getClassId() == 91 || preset.getClassId() == 106 || preset.getClassId() == 113 || BotTactics.strictMeleePressure(bot, enemy, preset));
		OlympiadChase.update(bot, enemy, approach, attackInRange && !closeControlTank && BotTactics.allowsBasicAttack(bot, preset),
			center[0], center[1], OLYMPIAD_ARENA_MAX_DISTANCE, !BERSERKER_STATES.containsKey(bot.getObjectId()));
	}

	/** Compatibility predicate: no active-battle MP floor for any class. */
	private static boolean olympiadMpFloorAllowed(Player player) { return false; }

	/** Keep the existing dead-task watchdog alive even while the combat gate is holding.
	 * It never arms combat and a failed readiness snapshot is not itself a timeout. */
	private static boolean watchOlympiadMatch(Player bot, boolean inOlympiad)
	{
		if (inOlympiad && bot.isOlympiadStart())
		{
			final long now = System.currentTimeMillis();
			final Player opponent = getOlympiadOpponent(bot);
			final boolean opponentGone = (opponent == null) || opponent.isAlikeDead();
			final long[] watch = OLY_MATCH_WATCH.computeIfAbsent(bot.getObjectId(), id -> new long[]
			{
				now, 0
			});
			if (opponentGone)
			{
				if (watch[1] == 0)
				{
					watch[1] = now;
				}
			}
			else
			{
				watch[1] = 0;
			}

			final boolean deadByOpponent = opponentGone && ((now - watch[1]) >= OLYMPIAD_DEAD_MATCH_RELEASE_MS);
			final boolean deadByTime = (now - watch[0]) >= OLYMPIAD_MAX_ACTIVE_MATCH_MS;
			if (deadByOpponent || deadByTime)
			{
				final String reason = deadByOpponent
					? ("opponent missing/dead for " + ((now - watch[1]) / 1000) + " s")
					: ("match exceeded the battle period (" + ((now - watch[0]) / 1000) + " s)");
				OLY_MATCH_WATCH.remove(bot.getObjectId());
				releaseBotFromDeadMatch(bot, reason);
				return true;
			}
		}
		else
		{
			OLY_MATCH_WATCH.remove(bot.getObjectId());
		}
		return false;
	}

	/** Stop on every failed readiness snapshot, including the pre-teleport countdown.
	 * A false start flag is NOT a leave: retain opener claim until actual departure. */
	private static boolean holdOlympiadIfNotReady(Player bot)
	{
		final Olympiad olympiad = Olympiad.getInstance();
		final boolean staying = bot.isInOlympiadMode() || bot.getOlympiadGameId() >= 0
			|| (olympiad != null && olympiad.playerInStadia(bot));
		if (!staying)
		{
			if (OLY_GATE_STAYS.remove(bot.getObjectId())) OlympiadCombatGate.forget(bot.getObjectId());
			return false;
		}
		OLY_GATE_STAYS.add(bot.getObjectId());
		if (OlympiadCombatGate.ready(bot)) return false;
		BotTactics.forget(bot.getObjectId()); // release scoped opponent modifiers on countdown/end/invalid match
		OlympiadCombatGate.hold(bot);
		return true;
	}

	/**
	 * Finds the enemy a bot should fight. In olympiad: the match opponent.
	 * In the world: the nearest valid (attackable) player or bot in range.
	 */
	private static Player findTargetFor(Player bot, boolean inOlympiad)
	{
		try
		{
			if (inOlympiad)
			{
				final int gameId = bot.getOlympiadGameId();
				if (gameId < 0)
				{
					return null;
				}
				final Player[] pair = Olympiad.getInstance().getPlayers(gameId);
				if (pair == null)
				{
					LOGGER.info("FakePlayersAI: oly-pair " + bot.getName() + ": gameId=" + gameId + " -> null pair");
					return null;
				}
				for (Player participant : pair)
				{
					if (participant == null)
					{
						continue;
					}
					if (participant != bot)
					{
						return participant.isAlikeDead() ? null : participant;
					}
				}
				LOGGER.info("FakePlayersAI: oly-pair " + bot.getName() + ": gameId=" + gameId + " -> no opponent in pair of " + pair.length);
				return null;
			}
			
			Player nearest = null;
			double nearestDist = Double.MAX_VALUE;
			for (Player candidate : World.getVisibleObjectsInRange(bot, Player.class, 1200, p -> (p != bot) && !p.isAlikeDead() && !p.isInOlympiadMode() && p.isAutoAttackable(bot)))
			{
				final double dist = bot.calculateDistance2D(candidate);
				if (dist < nearestDist)
				{
					nearest = candidate;
					nearestDist = dist;
				}
			}
			return nearest;
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: findTargetFor(" + bot.getName() + ") failed: " + e);
			return null;
		}
	}

	/**
	 * @return true when the class preset is an olympiad kiter: the bow bots (Sagittarius,
	 *         Moonlight Sentinel, Ghost Sentinel). The Dominator (115) used to kite as
	 *         well, but the retreat loop kept him running for most of the match instead
	 *         of fighting (admin report) - he now fights like every other caster: closes
	 *         in to the hold range and casts from there, the rotation is never paused.
	 */
	private static boolean isKiterPreset(BotClassPreset preset)
	{
		return (preset != null) && (preset.getWeaponKind() == BotClassPreset.WeaponKind.BOW);
	}

	/**
	 * @return the olympiad match opponent of the player (null when the player is not in a
	 *         match or the pair is not resolvable). Unlike findTargetFor this returns the
	 *         opponent unconditionally (alive or not) - it is the identity check for the
	 *         "the only valid olympiad target is the opponent" enforcement.
	 */
	static int[] arenaCenter(Player bot)
	{
		int game=bot.getOlympiadGameId();
		return game>=0&&game<OLYMPIAD_STADIUM_CENTERS.length?OLYMPIAD_STADIUM_CENTERS[game].clone():null;
	}

	private static Player getOlympiadOpponent(Player player)
	{
		try
		{
			final Olympiad olympiad = Olympiad.getInstance();
			final int gameId = player.getOlympiadGameId();
			if ((olympiad == null) || (gameId < 0))
			{
				return null;
			}
			final Player[] pair = olympiad.getPlayers(gameId);
			if (pair == null)
			{
				return null;
			}
			for (Player participant : pair)
			{
				if ((participant != null) && (participant != player))
				{
					return participant;
				}
			}
		}
		catch (Throwable e)
		{
			// Swallowed: a null result just means "no enforcement this tick".
		}
		return null;
	}

	/**
	 * Computes the retreat point of a kiting bot (the bow bots on the olympiad).
	 * <ul>
	 * <li>Primary direction: away from the enemy.</li>
	 * <li>Flip (the admin rule): when the bot is within KITE_FLIP_DISTANCE of its OWN arena
	 * spawn point, the direction is blended toward the ENEMY spawn point - the kite crosses
	 * the arena instead of backing the bot into the wall/corner behind its own spawn.</li>
	 * <li>Wall safety (the priority): every candidate point is checked with geodata
	 * (canMoveToTarget); rotated fallbacks (±45°, ±90°, ±135°) are tried next; a candidate
	 * that would come significantly CLOSER to the enemy is skipped. No safe candidate
	 * (null) = the bot stands where it is and fights.</li>
	 * </ul>
	 * @param bot the kiting bot
	 * @param enemy the pressing enemy
	 * @param ownSpawn the bot's own arena spawn point (may be null)
	 * @param enemySpawn the enemy's arena spawn point (may be null)
	 * @return the retreat point, or null when no safe point exists (stand and fight)
	 */
	private static Location kitePoint(Player bot, Creature enemy, int[] ownSpawn, int[] enemySpawn)
	{
		if (bot.isInOlympiadMode() && enemy.isPlayer()) return ArenaCombatRules.kitePoint(bot, enemy.asPlayer(), KITE_STEP);
		final double distToEnemy = bot.calculateDistance2D(enemy);
		if (distToEnemy <= 1)
		{
			return null;
		}

		// Away from the enemy (normalized).
		double dirX = (bot.getX() - enemy.getX()) / distToEnemy;
		double dirY = (bot.getY() - enemy.getY()) / distToEnemy;

		// Flip near the own spawn point: blend the retreat with the direction toward the
		// enemy spawn point (the kite runs back across the arena, never into the corner).
		if ((ownSpawn != null) && (enemySpawn != null) && (bot.calculateDistance2D(ownSpawn[0], ownSpawn[1], ownSpawn[2]) < KITE_FLIP_DISTANCE))
		{
			final double ex = enemySpawn[0] - bot.getX();
			final double ey = enemySpawn[1] - bot.getY();
			final double el = Math.sqrt((ex * ex) + (ey * ey));
			if (el > 1)
			{
				final double bx = (dirX * 0.6) + ((ex / el) * 1.0);
				final double by = (dirY * 0.6) + ((ey / el) * 1.0);
				final double bl = Math.sqrt((bx * bx) + (by * by));
				if (bl > 0.001)
				{
					dirX = bx / bl;
					dirY = by / bl;
				}
			}
		}

		// Candidate directions: primary + rotated fallbacks, first geodata-passable wins.
		final double[] angles =
		{
			0.0, Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2, (3 * Math.PI) / 4, (-3 * Math.PI) / 4
		};
		for (double angle : angles)
		{
			final double cos = Math.cos(angle);
			final double sin = Math.sin(angle);
			final int px = bot.getX() + (int) (((dirX * cos) - (dirY * sin)) * KITE_STEP);
			final int py = bot.getY() + (int) (((dirX * sin) + (dirY * cos)) * KITE_STEP);
			// Wall check: the move must be walkable.
			if (!GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), px, py, bot.getZ(), bot.getInstanceId()))
			{
				continue;
			}
			// Do not run INTO the enemy: the point must not come significantly closer.
			final double pdx = px - enemy.getX();
			final double pdy = py - enemy.getY();
			if (Math.sqrt((pdx * pdx) + (pdy * pdy)) < (distToEnemy - 30))
			{
				continue;
			}
			return new Location(px, py, bot.getZ());
		}
		return null;
	}
	
	// ------------------------------------------------------------------
	// Berserker (Doombringer) olympiad duel driver
	// ------------------------------------------------------------------

	/**
	 * Creates / resets / drops the berserker duel state of the bot. The state is created
	 * by the pre-start hook (the remembered appearance point); this pass is the fallback
	 * (the first tick of an active match) and the reset on a new game id. When the match
	 * is over, the state is dropped and the FULL combat setup is restored (the driver
	 * kept a trimmed live rotation while the match lasted).
	 * @param bot the berserker bot
	 * @param preset its class preset (may be null)
	 * @param inActiveMatch true while the bot fights an olympiad match (isOlympiadStart)
	 */
	private static void updateBerserkerState(Player bot, BotClassPreset preset, boolean inActiveMatch)
	{
		final int objectId = bot.getObjectId();
		final BerserkerState state = BERSERKER_STATES.get(objectId);
		if (inActiveMatch)
		{
			final int gameId = bot.getOlympiadGameId();
			if ((state == null) || (state.gameId != gameId))
			{
				BERSERKER_STATES.put(objectId, new BerserkerState(bot.getX(), bot.getY(), bot.getZ(), gameId));
				LOGGER.info("FakePlayersAI: berserker " + bot.getName() + ": spawn point recorded (" + bot.getX() + ", " + bot.getY() + "), game " + gameId + ".");
			}
		}
		else if (state != null)
		{
			BERSERKER_STATES.remove(objectId);
			if ((preset != null) && !bot.isAlikeDead())
			{
				BotSpawner.applyAutoCombat(bot, preset);
				LOGGER.info("FakePlayersAI: berserker " + bot.getName() + ": match is over, full combat setup restored.");
			}
		}
	}

	/** Serialized phase driver. A requested rush is not a landed rush. */
	private static boolean driveBerserker(Player bot, BotClassPreset preset, BerserkerState state, Creature enemy)
	{
		if (!OlympiadCombatGate.ready(bot))
		{
			OlympiadCombatGate.hold(bot);
			return true;
		}
		synchronized (state)
		{
			if (enemy.isPlayer() && BotTactics.tickInvulnerableMelee(bot, enemy.asPlayer(), preset, true))
			{
				state.enginesStopped = true;
				return true;
			}
			// Root must not hit the movement-disabled early return before magical self-cleanse.
			// This is the sole emergency exception to the enemy-STUN Rush-only phase.
			if (enemy.isPlayer() && BotTactics.tickSoulCleanse(bot, enemy.asPlayer(), preset, true))
			{
				state.enginesStopped = true;
				return true;
			}
			final long now = System.currentTimeMillis();
			if (now < state.nextDecisionAt)
			{
				return true;
			}
			state.nextDecisionAt = now + (TacticalSettings.isEnabled() ? TacticsPolicy.Difficulty.parse(BotTactics.getDifficulty(bot.getObjectId())).reactionMs : 250);
			return driveBerserkerStep(bot, preset, state, enemy, now);
		}
	}

	private static boolean driveBerserkerStep(Player bot, BotClassPreset preset, BerserkerState state, Creature enemy, long now)
	{
		if (bot.isOutOfControl() || bot.isAllSkillsDisabled())
		{
			return true; // CC is real: no aborting a cast or changing phase under control.
		}
		if (bot.isMovementDisabled())
		{
			stopBerserkerEngines(bot, state);
			if (enemy.isPlayer()) BotTactics.tickRootedBerserker(bot, enemy.asPlayer(), preset);
			return true; // root forbids movement, not legal stationary magic/physical skills
		}
		final Skill rush = bot.getKnownSkill(BERSERKER_RUSH_IMPACT_ID);
		if (rush == null)
		{
			armBerserkerCombat(bot, preset, state);
			return false;
		}
		final double dist = bot.calculateDistance2D(enemy);
		// A live native STUN owns this phase, even when MP is too low to repeat:
		// wait, never substitute autos/heals/fillers until the stun really ends.
		if (driveOwnRushStunLoop(bot, preset, state, enemy, rush, now)) return true;
		if (!state.movement.rushPending() && !bot.isCastingNow() && !bot.isCastingSimultaneouslyNow()
			&& enemy.isPlayer() && BotTactics.tickDefense(bot, enemy.asPlayer(), preset, true)) return true;
		final boolean manaFallback = BotTactics.needsManaAutoAttack(bot, preset) && BotTactics.allowsBasicAttack(bot, preset);
		if (state.movement.rushPending())
		{
			final BerserkerCycle.RushStatus status = state.movement.pollRush(now, bot.isCastingNow(), dist, bot.getX(), bot.getY());
			if (status == BerserkerCycle.RushStatus.WAIT)
			{
				return true; // Keep engines OFF until the rush actually finishes.
			}
			if (status == BerserkerCycle.RushStatus.TIMED_OUT)
			{
				final Skill current = bot.getCurrentSkill() != null ? bot.getCurrentSkill().getSkill() : null;
				if ((current != null) && (current.getId() == BERSERKER_RUSH_IMPACT_ID))
				{
					bot.abortCast(); // Only the driver's own stuck rush; core sends cancellation.
					bot.setQueuedSkill(null, false, false);
				}
			}
			if (status == BerserkerCycle.RushStatus.LANDED)
			{
				state.cycle++;
				LOGGER.info("FakePlayersAI: berserker " + bot.getName() + ": rush completed, cycle " + state.cycle + ".");
			}
			else
			{
				LOGGER.info("FakePlayersAI: berserker " + bot.getName() + ": rush not completed (" + status + "), returning to combat.");
			}
			state.rushDone = true;
			state.disarmDone = state.enumaDone = state.crushingDone = false;
			armBerserkerCombat(bot, preset, state);
			return true;
		}
		if (bot.isCastingNow())
		{
			return true; // Never abort a real filler for a rush.
		}
		if (bot.isCastingSimultaneouslyNow()) return true;
		if (manaFallback)
		{
			stopRushStunMove(bot, state);
			state.movement.endRetreat(now);
			state.retreating = false;
			if (bot.getAI().getIntention() == Intention.CAST) bot.getAI().setIntentionIdle(); // No real cast/pending remains here.
			return false; // Parent owns ordinary armed pursuit/attack, not recovery kiting.
		}
        if (TacticalSettings.isEnabled() && enemy.isPlayer() && BotTactics.recoveryOnly(bot, enemy.asPlayer(), preset))
        {
            stopBerserkerEngines(bot, state);
            return true;
        }
		if ((bot.getAI().getIntention() == Intention.CAST) && (dist > CHASE_RANGE))
		{
			// Filler useMagic can leave CAST while merely following an out-of-range foe.
			// Give that non-casting approach back to the phase driver's movement.
			stopBerserkerEngines(bot, state);
			moveForOlympiadChase(bot, CHASE_RANGE, true);
			return true;
		}

		if ((!enemy.isPlayer() || !enemy.asPlayer().isMageClass())
			&& (bot.getCurrentCpPercent() < BERSERKER_SWORD_SHIELD_CP_PERCENT)
			&& !bot.getEffectList().isAffectedBySkill(BERSERKER_SWORD_SHIELD_ID)
			&& castSkill(bot, BERSERKER_SWORD_SHIELD_ID))
		{
			return true; // Shield and rush cannot overwrite one another in one tick.
		}

        final boolean pressFinish = BerserkerCycle.shouldPressFinish(enemy.getCurrentHp(), enemy.isPlayer() ? enemy.asPlayer().getCurrentCp() : 0,
            enemy.getMaxHp(), enemy.isPlayer() ? enemy.asPlayer().getMaxCp() : 0, dist);
		if (!state.rushDone)
		{
			if ((dist >= 220) && canBerserkerCast(bot, BERSERKER_RUSH_IMPACT_ID) && state.movement.canRush(now))
			{
				stopBerserkerEngines(bot, state);
				if ((dist > rush.getCastRange()) || !GeoEngine.getInstance().canSeeTarget(bot, enemy)
					|| !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), enemy.getZ(), bot.getInstanceId()))
				{
					// Walk normally into range; do NOT start a fake long-range cast.
					moveForOlympiadChase(bot, Math.max(220, rush.getCastRange() - 50), false);
					return true;
				}
				// CHARGE moves XYZ synchronously inside beginCast: capture BEFORE useMagic.
				final int rushX = bot.getX();
				final int rushY = bot.getY();
				final BuffInfo previousStun = ownRushStun(bot, enemy);
				if (castSkill(bot, BERSERKER_RUSH_IMPACT_ID))
				{
					state.rushStun.requested(enemy, previousStun, now);
					state.movement.requested(now, rushX, rushY);
					return true;
				}
			}
			else if ((dist < 220) && !pressFinish && canBerserkerCast(bot, BERSERKER_RUSH_IMPACT_ID)
				&& state.movement.canRetreat(now, dist))
			{
				final boolean firstRetreatStep = !state.retreating;
				if (firstRetreatStep)
				{
					state.retreating = true;
					state.movement.beginRetreat(now, dist);
				}
				if (!state.movement.retreatUnproductive(now, dist))
				{
					final Location point = berserkerRetreatPoint(bot, enemy);
					if (point != null)
					{
						stopBerserkerEngines(bot, state);
						if (firstRetreatStep || !bot.isMoving())
						{
							bot.getAI().setIntentionMoveTo(point);
						}
						return true;
					}
				}
			}
			// Too close / unproductive retreat / failed or unavailable rush: fight,
			// rather than running across the entire arena and taking free damage.
			state.movement.endRetreat(now);
			state.retreating = false;
			state.rushDone = true;
			state.disarmDone = state.enumaDone = state.crushingDone = false;
		}

		if (dist <= CHASE_RANGE)
		{
			armBerserkerCombat(bot, preset, state);
		}
		// castSkill pauses the filler engine only after a skill passes the range gates.
		// Do not reset the rotation order every 250 ms while merely considering a cast.
		if (state.disarmDone && !enemy.isAffectedBySkill(BERSERKER_DISARM_ID) && canBerserkerCast(bot, BERSERKER_DISARM_ID)) state.disarmDone = false;
		if (state.enumaDone && canBerserkerCast(bot, BERSERKER_ENUMA_ELISH_ID)) state.enumaDone = false;
		if (state.crushingDone && canBerserkerCast(bot, BERSERKER_CRUSHING_PAIN_ID)) state.crushingDone = false;
		if (!state.disarmDone && (dist <= BERSERKER_DISARM_MAX_DISTANCE) && castSkill(bot, BERSERKER_DISARM_ID))
		{
			state.disarmDone = true;
			return true;
		}
		if (!state.enumaDone && castSkill(bot, BERSERKER_ENUMA_ELISH_ID))
		{
			state.enumaDone = true;
			return true;
		}
		if (!state.crushingDone && castSkill(bot, BERSERKER_CRUSHING_PAIN_ID))
		{
			state.crushingDone = true;
			return true;
		}
		if (!state.violentDone && (state.cycle > 0) && castSkill(bot, BERSERKER_VIOLENT_TEMPER_ID))
		{
			state.violentDone = true;
			return true;
		}
		if (isBerserkerSkillResolved(bot, BERSERKER_ENUMA_ELISH_ID, state.enumaDone)
			&& isBerserkerSkillResolved(bot, BERSERKER_CRUSHING_PAIN_ID, state.crushingDone))
		{
			// Request another cycle only after the bounded retreat cooldown.
			if (canBerserkerCast(bot, BERSERKER_RUSH_IMPACT_ID)
				&& ((dist >= 220 && state.movement.canRush(now)) || (!pressFinish && state.movement.canRetreat(now, dist))))
			{
				state.rushDone = false;
			}
		}
		if (dist > CHASE_RANGE)
		{
			// No in-range phase skill to cast: keep BOTH engines stopped for this
			// approach. Re-arming every 250 ms would recreate the CAST-follow loop.
			stopBerserkerEngines(bot, state);
			moveForOlympiadChase(bot, CHASE_RANGE, true);
			return true;
		}
		AutoUseTaskManager.getInstance().startAutoUseTask(bot);
		if (OlympiadCombatGate.ready(bot) && BotTactics.allowsBasicAttack(bot, preset) && bot.getAI().getIntention() != Intention.ATTACK)
		{
			bot.getAI().setIntentionAttack(enemy);
		}
		return dist <= CHASE_RANGE;
	}

	/** Applied BuffInfo, not Skill.hasEffectType or an arbitrary target.isStunned(). */
	private static boolean isOwnRushStun(Player bot, Creature enemy, BuffInfo info)
	{
		if (bot == null || enemy == null || info == null || info.getEffector() != bot || info.getEffected() != enemy || !info.isInUse()
			|| info.isRemoved() || info.getSkill() == null || info.getSkill().getId() != BERSERKER_RUSH_IMPACT_ID) return false;
		for (var effect : info.getEffects())
		{
			if (effect.getEffectType() == EffectType.STUN && (effect.getEffectFlags() & EffectFlag.STUNNED.getMask()) != 0) return true;
		}
		return false; // Damage-only/resisted Rush is not a stun, even when its template has Stun.
	}

	private static BuffInfo ownRushStun(Player bot, Creature enemy)
	{
		return RushStunSupport.stun(enemy);
	}

	/** High-priority ranked control phase, bounded by the current actually applied STUN.
	 * No synthetic status, effect timer edit, teleport, forced reuse, or filler-cast abort. */
	private static boolean driveOwnRushStunLoop(Player bot, BotClassPreset preset, BerserkerState state, Creature enemy, Skill rush, long now)
	{
		if (preset == null || preset.getClassId() != CLASS_DOOMBRINGER || !enemy.isPlayer()
			|| state.gameId != bot.getOlympiadGameId() || getOlympiadOpponent(bot) != enemy) return false;
		final BuffInfo applied = ownRushStun(bot, enemy);
		final int observation = state.rushStun.observe(enemy, applied, now);
		if (observation == RushStunLoop.ENDED) state.stunLoopReleasing = true;
		else if (observation == RushStunLoop.ACTIVE) state.stunLoopReleasing = false; // A genuinely new STUN can land while the last cast finishes.
		if (state.stunLoopReleasing)
		{
			if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) return true; // The actual last charge is allowed to finish.
			stopRushStunMove(bot, state);
			state.stunLoopReleasing = false;
			state.movement.pollRush(now, false, bot.calculateDistance2D(enemy), bot.getX(), bot.getY());
			state.rushDone = true;
			state.disarmDone = state.enumaDone = state.crushingDone = false;
			armBerserkerCombat(bot, preset, state);
			return false; // No live STUN: ordinary phase driver resumes immediately.
		}
		if (observation != RushStunLoop.ACTIVE) return false;
		// Force both pools off, even if an external maintenance call re-armed them.
		AutoPlayTaskManager.getInstance().stopAutoPlay(bot);
		AutoUseTaskManager.getInstance().stopAutoUseTask(bot);
		state.enginesStopped = true;
		if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) return true;
		bot.abortAttack();
		bot.setQueuedSkill(null, false, false);
		if (bot.getAI().getIntention() == Intention.ATTACK || bot.getAI().getIntention() == Intention.CAST)
		{
			bot.getAI().notifyActionCancel();
			bot.getAI().setIntentionIdle();
		}
		if (bot.getTarget() != enemy) bot.setTarget(enemy);
		final double distance = bot.calculateDistance2D(enemy);
		if (distance < BERSERKER_RUSH_MIN_DISTANCE)
		{
			if (state.stunLoopMoving && now - state.stunLoopMoveAt < 1000 && bot.isMoving()) return true;
			// A blocked/stalled retreat cannot become an unbounded run or cause a fake cast.
			if (state.stunLoopMoving && now - state.stunLoopMoveAt >= 1000
				&& distance - state.stunLoopMoveDistance < 20)
			{
				stopRushStunMove(bot, state);
				return true;
			}
			final int game = bot.getOlympiadGameId();
			if (game < 0 || game >= OLYMPIAD_STADIUM_CENTERS.length) return true;
			final int[] center = OLYMPIAD_STADIUM_CENTERS[game];
			for (Location point : rushStunRetreatCandidates(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), center[0], center[1], ArenaCombatRules.retreatRadius(bot, enemy, OLYMPIAD_ARENA_MAX_DISTANCE)))
			{
				if (!GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), point.getX(), point.getY(), point.getZ(), bot.getInstanceId())) continue;
				// Re-read the exact identity and official gate immediately before changing movement.
				if (!OlympiadCombatGate.ready(bot) || ownRushStun(bot, enemy) != state.rushStun.original) return true;
				state.stunLoopMoving = true;
				state.stunLoopMoveAt = now;
				state.stunLoopMoveDistance = distance;
				bot.getAI().setIntentionMoveTo(point);
				return true;
			}
			stopRushStunMove(bot, state);
			return true;
		}
		stopRushStunMove(bot, state);
		if (!rushStunRepeatAllowed(ownRushStun(bot, enemy) == state.rushStun.original, OlympiadCombatGate.ready(bot),
			canBerserkerCast(bot, BERSERKER_RUSH_IMPACT_ID), bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled(),
			bot.isDisarmed(), rush.isBlockedInOlympiad(), GeoEngine.getInstance().canSeeTarget(bot, enemy),
			GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), enemy.getZ(), bot.getInstanceId()),
			rush.checkCondition(bot, enemy, false), distance, rush.getCastRange(), Math.abs((double) bot.getZ() - enemy.getZ()))) return true;
		castSkill(bot, BERSERKER_RUSH_IMPACT_ID); // Normal useMagic pays MP/reuse/conditions; no synthetic stun or duration refresh.
		return true;
	}

	/** Pure safety matrix shared by the real repeat branch and server-free regression tests.
	 * canCast is the ordinary known-skill/reuse/MP/item/charge check, not a fake readiness flag. */
	private static boolean rushStunRepeatAllowed(boolean sameOriginal, boolean gateReady, boolean canCast, boolean controlled,
		boolean disarmed, boolean blocked, boolean visible, boolean walkable, boolean conditions, double distance, double castRange, double deltaZ)
	{
		return sameOriginal && gateReady && canCast && !controlled && !disarmed && !blocked && visible && walkable && conditions
			&& Double.isFinite(distance) && Double.isFinite(castRange) && Double.isFinite(deltaZ)
			&& distance >= BERSERKER_RUSH_MIN_DISTANCE && distance <= castRange && deltaZ < 150;
	}

	private static void stopRushStunMove(Player bot, BerserkerState state)
	{
		if (!state.stunLoopMoving) return;
		if (!bot.isCastingNow() && !bot.isCastingSimultaneouslyNow() && bot.getAI().getIntention() == Intention.MOVE_TO)
		{
			bot.stopMove(null);
			bot.getAI().setIntentionIdle();
		}
		state.stunLoopMoving = false;
	}

	/** Pure bounded geometry; callers must still validate server geodata and the live gate. */
	private static List<Location> rushStunRetreatCandidates(int x, int y, int z, int enemyX, int enemyY, int centerX, int centerY, double arenaRadius)
	{
		return RushStunSupport.retreat(x, y, z, enemyX, enemyY, centerX, centerY, arenaRadius);
	}

	/** Short local retreat, never an unconditional run to the arena spawn point. */
	private static Location berserkerRetreatPoint(Player bot, Creature enemy)
	{
		final int[] center = arenaCenter(bot);
		if (center == null) return null;
		for (Location point : RushStunSupport.retreat(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), center[0], center[1],
			ArenaCombatRules.retreatRadius(bot, enemy, OLYMPIAD_ARENA_MAX_DISTANCE)))
			if (GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), point.getX(), point.getY(), point.getZ(), bot.getInstanceId())) return point;
		return null;
	}

	/**
	 * @return true when the burst skill needs no further action this cycle: it was cast,
	 *         or it is still on its reuse delay (it is never waited for), or it is unknown.
	 */
	private static boolean isBerserkerSkillResolved(Player bot, int skillId, boolean done)
	{
		if (done)
		{
			return true;
		}
		final Skill skill = bot.getKnownSkill(skillId);
		return (skill == null) || bot.isSkillDisabled(skill);
	}

	/**
	 * Casts one of the driver's skills through the regular player cast path (full reuse,
	 * condition, MP, consumable and olympiad checks apply; an out-of-range request
	 * is rejected instead of creating a cast intention that walks across the arena). The bot's target is already the enemy (or the skill is
	 * self-targeted), so no target juggling is needed.
	 * @return true when the cast was initiated
	 */
	private static boolean castSkill(Player bot, int skillId)
	{
		if (bot.isCastingNow() || bot.isOutOfControl() || bot.isMovementDisabled()
			|| (bot.getAI().getIntention() == Intention.CAST) || !canBerserkerCast(bot, skillId))
		{
			return false;
		}
		final Skill skill = bot.getKnownSkill(skillId);
		final WorldObject target = bot.getTarget();
		if (skill.getCastRange() > 0 && ((target == null)
			|| (bot.calculateDistance2D(target) > skill.getCastRange() + bot.getTemplate().getCollisionRadius())
			|| !GeoEngine.getInstance().canSeeTarget(bot, target)))
		{
			return false;
		}
		AutoUseTaskManager.getInstance().stopAutoUseTask(bot);
		return bot.useMagic(skill, true, true);
	}

	/**
	 * Cheap pre-check of a driver skill cast (reuse, MP, consumables, charges) WITHOUT
	 * initiating it - used to decide the phase before moving (a rush that cannot be paid
	 * for must not trigger a distance-breaking run).
	 */
	private static boolean canBerserkerCast(Player bot, int skillId)
	{
		final Skill skill = bot.getKnownSkill(skillId);
		if (skill == null)
		{
			return false;
		}
		if (bot.isSkillDisabled(skill))
		{
			return false;
		}
		final int mp = skill.getMpInitialConsume() + skill.getMpConsume();
		if ((mp > 0) && (bot.getCurrentMp() < mp))
		{
			return false;
		}
		if ((skill.getItemConsumeCount() > 0)
			&& (bot.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()))
		{
			return false;
		}
		return bot.getCharges() >= skill.getChargeConsumeCount();
	}

	/**
	 * Stops both auto combat engines for the distance-breaking run: no autoattack (the
	 * bot must not stop to swing), no rotation casts and no auto buffs (nothing may
	 * interrupt the run toward the spawn point). Called once per run (the flag guards it).
	 */
	private static void stopBerserkerEngines(Player bot, BerserkerState state)
	{
		if (!state.enginesStopped)
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(bot);
			AutoUseTaskManager.getInstance().stopAutoUseTask(bot);
			state.enginesStopped = true;
		}
	}

	/**
	 * (Re)arms the auto combat engines of a berserker bot inside its olympiad match and
	 * trims the live rotation down to the non-managed skills (see BERSERKER_MANAGED_SKILLS):
	 * the opener/burst skills and the other rush/charge skills are cast by the driver
	 * only, so they must not be fired by the rotation engine on its own. Cheap: it does
	 * real work only when the engines were stopped, went missing, or the live rotation
	 * contains a managed skill again (e.g. after an admin re-apply).
	 */
	private static void armBerserkerCombat(Player bot, BotClassPreset preset, BerserkerState state)
	{
		if (state.rushStun.original != null || state.stunLoopReleasing) return;
		if (!OlympiadCombatGate.ready(bot))
		{
			OlympiadCombatGate.hold(bot);
			return;
		}
		if (state.enginesStopped || !bot.isAutoPlaying() || rotationHasManagedSkill(bot))
		{
			BotSpawner.applyAutoCombat(bot, preset);
			trimBerserkerRotation(bot);
			state.enginesStopped = false;
			state.retreating = false;
		}
	}

	/**
	 * Removes the driver-managed skills from the bot's live auto rotation (the preset and
	 * the //bot overrides stay untouched - the trim lives only while the match lasts and
	 * is undone by updateBerserkerState when the match is over).
	 */
	private static void trimBerserkerRotation(Player bot)
	{
		bot.getAutoUseSettings().getAutoSkills().removeIf(BotManager::isManagedBerserkerSkill);
	}

	/**
	 * @return true when the bot's live rotation still contains a driver-managed skill.
	 */
	private static boolean rotationHasManagedSkill(Player bot)
	{
		for (int skillId : bot.getAutoUseSettings().getAutoSkills())
		{
			if (isManagedBerserkerSkill(skillId))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true when the skill id belongs to the berserker driver (cast by the driver
	 *         itself or excluded from the olympiad rotation to protect the cycle).
	 */
	private static boolean isManagedBerserkerSkill(int skillId)
	{
		for (int managed : BERSERKER_MANAGED_SKILLS)
		{
			if (managed == skillId)
			{
				return true;
			}
		}
		return false;
	}

	public static int[] randomSpawnPoint()
	{
		final BotSettings settings = SETTINGS;
		final int angle = Rnd.get(360);
		final int radius = Rnd.get(0, settings.getSpawnRadius());
		final int x = settings.getSpawnX() + (int) (radius * Math.cos(Math.toRadians(angle)));
		final int y = settings.getSpawnY() + (int) (radius * Math.sin(Math.toRadians(angle)));
		return new int[]
		{
			x,
			y,
			settings.getSpawnZ()
		};
	}
	
	// ------------------------------------------------------------------
	// Spawn / despawn
	// ------------------------------------------------------------------
	
	/**
	 * Covers missing roster professions first, then cycles for explicitly configured extra slots.
	 */
	public static synchronized int spawnAll()
	{
		if (!SETTINGS.isEnabled() || SETTINGS.getBotCount() <= 0)
		{
			return 0;
		}
		
		int spawned = 0;
		final List<BotClassPreset> missing = missingRosterPresets();
		// A stale18 limit or restored duplicates must not silently exclude Cardinal.
		// Keep existing actors; class coverage takes precedence over the configured count.
		final int targetCount = Math.max(SETTINGS.getBotCount(), BOTS.size() + missing.size());
		if (targetCount > SETTINGS.getBotCount())
			LOGGER.warning("FakePlayersAI: extending roster target to " + targetCount + " for missing professions; configured BotCount=" + SETTINGS.getBotCount());
		// Cover missing professions before creating second/third bots of existing classes.
		for (BotClassPreset preset : missing)
		{
			if (spawnBot(preset) != null) spawned++;
		}
		final List<BotClassPreset> stillMissing = missingRosterPresets();
		if (!stillMissing.isEmpty())
		{
			LOGGER.warning("FakePlayersAI: roster incomplete; missing classes " + stillMissing.stream().map(BotClassPreset::getClassName).toList()
				+ "; leaving slots open instead of hiding spawn failures behind duplicate classes.");
			return spawned;
		}
		int guard = 0;
		while ((BOTS.size() < targetCount) && (guard++ < 200))
		{
			final BotClassPreset preset = BotPresets.getNextPreset();
			if (preset == null)
			{
				break;
			}
			if (spawnBot(preset) != null)
			{
				spawned++;
			}
		}
		
		if (spawned > 0)
		{
			LOGGER.info("FakePlayersAI: spawned " + spawned + " bots (total " + BOTS.size() + ").");
		}
		return spawned;
	}
	
	private static List<BotClassPreset> missingRosterPresets()
	{
		final Set<Integer> active = new java.util.HashSet<>();
		for (Player bot : BOTS.values()) active.add(getBotClassId(bot));
		final List<BotClassPreset> missing = new ArrayList<>();
		for (BotClassPreset preset : BotPresets.getPresets())
			if (!active.contains(preset.getClassId())) missing.add(preset);
		return missing;
	}

	public static synchronized Player spawnBot(BotClassPreset preset)
	{
		try
		{
			// Older saves could omit the last bot although its character still owns the name.
			// Restore only verified offline generated-bot accounts, never arbitrary names.
			for (BotRosterRecovery.Candidate candidate : BotRosterRecovery.findCardinal(preset))
			{
				if (BOT_CLASSES.putIfAbsent(candidate.objectId(), preset.getClassId()) == null)
				{
					REGISTRY_NAMES.put(candidate.objectId(), candidate.name());
					LOGGER.info("FakePlayersAI: recovered Cardinal registry candidate " + candidate.name() + " objectId=" + candidate.objectId());
				}
			}
			// Prefer the canonical roster owner, not an arbitrary old suffixed duplicate.
			final List<Map.Entry<Integer, Integer>> candidates = new ArrayList<>(BOT_CLASSES.entrySet());
			if (BotSpawner.staticClassNickname(preset))
			{
				final List<Integer> owners = new ArrayList<>();
				for (String name : BotSpawner.fixedClassNames(BotSpawner.classNickname(preset)))
					owners.add(org.l2jmobius.gameserver.data.sql.CharInfoTable.getInstance().getIdByName(name));
				candidates.sort(java.util.Comparator.comparingInt(e -> owners.contains(e.getKey()) ? 0 : 1));
			}
			for (Map.Entry<Integer, Integer> entry : candidates)
			{
				if ((entry.getValue() == preset.getClassId()) && !BOTS.containsKey(entry.getKey()))
				{
					final Player loaded = loadBot(entry.getKey());
					if (loaded != null)
					{
						return registerBot(loaded, preset);
					}
				}
			}
			
			// Create a brand new character: the nickname is the class name itself
			// (stable semantic fallback for Cardinal/Duelist, no suffix) -
			// the bot name displays its class from
			// the very start. ensureClassNickname() in registerBot() only handles
			// renames of bots restored from the registry/database.
			final String name = BotSpawner.generateClassNickname(preset);
			final Player bot = BotSpawner.createBot(preset, name);
			if (bot == null)
			{
				return null;
			}
			return registerBot(bot, preset);
		}
		catch (Throwable e)
		{
			LOGGER.log(Level.WARNING, "FakePlayersAI: spawnBot(" + preset.getClassName() + ") failed", e);
			return null;
		}
	}
	
	private static Player loadBot(int objectId)
	{
		try
		{
			final Player player = Player.load(objectId);
			return player;
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: failed to load bot objectId " + objectId + ": " + e);
			BOT_CLASSES.remove(objectId);
			return null;
		}
	}
	
	private static Player registerBot(Player bot, BotClassPreset preset)
	{
		// Class-based nickname: the displayed name shows the bot's class ("Duelist"
		// for the Duelist; static semantic fallback for97/88, numeric suffix for others). Existing bots are
		// renamed too - the rename happens BEFORE world entry, so the database row and
		// the CharInfo broadcast of the spawn carry the final name.
		BotSpawner.ensureClassNickname(bot, preset);
		// The combat title (HP+CP percent) is set BEFORE the world entry as well, so the
		// spawn broadcast (CharInfo) already carries it - no extra title packet needed.
		updateCombatTitle(bot);
		final int[] point = randomSpawnPoint();
		if (!BotSpawner.enterWorld(bot, point[0], point[1], point[2]))
		{
			return null;
		}
		// Gear integrity: weapon/armor per tier settings; permanent five-piece epic jewelry must
		// be equipped and must pass its item conditions (a restricted piece - e.g. the
		// Kamael-incompatible Vesper heavy - would be unequipped by the core right at the
		// olympiad arena entry). Re-equips the preset set when something is wrong. Idempotent.
		BotSpawner.ensurePresetGear(bot, preset);
		// Skill enchant policy (debuff Chance first, others unchanged, real route maximum) - covers both freshly
		// created bots and bots restored from the registry/database. Idempotent.
		BotSpawner.applyClassSkills(bot);
		BotSpawner.applyBotOnlySkills(bot, preset);
		BotSpawner.applySkillEnchants(bot);
		// The class opening curse (mage bots: the elemental Surrender, the necromancer:
		// Curse Gloom) must sit on the maximum Chance enchant route (highest trigger
		// rate) - guaranteed here, independent of the general enchant policy. Idempotent.
		BotSpawner.ensureOpeningCurseEnchant(bot, preset);
		BotSpawner.applyAutoCombat(bot, preset);
		BOTS.put(bot.getObjectId(), bot);
		BOT_CLASSES.put(bot.getObjectId(), preset.getClassId());
		REGISTRY_NAMES.put(bot.getObjectId(), bot.getName());
		// Universal passive (Subclass Counter Celestial, skill 655) for freshly created
		// bots; bots restored via Player.load receive it from the core restoreSkills().
		BotSpawner.applyCounterCelestial(bot);
		// All bots: clear the legacy fixed25% gate; use native skill/resistance/enchant rules.
		updateTankDebuffRegistration(bot, preset);
		// Tank bots: P.Def/M.Def reduced by 50% (admin rule) - two MUL stat functions with
		// a dedicated owner; non-tank bots get the functions removed the same way.
		updateTankDefenseReduction(bot, preset);
		// Persist EVERY successful registration, including an already-correct freshly created
		// name. Saving before registerBot omitted the last new bot from the on-disk roster.
		saveRegistry();
		LOGGER.info("FakePlayersAI: bot " + bot.getName() + " (" + preset.getClassName() + ") is in the world.");
		return bot;
	}
	
	public static synchronized void despawnBot(Player bot)
	{
		if (bot == null)
		{
			return;
		}
		try
		{
			BotSpawner.stopAutoCombat(bot);
			Disconnection.of(bot).storeAndDelete();
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: despawn of " + bot.getName() + " failed: " + e);
		}
		BotTactics.forget(bot.getObjectId());
		OlympiadRegistration.clear(bot.getObjectId());
		BOTS.remove(bot.getObjectId());
		OLY_GATE_STAYS.remove(bot.getObjectId());
		OlympiadCombatGate.forget(bot.getObjectId());
		OlympiadChase.forget(bot.getObjectId());
		CHASE_POINTS.remove(bot.getObjectId());
		OPEN_CURSE_TARGETS.remove(bot.getObjectId());
		CURSE_LAST_USE.remove(bot.getObjectId());
		KITE_USE_STOPPED.remove(bot.getObjectId());
		BERSERKER_STATES.remove(bot.getObjectId());
		OLYMPIAD_PARTICIPANTS.remove(bot.getObjectId());
		OLY_SPAWN_POINTS.remove(bot.getObjectId());



		BOT_TITLES.remove(bot.getObjectId());
		// Tank-bot fixed debuff chance: drop the registration (no leak into reuse of
		// the objectId by another character).
		Formulas.FIXED_DEBUFF_RATE_PLAYERS.remove(bot.getObjectId());
		// Tank-bot defense reduction: drop the stat functions (same leak protection).
		bot.removeStatsOwner(TANK_DEFENSE_FUNC_OWNER);
	}
	
	public static synchronized void despawnAll()
	{
		for (Player bot : new ArrayList<>(BOTS.values()))
		{
			despawnBot(bot);
		}
	}
	
	/**
	 * Deletes bot characters from the database and removes them from the registry.
	 */
	public static synchronized int deleteAll()
	{
		int count = 0;
		for (Player bot : new ArrayList<>(BOTS.values()))
		{
			despawnBot(bot);
			count++;
		}
		BOT_CLASSES.clear();
		DEATH_TIMES.clear();
		saveRegistry();
		return count;
	}
	
	// ------------------------------------------------------------------
	// Registry persistence (objectId;name;classId)
	// ------------------------------------------------------------------
	
	public static void saveRegistry()
	{
		final Properties props = new Properties();
		for (Map.Entry<Integer, Integer> entry : BOT_CLASSES.entrySet())
		{
			final Player bot = BOTS.get(entry.getKey());
			// Bots outside the world keep their registry name (a rename must not be
			// lost in the file by a despawned bot's entry).
			final String name = bot != null ? bot.getName() : REGISTRY_NAMES.getOrDefault(entry.getKey(), "unknown");
			props.setProperty(String.valueOf(entry.getKey()), name + ";" + entry.getValue());
		}
		try (OutputStream os = new FileOutputStream(REGISTRY_PATH))
		{
			props.store(os, "# FakePlayersAI bot registry: objectId = name;classId");
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: registry save failed: " + e.getMessage());
		}
	}
	
	public static void loadRegistry()
	{
		BOT_CLASSES.clear();
		REGISTRY_NAMES.clear();
		final File file = new File(REGISTRY_PATH);
		if (!file.exists())
		{
			return;
		}
		final Properties props = new Properties();
		try (InputStream is = new FileInputStream(file))
		{
			props.load(is);
		}
		catch (IOException e)
		{
			LOGGER.warning("FakePlayersAI: registry load failed: " + e.getMessage());
			return;
		}
		for (String key : props.stringPropertyNames())
		{
			try
			{
				final int objectId = Integer.parseInt(key);
				final String[] parts = props.getProperty(key).split(";");
				final int classId = Integer.parseInt(parts[1]);
				BOT_CLASSES.put(objectId, classId);
				REGISTRY_NAMES.put(objectId, parts[0]);
			}
			catch (Exception e)
			{
				LOGGER.warning("FakePlayersAI: bad registry entry: " + key);
			}
		}
		LOGGER.info("FakePlayersAI: registry loaded, " + BOT_CLASSES.size() + " known bots.");
	}
	
	// ------------------------------------------------------------------
	// Accessors
	// ------------------------------------------------------------------
	
	/**
	 * Re-applies the (possibly edited) skill rotation of a preset to all live bots of that class.
	 */
	public static void applyRotationToBots(BotClassPreset preset)
	{
		if (preset == null)
		{
			return;
		}
		for (Player bot : BOTS.values())
		{
			if (getBotClassId(bot) == preset.getClassId())
			{
				BotSpawner.applyAutoCombat(bot, preset);
			}
		}
	}
	
	public static BotSettings getSettings()
	{
		return SETTINGS;
	}
	
	public static Map<Integer, Player> getBots()
	{
		return BOTS;
	}
	
	public static int getBotClassId(Player player)
	{
		return BOT_CLASSES.getOrDefault(player.getObjectId(), player.getPlayerClass().getId());
	}
	
	public static boolean isBot(Player player)
	{
		return (player != null) && BOTS.containsKey(player.getObjectId());
	}

	/**
	 * @return true when the player is a FakePlayersAI bot of a dagger class (Adventurer,
	 *         Wind Rider, Ghost Hunter). The skill effect handlers (handlers.skill.effects)
	 *         give such bots a fixed 70% success chance (DAGGER_SKILL_SUCCESS_PERCENT) on
	 *         all damage-dealing skills - the calcSuccess rolls (skill evasion, blow
	 *         chance, Backstab behind requirement, magic resistance) are replaced by
	 *         isDaggerBotSkillSuccess() for them. Real players keep the retail rates.
	 */
	public static boolean isDaggerBot(Player player)
	{
		if ((player == null) || !BOTS.containsKey(player.getObjectId()))
		{
			return false;
		}
		final int classId = getBotClassId(player);
		for (int daggerClassId : DAGGER_CLASS_IDS)
		{
			if (daggerClassId == classId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The fixed success roll of a dagger bot's damage-dealing skill: passes with
	 * DAGGER_SKILL_SUCCESS_PERCENT probability. The effect handlers return this result
	 * directly (instead of running their retail checks), so the rate is exactly 70%.
	 * The caller must have verified isDaggerBot() already.
	 * @param player the casting dagger bot
	 * @return true when the fixed roll passed
	 */
	public static boolean isDaggerBotSkillSuccess(Player player)
	{
		return (player != null) && (Rnd.get(100) < DAGGER_SKILL_SUCCESS_PERCENT);
	}

	/**
	 * @return true when the player is a FakePlayersAI bot of a tank class (Phoenix Knight,
	 *         Hell Knight, Shillien Templar).
	 */
	public static boolean isTankBot(Player player)
	{
		if ((player == null) || !BOTS.containsKey(player.getObjectId()))
		{
			return false;
		}
		final int classId = getBotClassId(player);
		for (int tankClassId : TANK_CLASS_IDS)
		{
			if (tankClassId == classId)
			{
				return true;
			}
		}
		return false;
	}

	/** Clear only this bot's legacy fixed-debuff registration. Native formulas apply. */
	public static void updateTankDebuffRegistration(Player bot, BotClassPreset preset)
	{
		if (bot != null) Formulas.FIXED_DEBUFF_RATE_PLAYERS.remove(bot.getObjectId());
	}

	/**
	 * Applies/removes the tank-bot defense reduction (admin rule of 16.09.2026): tank bots
	 * (Phoenix Knight 90 / Hell Knight 91 / Shillien Templar 106) get two MUL stat functions
	 * that multiply their P.Def and M.Def by TANK_DEFENSE_MULTIPLIER (0.5 = the defense is
	 * halved); all other bots get the functions removed. Called on every bot registration
	 * (idempotent: the previous owner-owned functions are dropped first) and the functions
	 * are dropped again in despawnBot. Order 1 = applied over the summed base/gear/buff
	 * values, exactly like the core MUL functions of the skill effect handlers.
	 * @param bot the bot
	 * @param preset the bot's class preset
	 */
	public static void updateTankDefenseReduction(Player bot, BotClassPreset preset)
	{
		if ((bot == null) || (preset == null))
		{
			return;
		}

		// Idempotency guard: drop this bot's previous functions of the owner first (a
		// re-registration would otherwise stack a second -50% over the first one).
		bot.removeStatsOwner(TANK_DEFENSE_FUNC_OWNER);

		boolean isTank = false;
		final int classId = preset.getClassId();
		for (int tankClassId : TANK_CLASS_IDS)
		{
			if (tankClassId == classId)
			{
				isTank = true;
				break;
			}
		}

		if (isTank)
		{
			bot.addStatFunc(new FuncMul(Stat.POWER_DEFENCE, 1, TANK_DEFENSE_FUNC_OWNER, TANK_DEFENSE_MULTIPLIER, null));
			bot.addStatFunc(new FuncMul(Stat.MAGIC_DEFENCE, 1, TANK_DEFENSE_FUNC_OWNER, TANK_DEFENSE_MULTIPLIER, null));
		}
	}

	/**
	 * Refreshes the combat title of the bot: the combined HP+CP percent - the average of
	 * the current HP and CP percentages (100% HP + 100% CP -> "100%", 100% HP + 0% CP ->
	 * "50%"). Called on every combat tick (2 seconds), right before the world entry and
	 * after out-of-combat heals; the title packet is only broadcast when the text actually
	 * changed. The title replaces the static BotTitle value for as long as the bot lives.
	 * @param bot the bot whose title is refreshed
	 */
	public static void updateCombatTitle(Player bot)
	{
		try
		{
			final double hpPercent = (bot.getMaxHp() > 0) ? ((bot.getCurrentHp() * 100.0) / bot.getMaxHp()) : 0.0;
			final double cpPercent = (bot.getMaxCp() > 0) ? ((bot.getCurrentCp() * 100.0) / bot.getMaxCp()) : 0.0;
			final String title = Math.round((hpPercent + cpPercent) / 2.0) + "%";
			if (!title.equals(BOT_TITLES.get(bot.getObjectId())))
			{
				BOT_TITLES.put(bot.getObjectId(), title);
				bot.setTitle(title);
				bot.broadcastTitleInfo();
			}
		}
		catch (Throwable e)
		{
			LOGGER.warning("FakePlayersAI: combat title update of " + bot.getName() + " failed: " + e);
		}
	}
	
	public static List<Player> getAliveBots()
	{
		final List<Player> list = new ArrayList<>();
		for (Player bot : BOTS.values())
		{
			if (!bot.isAlikeDead())
			{
				list.add(bot);
			}
		}
		return list;
	}
	
	/**
	 * Script entry point (called by the ScriptEngine for each script file).
	 */
	public static void main(String[] args)
	{
		loadRegistry();
		init();
	}
}
