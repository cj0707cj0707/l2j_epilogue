/* Pure, engine-independent tactical policy. No stat or resource adjustments. */
package custom.fakeplayersai;

import java.util.List;

final class TacticsPolicy
{
	enum Difficulty
	{
		NORMAL(850, .24, 45), EXPERIENCED(500, .10, 20), STRONG(250, .035, 8);
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
		return now - observedAt >= (urgent ? Math.max(250, d.reactionMs / 2) : d.reactionMs);
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
