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
		return switch (id) { case 110,760 -> 20; case 368 -> 12; case 916 -> 8; default -> 0; };
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
