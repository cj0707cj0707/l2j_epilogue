/* Contextual arena combat. One owner, real useMagic, no resource/stat bonuses. */
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
 * All state is IDs/scalars, bounded to 2048 bots and 64 skill failures per bot.
 */
public final class BotTactics
{
	private static final int MAX_BOTS = 2048;
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
		boolean trainingBerserkerMode;
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
			STATES.entrySet().removeIf(e -> time - e.getValue().seen > TTL_MS);
			nextSweep = time + 60_000;
		}
		State s = STATES.get(id);
		if (s == null)
		{
			if (STATES.size() >= MAX_BOTS) { STATES.remove(STATES.keySet().iterator().next()); }
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
		return bot != null && preset != null && TacticsPolicy.basicAttackAllowed(
			preset.getWeaponKind() == BotClassPreset.WeaponKind.DAGGER, preset.isCaster(), weaponUnavailable(bot));
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
		final boolean ranged = preset.isCaster() || preset.getWeaponKind() == BotClassPreset.WeaponKind.BOW
			|| preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW;
		final State s = STATES.get(bot.getObjectId());
		final long time = now();
		final boolean trainingBerserker = preset.getClassId() == 131 && !bot.isInOlympiadMode() && s != null
			&& s.trainingEnemy == enemy.getObjectId() && s.trainingInstance == bot.getInstanceId();
		final boolean primaryRush = bot.getKnownSkill(793) != null && java.util.Arrays.stream(preset.getRotation()).anyMatch(id -> id == 793);
		double reach = -1, directReach = -1;
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
			if (!TacticsPolicy.sleepOffenseAllowed(preset.getClassId() == 115, enemy.isSleeping(),
				s == null || s.sleepObserved < 0 ? 0 : time - s.sleepObserved, directDamage(skill), id == 1097 || id == 1069)) { continue; }
			if (!directDamage(skill) && active(enemy, skill)) { continue; }
			if (skill.getFlyType() == FlyType.CHARGE && (bot.isMovementDisabled() || !GeoEngine.getInstance().canMoveToTarget(
				bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), enemy.getZ(), bot.getInstanceId()))) { continue; }
			final boolean aura = skill.getTargetType() == TargetType.AURA || skill.getTargetType() == TargetType.FRONT_AURA || skill.getTargetType() == TargetType.BEHIND_AURA;
			final double range = TacticsPolicy.effectiveRange(aura, skill.getCastRange(), skill.getAffectRange(), bot.getPhysicalAttackRange(), bot.getTemplate().getCollisionRadius());
			if (range <= 0) { continue; }
			reach = reach < 0 ? range : ranged ? Math.max(reach, range) : Math.min(reach, range);
			if (directDamage(skill)) { directReach = Math.max(directReach, range); }
		}
		if (ranged && directReach > 0) { reach = directReach; }
		if (reach < 0) { reach = bot.getPhysicalAttackRange() + bot.getTemplate().getCollisionRadius(); }
		return TacticsPolicy.boundedApproachRange(reach);
	}

	/** Explicit teardown; parent restores world auto-engines only after this call. */
	public static synchronized void forget(int botId) { STATES.remove(botId); }
	public static synchronized String describe(int botId)
	{
		final State s = STATES.get(botId);
		return "tactics:" + getDifficulty(botId) + ":owned=" + (s != null && s.owned)
			+ ":pending=" + (s == null ? 0 : s.pendingId) + ":backoffs=" + (s == null ? 0 : s.failures.size());
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

	private static boolean valid(Player bot, Player enemy, boolean ranked, State s)
	{
		if (bot == null || enemy == null || bot == enemy || bot.getClient() != null || bot.isAlikeDead() || enemy.isAlikeDead()
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
		final long time = now();
		s = state(bot.getObjectId(), time);
		prepare(bot, enemy, s, time);
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
			s.enemy = enemy.getObjectId(); s.game = game; s.instance = bot.getInstanceId();
			s.anchorX = bot.getX(); s.anchorY = bot.getY(); s.observed = time;
			s.trainingBurst.reset(); s.trainingBerserkerMode = false;
			s.sleepObserved = -1; s.nextDecision = time; s.nextStep = time; s.pendingId = 0; s.failures.clear(); s.enemyCasting = false; s.moveUntil = 0;
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
		if (trainingBerserker && (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()))
		{
			pollAttempt(bot, enemy, s, time);
			return true; // never Idle, cancel an attack, or retarget during a real cast
		}
		if (!defenseOnly && !allowsBasicAttack(bot, preset)) { stopBasicAttack(bot); }
		pollAttempt(bot, enemy, s, time);
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
		int chargeDemand = 4;
		for (Skill skill : skills.values())
		{
			// Respect the existing bot 4..7 resource window; never synthesize charges.
			if (skill.getChargeConsumeCount() <= 7) { chargeDemand = Math.max(chargeDemand, skill.getChargeConsumeCount()); }
		}
		final TacticsPolicy.Context context = new TacticsPolicy.Context(hp, visible ? enemy.getCurrentHp() / Math.max(1, enemy.getMaxHp()) : 1,
			distance, sleeping, s.enemyCasting, interruptReady, visible && enemy.isMoving(), window, bot.getCharges() < chargeDemand);
		final List<TacticsPolicy.Option> options = new ArrayList<>();
		boolean offenseAvailable = false;
		for (Skill skill : skills.values())
		{
			if (BotSpawner.isOpenerOnlySkill(skill.getId())) { continue; } // training as well as ranked
			if (trainingBerserker && !s.trainingBurst.allowsSkill(skill.getId(), distance, skills.containsKey(793))) { continue; }
			final TacticsPolicy.Kind detected = kind(skill);
			final boolean direct = directDamage(skill);
			final TacticsPolicy.Kind kind = dominator && sleeping && sleepElapsed >= 5000 && direct ? TacticsPolicy.Kind.DAMAGE : detected;
			if (kind == null || (defenseOnly && kind != TacticsPolicy.Kind.HEAL && kind != TacticsPolicy.Kind.DEFENSE)) { continue; }
			final boolean self = self(kind);
			if (!self && !TacticsPolicy.sleepOffenseAllowed(dominator, sleeping, sleepElapsed, direct, skill.getId() == 1097 || skill.getId() == 1069)) { continue; }
			final boolean protectedSleep = sleeping && !dominator;
			if (!BotSpawner.tacticalSkillAllowed(bot, enemy, skill, self)) { continue; }
			if ((kind == TacticsPolicy.Kind.HEAL || kind == TacticsPolicy.Kind.DEFENSE)
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
			if (!self && TacticsPolicy.allowed(kind, ready, true, visible, active, protectedSleep || enemy.isInvul(), conditions, interrupt)) { offenseAvailable = true; }
			if (!TacticsPolicy.allowed(kind, ready, inRange, visible, active, protectedSleep || enemy.isInvul(), conditions, interrupt)) { continue; }
			// Actual Backstab/FatalBlow handlers own positional rules and the admin bot success roll.
			final double base = dominator && sleeping && sleepElapsed >= 5000 && direct ? 135 : kind == TacticsPolicy.Kind.DEFENSE
				? TacticsPolicy.defenseScore(hp, cpRatio(bot), visible && defenseThreat(bot, enemy))
				: kind == TacticsPolicy.Kind.HEAL && skill.hasEffectType(EffectType.CPHEAL) && !skill.hasEffectType(EffectType.HEAL)
					? TacticsPolicy.score(kind, new TacticsPolicy.Context(cpRatio(bot), context.enemyHp(), distance, sleeping, s.enemyCasting, interruptReady, context.enemyMoving(), window, context.needsCharges()))
					: TacticsPolicy.score(kind, context);
			final double prioritized = base > 0 && kind == TacticsPolicy.Kind.DEFENSE ? base + TacticsPolicy.defensePriority(skill.getId()) : base;
			final double score = TacticsPolicy.adapt(prioritized, kind, s.fleeingSamples >= 3, skill.getFlyType() == FlyType.CHARGE);
			if (score > 0) { options.add(new TacticsPolicy.Option(skill.getId(), kind, score)); }
		}
		final ThreadLocalRandom random = ThreadLocalRandom.current();
		final int selected = trainingBerserker
			? s.trainingBurst.choose(options, distance, skills.containsKey(793), difficulty(s), random.nextDouble(), random.nextDouble())
			: TacticsPolicy.select(options, difficulty(s), random.nextDouble(), random.nextDouble());
		TacticsPolicy.Kind chosen = null;
		for (TacticsPolicy.Option o : options) { if (o.id() == selected) { chosen = o.kind(); break; } }
		final boolean urgent = chosen == TacticsPolicy.Kind.HEAL || chosen == TacticsPolicy.Kind.DEFENSE
			|| (chosen == TacticsPolicy.Kind.CONTROL && interruptReady && s.enemyCasting);
		if (!defenseOnly && !urgent && time < s.moveUntil && bot.isMoving() && Math.hypot(bot.getXdestination() - s.moveX, bot.getYdestination() - s.moveY) < 40) { return true; }
		final boolean offenseReady = options.stream().anyMatch(o -> o.kind() == TacticsPolicy.Kind.DAMAGE || o.kind() == TacticsPolicy.Kind.CONTROL || o.kind() == TacticsPolicy.Kind.SLOW);
		final boolean trainingPressure = TacticsPolicy.trainingBerserkerPressure(trainingBerserker,
			allowsBasicAttack(bot, preset), bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled() || bot.isDisabled(),
			sleeping, enemy.isInvul(), visible, enemy.isAutoAttackable(bot), bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || s.pendingId != 0);
        if (!defenseOnly && (!sleeping || dominator) && !urgent && time >= s.nextStep && !trainingPressure && step(bot, enemy, preset, s, time, distance, visible, hp, offenseReady, offenseAvailable)) { return true; }
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
			final boolean accepted = bot.useMagic(skill, true, true); // shiftPressed: NEVER walk into range.
			s.nextDecision = time + difficulty(s).reactionMs;
			if (accepted)
			{
				CombatAnalytics.tacticalRequest(bot.getObjectId(), chosen.name());
                if (chosen == TacticsPolicy.Kind.DAMAGE || chosen == TacticsPolicy.Kind.CONTROL || chosen == TacticsPolicy.Kind.SLOW)
                    s.offensiveActionsSinceStep = Math.min(4,s.offensiveActionsSinceStep+1);
				s.pendingId = selected; s.pendingAt = time; s.pendingSelf = self; s.pendingDebuff = skill.isDebuff() && !directDamage(skill); s.sawCast = bot.isCastingNow();
			}
			else { failure(s, selected, time); bot.setTarget(enemy); }
			// Self targeting remains until cast resolves; next offensive action restores enemy.
			return true;
		}
		if (sleeping && !defenseOnly && (!dominator || !offenseAvailable)) { return true; }
		if (!defenseOnly && time < s.moveUntil && bot.isMoving() && Math.hypot(bot.getXdestination() - s.moveX, bot.getYdestination() - s.moveY) < 40) { return true; }
		// Training 131 has no auto engines to resume after its burst. Yield normal
		// armed pressure/pursuit only AFTER every ready in-range skill was considered.
		// A stunned FOE is a damage window, not a reason for this bot to stand idle.
		if (trainingPressure) { return false; }
		// Parent may pursue when merely out of range, but may never autoattack a dagger.
		if (!defenseOnly && !offenseAvailable) { stopBasicAttack(bot); return true; }
		return false;
	}

	private static void stopBasicAttack(Player bot)
	{
		if (bot.getAI().getIntention() == Intention.ATTACK) { bot.getAI().setIntentionIdle(); }
		if (bot.isAttackingNow()) { bot.abortAttack(); }
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
		if (s.trainingBerserkerMode)
			s.trainingBurst.resolved(s.pendingId, paid, Math.hypot(bot.getX() - s.pendingX, bot.getY() - s.pendingY), bot.calculateDistance2D(enemy));
		if (!paid || !landed) { failure(s, s.pendingId, time); }
		else
		{
			s.failures.remove(s.pendingId);
			if (skill != null && kind(skill) == TacticsPolicy.Kind.CONTROL) CombatAnalytics.controlEffectObserved(bot.getObjectId());
		}
		s.pendingId = 0;
		bot.setTarget(enemy);
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
	private static boolean supportedTarget(Skill skill, boolean self)
	{
		if (self) { return skill.getId() == 368 && skill.getTargetType() == TargetType.AURA || skill.getTargetType() == TargetType.SELF || skill.getTargetType() == TargetType.ONE
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
		final boolean recovery = TacticsPolicy.recoveryStepAllowed(weaponUnavailable(bot), offenseAvailable, offenseReady,
			distance, TacticsPolicy.recoveryEnemyControlled(preset.getClassId() == 115, enemy.isSleeping(), enemy.isMovementDisabled(), enemy.isOutOfControl()));
		if (recovery)
		{
			x = bot.getX() + 120 * (bot.getX() - enemy.getX()) / distance;
			y = bot.getY() + 120 * (bot.getY() - enemy.getY()) / distance;
		}
		else if (!offenseReady && preset.getWeaponKind() == BotClassPreset.WeaponKind.DAGGER && distance < 170
			&& !TacticsPolicy.behind(enemy.getX(), enemy.getY(), enemy.getHeading(), bot.getX(), bot.getY()))
		{
			final double angle = enemy.getHeading() * (2 * Math.PI / 65536.0);
			// A flank waypoint; reaching rear takes real travel while the opponent may turn.
			x = enemy.getX() - 85 * Math.cos(angle) - 70 * Math.sin(angle);
			y = enemy.getY() - 85 * Math.sin(angle) + 70 * Math.cos(angle);
		}
		else if ((preset.isCaster() || preset.getWeaponKind() == BotClassPreset.WeaponKind.BOW || preset.getWeaponKind() == BotClassPreset.WeaponKind.CROSSBOW)
            && TacticsPolicy.rangedStepAllowed(preset.isCaster(), !enemy.isMageClass() && enemy.getPhysicalAttackRange() < 250,
                distance, hp, enemy.isSleeping() || enemy.isMovementDisabled() || enemy.isOutOfControl(), offenseReady, s.offensiveActionsSinceStep))
		{
			x = bot.getX() + 100 * (bot.getX() - enemy.getX()) / distance;
			y = bot.getY() + 100 * (bot.getY() - enemy.getY()) / distance;
		}
		else { return false; } // Tanks/other melee maintain pressure via parent pursuit.
		final double dx = x - bot.getX(), dy = y - bot.getY(), length = Math.hypot(dx, dy);
		if (length > 160) { x = bot.getX() + dx * 160 / length; y = bot.getY() + dy * 160 / length; }
		final int px = (int) Math.round(x), py = (int) Math.round(y);
		if (Math.hypot(px - s.anchorX, py - s.anchorY) > 1400 || !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), px, py, bot.getZ(), bot.getInstanceId())) { s.nextStep = time + 1500; return false; }
		stopBasicAttack(bot);
		bot.getAI().setIntentionMoveTo(new Location(px, py, bot.getZ()));
		s.nextStep = time + (recovery ? 600 : preset.getWeaponKind() == BotClassPreset.WeaponKind.DAGGER ? 2500 : 4500);
		if (bot.getAI().getIntention() != Intention.MOVE_TO || !bot.isMoving()
			|| Math.hypot(bot.getXdestination() - px, bot.getYdestination() - py) >= 40) { return false; }
		s.moveX = px; s.moveY = py; s.moveUntil = time + (recovery ? 450 : 200);
        s.offensiveActionsSinceStep = 0;
		return true;
	}
}
