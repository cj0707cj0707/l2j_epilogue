/* Contextual arena combat. One owner, real useMagic; native MP with the scoped Cardinal exception and opening Clarity. */
package custom.fakeplayersai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageDealt;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

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
 * State is bounded to 2048 bots/64 failures; retreat effect identity uses weak references.
 * A pending damage observer retains its two actors only until cast resolution/context cleanup.
 */
public final class BotTactics
{
	private static final int MAX_BOTS = 2048;
	private static final int SOUL_CLEANSE_ID = 1510;
	private static final int ESCAPE_SHACKLE_ID = 453;
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
	/** One native direct-damage observation per pending Dominator cast. Callback takes
	 * no tactical/analytics locks and never retains the pooled event payload. */
	private static final class CastEvidence
	{
		private final Player bot, enemy;
		private final int skillId;
		private final ConsumerEventListener listener;
		volatile boolean hit;
		CastEvidence(Player bot, Player enemy, int skillId)
		{
			this.bot = bot; this.enemy = enemy; this.skillId = skillId;
			Consumer<OnCreatureDamageDealt> consumer = this::observe;
			listener = new ConsumerEventListener(bot, EventType.ON_CREATURE_DAMAGE_DEALT, consumer, BotTactics.class);
			bot.addListener(listener);
		}
		void observe(OnCreatureDamageDealt e)
		{
			if (e.getAttacker() == bot && e.getTarget() == enemy && e.getSkill() != null
				&& e.getSkill().getId() == skillId && !e.isDamageOverTime()
				&& Double.isFinite(e.getDamage()) && e.getDamage() > 0) hit = true;
		}
		void close() { bot.removeListener(listener); }
	}
	private static void clearEvidence(State s)
	{
		if (s.pendingEvidence != null) { s.pendingEvidence.close(); s.pendingEvidence = null; }
	}
	private static final class State
	{
		TacticsPolicy.Difficulty difficulty; // null follows the global default; training overrides are explicit.
		long seen, nextDecision, observed, hpObserved, castObserved = -1, nextStep, moveUntil, sleepObserved = -1;
		int hpBand = -1;
		int trainingEnemy, trainingInstance, enemy, game = -1, instance, anchorX, anchorY, moveX, moveY;
		boolean owned, enemyCasting, cleanseLease, cardinalTrancePriority;
		int fleeingSamples, offensiveActionsSinceStep = 2;

		long nextHabitSample;
		double previousDistance = -1;
		int pendingId;
		boolean pendingSelf, pendingDebuff, sawCast;
		double pendingMp, pendingHp;
		double pendingCompletionMp;
		CastEvidence pendingEvidence;
		int pendingCharges, pendingX, pendingY;
		boolean trainingBerserkerMode, pendingRushLoop;
		final TacticsPolicy.CardinalProminence cardinalProminence = new TacticsPolicy.CardinalProminence();
		final CardinalArenaSupport cardinalSupport = new CardinalArenaSupport();
		final InvulnerableMeleeKite invulnerableKite = new InvulnerableMeleeKite();
        final InvulnerableRangedTarget invulnerableRanged = new InvulnerableRangedTarget();
        boolean rangedInvulnerableLease, rangedPreviousOwned;
		final InvulnerableMeleeKite survivalKite = new InvulnerableMeleeKite();
        final CardinalSurvival.Window clarityWindow = new CardinalSurvival.Window();
        final BluffRear bluffRear = new BluffRear();
		boolean invulnerableLease, invulnerablePreviousOwned;
		long combatStartedAt;
		final ManaAutoAttackSupport manaAutoAttack = new ManaAutoAttackSupport();
		int arenaCenterX, arenaCenterY;
		boolean hasArenaCenter;
		final TacticsPolicy.TankDefenseSequence tankDefense = new TacticsPolicy.TankDefenseSequence();
		final TacticsPolicy.DominatorCpWeave dominatorCp = new TacticsPolicy.DominatorCpWeave();
		final TacticsPolicy.DominatorPressure dominatorPressure = new TacticsPolicy.DominatorPressure();
        final RoleCombatPolicy.DominatorCycle rangedCycle = new RoleCombatPolicy.DominatorCycle();
        java.lang.ref.WeakReference<Player> currentOpponent = new java.lang.ref.WeakReference<>(null);
        long chargePursuitUntil;
		final SleepRetreat sleepRetreat = new SleepRetreat();
        final SleepRetreat tankSleepRetreat = new SleepRetreat();
		final DominatorControlRetreat controlRetreat = new DominatorControlRetreat();
		final SummonAttackPulse summonAttack = new SummonAttackPulse();
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
				e.getValue().invulnerableKite.stop();
                e.getValue().invulnerableRanged.stop();
				e.getValue().survivalKite.stop();
				e.getValue().manaAutoAttack.clear();
				clearEvidence(e.getValue());
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
		return bot != null && preset != null && !BluffRear.targetStunned(bot) && (TacticsPolicy.basicAttackAllowed(
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

	public static boolean strictMeleePressure(Player bot, Player enemy, BotClassPreset preset)
	{
		if (bot == null || enemy == null || preset == null || !TacticsPolicy.chargeMeleeClass(preset.getClassId())) return false;
		return bot.getClient()==null && bot.getPlayerClass().getId()==preset.getClassId();
	}

	/** Scoped by the arena drivers; human actors and melee opponents keep their policy. */
	private static boolean dominatorRangedPressure(Player bot, Player enemy, BotClassPreset preset)
	{
		return bot != null && enemy != null && preset != null && bot.getClient() == null
			&& preset.getClassId() == 115 && bot.getPlayerClass().getId() == 115 && !meleeOpponent(enemy);
	}
    private static boolean rangedSleepReady(Player bot,Player enemy,State s,long time)
    {
        Skill skill=bot.getKnownSkill(1097);Failure failure=s==null?null:s.failures.get(1097);
        return skill!=null&&!bot.isMuted()&&!bot.isOutOfControl()&&!bot.isAllSkillsDisabled()&&!bot.isSkillDisabled(skill)
            &&(failure==null||time>=failure.until)&&(!bot.isInOlympiadMode()||!skill.isBlockedInOlympiad())
            &&resources(bot,skill)&&skill.checkCondition(bot,enemy,false);
    }
    /** Strong-defense escape may pause for a native-legal Dreaming Spirit, not generic offense. */
    private static boolean dominatorTankSleepReady(Player bot,Player enemy,BotClassPreset preset,State s,long time)
    {
        if(bot.getClient()!=null||preset.getClassId()!=115||bot.getPlayerClass().getId()!=115
            ||!TankClassMechanics.defendedTank(enemy)||enemy.isSleeping()||enemy.isInvul())return false;
        Skill skill=bot.getKnownSkill(1097);Failure f=s.failures.get(1097);
        return skill!=null&&!bot.isAllSkillsDisabled()&&!bot.isOutOfControl()&&!bot.isMuted()
            &&!bot.isSkillDisabled(skill)&&(f==null||time>=f.until)
            &&(!bot.isInOlympiadMode()||!skill.isBlockedInOlympiad())
            &&resources(bot,skill)&&skill.checkCondition(bot,enemy,false)
            &&inRange(bot,enemy,skill,bot.calculateDistance2D(enemy))&&GeoEngine.getInstance().canSeeTarget(bot,enemy);
    }
    /** Native ready Cancel precedes damage/kite against melee and Cardinal, not all ranged classes. */
    private static boolean mageCancelReady(Player bot,Player enemy,BotClassPreset preset,State s,long time)
    {
        int cid=preset.getClassId();
        if(bot.getClient()!=null||(cid!=94&&cid!=103)||bot.getPlayerClass().getId()!=cid
            ||(!meleeOpponent(enemy)&&enemy.getPlayerClass().getId()!=97)||enemy.isSleeping()||enemy.isInvul())return false;
        Skill skill=bot.getKnownSkill(1056);Failure f=s.failures.get(1056);
        return skill!=null&&!bot.isAllSkillsDisabled()&&!bot.isOutOfControl()&&!bot.isMuted()
            &&!bot.isSkillDisabled(skill)&&(f==null||time>=f.until)
            &&(!bot.isInOlympiadMode()||!skill.isBlockedInOlympiad())
            &&resources(bot,skill)&&skill.checkCondition(bot,enemy,false)
            &&inRange(bot,enemy,skill,bot.calculateDistance2D(enemy))&&GeoEngine.getInstance().canSeeTarget(bot,enemy);
    }
    /** A real sleeping protected tank gets one retreat then HOLD, never an orbit or damage. */
    private static boolean dominatorTankSleepHold(Player bot,Player enemy,BotClassPreset preset,State s,long time)
    {
        if(bot.getClient()!=null||preset.getClassId()!=115||bot.getPlayerClass().getId()!=115
            ||!TankClassMechanics.defendedTank(enemy)||!enemy.isSleeping())
        { s.tankSleepRetreat.clear(bot); return false; }
        s.invulnerableKite.stop(); s.survivalKite.stop(); s.moveUntil=0;
        stopBasicAttack(bot);
        if(bot.isCastingNow()||bot.isCastingSimultaneouslyNow()||s.pendingId!=0)return true;
        // Only an actual own Sleep effect starts the retreat; a foreign/unknown sleep still means HOLD.
        if(s.tankSleepRetreat.tick(bot,enemy,time,s.hasArenaCenter
            ? new int[]{s.arenaCenterX,s.arenaCenterY} : ArenaCombatRules.center(bot)))return true;
        supportBeforeMovement(bot,enemy,preset,s,time); // optional legal CP recovery while stationary
        return true; // arrived, blocked terrain, CC or missing effect must not fall back to orbit/damage
    }
	private static boolean daggerShadow(Player bot, Player enemy, BotClassPreset preset, Skill skill)
    {
        int cid = preset.getClassId();
        BotClassPreset target = BotPresets.getPresetByClassId(enemy.getPlayerClass().getId());
        return bot.getClient() == null && bot.getPlayerClass().getId() == cid && (cid == 93 || cid == 101 || cid == 108)
            && skill.getId() == 821 && (enemy.isMageClass() || target != null && target.isCaster());
    }
    /** Shared candidate guard: this AI restriction never modifies a human Dominator's skills. */
    public static boolean dominatorStunAllowed(Player bot, Player enemy, Skill skill)
    {
        return bot.getClient() != null || bot.getPlayerClass().getId() != 115
            || !skill.hasEffectType(EffectType.STUN) || enemy == null || meleeOpponent(enemy);
    }
    private static boolean cardinalTrancePressure(Player bot,Player enemy,BotClassPreset preset)
    { return preset.getClassId()==CARDINAL_CLASS_ID&&CardinalSurvival.tranceTarget(bot,enemy); }
    /** Range/LOS belong to the approach stage, not the all-opponent sleep priority. */
    private static boolean cardinalTranceReady(Player bot,Player enemy,State s,long time)
    {
        if(!CardinalSurvival.tranceTarget(bot,enemy))return false;
        Skill skill=bot.getKnownSkill(1394);Failure f=s.failures.get(1394);
        return !bot.isAllSkillsDisabled()&&!bot.isOutOfControl()&&!bot.isMuted()
            &&!bot.isSkillDisabled(skill)&&(f==null||time>=f.until)
            &&(!bot.isInOlympiadMode()||!skill.isBlockedInOlympiad())
            &&resources(bot,skill)&&skill.checkCondition(bot,enemy,false);
    }
    /** Native-legal self recovery wins below75%; no fabricated HP/MP or reuse bypass. */
    private static boolean cardinalHealReady(Player bot,State s,long time)
    {
        if(!CardinalSurvival.urgentHeal(bot)||s.pendingId!=0)return false;
        Skill skill=bot.getKnownSkill(CARDINAL_HEAL_ID);
        Failure f=s.failures.get(CARDINAL_HEAL_ID);
        return skill!=null&&BotRecoveryRules.selfHealAllowed(bot,skill)
            &&!bot.isAllSkillsDisabled()&&!bot.isOutOfControl()&&!bot.isMuted()
            &&!bot.isSkillDisabled(skill)&&(f==null||time>=f.until)
            &&(!bot.isInOlympiadMode()||!skill.isBlockedInOlympiad())
            &&resources(bot,skill)&&skill.checkCondition(bot,bot,false);
    }
    /** Only a fully legal, in-range native nuke may override the normal Trance priority. */
    private static boolean cardinalProminenceReady(Player bot,Player enemy,State s,long time)
    {
        if(bot.getClient()!=null||bot.getPlayerClass().getId()!=CARDINAL_CLASS_ID
            ||!CardinalSurvival.prominenceAllowed(bot,enemy)||!s.cardinalProminence.ready(time))return false;
        Skill skill=bot.getKnownSkill(CARDINAL_PROMINENCE_ID);
        Failure f=s.failures.get(CARDINAL_PROMINENCE_ID);
        return skill!=null&&!bot.isAllSkillsDisabled()&&!bot.isOutOfControl()
            &&!bot.isSkillDisabled(skill)&&(f==null||time>=f.until)
            &&(!bot.isInOlympiadMode()||!skill.isBlockedInOlympiad())
            &&resources(bot,skill)&&skill.checkCondition(bot,enemy,false)
            &&inRange(bot,enemy,skill,bot.calculateDistance2D(enemy))&&GeoEngine.getInstance().canSeeTarget(bot,enemy);
    }
    private static boolean pressureControl(Skill skill)
	{
		return skill.hasEffectType(EffectType.SLEEP, EffectType.STUN);
	}
	private static boolean pressureCurse(Skill skill)
	{
		final TacticsPolicy.Kind k = kind(skill);
		return !pressureControl(skill) && !directDamage(skill)
			&& (k == TacticsPolicy.Kind.CONTROL || k == TacticsPolicy.Kind.SLOW || skill.getId() == 1105);
	}

	/** Center-to-center pursuit stop distance, not permission to attack. Uses the
	 * same effective learned offense and legality as selection. In particular a
	 * dagger must reach its 40+collision blow radius, not an 80/110 auto envelope. */
	public static synchronized int tacticalApproachRange(Player bot, Player enemy, BotClassPreset preset)
	{
		if (bot == null || enemy == null || preset == null) { return 40; }
		// New ranged cycle attacks at native Steal range; approach Sleep only when due.
        if(RoleCombatPolicy.rangedDominator(bot,enemy))
        {
            State rs=STATES.get(bot.getObjectId());
            Skill chosen=bot.getKnownSkill(!RoleCombatPolicy.archer(enemy)&&RoleCombatPolicy.ownSeal(bot,enemy)==null&&rs!=null&&rs.rangedCycle.sleepDue()&&rangedSleepReady(bot,enemy,rs,now())?1097:1245);
            if(chosen!=null)return TacticsPolicy.boundedApproachRange(bot.getMagicalAttackRange(chosen));
        }
		if (TacticsPolicy.tankClass(preset.getClassId()) || strictMeleePressure(bot, enemy, preset))
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
			if (preset.getClassId() == CARDINAL_CLASS_ID && id == CARDINAL_TRANCE_ID) tranceReach = range;
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
		if (removed != null) clearEvidence(removed);
		if (removed != null) removed.cardinalSupport.clear();
		if (removed != null) removed.manaAutoAttack.clear();
		if (removed != null) removed.invulnerableKite.stop();
        if (removed != null) removed.invulnerableRanged.stop();
		if (removed != null) removed.survivalKite.stop();
	}
	public static synchronized String describe(int botId)
	{
		final State s = STATES.get(botId);
		return "tactics:" + getDifficulty(botId) + ":owned=" + (s != null && s.owned)
			+ ":pending=" + (s == null ? 0 : s.pendingId) + ":backoffs=" + (s == null ? 0 : s.failures.size())
			+ ":cardinalClarity=" + (s != null && s.cardinalSupport.active())
            + ":bluffRear=" + (s == null ? "idle" : s.bluffRear.describe())
            + ":cardinalTrancePriority=" + (s != null && s.cardinalTrancePriority)
            + ":cardinalSleep=" + (s == null ? "idle" : s.sleepRetreat.describe())
            + ":dominatorTankSleep=" + (s == null ? "idle" : s.tankSleepRetreat.describe())
            + ":cardinalWake=" + (s == null ? "rotation" : s.clarityWindow.describe());
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
		if (ArenaCombatRules.update(bot, enemy, preset)) return true;
        if (tickInvulnerableTarget(bot, enemy, preset, ranked)) return true;
        if (tickRoleCombat(bot, enemy, preset, ranked)) return true;
        if (tickBluffRear(bot, enemy, preset, ranked)) return true;
        if (tickCardinalControl(bot, enemy, preset, ranked)) return true;
        if (tickMageCancel(bot, enemy, preset, ranked)) return true;
		if (tickInvulnerableMelee(bot, enemy, preset, ranked)) return true;
		// Existing Rush/retreat/phase driver retains total ownership.
		if (ranked && preset.getClassId() == 131) { return false; }
		final long time = now();
		s = state(bot.getObjectId(), time);
		prepare(bot, enemy, s, time);
		s.summonAttack.tick(bot, enemy, time);
		s.manaAutoAttack.update(bot, preset);
		claim(bot, s);
		return act(bot, enemy, preset, s, time, false);
	}

    /** Mandatory live-Bluff owner, independent of the optional tactical toggle. */
    public static synchronized boolean tickBluffRear(Player bot, Player enemy, BotClassPreset preset, boolean ranked)
    {
        if (bot == null || preset == null || !valid(bot, enemy, ranked, STATES.get(bot.getObjectId())) || BluffRear.stun(bot, enemy) == null) return false;
        if (ArenaCombatRules.update(bot, enemy, preset)) return true;
        final long time = now(); final State s = state(bot.getObjectId(), time);
        prepare(bot, enemy, s, time); claim(bot, s);
        return act(bot, enemy, preset, s, time, false);
    }

    /** Mandatory all-opponent Cardinal control owns both arena drivers before generic kiting. */
    public static synchronized boolean tickCardinalControl(Player bot,Player enemy,BotClassPreset preset,boolean ranked)
    {
        if (bot == null || preset == null || preset.getClassId() != CARDINAL_CLASS_ID || !valid(bot,enemy,ranked,STATES.get(bot.getObjectId()))) return false;
        final long time=now(); final State s=state(bot.getObjectId(),time);
        prepare(bot,enemy,s,time);
        // All Cardinal opponents, not just melee or a live Clarity/shield window.
        if (ArenaCombatRules.update(bot,enemy,preset)) return true;
        claim(bot,s); return act(bot,enemy,preset,s,time,false);
    }

    /** Mandatory ready Cancel, also with optional TacticalAI disabled. Never interrupts another cast. */
    public static synchronized boolean tickMageCancel(Player bot,Player enemy,BotClassPreset preset,boolean ranked)
    {
        if(bot==null||preset==null||(preset.getClassId()!=94&&preset.getClassId()!=103)
            ||!valid(bot,enemy,ranked,STATES.get(bot.getObjectId())))return false;
        long time=now();State s=state(bot.getObjectId(),time);prepare(bot,enemy,s,time);
        pollAttempt(bot,enemy,s,time);
        if(s.pendingId!=1056&&!mageCancelReady(bot,enemy,preset,s,time))return false;
        if(ArenaCombatRules.update(bot,enemy,preset))return true;
        claim(bot,s);return act(bot,enemy,preset,s,time,false);
    }

    static synchronized Player currentOpponent(Player bot)
    {
        State s=STATES.get(bot.getObjectId());Player e=s==null?null:s.currentOpponent.get();
        if(e!=null&&e.getInstanceId()==bot.getInstanceId())return e;
        if(bot.isInOlympiadMode())return BotManager.getOlympiadOpponent(bot);
        return bot.getTarget()!=null&&bot.getTarget().isPlayer()&&bot.getTarget()!=bot?bot.getTarget().asPlayer():null;
    }
    /** Mandatory role driver. Never delegates a sleeping ranged target to auto-play. */
    public static synchronized boolean tickRoleCombat(Player bot,Player enemy,BotClassPreset preset,boolean ranked)
    {
        if(bot==null||preset==null||!valid(bot,enemy,ranked,STATES.get(bot.getObjectId())))return false;
        boolean ranged=RoleCombatPolicy.rangedDominator(bot,enemy),charge=TacticsPolicy.chargeMeleeClass(preset.getClassId());
        if(!ranged&&!charge)return false;
        if(ArenaCombatRules.update(bot,enemy,preset))return true;
        long time=now();State s=state(bot.getObjectId(),time);prepare(bot,enemy,s,time);claim(bot,s);
        s.summonAttack.tick(bot,enemy,time);
        boolean handled=act(bot,enemy,preset,s,time,false);
        if(bot.isCastingNow()||bot.isCastingSimultaneouslyNow()||s.pendingId!=0||bot.isOutOfControl()||bot.isAllSkillsDisabled()||enemy.isInvul())return true;
        double range=charge?bot.getPhysicalAttackRange()+bot.getTemplate().getCollisionRadius()+enemy.getTemplate().getCollisionRadius():tacticalApproachRange(bot,enemy,preset);
        boolean sealed=ranged&&RoleCombatPolicy.ownSeal(bot,enemy)!=null;
        if(ranged&&enemy.isSleeping()&&!sealed)
        {
            Skill silence=bot.getKnownSkill(1246);
            if(!RoleCombatPolicy.archer(enemy)&&SleepRetreat.ownSleep(bot,enemy)!=null&&silence!=null&&!active(enemy,silence))
                roleApproach(bot,enemy,s,time,Math.max(1,silence.getAffectRange()-10));
            return true;
        }
        if(charge&&requiresInvulnerableKite(bot,enemy)&&!rootedByTank(bot,enemy))return true;
        if((!enemy.isSleeping()||sealed)&&bot.calculateDistance2D(enemy)>range)roleApproach(bot,enemy,s,time,range);
        else if(charge&&!handled)stationaryAttack(bot,enemy,preset,s);
        return true;
    }
    private static void roleApproach(Player bot,Player enemy,State s,long time,double range)
    {
        if(bot.isMovementDisabled()||bot.isDisabled()||time<s.nextStep)return;
        double distance=bot.calculateDistance2D(enemy);
        if(distance<=range&&GeoEngine.getInstance().canSeeTarget(bot,enemy))return;
        int[] center=ArenaCombatRules.center(bot);
        if(center==null||Math.hypot((double)enemy.getX()-center[0],(double)enemy.getY()-center[1])>ArenaCombatRules.attackRadius(bot))return;
        s.nextStep=time+400;
        if(bot.isMoving()&&Math.hypot((double)bot.getXdestination()-enemy.getX(),(double)bot.getYdestination()-enemy.getY())<40)return;
        double fraction=Math.min(1,Math.max(0,(distance-Math.max(0,range-10))/Math.max(1,distance)));
        Location goal=new Location(bot.getX()+(int)Math.round((enemy.getX()-bot.getX())*fraction),bot.getY()+(int)Math.round((enemy.getY()-bot.getY())*fraction),enemy.getZ());
        if(GeoEngine.getInstance().canMoveToTarget(bot.getX(),bot.getY(),bot.getZ(),goal.getX(),goal.getY(),goal.getZ(),bot.getInstanceId()))bot.getAI().setIntentionMoveTo(goal);
    }

    /** Mandatory ranged-target immunity phase, also with optional TacticalAI disabled.
     * Existing invulnerable-melee behavior is serviced by tickInvulnerableMelee. */
    public static synchronized boolean tickInvulnerableTarget(Player bot,Player enemy,BotClassPreset preset,boolean ranked)
    {
        if(bot==null||preset==null||!valid(bot,enemy,ranked,STATES.get(bot.getObjectId())))return false;
        if(ArenaCombatRules.update(bot,enemy,preset))return true;
        long time=now();State s=state(bot.getObjectId(),time);prepare(bot,enemy,s,time);
        return rangedInvulnerable(bot,enemy,preset,s,time);
    }
    private static boolean rangedInvulnerable(Player bot,Player enemy,BotClassPreset preset,State s,long time)
    {
        boolean required=enemy.isInvul()&&!meleeOpponent(enemy);
        if(!required&&!s.rangedInvulnerableLease)return false;
        if(!required)
        {
            // Keep the route handle until the real cast ends; never abort a cast to clean up.
            if(bot.isCastingNow()||bot.isCastingSimultaneouslyNow())return true;
            s.invulnerableRanged.stop();s.owned=s.rangedPreviousOwned;s.rangedInvulnerableLease=false;
            s.nextDecision=0;s.nextStep=0;return false;
        }
        if(!s.rangedInvulnerableLease)
        {
            s.rangedPreviousOwned=s.owned;s.rangedInvulnerableLease=true;
            if(bot.isMoving()&&!bot.isCastingNow()&&!bot.isCastingSimultaneouslyNow())bot.getAI().setIntentionIdle();
        }
        claim(bot,s);
        if(bot.isCastingNow()||bot.isCastingSimultaneouslyNow())return true;
        stopBasicAttack(bot);s.summonAttack.tick(bot,enemy,time);pollAttempt(bot,enemy,s,time);restoreCardinalMana(bot,preset);
        if(s.pendingId!=0)return true;
        if(escapeShackle(bot,enemy,preset,s,time)||soulCleanse(bot,enemy,preset,s,time))return true;
        if(supportBeforeMovement(bot,enemy,preset,s,time))return true;
        return s.invulnerableRanged.tick(bot,enemy,meleeOpponent(bot),time);
    }

    /** One policy for the mandatory hook, normal act and movement owner.
     * Tank mirrors keep fighting through UD/Vengeance/defense760; real invulnerability is unchanged. */
    private static boolean requiresInvulnerableKite(Player bot, Player enemy)
    {
        return (enemy.isInvul() && meleeOpponent(enemy))
            || (TankClassMechanics.defendedTank(enemy) && !TankClassMechanics.tank(bot));
    }

    /** Arena escape from invulnerable melee or a defended tank facing a non-tank, including ranked131. */
    public static synchronized boolean tickInvulnerableMelee(Player bot, Player enemy, BotClassPreset preset, boolean ranked)
    {
        if (bot == null || preset == null || !valid(bot, enemy, ranked, STATES.get(bot.getObjectId()))) return false;
        if (ArenaCombatRules.update(bot, enemy, preset)) return true;
        final long time = now();
        final State s = state(bot.getObjectId(), time);
        prepare(bot, enemy, s, time);
        final boolean required = requiresInvulnerableKite(bot, enemy);
        if (!required && !s.invulnerableLease) return false;
        if (required && !s.invulnerableLease)
        { s.invulnerablePreviousOwned = s.owned; s.invulnerableLease = true; }
        pollAttempt(bot, enemy, s, time);
        restoreCardinalMana(bot, preset);
        // Retain the lease until ROOT ends even if the tank's defense expires first.
        if(rootedByTank(bot,enemy)) { claim(bot,s);act(bot,enemy,preset,s,time,false);return true; }
        if (s.invulnerableLease && !required)
        {
            // Keep the route handle until the cast ends, so cleanup can still stop our old kite.
            // Never abort a real cast when invulnerability expires while UD remains.
            if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) return true;
            s.invulnerableKite.stop();
            s.tankSleepRetreat.clear(bot);
            s.owned = s.invulnerablePreviousOwned; s.invulnerableLease = false;
            s.nextStep = 0; s.nextDecision = 0; return false;
        }
        claim(bot, s);
        if (CardinalSurvival.active(bot, enemy) || preset.getClassId() == CARDINAL_CLASS_ID) return act(bot, enemy, preset, s, time, false);
        if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) return true;
        if (escapeShackle(bot, enemy, preset, s, time) || soulCleanse(bot, enemy, preset, s, time)) return true;
        if (dominatorTankSleepHold(bot,enemy,preset,s,time)) return true;
        if (supportBeforeMovement(bot, enemy, preset, s, time)) return true;
        return invulnerableMelee(bot, enemy, s, time);
    }
    /** Actual active ROOT from the current tank; never confuse stun, self-UD or a request with root. */
    static boolean rootedByTank(Player bot,Player enemy)
    {
        if(bot==null||bot.getClient()!=null||!bot.isRooted()||!TankClassMechanics.tank(enemy))return false;
        for(BuffInfo info:bot.getEffectList().getEffects())
        {
            if(info==null||info.isRemoved()||!info.isInUse()||info.getEffector()!=enemy||info.getEffected()!=bot
                ||info.getSkill()==null||info.getEffects()==null
                ||bot.getEffectList().getBuffInfoByAbnormalType(info.getSkill().getAbnormalType())!=info)continue;
            for(var effect:info.getEffects())
                if(!effect.isInstant()&&effect.getEffectType()==EffectType.ROOT
                    &&(effect.getEffectFlags()&EffectFlag.ROOTED.getMask())!=0)return true;
        }
        return false;
    }
    private static boolean invulnerableMelee(Player bot, Player enemy, State s, long time)
    {
        if(rootedByTank(bot,enemy)){s.invulnerableKite.stop();return false;}
        return s.invulnerableKite.tick(bot, enemy, time, requiresInvulnerableKite(bot, enemy));
    }

	/** Frequent active-match hook; shares the same3s clock with maintenance/training. */
	public static synchronized void tickSummonAttack(Player bot, Player enemy, boolean ranked)
	{
		if (bot == null || !valid(bot, enemy, ranked, STATES.get(bot.getObjectId()))) return;
		final long time = now();
		final State s = state(bot.getObjectId(), time);
		prepare(bot, enemy, s, time);
		s.summonAttack.tick(bot, enemy, time);
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

	/** Emergency magic self-cast, before the ranked movement/phase/reaction gates.
	 * A short ownership lease protects ONE-target Soul Cleanse from parent retargeting. */
	public static synchronized boolean tickSoulCleanse(Player bot, Player enemy, BotClassPreset preset, boolean ranked)
	{
		if (preset == null || preset.getClassId() != 131 || !valid(bot, enemy, ranked, bot == null ? null : STATES.get(bot.getObjectId()))) return false;
		final long time = now();
		final State s = state(bot.getObjectId(), time);
		prepare(bot, enemy, s, time);
		pollAttempt(bot, enemy, s, time);
		if (s.cleanseLease && s.pendingId != SOUL_CLEANSE_ID) { s.owned = false; s.cleanseLease = false; }
		final boolean handled = soulCleanse(bot, enemy, preset, s, time);
		if (handled && !s.owned) { s.owned = true; s.cleanseLease = true; }
		return handled;
	}

	/** Root/physical-skill mute do NOT prohibit magic. Native magic silence, CC,
	 * reuse, MP and race/target conditions still apply. Never cancel another cast. */
	private static boolean soulCleanse(Player bot, Player enemy, BotClassPreset preset, State s, long time)
	{
		if (preset.getClassId() != 131 || bot.getPlayerClass().getId() != 131 || bot.getClient() != null) return false;
		if (s.pendingId == SOUL_CLEANSE_ID) return true;
		if (s.pendingId != 0 || bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()
			|| !(bot.isRooted() || bot.isPhysicalMuted())) return false;
		final Skill skill = bot.getKnownSkill(SOUL_CLEANSE_ID);
		final Failure backoff = s.failures.get(SOUL_CLEANSE_ID);
		if (skill == null || !skill.isMagic() || skill.getTargetType() != TargetType.ONE || skill.isPassive()
			|| bot.isSkillDisabled(skill) || bot.isInOlympiadMode() && skill.isBlockedInOlympiad()
			|| !resources(bot, skill) || backoff != null && time < backoff.until) return false;
		// Soul Cleanse is targeted magic, NOT a physical attack and NOT targetType SELF.
		bot.setTarget(bot);
		if (!skill.checkCondition(bot, bot, false)) { bot.setTarget(enemy); return false; }
		AutoUseTaskManager.getInstance().stopAutoUseTask(bot);
		AutoPlayTaskManager.getInstance().stopAutoPlay(bot);
		stopBasicAttack(bot);
		bot.setTarget(bot); // Idle/attack cleanup must not leave a different target before useMagic.
		s.pendingMp = bot.getCurrentMp(); s.pendingHp = bot.getCurrentHp(); s.pendingCharges = bot.getCharges();
		s.pendingX = bot.getX(); s.pendingY = bot.getY(); s.pendingRushLoop = false;
		if (!bot.useMagic(skill, true, true)) { failure(s, SOUL_CLEANSE_ID, time); bot.setTarget(enemy); return false; }
		s.pendingId = SOUL_CLEANSE_ID; s.pendingAt = time; s.pendingSelf = true; s.pendingDebuff = false; s.sawCast = bot.isCastingNow();
		s.moveUntil = 0;
		CombatAnalytics.tacticalRequest(bot.getObjectId(), "DEFENSE");
		return true; // Preserve self target until pollAttempt observes resolution.
	}

	/** Learned dagger-only root escape. Unavailable means continue normal combat,
	 * not wait here; retry before the normal decision gate while ROOT persists. */
	private static boolean escapeShackle(Player bot, Player enemy, BotClassPreset preset, State s, long time)
	{
		final int classId = preset.getClassId();
		if ((classId != 93 && classId != 101 && classId != 108) || bot.getPlayerClass().getId() != classId || bot.getClient() != null) return false;
		if (s.pendingId == ESCAPE_SHACKLE_ID) return true;
		// Root escape takes priority over the own-Bluff rear phase (87).
		if (!bot.isRooted() || s.pendingId != 0 || bot.isCastingNow() || bot.isCastingSimultaneouslyNow()
			|| bot.isOutOfControl() || bot.isAllSkillsDisabled()) return false;
		final Skill skill = bot.getKnownSkill(ESCAPE_SHACKLE_ID);
		final Failure backoff = s.failures.get(ESCAPE_SHACKLE_ID);
		if (skill == null || skill.isMagic() || skill.getTargetType() != TargetType.SELF || skill.isPassive()
			|| bot.isSkillDisabled(skill) || bot.isInOlympiadMode() && skill.isBlockedInOlympiad()
			|| !resources(bot, skill) || backoff != null && time < backoff.until) return false;
		bot.setTarget(bot);
		if (!skill.checkCondition(bot, bot, false)) { bot.setTarget(enemy); return false; }
		AutoUseTaskManager.getInstance().stopAutoUseTask(bot);
		AutoPlayTaskManager.getInstance().stopAutoPlay(bot);
		stopBasicAttack(bot);
		bot.setTarget(bot);
		s.pendingMp = bot.getCurrentMp(); s.pendingHp = bot.getCurrentHp(); s.pendingCharges = bot.getCharges();
		s.pendingX = bot.getX(); s.pendingY = bot.getY(); s.pendingRushLoop = false;
		if (!bot.useMagic(skill, true, true)) { failure(s, ESCAPE_SHACKLE_ID, time); bot.setTarget(enemy); return false; }
		s.pendingId = ESCAPE_SHACKLE_ID; s.pendingAt = time; s.pendingSelf = true; s.pendingDebuff = false; s.sawCast = bot.isCastingNow();
		s.moveUntil = 0;
		CombatAnalytics.tacticalRequest(bot.getObjectId(), "DEFENSE");
		return true;
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
		s.currentOpponent = new java.lang.ref.WeakReference<>(enemy);
        final int game = bot.isInOlympiadMode() ? bot.getOlympiadGameId() : -1;
		if (s.enemy != enemy.getObjectId() || s.game != game || s.instance != bot.getInstanceId())
		{
			if(s.rangedInvulnerableLease){s.owned=s.rangedPreviousOwned;s.rangedInvulnerableLease=false;}
            s.invulnerableRanged.stop();
            if (s.invulnerableLease) { s.owned = s.invulnerablePreviousOwned; s.invulnerableLease = false; }
			s.invulnerableKite.reset(); s.survivalKite.reset(); s.clarityWindow.reset(); s.bluffRear.stop(bot);
			if (s.cleanseLease) { s.owned = false; s.cleanseLease = false; }
			s.cardinalSupport.clear();
			s.manaAutoAttack.clear();
			clearEvidence(s);
			s.enemy = enemy.getObjectId(); s.game = game; s.instance = bot.getInstanceId();
			s.anchorX = bot.getX(); s.anchorY = bot.getY(); s.observed = time;
			final int[] center = ArenaCombatRules.center(bot);
			s.arenaCenterX = center != null ? center[0] : 0;
			s.arenaCenterY = center != null ? center[1] : 0;
			s.hasArenaCenter = center != null;
			s.combatStartedAt = time; s.invulnerableKite.reset(); s.invulnerableLease = false;
			s.trainingBurst.reset(); s.trainingBerserkerMode = false; s.dominatorCp.reset(); s.dominatorPressure.reset(); s.sleepRetreat.reset(); s.tankSleepRetreat.clear(bot); s.summonAttack.reset(); s.controlRetreat.reset();
			s.tankDefense.reset(time); s.rushLoop.clear(); s.nextRushStep = time; s.pendingRushLoop = false; s.rushMoving = false; s.rushDirectionX = 0; s.rushDirectionY = 0;
			s.sleepObserved = -1; s.nextDecision = time; s.nextStep = time; s.pendingId = 0; s.failures.clear(); s.enemyCasting = false; s.moveUntil = 0;
			s.cardinalProminence.reset(); s.cardinalTrancePriority = false;
			s.rangedCycle.reset(); s.chargePursuitUntil=0;
            s.fleeingSamples = 0; s.offensiveActionsSinceStep = 2; s.previousDistance = -1; s.nextHabitSample = time;
		}
		if (bot.getPlayerClass().getId() == CARDINAL_CLASS_ID) s.cardinalSupport.start(bot, enemy, s.combatStartedAt, time);
		final double hp = bot.getCurrentHp() / Math.max(1, bot.getMaxHp());
		final int hpBand = hp < .35 ? 0 : hp < .65 || defenseThreat(bot, enemy) && cpRatio(bot) < .35 ? 1 : 2;
		if (hpBand != s.hpBand) { s.hpBand = hpBand; s.hpObserved = time; }
		final boolean casting = GeoEngine.getInstance().canSeeTarget(bot, enemy) && enemy.isCastingNow();
		if (casting && !s.enemyCasting) { s.castObserved = time; }
		if (!casting) { s.castObserved = -1; }
		s.enemyCasting = casting;
	}

	/** A rooted ranked Berserker can cast legal stationary skills without lending permanent ownership. */
	public static synchronized boolean tickRootedBerserker(Player bot, Player enemy, BotClassPreset preset)
	{
		if (preset == null || preset.getClassId() != 131 || !bot.isMovementDisabled()
			|| !valid(bot, enemy, true, STATES.get(bot.getObjectId()))) return false;
		final long time = now(); final State s = state(bot.getObjectId(), time);
		prepare(bot, enemy, s, time);
		// Preserve the existing live-STUN Rush-only phase delegated to RushStunSupport.
		if (ownRushStun(bot, enemy) != null) return true;
		return act(bot, enemy, preset, s, time, false);
	}

	private static boolean supportBeforeMovement(Player bot, Player enemy, BotClassPreset preset, State s, long time)
	{
		// Preserve other classes' existing Celestial handling; Cardinal recovery uses native legality.
        // Never replace a live cast or a pending request.
		if (bot.isAffectedBySkill(1418) && preset.getClassId()!=CARDINAL_CLASS_ID || s.pendingId != 0 || bot.isOutOfControl() || bot.isAllSkillsDisabled() || BluffRear.stun(bot, enemy) != null) return false;
		return act(bot, enemy, preset, s, time, true, true);
	}

	private static boolean act(Player bot, Player enemy, BotClassPreset preset, State s, long time, boolean defenseOnly, boolean movingSupport)
	{
        if(!defenseOnly && rangedInvulnerable(bot,enemy,preset,s,time))return true;
		BotControlChance.apply(bot);
		final boolean survival = (!defenseOnly || movingSupport) && CardinalSurvival.active(bot, enemy);
        final boolean rootedKite = !defenseOnly && rootedByTank(bot,enemy);
        final boolean cardinalKiting = !defenseOnly && cardinalTrancePressure(bot, enemy, preset);
        final boolean previousTrancePriority = s.cardinalTrancePriority;
        s.clarityWindow.observe(bot, enemy);
        final boolean cardinalSleeping = !defenseOnly && preset.getClassId() == CARDINAL_CLASS_ID && enemy.isSleeping();
        final boolean cardinalNoKite = CardinalSurvival.noKite(bot);
        // Rule76: a legal ready Prominence at >90% HP precedes Trance, regardless of Clarity.
        boolean healPriority = cardinalHealReady(bot,s,time);
        final boolean prominencePriority = !defenseOnly && !healPriority && cardinalProminenceReady(bot,enemy,s,time);
        // While protected, even native reuse/silence gaps must not fall back to fleeing.
        s.cardinalTrancePriority = !defenseOnly && !healPriority && !prominencePriority && CardinalSurvival.tranceTarget(bot,enemy)
            && (cardinalNoKite || cardinalTranceReady(bot,enemy,s,time));
        final boolean trancePriority = s.cardinalTrancePriority;
        final boolean tankSleepPriority = (movingSupport || rootedKite) && dominatorTankSleepReady(bot,enemy,preset,s,time);
        final boolean cancelPriority = (!defenseOnly || movingSupport) && mageCancelReady(bot,enemy,preset,s,time);
		if (!survival && !cardinalKiting && !movingSupport) s.survivalKite.stop();
        final BuffInfo bluffStun = BluffRear.stun(bot, enemy);
        final boolean bluffHold = bluffStun != null;
		final boolean trainingBerserker = !defenseOnly && !rootedKite && preset.getClassId() == 131 && !bot.isInOlympiadMode()
			&& s.trainingEnemy == enemy.getObjectId() && s.trainingInstance == bot.getInstanceId();
		s.trainingBerserkerMode = trainingBerserker;
		if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow())
		{
			pollAttempt(bot, enemy, s, time);
			restoreCardinalMana(bot, preset);
			return true; // never Idle, cancel an attack, or retarget during a real cast
		}
        if(rootedKite)
        {
            s.invulnerableKite.stop();s.survivalKite.stop();s.moveUntil=0;
            if(bot.isMoving())bot.getAI().setIntentionIdle();
        }
		if (!defenseOnly && !allowsBasicAttack(bot, preset)) { stopBasicAttack(bot); }
		if (bluffHold) stopBasicAttack(bot); // never Idle/abort an already running real cast
		pollAttempt(bot, enemy, s, time);
		restoreCardinalMana(bot, preset);
		// Recheck after polling and the existing Cardinal MP policy; pending completion may unlock recovery.
        healPriority = cardinalHealReady(bot,s,time);
        // Reuse the normal self-skill admission/cast bookkeeping, before any movement phase.
        if (healPriority && (!defenseOnly || movingSupport))
        {
            act(bot,enemy,preset,s,time,true,false);
            return true; // own the recovery phase even during ordinary reaction/decision latency
        }
        if (healPriority)
        {
            s.invulnerableKite.stop(); s.survivalKite.stop(); s.moveUntil=0;
            if (bot.isMoving()) bot.getAI().setIntentionIdle();
        }
        if (escapeShackle(bot, enemy, preset, s, time)) return true;
        if (soulCleanse(bot, enemy, preset, s, time)) return true;
		if (!defenseOnly && dominatorTankSleepHold(bot,enemy,preset,s,time)) return true;
        if (!defenseOnly && !rootedKite && !cancelPriority && !prominencePriority && !cardinalNoKite && !cardinalSleeping && !s.clarityWindow.pending() && !survival && !trancePriority && !bluffHold && !CardinalSurvival.ownCelestial(bot))
		{
			final boolean mustKite = requiresInvulnerableKite(bot, enemy);
			if (mustKite && supportBeforeMovement(bot, enemy, preset, s, time)) return true;
			if (invulnerableMelee(bot, enemy, s, time)) return true; // also releases the previous route when protection ends
		}
		if (trainingBerserker && (bot.isOutOfControl() || bot.isAllSkillsDisabled() || s.pendingId != 0))
		{
			stopBasicAttack(bot); // no auto continuation under cast/CC; never abort a cast
			return true;
		}
		s.sleepObserved = TacticsPolicy.observeSleep(s.sleepObserved, enemy.isSleeping(), time);
		// Real casts and CC are never aborted or replaced, including by urgent heals.
		if (bot.isCastingNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()) { return !defenseOnly; }
		if (s.pendingId != 0) { return !defenseOnly; }
        if (!defenseOnly && prominencePriority)
        {
            s.invulnerableKite.stop(); s.survivalKite.stop(); s.moveUntil=0;
            if(bot.isMoving())bot.getAI().setIntentionIdle();
        }
        if (!defenseOnly && cardinalNoKite)
        {
            s.invulnerableKite.stop(); s.survivalKite.stop(); s.moveUntil=0;
            if (bot.isMoving() && (!trancePriority || !previousTrancePriority || inRange(bot,enemy,bot.getKnownSkill(1394),bot.calculateDistance2D(enemy)))) bot.getAI().setIntentionIdle();
        }
        if (cardinalSleeping)
        {
            // One center-crossing retreat, then HOLD. Never orbit, chase or wake with damage.
            s.invulnerableKite.stop(); s.survivalKite.stop(); s.moveUntil=0;
            stopBasicAttack(bot);
            if (supportBeforeMovement(bot,enemy,preset,s,time)) return true;
            s.sleepRetreat.tick(bot,enemy,time,s.hasArenaCenter ? new int[]{s.arenaCenterX,s.arenaCenterY} : ArenaCombatRules.center(bot));
            return true; // including root/no route/arrived/missing effect; no generic fallback
        }
        if (!defenseOnly && preset.getClassId() == CARDINAL_CLASS_ID)
            s.sleepRetreat.tick(bot,enemy,time,s.hasArenaCenter ? new int[]{s.arenaCenterX,s.arenaCenterY} : ArenaCombatRules.center(bot));
        if (trancePriority)
        {
            s.invulnerableKite.stop(); s.survivalKite.stop(); s.moveUntil = 0;
            // This phase owns movement, but never cancels a native cast above.
            if (bot.isMoving() && (!previousTrancePriority || inRange(bot, enemy, bot.getKnownSkill(1394), bot.calculateDistance2D(enemy)))) bot.getAI().setIntentionIdle();
        }

		final int[] bluffCenter = s.hasArenaCenter ? new int[]{s.arenaCenterX, s.arenaCenterY} : ArenaCombatRules.center(bot);
        if (!defenseOnly && s.bluffRear.tick(bot, enemy, bluffStun, time, bluffCenter)) return true;
        final boolean sleeping = enemy.isSleeping();
        final BuffInfo meleeControl = (!defenseOnly || movingSupport) && bot.getPlayerClass().getId() == 115 && meleeOpponent(enemy)
            ? DominatorControlRetreat.control(bot, enemy) : null;
        final boolean controlHold = meleeControl != null;
        final int[] arenaCenter = s.hasArenaCenter ? new int[]{s.arenaCenterX, s.arenaCenterY} : ArenaCombatRules.center(bot);
        if (!defenseOnly && meleeControl != null && supportBeforeMovement(bot, enemy, preset, s, time)) return true;
        if (!defenseOnly && s.controlRetreat.tick(bot, meleeControl, time, arenaCenter)) return true;
        if (controlHold) stopBasicAttack(bot);
		// Actual landed own sleep, never a mere accepted request or another caster's effect.
		final boolean roleRanged = RoleCombatPolicy.rangedDominator(bot,enemy);
        final boolean ownSleep = !defenseOnly && !roleRanged && !controlHold && SleepRetreat.ownSleep(bot, enemy) != null;
		if (ownSleep && !survival && supportBeforeMovement(bot, enemy, preset, s, time)) return true;
		if (!defenseOnly && !roleRanged && !survival && !controlHold && s.sleepRetreat.tick(bot, enemy, time, s.hasArenaCenter ? new int[]{s.arenaCenterX, s.arenaCenterY} : ArenaCombatRules.center(bot))) return true;
		final boolean manaFallback = !defenseOnly && needsManaAutoAttack(bot, preset) && allowsBasicAttack(bot, preset);
		final boolean dominator = preset.getClassId() == 115;
		final boolean pressure = false; // Legacy ranged curse ladder superseded by role cycle88.
        s.rangedCycle.observe(enemy.isSleeping());
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
		if (!tankSleepPriority && (time < s.nextDecision || !TacticsPolicy.reactionReady(difficulty(s), time, s.observed, hp < .35)))
        {
            // Reaction latency must never pause the universal invulnerable-melee escape.
            if (survival && !trancePriority && !cardinalNoKite) invulnerableMelee(bot, enemy, s, time);
            return !defenseOnly;
        }
		final boolean visible = GeoEngine.getInstance().canSeeTarget(bot, enemy);
		final double distance = bot.calculateDistance2D(enemy);
		final boolean pressureClosing = pressure && !ownSleep && (!visible || distance > tacticalApproachRange(bot, enemy, preset));
		final boolean tank = TacticsPolicy.tankClass(preset.getClassId());
		final boolean timedTank = tank;
		final boolean chargeMelee = strictMeleePressure(bot, enemy, preset);
        final boolean chargeKited = chargeMelee && RoleCombatPolicy.kited(bot,enemy) && time>=s.chargePursuitUntil;
		final boolean chargeClosing = !rootedKite && chargeMelee && (!visible || distance > TacticsPolicy.tankApproachRange(bot.getPhysicalAttackRange(), bot.getTemplate().getCollisionRadius()));
		final boolean tankClosing = !rootedKite && tank && (!visible || distance > TacticsPolicy.tankApproachRange(bot.getPhysicalAttackRange(), bot.getTemplate().getCollisionRadius()));
		final int activeStrong = bot.isAffectedBySkill(110) ? 110 : bot.isAffectedBySkill(368) ? 368 : bot.isAffectedBySkill(760) ? 760 : 0;
		if (timedTank) { s.tankDefense.observed(activeStrong); }
		final int rushAction = trainingBerserker ? trainingRushAction(bot, enemy, preset, s, time, distance, visible) : -1;
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
		// Mandatory learned recovery is independent of persisted buff/rotation overrides.
		// Eligibility (HP <=40%, invulnerability, STUN phase, MP/reuse) is checked below.
		if (preset.getClassId() == 131 && bot.getKnownSkill(833) != null) skills.put(833, bot.getKnownSkill(833));
		if(roleRanged)for(int id:new int[]{1245,1097,1246}){Skill learned=bot.getKnownSkill(id);if(learned!=null)skills.put(id,learned);}
        if (controlHold && bot.getKnownSkill(1245) != null) skills.put(1245, bot.getKnownSkill(1245));
        Skill shadow = bot.getKnownSkill(821);
        if (!defenseOnly && shadow != null && daggerShadow(bot, enemy, preset, shadow)) skills.put(821, shadow);
        if (survival) for (int id : new int[]{1394,1398,1230,1218})
        { Skill learned=bot.getKnownSkill(id); if(learned!=null)skills.put(id,learned); }
        if (healPriority) skills.put(CARDINAL_HEAL_ID,bot.getKnownSkill(CARDINAL_HEAL_ID));
        if (trancePriority) skills.put(1394, bot.getKnownSkill(1394));
        if (tankSleepPriority) skills.put(1097,bot.getKnownSkill(1097));
        if (cancelPriority) skills.put(1056,bot.getKnownSkill(1056));
        if (prominencePriority) skills.put(CARDINAL_PROMINENCE_ID,bot.getKnownSkill(CARDINAL_PROMINENCE_ID));
        // Learned class mechanics remain available with persisted rotation overrides.
        for (int id : TankClassMechanics.skills(bot.getPlayerClass().getId()))
        { Skill learned=bot.getKnownSkill(id); if(learned!=null)skills.put(id,learned); }
		int chargeDemand = 4;
		for (Skill skill : skills.values())
		{
			// Respect the bot ceiling7; active arena charges come only from native skills.
			if (skill.getChargeConsumeCount() <= 7) { chargeDemand = Math.max(chargeDemand, skill.getChargeConsumeCount()); }
		}
		final TacticsPolicy.Context context = new TacticsPolicy.Context(hp, visible ? enemy.getCurrentHp() / Math.max(1, enemy.getMaxHp()) : 1,
			distance, sleeping, s.enemyCasting, interruptReady, visible && enemy.isMoving(), window, bot.getCharges() < chargeDemand);
		final List<TacticsPolicy.Option> options = new ArrayList<>();
		final List<Integer> tankStuns = new ArrayList<>(), tankRoots = new ArrayList<>(), tankMelee = new ArrayList<>();
		final List<Integer> pressureControls = new ArrayList<>(), pressureCurses = new ArrayList<>();
		boolean offenseAvailable = false;
		for (Skill skill : skills.values())
		{
			if (!CardinalSurvival.skillAllowed(bot,skill.getId())) continue;
            if(roleRanged&&!RoleCombatPolicy.candidate(bot,enemy,skill,self(kind(skill))))continue;
            if(roleRanged&&skill.getId()==1097&&!s.rangedCycle.sleepDue())continue;
            if(roleRanged&&skill.getId()==1245&&!enemy.isSleeping()&&!RoleCombatPolicy.archer(enemy)&&s.rangedCycle.sleepDue()
                &&!bot.isMovementDisabled()&&rangedSleepReady(bot,enemy,s,time)
                &&!inRange(bot,enemy,bot.getKnownSkill(1097),distance))continue;
            if(chargeMelee&&RoleCombatPolicy.builder(skill.getId())&&(!RoleCombatPolicy.needsCharge(bot)||chargeClosing&&!chargeKited))continue;
            if (healPriority && skill.getId()!=CARDINAL_HEAL_ID) continue;
            if (tankSleepPriority && skill.getId()!=1097) continue;
            if (cancelPriority && skill.getId()!=1056) continue;
            if (trancePriority && skill.getId() != 1394) continue;
            if (survival && !CardinalSurvival.allowed(skill.getId())) continue;
            if (survival && skill.getId() == 1398 && !s.clarityWindow.manaBurn(enemy)) continue;
            if (preset.getClassId()==CARDINAL_CLASS_ID && skill.getId()==CARDINAL_PROMINENCE_ID && !CardinalSurvival.prominenceAllowed(bot,enemy)) continue;
            if (bluffHold && (defenseOnly || skill.getId() != 531 && !(skill.isPhysical() && directDamage(skill)))) continue;
            if (!TankClassMechanics.allowed(bot, skill.getId())) continue;
			if (controlHold && !(tankSleepPriority && skill.getId()==1097) && skill.getId() != 1245 && !TacticsPolicy.dominatorCpSkill(skill.getId())) continue;
			if (BotSpawner.isOpenerOnlySkill(skill.getId())) { continue; } // training as well as ranked
			// Body Reconstruction (833): Doombringer-exclusive HP-threshold self-heal.
			// Activate at40% HP or below; the shared guard also protects legacy auto-use.
			// urgency. Applies to both training (defenseOnly=false) and olympiad
			// (defenseOnly=true via tickDefense) paths.
			if (!BotRecoveryRules.selfHealAllowed(bot, skill)) { continue; }
			if (preset.getClassId() == CARDINAL_CLASS_ID) {
				if (skill.getId() == CARDINAL_PROMINENCE_ID && !s.cardinalProminence.ready(time)) { continue; }
			}
			if (trainingRushActive && skill.getId() != 793) { continue; }
			final boolean timedPool = timedTank && BotSpawner.isTankDefenseSkill(skill.getId());
			if (timedPool && !s.tankDefense.due(time)) { continue; }
			if (trainingBerserker && !trainingRushActive && !s.trainingBurst.allowsSkill(skill.getId(), distance, skills.containsKey(793))) { continue; }
			final TacticsPolicy.Kind detected = TankClassMechanics.support(skill.getId()) ? TacticsPolicy.Kind.DEFENSE : pressure && skill.getId() == 1105 ? TacticsPolicy.Kind.SLOW : kind(skill);
			final boolean direct = directDamage(skill);
			final TacticsPolicy.Kind kind = dominator && sleeping && (!pressure || !pressureControl(skill)) && sleepElapsed >= 5000 && direct ? TacticsPolicy.Kind.DAMAGE : detected;
			if (kind == null || (defenseOnly && !tankSleepPriority && !cancelPriority && kind != TacticsPolicy.Kind.HEAL && kind != TacticsPolicy.Kind.DEFENSE)) { continue; }
			// Rescue casts must not immobilize the escaping bot or spend a tick building icon charges.
			if (movingSupport && !tankSleepPriority && !cancelPriority && (!self(kind) || immobilizingSupport(skill) || skill.getId() == 368
				|| skill.getId() == 784 || skill.getId() == 761 || skill.getId() == 788
				|| kind == TacticsPolicy.Kind.HEAL && !TacticsPolicy.dominatorCpSkill(skill.getId()) && hp >= .65)) continue;
			if (chargeClosing && !chargeKited && !self(kind)) { continue; }
			if (pressure && !self(kind) && (pressureClosing
				|| pressureControl(skill) && (s.dominatorPressure.controlFinished() || enemy.isSleeping() || enemy.isStunned())
				|| pressureCurse(skill) && s.dominatorPressure.curseDone(skill.getId()))) { continue; }
			final boolean self = self(kind);
			// PARTY_CLAN may include a same-clan/party opponent. Never heal/buff them;
			// query ordinary targets without changing the target or skill implementation.
			if (self && (skill.getId() == 1305 || skill.getId() == 1414 || skill.getId() == 785 || skill.getId() == 789)
				&& !TacticsPolicy.selfOnlyTargets(bot, skill.getTargetList(bot, false, bot))) { continue; }
			final boolean cpSupport = dominator && kind == TacticsPolicy.Kind.HEAL && TacticsPolicy.dominatorCpSkill(skill.getId()) && skill.hasEffectType(EffectType.CPHEAL);
			if (cpSupport && !roleRanged && !(controlHold ? TacticsPolicy.dominatorControlCpNeeded(cpRatio(bot)) : TacticsPolicy.dominatorCpNeeded(cpRatio(bot)))) { continue; }
			if (movingSupport && controlHold && cpSupport && s.dominatorCp.owesAttack()) continue;
			final boolean tankStun = tank && !self && skill.hasEffectType(EffectType.STUN);
			final boolean tankRoot = tank && !self && skill.hasEffectType(EffectType.ROOT);
			if (tank && !TacticsPolicy.tankClosingSkillAllowed(tankClosing, self, kind, tankStun || tankRoot)) { continue; }
			if (tank && self && !TacticsPolicy.tankSupportAllowed(tankClosing, immobilizingSupport(skill))) { continue; }
			if (tankStun && enemy.isStunned() || tankRoot && enemy.isMovementDisabled()) { continue; }
			// Prominence (1230) also bypasses the sleep-offense gate, same as Mana Burn (1398).
			final boolean cardinalMbException = !survival && TacticsPolicy.cardinalSleepException(preset.getClassId(), skill.getId());
			if (!roleRanged && !self && !pressure && !cardinalMbException && !trainingRushActive && !TacticsPolicy.sleepOffenseAllowed(dominator, sleeping, sleepElapsed, direct, skill.getId() == 1097 || skill.getId() == 1069)) { continue; }
			final boolean protectedSleep = sleeping && !dominator && !cardinalMbException && !trainingRushActive;
			if (!BotSpawner.tacticalSkillAllowed(bot, enemy, skill, self)) { continue; }
			if (!timedPool && (kind == TacticsPolicy.Kind.HEAL || kind == TacticsPolicy.Kind.DEFENSE)
				&& !TacticsPolicy.reactionReady(difficulty(s), time, s.hpObserved, hp < .35)) { continue; }
			if (!supportedTarget(skill, self) || skill.isPassive() || skill.isToggle() || skill.isSuicideAttack()
				|| skill.hasEffectType(EffectType.TELEPORT, EffectType.TELEPORT_TO_TARGET) && !daggerShadow(bot, enemy, preset, skill)) { continue; }
            if (skill.getId() == 821 && (!daggerShadow(bot, enemy, preset, skill) || bot.isMovementDisabled()
                || !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), enemy.getZ(), bot.getInstanceId()))) continue;
			final Failure f = s.failures.get(skill.getId());
			final boolean ready = !bot.isSkillDisabled(skill) && (f == null || time >= f.until) && (!bot.isInOlympiadMode() || !skill.isBlockedInOlympiad());
			final boolean inRange = self || inRange(bot, enemy, skill, distance);
			final boolean active = (self || !directDamage(skill)) && (skill.isDebuff() || kind != TacticsPolicy.Kind.DAMAGE && kind != TacticsPolicy.Kind.HEAL && kind != TacticsPolicy.Kind.CHARGE)
				&& active(self ? bot : enemy, skill);
			final boolean interrupt = !s.enemyCasting || interruptReady;
			final boolean resource = resources(bot, skill) && (self || !weaponUnavailable(bot) || !skill.isPhysical());
			final boolean conditions = resource && skill.checkCondition(bot, self ? bot : enemy, false);
			if (skill.getFlyType() == FlyType.CHARGE && (bot.isMovementDisabled() || !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), enemy.getZ(), bot.getInstanceId()))) { continue; }
            // No offensive candidate may target actual invulnerability, regardless of role.
            final boolean offBlocked = protectedSleep || enemy.isInvul();
			if (!self && TacticsPolicy.allowed(kind, ready, true, visible, active, offBlocked, conditions, interrupt)) { offenseAvailable = true; }
			if (!TacticsPolicy.allowed(kind, ready, inRange, visible, active, offBlocked, conditions, interrupt)) { continue; }
			// Actual Backstab/FatalBlow handlers own positional rules and the admin bot success roll.
			final boolean cardinalHealUrgent = preset.getClassId() == CARDINAL_CLASS_ID && skill.getId() == CARDINAL_HEAL_ID && hp < 0.95;
			final boolean recoveryHeal = preset.getClassId() == 90 && skill.getId() == 69 || preset.getClassId() == 131 && skill.getId() == 833;
			final double base = roleRanged && cpSupport ? 200 : chargeMelee && RoleCombatPolicy.builder(skill.getId()) ? 1 : recoveryHeal || cardinalHealUrgent ? 300 : TankClassMechanics.support(skill.getId()) ? 230 : timedPool ? 210 : cpSupport ? 145 : dominator && sleeping && sleepElapsed >= 5000 && direct ? 135 : kind == TacticsPolicy.Kind.DEFENSE
				? TacticsPolicy.defenseScore(hp, cpRatio(bot), visible && defenseThreat(bot, enemy))
				: kind == TacticsPolicy.Kind.HEAL && skill.hasEffectType(EffectType.CPHEAL) && !skill.hasEffectType(EffectType.HEAL)
					? TacticsPolicy.score(kind, new TacticsPolicy.Context(cpRatio(bot), context.enemyHp(), distance, sleeping, s.enemyCasting, interruptReady, context.enemyMoving(), window, context.needsCharges()))
					: TacticsPolicy.score(kind, context);
			final double prioritized = base > 0 && kind == TacticsPolicy.Kind.DEFENSE ? base + TacticsPolicy.defensePriority(skill.getId()) : base;
			final double score = TacticsPolicy.adapt(prioritized, kind, s.fleeingSamples >= 3, skill.getFlyType() == FlyType.CHARGE);
			if (score > 0)
			{
				options.add(new TacticsPolicy.Option(skill.getId(), kind, score));
				if (pressure && pressureControl(skill)) pressureControls.add(skill.getId());
				else if (pressure && pressureCurse(skill)) pressureCurses.add(skill.getId());
				if (tankStun) { tankStuns.add(skill.getId()); }
				else if (tankRoot) { tankRoots.add(skill.getId()); }
				else if (tank && !self && direct && skill.isPhysical()) { tankMelee.add(skill.getId()); }
			}
		}
		if(chargeMelee)RoleCombatPolicy.buildersLast(options);
        final int roleChoice=roleRanged?s.rangedCycle.choose(options,RoleCombatPolicy.archer(enemy),sleeping,RoleCombatPolicy.ownSeal(bot,enemy)!=null):-1;
        final ThreadLocalRandom random = ThreadLocalRandom.current();
		final int tankSelected = timedTank ? s.tankDefense.choose(options, time, activeStrong != 0, enemy.isMageClass()) : -1;
		final int tankChoice = tank ? TacticsPolicy.tankActionChoice(options, tankStuns, tankRoots, tankMelee, tankSelected,
			!enemy.isMovementDisabled() && !enemy.isOutOfControl()) : -1;
		final int cpProposed = dominator ? s.dominatorCp.choose(options, cpRatio(bot), controlHold) : -1;
		// CP heal may interrupt any phase; the owed offensive action follows this phase, not random damage.
		final int cpChoice = pressure && options.stream().anyMatch(o -> o.id() == cpProposed && !self(o.kind())) ? -1 : cpProposed;
		final int pressureChoice = pressure ? s.dominatorPressure.choose(options, pressureControls, pressureCurses, sleeping || enemy.isStunned()) : -1;
        final int tranceKiteChoice = cardinalKiting ? TacticsPolicy.cardinalTranceKiteChoice(options, hp) : -1;
		final int cardinalChoice = survival || s.clarityWindow.pending() ? s.clarityWindow.choose(options, hp)
            : tranceKiteChoice >= 0 ? tranceKiteChoice : s.cardinalProminence.choose(preset.getClassId(), options, time);
		final int shadowChoice = !defenseOnly && options.stream().anyMatch(o -> o.id() == 821) ? 821 : -1;
		final int tankMechanicChoice = TankClassMechanics.choose(options);
		// A legal charged Flame wins over both low-HP Angelic and Sacrifice; no waiting for unavailable charges.
		final int recoveryChoice = (tankMechanicChoice == 785 || tankMechanicChoice == 406) ? tankMechanicChoice : options.stream().filter(o -> preset.getClassId() == 90 && o.id() == 69 || preset.getClassId() == 131 && o.id() == 833).mapToInt(TacticsPolicy.Option::id).findFirst().orElse(-1);
		final int selected = roleRanged ? roleChoice : cancelPriority && options.stream().anyMatch(o -> o.id()==1056) ? 1056 : tankSleepPriority && options.stream().anyMatch(o -> o.id()==1097) ? 1097 : prominencePriority && options.stream().anyMatch(o -> o.id()==CARDINAL_PROMINENCE_ID) ? CARDINAL_PROMINENCE_ID : recoveryChoice >= 0 ? recoveryChoice : cardinalChoice >= 0 ? cardinalChoice : shadowChoice >= 0 ? shadowChoice : cpChoice >= 0 ? cpChoice : pressureChoice >= 0 ? pressureChoice : tankMechanicChoice >= 0 ? tankMechanicChoice : tankChoice >= 0 ? tankChoice : trainingRushActive
			? options.stream().anyMatch(option -> option.id() == 793) ? 793 : -1
			: trainingBerserker
			? s.trainingBurst.choose(options, distance, skills.containsKey(793), difficulty(s), random.nextDouble(), random.nextDouble())
			: TacticsPolicy.select(options, difficulty(s), random.nextDouble(), random.nextDouble());
		TacticsPolicy.Kind chosen = null;
		for (TacticsPolicy.Option o : options) { if (o.id() == selected) { chosen = o.kind(); break; } }
		final boolean urgent = cancelPriority && selected==1056 || tankSleepPriority && selected==1097 || selected==CARDINAL_PROMINENCE_ID && preset.getClassId()==CARDINAL_CLASS_ID || tankMechanicChoice >= 0 || shadowChoice >= 0 || cardinalChoice >= 0 || chosen == TacticsPolicy.Kind.HEAL || chosen == TacticsPolicy.Kind.DEFENSE
			|| (chosen == TacticsPolicy.Kind.CONTROL && interruptReady && s.enemyCasting);
        if (!rootedKite && !prominencePriority && !cardinalNoKite && !trancePriority && (survival || cardinalKiting) && chosen != TacticsPolicy.Kind.HEAL && chosen != TacticsPolicy.Kind.DEFENSE)
        {
            // A ready Trance interrupts the owned route; move only in the gaps until sleep lands.
            // A native-unavailable Trance may leave the old wake combo as fallback.
            if (survival && s.sleepRetreat.tick(bot, enemy, time, arenaCenter)) return true;
            if (!s.clarityWindow.pending() && invulnerableMelee(bot, enemy, s, time)) return true;
            final boolean wakeCombo = s.clarityWindow.pending() && (selected == 1398 || selected == 1230);
            boolean escape = !ownSleep && distance < 450 && selected != 1394 && !wakeCombo;
            if (s.survivalKite.tick(bot, enemy, time, escape)) return true;
        }
		// No recovery retreat/flank/kite when MP cannot pay any offensive skill.
		// Parents still enforce real range, LOS, sleep, weapon and arena authority.
		if (trainingRushActive && selected < 0) { stopBasicAttack(bot); return true; }
		if (!bluffHold && !controlHold && manaFallback && selected < 0 && !sleeping) { s.moveUntil = 0; return false; }
		if (!defenseOnly && !bluffHold && !controlHold && !pressure && !urgent && time < s.moveUntil && bot.isMoving() && Math.hypot(bot.getXdestination() - s.moveX, bot.getYdestination() - s.moveY) < 40) { return true; }
		final boolean offenseReady = options.stream().anyMatch(o -> o.kind() == TacticsPolicy.Kind.DAMAGE || o.kind() == TacticsPolicy.Kind.CONTROL || o.kind() == TacticsPolicy.Kind.SLOW);
		final boolean trainingPressure = TacticsPolicy.trainingBerserkerPressure(trainingBerserker && !trainingRushActive,
			allowsBasicAttack(bot, preset), bot.isMovementDisabled() || bot.isOutOfControl() || bot.isAllSkillsDisabled() || bot.isDisabled(),
			sleeping, enemy.isInvul(), visible, enemy.isAutoAttackable(bot), bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || s.pendingId != 0);
        if (!defenseOnly && !cardinalNoKite && !trancePriority && !bluffHold && !survival && (!sleeping || dominator || preset.getClassId() == CARDINAL_CLASS_ID) && !pressure && !ownSleep && !controlHold && !urgent && time >= s.nextStep && !trainingPressure && step(bot, enemy, preset, s, time, distance, visible, hp, offenseReady, offenseAvailable)) { return true; }
		if (selected >= 0)
		{
			final Skill skill = skills.get(selected);
			// Stop only the old escape route, after live-cast/pending/CC and native legality checks.
            if(tankSleepPriority && selected==1097 || cancelPriority && selected==1056)
            {
                s.invulnerableKite.stop(); s.survivalKite.stop(); s.moveUntil=0;
                if(bot.isMoving())bot.getAI().setIntentionIdle();
            }
            // Caller lends the phase to recovery, or this narrowly admitted tank-control cast.
			if (defenseOnly) { AutoUseTaskManager.getInstance().stopAutoUseTask(bot); AutoPlayTaskManager.getInstance().stopAutoPlay(bot); }
			final boolean self = self(chosen);
			stopBasicAttack(bot); // end auto loop before requesting an ordinary cast
			bot.setTarget(self ? bot : enemy);
			s.pendingMp = bot.getCurrentMp(); s.pendingHp = bot.getCurrentHp(); s.pendingCharges = bot.getCharges();
			s.pendingX = bot.getX(); s.pendingY = bot.getY();
			// Native initial MP is paid at beginCast; regular MP only at onMagicHitTimer.
			s.pendingCompletionMp = Math.max(0, bot.getStat().getMpInitialConsume(skill)) + Math.max(0, bot.getStat().getMpConsume(skill));
			if (pressure) s.dominatorPressure.requested(selected, pressureControl(skill), pressureCurse(skill));
			s.pendingRushLoop = trainingRushActive;
			clearEvidence(s);
			if ((pressure || roleRanged) && directDamage(skill)) s.pendingEvidence = new CastEvidence(bot, enemy, selected);
			final boolean accepted = bot.useMagic(skill, true, true); // shiftPressed: NEVER walk into range.
			s.nextDecision = time + difficulty(s).reactionMs;
			if (accepted)
            {
                if (preset.getClassId() == CARDINAL_CLASS_ID) s.clarityWindow.requested(selected);
				if (trainingRushActive && selected == 793) { s.rushLoop.loopRequestAccepted(); }
				if (preset.getClassId() == CARDINAL_CLASS_ID) {
					if (selected == CARDINAL_PROMINENCE_ID) { s.cardinalProminence.accepted(time); }
				}
				CombatAnalytics.tacticalRequest(bot.getObjectId(), chosen.name());
                if (chosen == TacticsPolicy.Kind.DAMAGE || chosen == TacticsPolicy.Kind.CONTROL || chosen == TacticsPolicy.Kind.SLOW)
                    s.offensiveActionsSinceStep = Math.min(4,s.offensiveActionsSinceStep+1);
				s.pendingId = selected; s.pendingAt = time; s.pendingSelf = self; s.pendingDebuff = skill.isDebuff() && !directDamage(skill); s.sawCast = bot.isCastingNow();
			}
			else { clearEvidence(s); failure(s, selected, time); bot.setTarget(enemy); }
			// Self targeting remains until cast resolves; next offensive action restores enemy.
			return true;
		}
		if (trancePriority)
        {
            // Reuse/mute/CC/MP/LOS failures never fall back to kite or damage.
            // Only approach a distant visible target, within the arena boundary.
            Skill trance = skills.get(1394);
            if (visible && !inRange(bot, enemy, trance, distance) && !bot.isMovementDisabled()
                && time >= s.nextStep)
            {
                int[] c = arenaCenter;
                double dx = enemy.getX()-bot.getX(), dy = enemy.getY()-bot.getY();
                double length = Math.max(1, Math.hypot(dx, dy)), step = Math.min(120, length);
                int x = bot.getX()+(int)Math.round(dx*step/length), y = bot.getY()+(int)Math.round(dy*step/length);
                if (c != null && Math.hypot(x-c[0], y-c[1]) <= 449
                    && GeoEngine.getInstance().canMoveToTarget(bot.getX(),bot.getY(),bot.getZ(),x,y,bot.getZ(),bot.getInstanceId()))
                    bot.getAI().setIntentionMoveTo(new Location(x,y,bot.getZ()));
                s.nextStep = time+400;
            }
            return true;
        }
        if (!defenseOnly && cardinalNoKite) { stopBasicAttack(bot); return true; } // no parent chase/kite under either shield
        if (defenseOnly) return false; // no legal rescue: immediately continue the existing route
		if (survival) { stopBasicAttack(bot); return true; } // never yield forbidden offense/chase
        if (bluffHold) { stopBasicAttack(bot); return true; } // no parent chase/autos during actual own Bluff
		if (controlHold) return true; // no parent autoattack/chase/Hammer Crush under own melee control
		if (ownSleep) return true; // no parent chase back into the sleeping target after retreat
		if (pressure && pressureClosing && !bot.isMovementDisabled() && !bot.isOutOfControl()) { s.moveUntil = 0; return false; }
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
		// Actual invulnerability never delegates offense to a parent/legacy engine.
        if (!defenseOnly && enemy.isInvul()) { stopBasicAttack(bot); return true; }
		// Ordinary unavailable offense still holds; MP-starved physical bots yielded above.
		if (!defenseOnly && chargeClosing && !bot.isMovementDisabled() && !bot.isOutOfControl()) return false;
		if (!defenseOnly && !offenseAvailable) { stopBasicAttack(bot); return true; }
		return false;
	}

	private static boolean act(Player bot, Player enemy, BotClassPreset preset, State s, long time, boolean defenseOnly)
	{
        boolean handled=act(bot,enemy,preset,s,time,defenseOnly,false);
        if(!defenseOnly&&rootedByTank(bot,enemy))
        {
            // Only the ordinary rotation's explicit auto fallback may swing, and only
            // already in physical range. Never delegate a rooted bot to a chase driver.
            if(!handled)stationaryAttack(bot,enemy,preset,s);
            return true;
        }
        return handled;
	}
    private static void stationaryAttack(Player bot,Player enemy,BotClassPreset preset,State s)
    {
        if(!allowsBasicAttack(bot,preset)||bot.isOutOfControl()||bot.isAttackDisabled()||bot.isAlikeDead()
            ||bot.isCastingNow()||bot.isCastingSimultaneouslyNow()||s.pendingId!=0
            ||enemy.isSleeping()||enemy.isInvul()||!enemy.isAutoAttackable(bot)
            ||!GeoEngine.getInstance().canSeeTarget(bot,enemy)
            ||bot.calculateDistance3D(enemy)>bot.getPhysicalAttackRange()+bot.getTemplate().getCollisionRadius()+enemy.getTemplate().getCollisionRadius())
        {stopBasicAttack(bot);return;}
        bot.setTarget(enemy);
        if(bot.getAI().getIntention()!=Intention.ATTACK)bot.getAI().setIntentionAttack(enemy);
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
            if(RoleCombatPolicy.rangedDominator(bot,enemy))
            {
                boolean completed=hasOwnEffect(s.pendingSelf?bot:enemy,bot,skill)||s.pendingEvidence!=null&&s.pendingEvidence.hit
                    ||s.pendingCompletionMp>0&&bot.getCurrentMp()<=s.pendingMp-s.pendingCompletionMp;
                s.rangedCycle.completed(s.pendingId,completed);
            }
            if (dominatorRangedPressure(bot, enemy, BotPresets.getPresetByClassId(bot.getPlayerClass().getId())))
			{
				// Starting a cast, initial MP and reuse alone do NOT prove a completed opener.
				// Require an actual own effect or the full native initial+hit MP debit.
				final boolean ownEffect = hasOwnEffect(s.pendingSelf ? bot : enemy, bot, skill);
				final boolean completed = ownEffect || s.pendingEvidence != null && s.pendingEvidence.hit || s.pendingCompletionMp > 0 && bot.getCurrentMp() <= s.pendingMp - s.pendingCompletionMp;
				s.dominatorPressure.resolved(s.pendingId, completed, ownEffect, pressureControl(skill), pressureCurse(skill), directDamage(skill));
			}
			if(TacticsPolicy.chargeMeleeClass(bot.getPlayerClass().getId())&&!s.pendingSelf&&directDamage(skill)
                &&bot.calculateDistance2D(enemy)>bot.getPhysicalAttackRange()+bot.getTemplate().getCollisionRadius()+enemy.getTemplate().getCollisionRadius())
                s.chargePursuitUntil=time+1200; // genuine pursuit window AFTER the ranged cast, not during it
            final TacticsPolicy.Kind resolvedKind = kind(skill);
			s.dominatorCp.resolved(s.pendingId, paid, resolvedKind == TacticsPolicy.Kind.DAMAGE || resolvedKind == TacticsPolicy.Kind.CONTROL || resolvedKind == TacticsPolicy.Kind.SLOW);
		}
		clearEvidence(s);
		s.pendingId = 0;
		bot.setTarget(enemy);
	}
	private static boolean hasOwnEffect(Player target, Player caster, Skill skill)
	{
		for (BuffInfo effect : target.getEffectList().getEffects())
			if (effect != null && !effect.isRemoved() && effect.getEffector() == caster && effect.getSkill().getId() == skill.getId() && effect.isInUse()) return true;
		return false;
	}
	private static boolean hasAppliedStun(BuffInfo effect)
	{
		return effect != null && effect.getEffects() != null && effect.getEffects().stream().anyMatch(applied ->
			applied.getEffectType() == EffectType.STUN && (applied.getEffectFlags() & EffectFlag.STUNNED.getMask()) != 0);
	}
	private static BuffInfo ownRushStun(Player bot, Player enemy)
	{
		return RushStunSupport.stun(enemy);
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
		if (!s.rushLoop.follow(stun, stun != null)) { stopRushMovement(bot, s); return -1; }
		if (!java.util.Arrays.stream(preset.getRotation()).anyMatch(id -> id == 793) || bot.getKnownSkill(793) == null
			|| !visible || distance > 600 || Math.abs(bot.getZ() - enemy.getZ()) > 100)
		{ s.rushLoop.retryRetreat(time); stopRushMovement(bot, s); stopBasicAttack(bot); return -2; }
		if (!s.rushLoop.retreatAllowed(time, distance)) { stopRushMovement(bot, s); stopBasicAttack(bot); return -2; }
		if (distance >= 220) { stopRushMovement(bot, s); return 793; } // all real skill gates still run in act
		stopBasicAttack(bot);
		if (bot.isMovementDisabled() || time < s.nextRushStep) { return -2; }
		s.nextRushStep = time + 250;
		final int[] center = ArenaCombatRules.center(bot);
		if (center == null) return -2;
		final double radius = ArenaCombatRules.retreatRadius(bot, enemy, 1000);
		for (Location point : RushStunSupport.retreat(bot.getX(), bot.getY(), bot.getZ(), enemy.getX(), enemy.getY(), center[0], center[1], radius))
		{
			final int x = point.getX(), y = point.getY();
			if (!TrainingArena.inside(x, y) || !GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), x, y, bot.getZ(), bot.getInstanceId())) continue;
			bot.getAI().setIntentionMoveTo(point);
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
		final double range = bot.getPlayerClass().getId() == CARDINAL_CLASS_ID && skill.getId() == CARDINAL_MANA_BURN_ID
			? bot.getMagicalAttackRange(skill) : TacticsPolicy.effectiveRange(aura, skill.getCastRange(), skill.getAffectRange(),
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
	static boolean directDamage(Skill skill)
	{
		return skill.isDamage() || effectNamed(skill, "Backstab", "FatalBlow", "EnergyDamage", "PhysicalSoulDamage", "CpDamPercent", "DeathLink");
	}
	private static TacticsPolicy.Kind kind(Skill skill)
	{
		// Targeted cleansing is emergency self-support only, never an offensive dispel candidate.
		if (skill.getId() == SOUL_CLEANSE_ID) return null;
		if (skill.getId() == ESCAPE_SHACKLE_ID) return null;
        if (skill.getId() == 821) return TacticsPolicy.Kind.CONTROL;
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
		if (dominatorRangedPressure(bot, enemy, preset)) return false; // keep closing; never recovery-orbit away
		if (!visible || bot.isMovementDisabled() || Math.abs(bot.getZ() - enemy.getZ()) > 100 || distance < 1) { return false; }
		if (time < s.nextStep) return false;
		final boolean armedDagger = preset.getWeaponKind() == BotClassPreset.WeaponKind.DAGGER && !weaponUnavailable(bot);
		double x, y;
		final boolean recovery = TacticsPolicy.daggerStepAllowed(preset.getWeaponKind() == BotClassPreset.WeaponKind.DAGGER, weaponUnavailable(bot))
			&& TacticsPolicy.chargeMeleeRecoveryAllowed(strictMeleePressure(bot, enemy, preset), weaponUnavailable(bot))
			&& TacticsPolicy.tankRecoveryAllowed(TacticsPolicy.tankClass(preset.getClassId()), weaponUnavailable(bot))
			&& TacticsPolicy.titanRecoveryAllowed(preset.getClassId(), weaponUnavailable(bot))
			&& TacticsPolicy.recoveryStepAllowed(weaponUnavailable(bot), offenseAvailable, offenseReady,
			distance, TacticsPolicy.recoveryEnemyControlled(preset.getClassId() == 115, enemy.isSleeping(), enemy.isMovementDisabled(), enemy.isOutOfControl()));
		if (armedDagger)
		{
			if (distance >= 170 || TacticsPolicy.behind(enemy.getX(), enemy.getY(), enemy.getHeading(), bot.getX(), bot.getY())) return false;
			// Restore ordinary rear/side approach, even with ready blows. Never restore recovery retreat.
			// Charge the attempt before geodata/AI: a blocked path must not retry every tick.
			s.nextStep = time + 2000;
			final double angle = enemy.getHeading() * (2 * Math.PI / 65536.0);
			x = enemy.getX() - 85 * Math.cos(angle) - 70 * Math.sin(angle);
			y = enemy.getY() - 85 * Math.sin(angle) + 70 * Math.cos(angle);
		}
		else if (recovery)
		{
			final Location orbit = ArenaCombatRules.kitePoint(bot, enemy, 120);
			if (orbit == null) return false;
			x = orbit.getX(); y = orbit.getY();
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
		if (!GeoEngine.getInstance().canMoveToTarget(bot.getX(), bot.getY(), bot.getZ(), px, py, bot.getZ(), bot.getInstanceId())) { if (!armedDagger) s.nextStep = time + 1500; return false; }
		stopBasicAttack(bot);
		bot.getAI().setIntentionMoveTo(new Location(px, py, bot.getZ()));
		if (!armedDagger) s.nextStep = time + (recovery || weaponUnavailable(bot) ? 600 : preset.getClassId() == 95 ? 800 : 4500);
		if (bot.getAI().getIntention() != Intention.MOVE_TO || !bot.isMoving()
			|| Math.hypot(bot.getXdestination() - px, bot.getYdestination() - py) >= 40) { return false; }
		s.moveX = px; s.moveY = py; s.moveUntil = time + (recovery ? 450 : 200);
        s.offensiveActionsSinceStep = 0;
		return true;
	}
}
