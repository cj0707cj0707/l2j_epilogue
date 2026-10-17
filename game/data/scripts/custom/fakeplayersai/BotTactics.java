/* Contextual arena combat. One owner, real useMagic; scoped Cardinal opponent-regeneration rule. */
package custom.fakeplayersai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

/**
 * Call on the parent's serialized arena pass after the pre-start opener.
 * true means no generic move/attack this tick (including sleep/reaction/CC holds).
 * false permits generic pursuit/attack, NOT restarting auto engines while isOwned.
 * State is bounded to 2048 bots/64 failures; effect identity uses only a weak reference.
 */
public final class BotTactics
{
	private static final int MAX_BOTS = 2048;
	// Cardinal-specific IDs for timing gates and Trance-kite logic.
	private static final int CARDINAL_CLASS_ID = 97;
	private static final int CARDINAL_TRANCE_ID = 1394;
	private static final int CARDINAL_MANA_BURN_ID = 1398;
	private static final int CARDINAL_PROMINENCE_ID = 1230;
	private static final int CARDINAL_HEAL_ID = 1218;
	private static final long TTL_MS = 30 * 60_000L;
	private static final Map<Integer, State> STATES = new LinkedHashMap<>(32, .75f, true);
	private static TacticsPolicy.Difficulty defaultDifficulty = TacticsPolicy.Difficulty.STRONG;
	private static long nextSweep;

	private static final class Failure
	{
		int count;
		long until;
	}
	private static final class State
	{
		TacticsPolicy.Difficulty difficulty; // null follows the global default; training overrides are explicit.
		long seen, nextDecision, observed, hpObserved, castObserved = -1, nextStep, moveUntil, sleepObserved = -1;
		int hpBand = -1;
		int trainingEnemy, trainingInstance, enemy, game = -1, instance, anchorX, anchorY, moveX, moveY;
		boolean owned, enemyCasting;
		int fleeingSamples, offensiveActionsSinceStep = 2;

		long nextHabitSample;
		double previousDistance = -1;
		int pendingId;
		boolean pendingSelf, pendingDebuff, sawCast;
		double pendingMp, pendingHp;
		int pendingCharges, pendingX, pendingY;
		boolean trainingBerserkerMode, pendingRushLoop;
		long nextCardinalTrance;
		final TacticsPolicy.CardinalProminence cardinalProminence = new TacticsPolicy.CardinalProminence();
		final CardinalArenaSupport cardinalSupport = new CardinalArenaSupport();
		final ManaAutoAttackSupport manaAutoAttack = new ManaAutoAttackSupport();
		int arenaCenterX, arenaCenterY;
		boolean hasArenaCenter;
		final TacticsPolicy.TankDefenseSequence tankDefense = new TacticsPolicy.TankDefenseSequence();
		final TacticsPolicy.DominatorCpWeave dominatorCp = new TacticsPolicy.DominatorCpWeave();
		final TacticsPolicy.OriginalRushStun<BuffInfo> rushLoop = new TacticsPolicy.OriginalRushStun<>();
		long nextRushStep;
		int rushAnchorX, rushAnchorY, rushMoveX, rushMoveY;
		double rushDirectionX, rushDirectionY;
		boolean rushMoving;
		final TacticsPolicy.TrainingBerserker trainingBurst = new TacticsPolicy.TrainingBerserker();
		long pendingAt;
		final Map<Integer, Failure> failures = new LinkedHashMap<>(16, .75f, true);
	}

	private BotTactics() { }
	private static long now() { return System.nanoTime() / 1_000_000L; }
	private static State state(int id, long time)
	{
		if (time >= nextSweep)
		{
			STATES.entrySet().removeIf(e ->
			{
				if (time - e.getValue().seen <= TTL_MS) return false;
				ArenaCombatRules.clear(e.getKey());
				e.getValue().cardinalSupport.clear();
				e.getValue().manaAutoAttack.clear();
				return true;
			});
			nextSweep = time + 60_000;
		}
		State s = STATES.get(id);
		if (s == null)
		{
			if (STATES.size() >= MAX_BOTS) { forget(STATES.keySet().iterator().next()); }
			s = new State(); STATES.put(id, s);
		}
		s.seen = time;
		return s;
	}

	public static synchronized void setDefaultDifficulty(String difficulty)
	{
		defaultDifficulty = TacticsPolicy.Difficulty.parse(difficulty);
	}
	public static synchronized String getDefaultDifficulty()
	{
		return defaultDifficulty.name().toLowerCase(java.util.Locale.ROOT);
	}
	public static synchronized void setDifficulty(int botId, String difficulty)
	{
		state(botId, now()).difficulty = TacticsPolicy.Difficulty.parse(difficulty);
	}
	public static synchronized String getDifficulty(int botId)
	{
		final State s = STATES.get(botId);
		return (s == null || s.difficulty == null ? defaultDifficulty : s.difficulty).name().toLowerCase(java.util.Locale.ROOT);
	}
	public static synchronized boolean isOwned(int botId)
	{
		final State s = STATES.get(botId);
		return s != null && s.owned;
	}
	/** Mandatory gate for every parent fallback, including training and phase drivers. */
	public static boolean allowsBasicAttack(Player bot, BotClassPreset preset)
	{
		return bot != null && preset != null && (TacticsPolicy.basicAttackAllowed(
			preset.getWeaponKind() == BotClassPreset.WeaponKind.DAGGER, preset.isCaster(), weaponUnavailable(bot))
			|| needsManaAutoAttack(bot, preset) && !weaponUnavailable(bot));
	}

	/** MP shortage, NOT a cooldown/LOS/item/charge bypass. Use actual learned rotation
	 * and core-adjusted initial+main costs; a payable offense still belongs to skills. */
	public static boolean needsManaAutoAttack(Player bot, BotClassPreset preset)
	{
		if (bot == null || preset == null || bot.getClient() != null || !bot.isOnline() || bot.isAlikeDead()
			|| preset.getClassId() == CARDINAL_CLASS_ID || preset.isCaster()
			|| bot.getPlayerClass().isMage() || bot.getPlayerClass().getId() != preset.getClassId()) { return false; }
		if (bot.getCurrentMp() <= 0) return true;
		boolean offense = false;
		for (int id : preset.getRotation())
		{
			final Skill skill = bot.getKnownSkill(id);
			if (skill == null || BotSpawner.isOpenerOnlySkill(id) || skill.isPassive() || skill.isToggle() || skill.isSuicideAttack()) continue;
			final TacticsPolicy.Kind type = kind(skill);
			if (type == null || self(type) || !supportedTarget(skill, false)) continue;
			offense = true;
			if (bot.getCurrentMp() >= bot.getStat().getMpInitialConsume(skill) + bot.getStat().getMpConsume(skill)) return false;
		}
		return offense;
	}

	/** World combat keeps its existing auto engines. Do not claim tactical ownership. */
	public static synchronized void maintainWorldManaFallback(Player bot, BotClassPreset preset)
	{
		if (bot == null || !BotManager.isBot(bot) || bot.isInOlympiadMode()) return;
		final boolean fighting = bot.getTarget() != null && bot.getTarget().isCreature()
			&& !bot.getTarget().asCreature().isAlikeDead() && bot.getTarget().asCreature().isAutoAttackable(bot)
			&& bot.getTarget().getInstanceId() == bot.getInstanceId() && !bot.isTeleporting();
		State s = STATES.get(bot.getObjectId());
		if (fighting && needsManaAutoAttack(bot, preset))
		{
			s = state(bot.getObjectId(), now());
			s.manaAutoAttack.update(bot, preset);
		}
		else if (s != null) s.manaAutoAttack.clear();
	}

    private static boolean weaponUnavailable(Player bot)
    {
        return bot.isDisarmed() || bot.getActiveWeaponItem() == null;
    }

    /** Phase driver lends ONLY a recovery movement tick; no tactical cast/ownership.
     * A real rush/cast must be drained by the caller before entering this hook. */
    public static synchronized boolean recoveryOnly(Player bot, Player enemy, BotClassPreset preset)
    {
        if (bot == null || preset == null || !valid(bot, enemy, true, STATES.get(bot.getObjectId()))
            || bot.isCastingNow() || bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) return false;
        if (needsManaAutoAttack(bot, preset) && allowsBasicAttack(bot, preset)) return false;
        boolean available = false;
        if (!weaponUnavailable(bot))
        {
            for (int id : BotSpawner.getTacticalRotation(bot, enemy))
            {
                Skill skill = bot.getKnownSkill(id);
                if (skill == null || BotSpawner.isOpenerOnlySkill(id) || skill.isPassive() || skill.isToggle() || skill.isSuicideAttack()) continue;
                TacticsPolicy.Kind type = kind(skill);
                if (type == null || self(type) || !supportedTarget(skill, false) || !BotSpawner.tacticalSkillAllowed(bot, enemy, skill, false)) continue;
                if (!bot.isSkillDisabled(skill) && !skill.isBlockedInOlympiad() && resources(bot, skill)
                    && skill.checkCondition(bot, enemy, false)) { available = true; break; }
            }
        }
        if (available) return false; // ready-but-distant offense belongs to the phase driver
        final long time = now();
        final State s = state(bot.getObjectId(), time);
        prepare(bot, enemy, s, time);
        AutoPlayTaskManager.getInstance().stopAutoPlay(bot);
        AutoUseTaskManager.getInstance().stopAutoUseTask(bot);
        stopBasicAttack(bot);
        if (time >= s.nextStep) step(bot, enemy, preset, s, time, bot.calculateDistance2D(enemy),
            GeoEngine.getInstance().canSeeTarget(bot, enemy), bot.getCurrentHp() / Math.max(1, bot.getMaxHp()), false, false);
        return true;
    }

	/** Center-to-center pursuit stop distance, not permission to attack. Uses the
	 * same effective learned offense and legality as selection. In particular a
	 * dagger must reach its 40+collision blow radius, not an 80/110 auto envelope. */
	public static synchronized int tacticalApproachRange(Player bot, Player enemy, BotClassPreset preset)
	{
		if (bot == null || enemy == null || preset == null) { return 40; }
		if (TacticsPolicy.tankClass(preset.getClassId()))
			return TacticsPolicy.tankApproachRange(bot.getPhysicalAttackRange(), bot.getTemplate().getCollisionRadius());
		if (needsManaAutoAttack(bot, preset))
			return TacticsPolicy.boundedApproachRange(bot.getPhysicalAttackRange() + bot.getTemplate().getCollisionRadius());
		final boolean ranged = preset.isCaster() || preset.getWeaponKind() == BotClassPreset.WeaponKind.BOW
			|| preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW;
		final State s = STATES.get(bot.getObjectId());
		final long time = now();
		final boolean trainingBerserker = preset.getClassId() == 131 && !bot.isInOlympiadMode() && s != null
			&& s.trainingEnemy == enemy.getObjectId() && s.trainingInstance == bot.getInstanceId();
		final boolean primaryRush = bot.getKnownSkill(793) != null && java.util.Arrays.stream(preset.getRotation()).anyMatch(id -> id == 793);
		double reach = -1, directReach = -1, tranceReach = -1;
		for (int id : preset.getRotation())
		{
			final Skill skill = bot.getKnownSkill(id);
			if (trainingBerserker && !s.trainingBurst.allowsSkill(id, bot.calculateDistance2D(enemy), primaryRush)) { continue; }
			if (skill == null || BotSpawner.isOpenerOnlySkill(id) || skill.isPassive() || skill.isToggle() || skill.isSuicideAttack()
				|| skill.hasEffectType(EffectType.TELEPORT, EffectType.TELEPORT_TO_TARGET)) { continue; }
			final TacticsPolicy.Kind kind = kind(skill);
			if (kind == null || self(kind) || !supportedTarget(skill, false) || !BotSpawner.tacticalSkillAllowed(bot, enemy, skill, false)) { continue; }
			final Failure failure = s == null ? null : s.failures.get(id);
			if (bot.isSkillDisabled(skill) || failure != null && time < failure.until
				|| bot.isInOlympiadMode() && skill.isBlockedInOlympiad() || weaponUnavailable(bot) && skill.isPhysical()
				|| !resources(bot, skill) || !skill.checkCondition(bot, enemy, false)) { continue; }
			if (!TacticsPolicy.cardinalSleepException(preset.getClassId(), id)
				&& !TacticsPolicy.sleepOffenseAllowed(preset.getClassId() == 115, enemy.isSleeping(),
				s == null || s.sleepObserved < 0 ? 0 : time - s.sleepObserved, directDamage(skill), id == 1097 || id == 1069)) { continue; }
			if (!directDamage(skill) && active(enemy, skill)) { continue; }
			if (skill.getFlyType() == FlyType.CHARGE && (bot.isMovementDisabled() || !GeoEngine.getInstance().canMoveToTarget(
				bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), enemy.getZ(), bot.getInstanceId()))) { continue; }
			final boolean aura = skill.getTargetType() == TargetType.AURA || skill.getTargetType() == TargetType.FRONT_AURA || skill.getTargetType() == TargetType.BEHIND_AURA;
			final double range = TacticsPolicy.effectiveRange(aura, skill.getCastRange(), skill.getAffectRange(), bot.getPhysicalAttackRange(), bot.getTemplate().getCollisionRadius());
			if (range <= 0) { continue; }
			if (preset.getClassId() == CARDINAL_CLASS_ID && id == CARDINAL_TRANCE_ID
				&& (s == null || time >= s.nextCardinalTrance)) tranceReach = range;
			reach = reach < 0 ? range : ranged ? Math.max(reach, range) : Math.min(reach, range);
			if (directDamage(skill)) { directReach = Math.max(directReach, range); }
		}
		if (ranged && directReach > 0) { reach = directReach; }
		if (reach < 0) { reach = bot.getPhysicalAttackRange() + bot.getTemplate().getCollisionRadius(); }
		return TacticsPolicy.boundedApproachRange(TacticsPolicy.cardinalApproachRange(preset.getClassId(), reach, tranceReach));
	}

	/** Explicit teardown; parent restores world auto-engines only after this call. */
	public static synchronized void forget(int botId)
	{
		ArenaCombatRules.clear(botId);
		State removed = STATES.remove(botId);
		if (removed != null) removed.cardinalSupport.clear();
		if (removed != null) removed.manaAutoAttack.clear();
	}
	public static synchronized String describe(int botId)
	{
		final State s = STATES.get(botId);
		return "tactics:" + getDifficulty(botId) + ":owned=" + (s != null && s.owned)
			+ ":pending=" + (s == null ? 0 : s.pendingId) + ":backoffs=" + (s == null ? 0 : s.failures.size())
			+ ":cardinalRegen=" + (s != null && s.cardinalSupport.active());
	}

	/** TrainingArena must authorize an isolated pair. Client-backed humans are NEVER bots. */
	public static synchronized boolean initializeTraining(Player bot, Player enemy)
	{
		if (bot == null || enemy == null || bot == enemy || bot.getClient() != null
			|| bot.getInstanceId() <= 0 || bot.getInstanceId() != enemy.getInstanceId() || bot.isInOlympiadMode()) { return false; }
		final State s = state(bot.getObjectId(), now());
		s.trainingEnemy = enemy.getObjectId(); s.trainingInstance = bot.getInstanceId();
		return true;
	}

	/** Caller invokes only at actual combat activation, never during preparation. */
	public static synchronized boolean beginCombat(Player bot, Player enemy)
	{
		if (bot == null || !valid(bot, enemy, bot.isInOlympiadMode(), STATES.get(bot.getObjectId()))) { return false; }
		final long time = now();
		prepare(bot, enemy, state(bot.getObjectId(), time), time);
		return true; // prepare initializes the combat clock once for this pair/context
	}

	private static boolean valid(Player bot, Player enemy, boolean ranked, State s)
	{
		if (bot == null || enemy == null || bot == enemy || bot.getClient() != null || !bot.isOnline() || !enemy.isOnline() || bot.isAlikeDead() || enemy.isAlikeDead()
			|| bot.isTeleporting() || enemy.isTeleporting() || bot.getInstanceId() != enemy.getInstanceId()) { return false; }
		if (ranked)
		{
			return BotManager.isBot(bot) && bot.isInOlympiadMode() && bot.isOlympiadStart() && enemy.isInOlympiadMode()
				&& enemy.isOlympiadStart() && bot.getOlympiadGameId() >= 0 && bot.getOlympiadGameId() == enemy.getOlympiadGameId()
				&& enemy.isAutoAttackable(bot);
		}
		return s != null && s.trainingEnemy == enemy.getObjectId() && s.trainingInstance > 0
			&& s.trainingInstance == bot.getInstanceId() && !bot.isInOlympiadMode() && !enemy.isInOlympiadMode();
	}

	public static synchronized boolean tick(Player bot, Player enemy, BotClassPreset preset, boolean ranked)
	{
		if (bot == null) { return false; }
		State s = STATES.get(bot.getObjectId());
		if (preset == null || !valid(bot, enemy, ranked, s))
		{
			// Do not commandeer a human even if an ID was accidentally registered.
			if (s != null) { forget(bot.getObjectId()); }
			return false;
		}
		// Existing Rush/retreat/phase driver retains total ownership.
		if (ranked && preset.getClassId() == 131) { return false; }
		if (ArenaCombatRules.update(bot, enemy, preset)) return true;
		final long time = now();
		s = state(bot.getObjectId(), time);
		prepare(bot, enemy, s, time);
		if (preset.getClassId() == CARDINAL_CLASS_ID) s.cardinalSupport.bind(bot, enemy);
		else s.cardinalSupport.clear();
		s.manaAutoAttack.update(bot, preset);
		claim(bot, s);
		return act(bot, enemy, preset, s, time, false);
	}

	/**
	 * OPTIONAL berserker hook: caller yields phase driver BEFORE calling and handles
	 * engine restoration afterwards. No movement, slow, attack, rush, or buff rotation.
	 * Never call during an active Rush/retreat phase. No enduring ownership is claimed.
	 */
	public static synchronized boolean tickDefense(Player bot, Player enemy, BotClassPreset preset, boolean ranked)
	{
		if (bot == null || preset == null || !valid(bot, enemy, ranked, STATES.get(bot.getObjectId()))) { return false; }
		final long time = now();
		final State s = state(bot.getObjectId(), time);
		prepare(bot, enemy, s, time);
		s.manaAutoAttack.update(bot, preset);
		pollAttempt(bot, enemy, s, time); // Resolve a completed heal even after it restored HP above the threshold.
		if (preset.getClassId() == 131 && bot.getCurrentHpPercent() > 40) return false;
		if ((!defenseThreat(bot, enemy) && bot.getCurrentHpPercent() >= 65) || bot.isCastingNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) { return false; }
		return act(bot, enemy, preset, s, time, true);
	}

	private static void claim(Player bot, State s)
	{
		// Stop pool membership, not just autoPlaying (AutoPlay ignores that flag).
		// Repeat at entry to remove a registration from a concurrent repair pass.
		AutoPlayTaskManager.getInstance().stopAutoPlay(bot);
		AutoUseTaskManager.getInstance().stopAutoUseTask(bot);
		bot.setAutoPlaying(false);
		s.owned = true;
	}
	private static TacticsPolicy.Difficulty difficulty(State s) { return s.difficulty == null ? defaultDifficulty : s.difficulty; }

	private static void prepare(Player bot, Player enemy, State s, long time)
	{
		final int game = bot.isInOlympiadMode() ? bot.getOlympiadGameId() : -1;
		if (s.enemy != enemy.getObjectId() || s.game != game || s.instance != bot.getInstanceId())
		{
			s.cardinalSupport.clear();
			s.manaAutoAttack.clear();
			s.enemy = enemy.getObjectId(); s.game = game; s.instance = bot.getInstanceId();
			s.anchorX = bot.getX(); s.anchorY = bot.getY(); s.observed = time;
			final int[] center = ArenaCombatRules.center(bot);
			s.arenaCenterX = center != null ? center[0] : 0;
			s.arenaCenterY = center != null ? center[1] : 0;
			s.hasArenaCenter = center != null;
			s.trainingBurst.reset(); s.trainingBerserkerMode = false; s.dominatorCp.reset();
			s.tankDefense.reset(time); s.rushLoop.clear(); s.nextRushStep = time; s.pendingRushLoop = false; s.rushMoving = false; s.rushDirectionX = 0; s.rushDirectionY = 0;
			s.sleepObserved = -1; s.nextDecision = time; s.nextStep = time; s.pendingId = 0; s.failures.clear(); s.enemyCasting = false; s.moveUntil = 0;
			s.nextCardinalTrance = 0; s.cardinalProminence.reset();
			s.fleeingSamples = 0; s.offensiveActionsSinceStep = 2; s.previousDistance = -1; s.nextHabitSample = time;
		}
		final double hp = bot.getCurrentHp() / Math.max(1, bot.getMaxHp());
		final int hpBand = hp < .35 ? 0 : hp < .65 || defenseThreat(bot, enemy) && cpRatio(bot) < .35 ? 1 : 2;
		if (hpBand != s.hpBand) { s.hpBand = hpBand; s.hpObserved = time; }
		final boolean casting = GeoEngine.getInstance().canSeeTarget(bot, enemy) && enemy.isCastingNow();
		if (casting && !s.enemyCasting) { s.castObserved = time; }
		if (!casting) { s.castObserved = -1; }
		s.enemyCasting = casting;
	}

	private static boolean act(Player bot, Player enemy, BotClassPreset preset, State s, long time, boolean defenseOnly)
	{
		final boolean trainingBerserker = !defenseOnly && preset.getClassId() == 131 && !bot.isInOlympiadMode()
			&& s.trainingEnemy == enemy.getObjectId() && s.trainingInstance == bot.getInstanceId();
		s.trainingBerserkerMode = trainingBerserker;
		if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow())
		{
			pollAttempt(bot, enemy, s, time);
			restoreCardinalMana(bot, preset);
			return true; // never Idle, cancel an attack, or retarget during a real cast
		}
		if (!defenseOnly && !allowsBasicAttack(bot, preset)) { stopBasicAttack(bot); }
		pollAttempt(bot, enemy, s, time);
		restoreCardinalMana(bot, preset);
		if (trainingBerserker && (bot.isOutOfControl() || bot.isAllSkillsDisabled() || s.pendingId != 0))
		{
			stopBasicAttack(bot); // no auto continuation under cast/CC; never abort a cast
			return true;
		}
		s.sleepObserved = TacticsPolicy.observeSleep(s.sleepObserved, enemy.isSleeping(), time);
		// Real casts and CC are never aborted or replaced, including by urgent heals.
		if (bot.isCastingNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) { return !defenseOnly; }
		if (s.pendingId != 0) { return !defenseOnly; }
		final boolean sleeping = enemy.isSleeping();
		final boolean manaFallback = !defenseOnly && needsManaAutoAttack(bot, preset) && allowsBasicAttack(bot, preset);
		final boolean dominator = preset.getClassId() == 115;
		final long sleepElapsed = s.sleepObserved < 0 ? 0 : time - s.sleepObserved;
		if (sleeping && !defenseOnly)
		{
			// Cancel only future autoattacks, never an already airborne hit or own cast.
			if (bot.getAI().getIntention() == Intention.ATTACK) { bot.getAI().setIntentionIdle(); }
			if (bot.isAttackingNow()) { bot.abortAttack(); }
		}
		// Never let a perpetually re-queued basic swing starve skills/defenses.
		// Forbidden basic attacks are cancelled immediately; real casts are untouched.
		final double hp = bot.getCurrentHp() / Math.max(1, bot.getMaxHp());
		if (time < s.nextDecision || !TacticsPolicy.reactionReady(difficulty(s), time, s.observed, hp < .35)) { return !defenseOnly; }
		final boolean visible = GeoEngine.getInstance().canSeeTarget(bot, enemy);
		final double distance = bot.calculateDistance2D(enemy);
		final boolean tank = TacticsPolicy.tankClass(preset.getClassId());
		final boolean timedTank = tank;
		final boolean tankClosing = tank && (!visible || distance > TacticsPolicy.tankApproachRange(bot.getPhysicalAttackRange(), bot.getTemplate().getCollisionRadius()));
		final int activeStrong = bot.isAffectedBySkill(110) ? 110 : bot.isAffectedBySkill(368) ? 368 : bot.isAffectedBySkill(760) ? 760 : 0;
		if (timedTank) { s.tankDefense.observed(activeStrong); }
		final Skill reconstruction = trainingBerserker ? bot.getKnownSkill(833) : null;
		final boolean recoveryReady = reconstruction != null && hp <= .40 && !bot.isSkillDisabled(reconstruction)
			&& BotRecoveryRules.selfHealAllowed(bot, reconstruction) && resources(bot, reconstruction)
			&& reconstruction.checkCondition(bot, bot, false);
		final int rushAction = trainingBerserker && !manaFallback && !recoveryReady ? trainingRushAction(bot, enemy, preset, s, time, distance, visible) : -1;
		if (rushAction == -2) { return true; }
		final boolean trainingRushActive = rushAction == 793;
		final boolean interruptReady = s.castObserved >= 0 && TacticsPolicy.reactionReady(difficulty(s), time, s.castObserved, false);
		if (time >= s.nextHabitSample)
		{
			// Bounded short memory of observed movement, not future input or learning
			// across players. Prefer legal slows/charges after sustained fleeing.
			final boolean fleeing = visible && enemy.isMoving() && distance > 250 && s.previousDistance >= 0 && distance >= s.previousDistance - 10;
			s.fleeingSamples = Math.max(0, Math.min(6, s.fleeingSamples + (fleeing ? 1 : -1)));
			s.previousDistance = distance; s.nextHabitSample = time + 500;
		}
		final boolean window = damageWindow(bot, preset);
		final Map<Integer, Skill> skills = new LinkedHashMap<>();
		// Effective lists preserve overrides; the shared defense pool includes learned tank defenses.
		for (int id : BotSpawner.getTacticalRotation(bot, enemy)) { final Skill skill = bot.getKnownSkill(id); if (skill != null) { skills.put(id, skill); } }
		for (int id : preset.getBuffs()) { final Skill skill = bot.getKnownSkill(id); if (skill != null) { skills.put(id, skill); } }
		for (int id : BotSpawner.getTacticalDefenses(bot, enemy)) { final Skill skill = bot.getKnownSkill(id); if (skill != null) { skills.put(id, skill); } }
		for (int id : BotSpawner.getTacticalCpSupport(bot, enemy)) { final Skill skill = bot.getKnownSkill(id); if (skill != null) { skills.put(id, skill); } }
		if (preset.getClassId() == 90 && bot.getKnownSkill(69) != null) skills.put(69, bot.getKnownSkill(69));
		int chargeDemand = 4;
		for (Skill skill : skills.values())
		{
			// Respect the existing bot 4..7 resource window; never synthesize charges.
			if (skill.getChargeConsumeCount() <= 7) { chargeDemand = Math.max(chargeDemand, skill.getChargeConsumeCount()); }
		}
		final TacticsPolicy.Context context = new TacticsPolicy.Context(hp, visible ? enemy.getCurrentHp() / Math.max(1, enemy.getMaxHp()) : 1,
			distance, sleeping, s.enemyCasting, interruptReady, visible && enemy.isMoving(), window, bot.getCharges() < chargeDemand);
		final List<TacticsPolicy.Option> options = new ArrayList<>();
		final List<Integer> tankStuns = new ArrayList<>(), tankRoots = new ArrayList<>(), tankMelee = new ArrayList<>();
		boolean offenseAvailable = false;
		for (Skill skill : skills.values())
		{
			if (BotSpawner.isOpenerOnlySkill(skill.getId())) { continue; } // training as well as ranked
			// Body Reconstruction (833): Doombringer-exclusive HP-threshold self-heal.
			// Activate at40% HP or below; the shared guard also protects legacy auto-use.
			// urgency. Applies to both training (defenseOnly=false) and olympiad
			// (defenseOnly=true via tickDefense) paths.
			if (!BotRecoveryRules.selfHealAllowed(bot, skill)) { continue; }
			if (preset.getClassId() == CARDINAL_CLASS_ID) {
				if (skill.getId() == CARDINAL_TRANCE_ID && time < s.nextCardinalTrance) { continue; }
				if (skill.getId() == CARDINAL_PROMINENCE_ID && !s.cardinalProminence.ready(time)) { continue; }
			}
			if (trainingRushActive && skill.getId() != 793) { continue; }
			final boolean timedPool = timedTank && BotSpawner.isTankDefenseSkill(skill.getId());
			if (timedPool && !s.tankDefense.due(time)) { continue; }
			if (trainingBerserker && !trainingRushActive && !s.trainingBurst.allowsSkill(skill.getId(), distance, skills.containsKey(793))) { continue; }
			final TacticsPolicy.Kind detected = kind(skill);
			final boolean direct = directDamage(skill);
			final TacticsPolicy.Kind kind = dominator && sleeping && sleepElapsed >= 5000 && direct ? TacticsPolicy.Kind.DAMAGE : detected;
			if (kind == null || (defenseOnly && kind != TacticsPolicy.Kind.HEAL && kind != TacticsPolicy.Kind.DEFENSE)) { continue; }
			final boolean self = self(kind);
			// PARTY_CLAN may include a same-clan/party opponent. Never heal/buff them;
			// query ordinary targets without changing the target or skill implementation.
			if (self && (skill.getId() == 1305 || skill.getId() == 1414)
				&& !TacticsPolicy.selfOnlyTargets(bot, skill.getTargetList(bot, false, bot))) { continue; }
			final boolean cpSupport = dominator && kind == TacticsPolicy.Kind.HEAL && TacticsPolicy.dominatorCpSkill(skill.getId()) && skill.hasEffectType(EffectType.CPHEAL);
			if (cpSupport && !TacticsPolicy.dominatorCpNeeded(cpRatio(bot))) { continue; }
			final boolean tankStun = tank && !self && skill.hasEffectType(EffectType.STUN);
			final boolean tankRoot = tank && !self && skill.hasEffectType(EffectType.ROOT);
			if (tank && !TacticsPolicy.tankClosingSkillAllowed(tankClosing, self, kind, tankStun || tankRoot)) { continue; }
			if (tank && self && !TacticsPolicy.tankSupportAllowed(tankClosing, immobilizingSupport(skill))) { continue; }
			if (tankStun && enemy.isStunned() || tankRoot && enemy.isMovementDisabled()) { continue; }
			// Prominence (1230) also bypasses the sleep-offense gate, same as Mana Burn (1398).
			final boolean cardinalMbException = TacticsPolicy.cardinalSleepException(preset.getClassId(), skill.getId());
			if (!self && !cardinalMbException && !TacticsPolicy.sleepOffenseAllowed(dominator, sleeping, sleepElapsed, direct, skill.getId() == 1097 || skill.getId() == 1069)) { continue; }
			final boolean protectedSleep = sleeping && !dominator && !cardinalMbException;
			if (!BotSpawner.tacticalSkillAllowed(bot, enemy, skill, self)) { continue; }
			if (!timedPool && (kind == TacticsPolicy.Kind.HEAL || kind == TacticsPolicy.Kind.DEFENSE)
				&& !TacticsPolicy.reactionReady(difficulty(s), time, s.hpObserved, hp < .35)) { continue; }
			if (!supportedTarget(skill, self) || skill.isPassive() || skill.isToggle() || skill.isSuicideAttack()
				|| skill.hasEffectType(EffectType.TELEPORT, EffectType.TELEPORT_TO_TARGET)) { continue; }
			final Failure f = s.failures.get(skill.getId());
			final boolean ready = !bot.isSkillDisabled(skill) && (f == null || time >= f.until) && (!bot.isInOlympiadMode() || !skill.isBlockedInOlympiad());
			final boolean inRange = self || inRange(bot, enemy, skill, distance);
			final boolean active = (self || !directDamage(skill)) && (skill.isDebuff() || kind != TacticsPolicy.Kind.DAMAGE && kind != TacticsPolicy.Kind.HEAL && kind != TacticsPolicy.Kind.CHARGE)
				&& active(self ? bot : enemy, skill);
			final boolean interrupt = !s.enemyCasting || interruptReady;
			final boolean resource = resources(bot, skill) && (self || !weaponUnavailable(bot) || !skill.isPhysical());
			final boolean conditions = resource && skill.checkCondition(bot, self ? bot : enemy, false);
			if (skill.getFlyType() == FlyType.CHARGE && (bot.isMovementDisabled() || !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), enemy.getZ(), bot.getInstanceId()))) { continue; }
			// During enemy invulnerability, DAMAGE-kind skills are still submitted (engine handles immunity).
			// CC/slow kinds are suppressed — they cannot land on invulnerable targets anyway.
			final boolean offBlocked = protectedSleep || (enemy.isInvul() && kind != TacticsPolicy.Kind.DAMAGE);
			if (!self && TacticsPolicy.allowed(kind, ready, true, visible, active, offBlocked, conditions, interrupt)) { offenseAvailable = true; }
			if (!TacticsPolicy.allowed(kind, ready, inRange, visible, active, offBlocked, conditions, interrupt)) { continue; }
			// Actual Backstab/FatalBlow handlers own positional rules and the admin bot success roll.
			final boolean cardinalHealUrgent = preset.getClassId() == CARDINAL_CLASS_ID && skill.getId() == CARDINAL_HEAL_ID && hp < 0.95;
			final boolean recoveryHeal = preset.getClassId() == 90 && skill.getId() == 69 || preset.getClassId() == 131 && skill.getId() == 833;
			final double base = recoveryHeal || cardinalHealUrgent ? 300 : timedPool ? 210 : cpSupport ? 145 : dominator && sleeping && sleepElapsed >= 5000 && direct ? 135 : kind == TacticsPolicy.Kind.DEFENSE
				? TacticsPolicy.defenseScore(hp, cpRatio(bot), visible && defenseThreat(bot, enemy))
				: kind == TacticsPolicy.Kind.HEAL && skill.hasEffectType(EffectType.CPHEAL) && !skill.hasEffectType(EffectType.HEAL)
					? TacticsPolicy.score(kind, new TacticsPolicy.Context(cpRatio(bot), context.enemyHp(), distance, sleeping, s.enemyCasting, interruptReady, context.enemyMoving(), window, context.needsCharges()))
					: TacticsPolicy.score(kind, context);
			final double prioritized = base > 0 && kind == TacticsPolicy.Kind.DEFENSE ? base + TacticsPolicy.defensePriority(skill.getId()) : base;
			final double score = TacticsPolicy.adapt(prioritized, kind, s.fleeingSamples >= 3, skill.getFlyType() == FlyType.CHARGE);
			if (score > 0)
			{
				options.add(new TacticsPolicy.Option(skill.getId(), kind, score));
				if (tankStun) { tankStuns.add(skill.getId()); }
				else if (tankRoot) { tankRoots.add(skill.getId()); }
				else if (tank && !self && direct && skill.isPhysical()) { tankMelee.add(skill.getId()); }
			}
		}
		final ThreadLocalRandom random = ThreadLocalRandom.current();
		final int tankSelected = timedTank ? s.tankDefense.choose(options, time, activeStrong != 0, enemy.isMageClass()) : -1;
		final int tankChoice = tank ? TacticsPolicy.tankActionChoice(options, tankStuns, tankRoots, tankMelee, tankSelected,
			!enemy.isMovementDisabled() && !enemy.isOutOfControl()) : -1;
		final int cpChoice = dominator ? s.dominatorCp.choose(options, cpRatio(bot)) : -1;
		final int cardinalChoice = s.cardinalProminence.choose(preset.getClassId(), options, time);
		final int recoveryChoice = options.stream().filter(o -> preset.getClassId() == 90 && o.id() == 69 || preset.getClassId() == 131 && o.id() == 833).mapToInt(TacticsPolicy.Option::id).findFirst().orElse(-1);
		final int selected = recoveryChoice >= 0 ? recoveryChoice : cardinalChoice >= 0 ? cardinalChoice : cpChoice >= 0 ? cpChoice : tankChoice >= 0 ? tankChoice : trainingRushActive
			? options.stream().anyMatch(option -> option.id() == 793) ? 793 : -1
			: trainingBerserker
			? s.trainingBurst.choose(options, distance, skills.containsKey(793), difficulty(s), random.nextDouble(), random.nextDouble())
			: TacticsPolicy.select(options, difficulty(s), random.nextDouble(), random.nextDouble());
		TacticsPolicy.Kind chosen = null;
		for (TacticsPolicy.Option o : options) { if (o.id() == selected) { chosen = o.kind(); break; } }
		final boolean urgent = cardinalChoice >= 0 || chosen == TacticsPolicy.Kind.HEAL || chosen == TacticsPolicy.Kind.DEFENSE
			|| (chosen == TacticsPolicy.Kind.CONTROL && interruptReady && s.enemyCasting);
		// No recovery retreat/flank/kite when MP cannot pay any offensive skill.
		// Parents still enforce real range, LOS, sleep, weapon and arena authority.
		if (manaFallback && selected < 0 && !sleeping) { s.moveUntil = 0; return false; }
		if (!defenseOnly && !urgent && time < s.moveUntil && bot.isMoving() && Math.hypot(bot.getXdestination() - s.moveX, bot.getYdestination() - s.moveY) < 40) { return true; }
		final boolean offenseReady = options.stream().anyMatch(o -> o.kind() == TacticsPolicy.Kind.DAMAGE || o.kind() == TacticsPolicy.Kind.CONTROL || o.kind() == TacticsPolicy.Kind.SLOW);
		final boolean trainingPressure = TacticsPolicy.trainingBerserkerPressure(trainingBerserker && !trainingRushActive,
			allowsBasicAttack(bot, preset), bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled() || bot.isDisabled(),
			sleeping, enemy.isInvul(), visible, enemy.isAutoAttackable(bot), bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || s.pendingId != 0);
        if (!defenseOnly && (!sleeping || dominator || preset.getClassId() == CARDINAL_CLASS_ID) && !urgent && time >= s.nextStep && !trainingPressure && step(bot, enemy, preset, s, time, distance, visible, hp, offenseReady, offenseAvailable)) { return true; }
		if (selected >= 0)
		{
			final Skill skill = skills.get(selected);
			// Caller deliberately lends the phase to this single genuine defensive cast.
			if (defenseOnly) { AutoUseTaskManager.getInstance().stopAutoUseTask(bot); AutoPlayTaskManager.getInstance().stopAutoPlay(bot); }
			final boolean self = self(chosen);
			stopBasicAttack(bot); // end auto loop before requesting an ordinary cast
			bot.setTarget(self ? bot : enemy);
			s.pendingMp = bot.getCurrentMp(); s.pendingHp = bot.getCurrentHp(); s.pendingCharges = bot.getCharges();
			s.pendingX = bot.getX(); s.pendingY = bot.getY();
			s.pendingRushLoop = trainingRushActive;
			final boolean accepted = bot.useMagic(skill, true, true); // shiftPressed: NEVER walk into range.
			s.nextDecision = time + difficulty(s).reactionMs;
			if (accepted)
			{
				if (trainingRushActive && selected == 793) { s.rushLoop.loopRequestAccepted(); }
				if (preset.getClassId() == CARDINAL_CLASS_ID) {
					if (selected == CARDINAL_TRANCE_ID) { s.nextCardinalTrance = time + 3000; }
					if (selected == CARDINAL_PROMINENCE_ID) { s.cardinalProminence.accepted(time); }
				}
				CombatAnalytics.tacticalRequest(bot.getObjectId(), chosen.name());
                if (chosen == TacticsPolicy.Kind.DAMAGE || chosen == TacticsPolicy.Kind.CONTROL || chosen == TacticsPolicy.Kind.SLOW)
                    s.offensiveActionsSinceStep = Math.min(4,s.offensiveActionsSinceStep+1);
				s.pendingId = selected; s.pendingAt = time; s.pendingSelf = self; s.pendingDebuff = skill.isDebuff() && !directDamage(skill); s.sawCast = bot.isCastingNow();
			}
			else { failure(s, selected, time); bot.setTarget(enemy); }
			// Self targeting remains until cast resolves; next offensive action restores enemy.
			return true;
		}
		if (trainingRushActive) { stopBasicAttack(bot); return true; } // no fillers/autos during the original own stun loop
		if (sleeping && !defenseOnly && (!dominator || !offenseAvailable) && preset.getClassId() != CARDINAL_CLASS_ID) { return true; }
		if (!defenseOnly && time < s.moveUntil && bot.isMoving() && Math.hypot(bot.getXdestination() - s.moveX, bot.getYdestination() - s.moveY) < 40) { return true; }
		// Training 131 has no auto engines to resume after its burst. Yield normal
		// armed pressure/pursuit only AFTER every ready in-range skill was considered.
		// A stunned FOE is a damage window, not a reason for this bot to stand idle.
		if (trainingPressure) { return false; }
		// Armed tanks keep closing/pressuring even when their short skills are on reuse.
		// A controlled target does not turn a tank into a stationary ranged drain caster.
		// Without LoS this yields MOVEMENT ONLY, including near pillars; parent attack gates retain LoS.
		if (TacticsPolicy.tankPursuit(tank, allowsBasicAttack(bot, preset), bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled() || bot.isDisabled(),
			sleeping, enemy.isInvul(), enemy.isAutoAttackable(bot), bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || s.pendingId != 0)) { return false; }
		// Titan keeps armed pursuit after ready skills; blind yield is movement ONLY.
		// Every parent actual attack still requires LoS, including near obstacles.
		if (TacticsPolicy.titanPursuit(preset.getClassId(), allowsBasicAttack(bot, preset), bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled() || bot.isDisabled(),
			sleeping, enemy.isInvul(), enemy.isAutoAttackable(bot), bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || s.pendingId != 0)) { return false; }
		// During enemy invulnerability: melee/archer bots keep auto-attacking (engine handles immunity).
		if (!defenseOnly && enemy.isInvul() && !preset.isCaster() && allowsBasicAttack(bot, preset)) { return false; }
		// Ordinary unavailable offense still holds; MP-starved physical bots yielded above.
		if (!defenseOnly && !offenseAvailable) { stopBasicAttack(bot); return true; }
		return false;
	}

	/** Active validated arena tick only. Poll the previous cast BEFORE refilling so
	 * its real MP payment remains observable. Never alter shared skill templates. */
	private static void restoreCardinalMana(Player bot, BotClassPreset preset)
	{
		if (bot == null || preset == null || preset.getClassId() != CARDINAL_CLASS_ID
			|| bot.getClient() != null || bot.getPlayerClass().getId() != CARDINAL_CLASS_ID
			|| !bot.isOnline() || bot.isAlikeDead()) { return; }
		final int maximum = bot.getMaxMp();
		if (maximum > 0 && bot.getCurrentMp() < maximum) { bot.setCurrentMp(maximum); }
	}

	private static void stopBasicAttack(Player bot)
	{
		if (bot.getAI().getIntention() == Intention.ATTACK) { bot.getAI().setIntentionIdle(); }
		if (bot.isAttackingNow()) { bot.abortAttack(); }
	}
	private static boolean meleeOpponent(Player enemy)
	{
		final BotClassPreset preset = BotPresets.getPresetByClassId(enemy.getPlayerClass().getId());
		return TacticsPolicy.meleeOpponent(preset != null, preset != null && preset.isCaster(),
			preset != null && (preset.getWeaponKind() == BotClassPreset.WeaponKind.BOW || preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW),
			enemy.isMageClass(), enemy.getPhysicalAttackRange());
	}

	private static double cpRatio(Player bot) { return bot.getMaxCp() <= 0 ? 1 : bot.getCurrentCp() / bot.getMaxCp(); }
	private static boolean defenseThreat(Player bot, Player enemy)
	{
		return enemy.getTarget() == bot && (enemy.isAttackingNow() || enemy.isCastingNow()
			|| !enemy.isMageClass() && bot.calculateDistance2D(enemy) <= 220);
	}

	private static void pollAttempt(Player bot, Player enemy, State s, long time)
	{
		if (s.pendingId == 0) { return; }
		if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) { s.sawCast = true; return; }
		final Skill skill = bot.getKnownSkill(s.pendingId);
		final boolean paid = s.sawCast || bot.getCurrentMp() < s.pendingMp || bot.getCharges() > s.pendingCharges
			|| (s.pendingSelf && bot.getCurrentHp() > s.pendingHp)
			|| (skill != null && (bot.isSkillDisabled(skill) || active(s.pendingSelf ? bot : enemy, skill)));
		if (!TacticsPolicy.attemptResolved(s.sawCast, paid, time - s.pendingAt)) { return; }
        final boolean landed = !s.pendingDebuff || skill != null && active(s.pendingSelf ? bot : enemy, skill);
		if (paid && s.pendingSelf && BotSpawner.isStrongTankDefenseSkill(s.pendingId)) { s.tankDefense.observed(s.pendingId); }
		if (s.trainingBerserkerMode && s.pendingId == 793 && s.pendingRushLoop)
		{
			final BuffInfo original = ownRushStun(bot, enemy);
			if (s.rushLoop.resolvedLoopLanding(original, original != null, paid,
				Math.hypot(bot.getX() - s.pendingX, bot.getY() - s.pendingY), bot.calculateDistance2D(enemy)))
			{
				s.nextRushStep = time; // each real landing starts a fresh bounded retreat
				s.rushDirectionX = s.pendingX - bot.getX(); s.rushDirectionY = s.pendingY - bot.getY();
				s.rushMoving = false;
			}
		}
		if (s.trainingBerserkerMode && s.pendingId == 793 && paid && !s.pendingRushLoop
			&& Math.hypot(bot.getX() - s.pendingX, bot.getY() - s.pendingY) >= 100 && bot.calculateDistance2D(enemy) < 600)
		{
			final BuffInfo stun = ownRushStun(bot, enemy);
			if (stun != null) { s.rushLoop.begin(stun); s.rushAnchorX = bot.getX(); s.rushAnchorY = bot.getY();
				s.rushDirectionX = s.pendingX - bot.getX(); s.rushDirectionY = s.pendingY - bot.getY(); }
		}
		if (s.trainingBerserkerMode)
			s.trainingBurst.resolved(s.pendingId, paid, Math.hypot(bot.getX() - s.pendingX, bot.getY() - s.pendingY), bot.calculateDistance2D(enemy));
		if (!paid || !landed) { failure(s, s.pendingId, time); }
		else
		{
			s.failures.remove(s.pendingId);
			if (skill != null && kind(skill) == TacticsPolicy.Kind.CONTROL) CombatAnalytics.controlEffectObserved(bot.getObjectId());
		}
		if (skill != null)
		{
			final TacticsPolicy.Kind resolvedKind = kind(skill);
			s.dominatorCp.resolved(s.pendingId, paid, resolvedKind == TacticsPolicy.Kind.DAMAGE || resolvedKind == TacticsPolicy.Kind.CONTROL || resolvedKind == TacticsPolicy.Kind.SLOW);
		}
		s.pendingId = 0;
		bot.setTarget(enemy);
	}
	private static boolean hasAppliedStun(BuffInfo effect)
	{
		return effect != null && effect.getEffects() != null && effect.getEffects().stream().anyMatch(applied ->
			applied.getEffectType() == EffectType.STUN && (applied.getEffectFlags() & EffectFlag.STUNNED.getMask()) != 0);
	}
	private static BuffInfo ownRushStun(Player bot, Player enemy)
	{
		final BuffInfo effect = enemy.getEffectList().getBuffInfoByAbnormalType(AbnormalType.STUN);
		return effect != null && !effect.isRemoved() && effect.isInUse() && enemy.isSleeping() && hasAppliedStun(effect)
			&& effect.getSkill().getId() == 793 && effect.getEffector() == bot && effect.getEffected() == enemy ? effect : null;
	}
	private static void stopRushMovement(Player bot, State s)
	{
		if (s.rushMoving && !bot.isCastingNow() && !bot.isCastingSimultaneouslyNow()
			&& bot.getAI().getIntention() == Intention.MOVE_TO
			&& Math.hypot(bot.getXdestination() - s.rushMoveX, bot.getYdestination() - s.rushMoveY) < 40)
			bot.getAI().setIntentionIdle(); // cancel only this loop's stale walk, never a cast
		s.rushMoving = false;
	}
	/** -1 normal rotation; -2 owns a bounded retreat/hold; 793 legal option search. */
	private static int trainingRushAction(Player bot, Player enemy, BotClassPreset preset, State s, long time, double distance, boolean visible)
	{
		final BuffInfo stun = ownRushStun(bot, enemy);
		if (!s.rushLoop.active(stun, stun != null)) { stopRushMovement(bot, s); return -1; }
		if (!java.util.Arrays.stream(preset.getRotation()).anyMatch(id -> id == 793) || bot.getKnownSkill(793) == null
			|| !visible || distance > 600 || Math.abs(bot.getZ() - enemy.getZ()) > 100)
		{ s.rushLoop.retryRetreat(time); stopRushMovement(bot, s); stopBasicAttack(bot); return -2; }
		if (enemy.isSleeping() || enemy.isInvul()) { stopRushMovement(bot, s); stopBasicAttack(bot); return -2; }
		if (!s.rushLoop.retreatAllowed(time, distance)) { stopRushMovement(bot, s); stopBasicAttack(bot); return -2; }
		if (distance >= 220) { stopRushMovement(bot, s); return 793; } // all real skill gates still run in act
		stopBasicAttack(bot);
		if (bot.isMovementDisabled() || time < s.nextRushStep) { return -2; }
		s.nextRushStep = time + 250;
		for (int attempt = 0; attempt < 8; attempt++)
		{
			final double[] point = TacticsPolicy.OriginalRushStun.retreatPoint(bot.getX(), bot.getY(), enemy.getX(), enemy.getY(), attempt, s.rushDirectionX, s.rushDirectionY);
			if (point == null) { continue; }
			final int x = (int)Math.round(point[0]), y = (int)Math.round(point[1]);
			if (!TrainingArena.inside(x, y) || Math.hypot(x - enemy.getX(), y - enemy.getY()) > 1600
				|| Math.hypot(x - s.rushAnchorX, y - s.rushAnchorY) > 650
				|| Math.hypot(x - enemy.getX(), y - enemy.getY()) <= distance + 10
				|| !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), x, y, bot.getZ(), bot.getInstanceId())) { continue; }
			bot.getAI().setIntentionMoveTo(new Location(x, y, bot.getZ()));
			s.rushMoveX = x; s.rushMoveY = y; s.rushMoving = true;
			return -2;
		}
		s.rushLoop.retryRetreat(time); stopRushMovement(bot, s); return -2;
	}

	private static void failure(State s, int id, long time)
	{
		if (!s.failures.containsKey(id) && s.failures.size() >= 64) { s.failures.remove(s.failures.keySet().iterator().next()); }
		final Failure f = s.failures.computeIfAbsent(id, k -> new Failure());
		f.count = Math.min(6, f.count + 1); f.until = time + TacticsPolicy.failureBackoff(f.count);
	}
	private static boolean resources(Player bot, Skill skill)
	{
		return bot.getCurrentMp() >= bot.getStat().getMpInitialConsume(skill) + bot.getStat().getMpConsume(skill) && bot.getCurrentHp() > skill.getHpConsume()
			&& (skill.isStatic() || (skill.isMagic() ? !bot.isMuted() : !bot.isPhysicalMuted()))
			&& bot.getCharges() >= skill.getChargeConsumeCount()
			&& (skill.getItemConsumeCount() <= 0 || bot.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) >= skill.getItemConsumeCount());
	}
	private static boolean active(Player target, Skill skill)
	{
		if (target.isAffectedBySkill(skill.getId())) { return true; }
		if (skill.getAbnormalType() == AbnormalType.NONE) { return false; }
		final BuffInfo effect = target.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
		return effect != null && effect.getSkill().getAbnormalLevel() >= skill.getAbnormalLevel();
	}
	private static boolean self(TacticsPolicy.Kind kind)
	{
		return kind == TacticsPolicy.Kind.HEAL || kind == TacticsPolicy.Kind.DEFENSE || kind == TacticsPolicy.Kind.BUFF || kind == TacticsPolicy.Kind.CHARGE;
	}
	private static boolean immobilizingSupport(Skill skill)
	{
		// Explicit user AI-permission exception, NOT a change to real Vengeance effects.
		if (skill.getId() == 368) { return false; }
		// Ultimate Defense and other actual immobilizing support still defer approach.
		if (skill.getId() == 110) { return true; }
		for (EffectScope scope : new EffectScope[] { EffectScope.GENERAL, EffectScope.SELF, EffectScope.PVP })
		{
			final var effects = skill.getEffects(scope);
			if (effects != null) for (var effect : effects)
				if ("ImmobileBuff".equals(effect.getName()) || "Root".equals(effect.getName())) { return true; }
		}
		return false;
	}
	private static boolean supportedTarget(Skill skill, boolean self)
	{
		if (self) { return (skill.getId() == 1305 || skill.getId() == 1414) && skill.getTargetType() == TargetType.PARTY_CLAN || skill.getId() == 368 && skill.getTargetType() == TargetType.AURA || skill.getTargetType() == TargetType.SELF || skill.getTargetType() == TargetType.ONE
			|| skill.getTargetType() == TargetType.PARTY || skill.getTargetType() == TargetType.CLAN
			|| skill.getTargetType() == TargetType.PARTY_MEMBER; }
		return skill.getTargetType() == TargetType.ONE || skill.getTargetType() == TargetType.AREA
			|| skill.getTargetType() == TargetType.AURA || skill.getTargetType() == TargetType.FRONT_AURA
			|| skill.getTargetType() == TargetType.FRONT_AREA || skill.getTargetType() == TargetType.BEHIND_AREA || skill.getTargetType() == TargetType.BEHIND_AURA;
	}
	private static boolean inRange(Player bot, Player enemy, Skill skill, double distance)
	{
		final boolean aura = skill.getTargetType() == TargetType.AURA || skill.getTargetType() == TargetType.FRONT_AURA || skill.getTargetType() == TargetType.BEHIND_AURA;
		// AURA defaults to castRange=-1; this is not an invalid skill. Physical
		// targeted -1 uses ordinary weapon reach, NEVER an unlimited cast range.
		final double range = TacticsPolicy.effectiveRange(aura, skill.getCastRange(), skill.getAffectRange(),
			bot.getPhysicalAttackRange(), bot.getTemplate().getCollisionRadius());
		return Math.abs(bot.getZ() - enemy.getZ()) < 150 && range > 0 && distance <= range;
	}
	/** Most direct attacks (including blows/energy attacks) report PHYSICAL_ATTACK.
	 * Named-effect fallback also covers CP damage and compound/custom handlers. */
	private static boolean effectNamed(Skill skill, String... names)
	{
		for (EffectScope scope : new EffectScope[] { EffectScope.GENERAL, EffectScope.PVP })
		{
			final var effects = skill.getEffects(scope);
			if (effects == null) { continue; }
			for (var effect : effects) for (String name : names) if (name.equals(effect.getName())) { return true; }
		}
		return false;
	}
	private static boolean directDamage(Skill skill)
	{
		return skill.isDamage() || effectNamed(skill, "Backstab", "FatalBlow", "EnergyDamage", "PhysicalSoulDamage", "CpDamPercent", "DeathLink");
	}
	private static TacticsPolicy.Kind kind(Skill skill)
	{
		// Explicit IDs, never translated skill-name substring guesses. Defense wins
		// over Vengeance's taunt; SELF CpDamPercent costs remain support, not attacks.
		if (TacticsPolicy.defenseSkill(skill.getId())) { return TacticsPolicy.Kind.DEFENSE; }
		if (skill.getId() == 50 || skill.getId() == 8) { return TacticsPolicy.Kind.CHARGE; }
		final boolean support = skill.getTargetType() == TargetType.SELF || !skill.hasNegativeEffect() && !directDamage(skill) && !skill.isDebuff();
		if (support)
		{
			if (skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL) || effectNamed(skill, "HealPercent", "HealOverTime")) { return TacticsPolicy.Kind.HEAL; }
			if (skill.isContinuous() && skill.getAbnormalType() != AbnormalType.NONE) { return TacticsPolicy.Kind.BUFF; }
		}
		if (skill.hasEffectType(EffectType.STUN, EffectType.SLEEP, EffectType.PARALYZE, EffectType.FEAR, EffectType.MUTE)
			|| effectNamed(skill, "PhysicalMute", "Disarm")) { return TacticsPolicy.Kind.CONTROL; }
		if (directDamage(skill)) { return TacticsPolicy.Kind.DAMAGE; }
		if (skill.getAbnormalType() == AbnormalType.SPEED_DOWN || skill.getAbnormalType() == AbnormalType.ALL_SPEED_DOWN || skill.hasEffectType(EffectType.ROOT)) { return TacticsPolicy.Kind.SLOW; }
		if (skill.isDebuff() || skill.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT, EffectType.DISPEL, EffectType.DISPEL_BY_SLOT)
			|| effectNamed(skill, "DispelByCategory", "CallSkill")) { return TacticsPolicy.Kind.SLOW; }
		return null; // target-cancel/hate manipulation, crafting, restoration and soul farming are not arena offense.
	}
	private static boolean damageWindow(Player bot, BotClassPreset preset)
	{
		if (preset.getClassId() == 113) { return bot.isAffectedBySkill(176) || bot.isAffectedBySkill(420) || bot.isAffectedBySkill(917); }
		if (preset.getClassId() == 114) { return bot.isAffectedBySkill(420) || bot.isAffectedBySkill(917); }
		return false;
	}

	/** Short ordinary geodata-checked steps. No future positions, facing writes or teleports. */
	private static boolean step(Player bot, Player enemy, BotClassPreset preset, State s, long time, double distance, boolean visible, double hp, boolean offenseReady, boolean offenseAvailable)
	{
		if (!visible || bot.isMovementDisabled() || Math.abs(bot.getZ() - enemy.getZ()) > 100 || distance < 1) { return false; }
		double x, y;
		final boolean recovery = TacticsPolicy.tankRecoveryAllowed(TacticsPolicy.tankClass(preset.getClassId()), weaponUnavailable(bot))
			&& TacticsPolicy.titanRecoveryAllowed(preset.getClassId(), weaponUnavailable(bot))
			&& TacticsPolicy.recoveryStepAllowed(weaponUnavailable(bot), offenseAvailable, offenseReady,
			distance, TacticsPolicy.recoveryEnemyControlled(preset.getClassId() == 115, enemy.isSleeping(), enemy.isMovementDisabled(), enemy.isOutOfControl()));
		if (recovery)
		{
			final Location orbit = ArenaCombatRules.kitePoint(bot, enemy, 120);
			if (orbit == null) return false;
			x = orbit.getX(); y = orbit.getY();
		}
		else if (!offenseReady && preset.getWeaponKind() == BotClassPreset.WeaponKind.DAGGER && distance < 170
			&& !TacticsPolicy.behind(enemy.getX(), enemy.getY(), enemy.getHeading(), bot.getX(), bot.getY()))
		{
			final double angle = enemy.getHeading() * (2 * Math.PI / 65536.0);
			// A flank waypoint; reaching rear takes real travel while the opponent may turn.
			x = enemy.getX() - 85 * Math.cos(angle) - 70 * Math.sin(angle);
			y = enemy.getY() - 85 * Math.sin(angle) + 70 * Math.cos(angle);
		}
		else if (preset.getClassId() == 95
			? TacticsPolicy.soultakerKite(meleeOpponent(enemy), distance, enemy.isSleeping() || enemy.isMovementDisabled() || enemy.isOutOfControl(), s.offensiveActionsSinceStep)
			: (preset.isCaster() || preset.getWeaponKind() == BotClassPreset.WeaponKind.BOW || preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW || weaponUnavailable(bot))
            && TacticsPolicy.rangedStepAllowed(preset.isCaster() || weaponUnavailable(bot), meleeOpponent(enemy),
                distance, hp, enemy.isSleeping() || enemy.isMovementDisabled() || enemy.isOutOfControl(), offenseReady, s.offensiveActionsSinceStep))
		{
			final Location orbit = ArenaCombatRules.kitePoint(bot, enemy, 100);
			if (orbit == null) return false;
			x = orbit.getX(); y = orbit.getY();
		}
		else { return false; } // Tanks/other melee maintain pressure via parent pursuit.
		final double dx = x - bot.getX(), dy = y - bot.getY(), length = Math.hypot(dx, dy);
		if (length > 160) { x = bot.getX() + dx * 160 / length; y = bot.getY() + dy * 160 / length; }
		final int px = (int) Math.round(x), py = (int) Math.round(y);
		if (!GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), px, py, bot.getZ(), bot.getInstanceId())) { s.nextStep = time + 1500; return false; }
		stopBasicAttack(bot);
		bot.getAI().setIntentionMoveTo(new Location(px, py, bot.getZ()));
		s.nextStep = time + (recovery || weaponUnavailable(bot) ? 600 : preset.getClassId() == 95 ? 800 : preset.getWeaponKind() == BotClassPreset.WeaponKind.DAGGER ? 2500 : 4500);
		if (bot.getAI().getIntention() != Intention.MOVE_TO || !bot.isMoving()
			|| Math.hypot(bot.getXdestination() - px, bot.getYdestination() - py) >= 40) { return false; }
		s.moveX = px; s.moveY = py; s.moveUntil = time + (recovery ? 450 : 200);
        s.offensiveActionsSinceStep = 0;
		return true;
	}
}
