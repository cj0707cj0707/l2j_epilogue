/* Pure, engine-independent tactical policy. No stat or resource adjustments. */
package custom.fakeplayersai;

import java.util.List;

final class TacticsPolicy
{
	enum Difficulty
	{
		NORMAL(850, .24, 45), EXPERIENCED(500, .10, 20), STRONG(100, .035, 8);
		final long reactionMs;
		final double mistakeRate;
		final double noise;
		Difficulty(long reaction, double mistake, double variation)
		{
			reactionMs = reaction; mistakeRate = mistake; noise = variation;
		}
		static Difficulty parse(String value)
		{
			if (value == null) { return EXPERIENCED; }
			for (Difficulty d : values()) { if (d.name().equalsIgnoreCase(value.trim())) { return d; } }
			return EXPERIENCED;
		}
	}
	enum Kind { DAMAGE, CONTROL, SLOW, HEAL, DEFENSE, BUFF, CHARGE }
	record Context(double hp, double enemyHp, double distance, boolean sleeping, boolean enemyCasting,
		boolean interruptReady, boolean enemyMoving, boolean damageWindow, boolean needsCharges) { }
	record Option(int id, Kind kind, double score) { }

	private TacticsPolicy() { }

	static boolean reactionReady(Difficulty d, long now, long observedAt, boolean urgent)
	{
		return now - observedAt >= (urgent ? Math.max(100, d.reactionMs / 2) : d.reactionMs);
	}

	/** All difficulties obey the exact same legality/sleep/resource gates. */
	static boolean allowed(Kind kind, boolean ready, boolean inRange, boolean visible, boolean active,
		boolean sleeping, boolean resource, boolean canInterrupt)
	{
		final boolean offensive = kind == Kind.DAMAGE || kind == Kind.CONTROL || kind == Kind.SLOW;
		return ready && inRange && resource && !active && (!offensive || (visible && !sleeping))
			&& (kind != Kind.CONTROL || canInterrupt);
	}

	static double score(Kind kind, Context c)
	{
		switch (kind)
		{
			case HEAL: return c.hp < .35 ? 160 : c.hp < .65 ? 100 : -1;
			case DEFENSE: return c.hp < .35 ? 150 : c.hp < .65 ? 85 : -1;
			case CONTROL: return c.enemyCasting && c.interruptReady ? 135 : c.distance < 450 ? 54 : 30;
			case SLOW: return c.enemyMoving && c.distance > 100 ? 92 : 25;
			case CHARGE: return c.needsCharges ? 94 : -1;
			case BUFF: return c.damageWindow ? -1 : c.distance < 450 ? 72 : 40;
			case DAMAGE: return 62 + (c.damageWindow ? 40 : 0) + (c.enemyHp < .25 ? 18 : 0);
			default: return -1;
		}
	}

	/** Only Cardinal's requested MP drain and borrowed nuke may break protected sleep. */
	static boolean cardinalSleepException(int classId, int skillId)
	{
		return classId == 97 && (skillId == 1398 || skillId == 1230);
	}

	/** A ready short-range Trance must not be starved by the900-range MP drain/nuke. */
	static double cardinalApproachRange(int classId, double normalRange, double legalTranceRange)
	{
		return classId == 97 && legalTranceRange > 0 ? Math.min(normalRange, legalTranceRange) : normalRange;
	}

	/** Ranked Cardinal encounters follow training: no automatic full-MP floor on either bot. */
	static boolean arenaMpFloorAllowed(boolean cardinalBot, boolean cardinalOpponentBot)
	{
		return !cardinalBot && !cardinalOpponentBot;
	}

	/** A due nuke wins over routine choices/kiting, never over a legal urgent recovery/interrupt.
	 * The caller supplies only candidates that passed range, LOS, resources and core conditions. */
	static final class CardinalProminence
	{
		private long nextAt;
		void reset() { nextAt = 0; }
		boolean ready(long time) { return time >= nextAt; }
		void accepted(long time) { nextAt = time + 10000; }
		int choose(int classId, List<Option> legal, long time)
		{
			if (classId != 97 || !ready(time)) return -1;
			boolean prominence = false;
			for (Option option : legal)
			{
				if (option.score() >= 130 && (option.kind() == Kind.HEAL || option.kind() == Kind.DEFENSE || option.kind() == Kind.CONTROL)) return -1;
				if (option.id() == 1230 && option.kind() == Kind.DAMAGE) prominence = true;
			}
			return prominence ? 1230 : -1;
		}
	}

	/** Bounded weighted mistakes: never choose an illegal option; emergencies dominate. */
	static int select(List<Option> options, Difficulty d, double mistakeRoll, double choiceRoll)
	{
		if (options.isEmpty()) { return -1; }
		double best = -Double.MAX_VALUE;
		for (Option o : options) { best = Math.max(best, o.score); }
		final boolean mistake = mistakeRoll < d.mistakeRate && best < 130;
		double total = 0;
		for (Option o : options) { total += weight(o.score, best, d.noise, mistake); }
		double cursor = Math.min(.999999, Math.max(0, choiceRoll)) * total;
		for (Option o : options)
		{
			final double weight = weight(o.score, best, d.noise, mistake);
			if (weight <= 0) { continue; }
			cursor -= weight;
			if (cursor <= 0) { return o.id; }
		}
		return options.get(options.size() - 1).id;
	}
	private static double weight(double score, double best, double noise, boolean mistake)
	{
		if (best >= 130 && score < best - 15) { return 0; }
		return mistake ? 1 : Math.exp(Math.max(-30, (score - best) / noise));
	}
	static double adapt(double score, Kind kind, boolean fleeingHabit, boolean movementCharge)
	{
		if (score <= 0 || !fleeingHabit) return score;
		if (kind == Kind.SLOW || movementCharge) return score + 20;
		if (kind == Kind.BUFF) return Math.max(1, score - 20);
		return score;
	}

    /** Attack-first ranged policy. A healthy ranged bot with a ready offense
     * does not sacrifice it for perpetual micro-kiting. No fleeing from casters/bows. */
    static boolean rangedStepAllowed(boolean caster, boolean meleeThreat, double distance, double hp,
        boolean enemyControlled, boolean offenseReady, int actionsSinceStep)
    {
        if (!meleeThreat || enemyControlled || distance >= (caster ? 145 : 110)) return false;
        if (hp < .35 && distance < 100) return true;
        if (offenseReady && (hp >= .65 || actionsSinceStep < 2)) return false;
        return true;
    }
    static boolean attemptResolved(boolean sawCast, boolean paid, long elapsed)
    {
        // A real completed cast need not wait out an arbitrary 750ms lock.
        // Unstarted requests get a short bounded grace, never a skill-speed hack.
        return sawCast || paid || elapsed >= 350;
    }

	static long observeSleep(long observed, boolean sleeping, long time)
	{
		return !sleeping ? -1 : observed < 0 ? time : observed;
	}

	static double defensePriority(int id)
	{
		return switch (id) { case 368 -> 24; case 110,760 -> 20; case 916 -> 8; default -> 0; };
	}

	/** Only Dominator gets a bounded curse phase, then damage to break sleep. */
	static boolean sleepOffenseAllowed(boolean dominator, boolean sleeping, long elapsed, boolean directDamage, boolean sleepSkill)
	{
		if (!sleeping) { return true; }
		if (!dominator || sleepSkill) { return false; }
		return elapsed < 5000 ? !directDamage : directDamage;
	}

	static boolean basicAttackAllowed(boolean dagger, boolean caster, boolean disarmed)
	{
		return !dagger && !caster && !disarmed;
	}
	static double effectiveRange(boolean aura, int castRange, int affectRange, int physicalRange, double collision)
	{
		if (aura && affectRange <= 0) { return 0; }
		return (aura ? affectRange : castRange > 0 ? castRange : Math.max(0, physicalRange)) + Math.max(0, collision);
	}
	static int boundedApproachRange(double reach)
	{
		return (int)Math.max(20,Math.min(1000,Math.floor(reach)));
	}
	static boolean tankClass(int classId) { return classId == 90 || classId == 91 || classId == 106; }
	/** Actual tank Shield Stun/Bash cast40, not their effect400 or drain600.
	 * Movement envelope deliberately does not expand when all close skills cool down. */
	static int tankApproachRange(double physicalRange, double collision)
	{
		return boundedApproachRange(Math.min(40, Math.max(0, physicalRange)) + Math.max(0, collision));
	}
	static boolean tankClosingSkillAllowed(boolean closing, boolean self, Kind kind, boolean catchControl)
	{
		return !closing || (self ? kind == Kind.HEAL || kind == Kind.DEFENSE : catchControl);
	}
	static boolean tankSupportAllowed(boolean closing, boolean immobilizing)
	{
		return !closing || !immobilizing;
	}
	static boolean tankRecoveryAllowed(boolean tank, boolean disarmed) { return !tank || disarmed; }
	/** Pursuit yield ONLY, never permission to cast or attack without LoS.
	 * Chase may need a geodata route even when a near target is behind a pillar. */
	static boolean tankPursuit(boolean tank, boolean armed, boolean blocked, boolean sleeping,
		boolean invulnerable, boolean attackable, boolean pendingCast)
	{
		return tank && armed && !blocked && !sleeping && !invulnerable && attackable && !pendingCast;
	}

	/** These subsets contain ONLY already-legal effective options, never learned-skill injection. */
	static int tankOffenseChoice(List<Option> legal, List<Integer> stuns, List<Integer> roots, List<Integer> melee)
	{
		for (Option option : legal) if (option.kind == Kind.HEAL && option.score >= 130) { return option.id; }
		for (List<Integer> priority : List.of(stuns, roots, melee))
			for (Option option : legal) if (priority.contains(option.id)) { return option.id; }
		return -1;
	}
	static int tankActionChoice(List<Option> legal, List<Integer> stuns, List<Integer> roots, List<Integer> melee, int defense, boolean freeFoe)
	{
		final int offense = tankOffenseChoice(legal, stuns, roots, melee);
		for (Option option : legal) if (option.id == offense && option.kind == Kind.HEAL && option.score >= 130) { return offense; }
		if (freeFoe && (stuns.contains(offense) || roots.contains(offense))) { return offense; }
		return defense >= 0 ? defense : offense;
	}
	static boolean selfOnlyTargets(Object caster, List<?> targets)
	{
		if (caster == null || targets == null || targets.isEmpty()) { return false; }
		for (Object target : targets) if (target != caster) { return false; }
		return true;
	}
	static boolean dominatorCpSkill(int id) { return id == 1305 || id == 1306; }
	static boolean dominatorCpNeeded(double ratio) { return Double.isFinite(ratio) && ratio >= 0 && ratio < .50; }
	/** One real CP cast may be woven between available attacks, not a permanent heal loop. */
	static final class DominatorCpWeave
	{
		private boolean owesAttack;
		void reset() { owesAttack = false; }
		void resolved(int id, boolean executed, boolean offensive)
		{
			if (!executed) { return; }
			if (dominatorCpSkill(id)) { owesAttack = true; }
			else if (offensive) { owesAttack = false; }
		}
		int choose(List<Option> legal, double cp)
		{
			// Never displace an ordinary life-saving HP heal.
			for (Option option : legal)
				if (option.kind == Kind.HEAL && !dominatorCpSkill(option.id) && option.score >= 150) { return option.id; }
			if (owesAttack)
			{
				for (Kind priority : new Kind[] { Kind.DAMAGE, Kind.CONTROL, Kind.SLOW })
					for (Option option : legal) if (option.kind == priority) { return option.id; }
			}
			if (!dominatorCpNeeded(cp)) { return -1; }
			// Honor is the shorter real cast; Ritual remains a genuine legal fallback.
			for (int id : new int[] { 1305, 1306 })
				for (Option option : legal) if (option.id == id && option.kind == Kind.HEAL) { return id; }
			return -1;
		}
	}
	static boolean titanRecoveryAllowed(int classId, boolean disarmed) { return classId != 113 || disarmed; }
	static boolean titanPursuit(int classId, boolean armed, boolean blocked, boolean sleeping,
		boolean invulnerable, boolean attackable, boolean pendingCast)
	{
		return classId == 113 && armed && !blocked && !sleeping && !invulnerable && attackable && !pendingCast;
	}
	static boolean recoveryStepAllowed(boolean disarmed, boolean offenseAvailable, boolean offenseReady, double distance, boolean enemyControlled)
	{
		return !enemyControlled && distance > 1 && distance < 350 && (disarmed || !offenseAvailable && !offenseReady);
	}
	static boolean recoveryEnemyControlled(boolean dominator, boolean sleeping, boolean immobile, boolean outOfControl)
	{
		// Dominator may reposition during its bounded sleep phase if no curse/damage
		// is usable; other classes preserve the existing sleep-protection hold.
		return dominator && sleeping ? false : sleeping || immobile || outOfControl;
	}
	static double defenseScore(double hp, double cp, boolean threatened)
	{
		if (hp < .35) { return 155; }
		if (threatened && (hp < .65 || cp < .35)) { return 140; }
		return hp < .65 ? 85 : -1;
	}
	static boolean defenseSkill(int id)
	{
		return switch (id)
		{
			case 110, 368, 350, 439, 527, 112, 82, 86, 760, 913, 351, 72, 916,
				111, 139, 447, 761, 788, 499 -> true;
			default -> false;
		};
	}


	/** Training owns no ranked phase engines. Only already-legal options enter
	 * this small ordered burst; real cooldowns/conditions remain authoritative. */
	static final class TrainingBerserker
	{
		private boolean rushed, violentUsed;

		void reset() { rushed = false; violentUsed = false; }
		boolean allowsSkill(int id, double distance, boolean primaryRush)
		{
			return switch (id)
			{
				case 494, 484, 493, 495 -> !primaryRush; // competing min-distance charge chains
				case 793 -> distance >= 200;
				case 485 -> distance <= 150; // ALSO requires actual 40+collision cast range
				case 501 -> rushed && !violentUsed;
				default -> true; // preserve every other effective override, never inject skills
			};
		}
		void resolved(int id, boolean executed, double rushTravel, double distance)
		{
			if (!executed) { return; }
			if (id == 793 && rushTravel >= 100 && distance < 600) { rushed = true; }
			// A real resisted cast still consumes the once-per-battle attempt.
			if (id == 501 && rushed) { violentUsed = true; }
		}
		int choose(List<Option> legal, double distance, boolean primaryRush, Difficulty difficulty, double mistake, double choice)
		{
			final List<Option> eligible = new java.util.ArrayList<>();
			Option emergency = null;
			for (Option option : legal)
			{
				if (!allowsSkill(option.id, distance, primaryRush)) { continue; }
				eligible.add(option);
				if ((option.kind == Kind.HEAL || option.kind == Kind.DEFENSE) && option.score >= 130
					&& (emergency == null || option.score > emergency.score)) { emergency = option; }
			}
			if (emergency != null) { return emergency.id; }
			for (int id : new int[] {793, 485, 526, 497, 501, 498, 477, 1435})
				for (Option option : eligible) if (option.id == id) { return id; }
			// Non-default explicit overrides remain available through the usual policy.
			return select(eligible, difficulty, mistake, choice);
		}
	}
	static boolean trainingBerserkerPressure(boolean authorized, boolean armed, boolean blocked,
		boolean sleeping, boolean invulnerable, boolean visible, boolean attackable, boolean pendingCast)
	{
		return authorized && armed && !blocked && !sleeping && !invulnerable && visible && attackable && !pendingCast;
	}


	static boolean meleeOpponent(boolean knownPreset, boolean casterPreset, boolean rangedWeapon, boolean mageClass, double attackRange)
	{
		return knownPreset ? !casterPreset && !rangedWeapon : !mageClass && attackRange < 250;
	}
	/** First actual combat tick, then real live-effect observations, not durations. */
	static final class TankDefenseSequence
	{
		private long combatAt = -1;
		private int lastStrong;
		void reset(long time) { combatAt = time; lastStrong = 0; }
		boolean due(long time) { return combatAt >= 0 && time - combatAt >= 5000; }
		void observed(int id) { if (id == 110 || id == 368 || id == 760) { lastStrong = id; } }
		int choose(List<Option> legal, long time, boolean strongActive)
		{
			if (!due(time) || strongActive) { return -1; }
			int first = lastStrong == 368 ? 110 : 368;
			int second = first == 110 ? 368 : 110;
			for (int id : new int[] { first, second, 350, 439, 527, 112, 82, 86 })
				for (Option option : legal) if (option.id == id) { return id; }
			return -1;
		}
	}
	/** Weak exact identity: never retain actors or adopt a replacement/refresh. */
	static final class OriginalRushStun<T>
	{
		private java.lang.ref.WeakReference<T> original;
		private long retreatAt = -1, retryAt;
		private boolean awaitingLanding;
		void clear() { original = null; retreatAt = -1; retryAt = 0; awaitingLanding = false; }
		void begin(T effect) { if (effect != null) { original = new java.lang.ref.WeakReference<>(effect); retreatAt = -1; retryAt = 0; awaitingLanding = false; } }
		void loopRequestAccepted()
		{
			if (original != null && original.get() != null) { awaitingLanding = true; retreatAt = -1; }
		}
		boolean resolvedLoopLanding(T current, boolean liveOwnRush, boolean executed, double travel, double distance)
		{
			if (!active(current, liveOwnRush)) { return false; }
			final boolean requested = awaitingLanding;
			awaitingLanding = false;
			if (!requested || !executed || travel < 100 || distance >= 600) { return false; }
			// Rearm movement ONLY. Keep the exact original weak effect and its lifetime.
			retreatAt = -1; retryAt = 0;
			return true;
		}
		void retryRetreat(long time) { retreatAt = -1; retryAt = time + 750; }
		boolean active(T current, boolean liveOwnRush)
		{
			if (current != null && original != null && original.get() == current && liveOwnRush) { return true; }
			clear(); return false;
		}
		boolean retreatAllowed(long time, double distance)
		{
			if (awaitingLanding) { return true; } // actual casting/landing is not retreat time
			if (distance >= 220) { retreatAt = -1; retryAt = 0; return true; }
			if (time < retryAt) { return false; }
			if (retreatAt < 0) { retreatAt = time; }
			if (time - retreatAt >= 2000) { retryRetreat(time); return false; }
			return true;
		}
		static double stepLength(double distance) { return Math.min(160, Math.max(0, 260 - distance)); }
		static double[] retreatPoint(double x, double y, double enemyX, double enemyY, int attempt)
		{
			return retreatPoint(x, y, enemyX, enemyY, attempt, 1, 0);
		}
		static double[] retreatPoint(double x, double y, double enemyX, double enemyY, int attempt, double previousX, double previousY)
		{
			final double distance = Math.hypot(x - enemyX, y - enemyY);
			if (attempt < 0 || attempt >= 8) { return null; }
			final double angle = new double[] {0, Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2,
				3 * Math.PI / 4, -3 * Math.PI / 4, Math.PI}[attempt];
			// Core CHARGE places the actor at EXACT target coordinates. Reuse the
			// genuine precharge direction for an ordinary walk; never write heading/XYZ.
			double dx = distance < 1 ? previousX : x - enemyX, dy = distance < 1 ? previousY : y - enemyY;
			double norm = Math.hypot(dx, dy);
			if (norm < 1) { dx = 1; dy = 0; norm = 1; }
			dx /= norm; dy /= norm;
			final double length = stepLength(distance);
			return new double[] { x + length * (dx * Math.cos(angle) - dy * Math.sin(angle)),
				y + length * (dx * Math.sin(angle) + dy * Math.cos(angle)) };
		}
	}

	static long failureBackoff(int failures)
	{
		return Math.min(8000, 500L << Math.min(4, Math.max(0, failures - 1)));
	}
	static boolean behind(double ex, double ey, int heading, double bx, double by)
	{
		final double angle = heading * (2 * Math.PI / 65536.0);
		final double dx = bx - ex, dy = by - ey;
		return Math.hypot(dx, dy) > 1 && (dx * Math.cos(angle) + dy * Math.sin(angle)) / Math.hypot(dx, dy) < -.5;
	}
}
