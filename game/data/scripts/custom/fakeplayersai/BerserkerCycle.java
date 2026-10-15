/* Pure timing/confirmation policy for the Doombringer olympiad driver. */
package custom.fakeplayersai;

final class BerserkerCycle
{
	enum RushStatus { NONE, WAIT, LANDED, FAILED, TIMED_OUT }
	static final long RUSH_TIMEOUT_MS = 6000;
	static final long RETREAT_MAX_MS = 1500;
	static final long RETREAT_PROGRESS_MS = 750;
	static final long RETREAT_COOLDOWN_MS = 5000;
	private long requestedAt = -1;
	private double rushX;
	private double rushY;
	private long nextRushAt;
	private long retreatAt = -1;
	private double retreatDistance;
	private long nextRetreatAt;

	boolean rushPending() { return requestedAt >= 0; }
	boolean canRush(long now) { return !rushPending() && now >= nextRushAt; }

	void requested(long now, double x, double y)
	{
		requestedAt = now;
		rushX = x;
		rushY = y;
		retreatAt = -1;
	}

	RushStatus pollRush(long now, boolean casting, double distance, double x, double y)
	{
		if (!rushPending())
		{
			return RushStatus.NONE;
		}
		if (casting && (now - requestedAt < RUSH_TIMEOUT_MS))
		{
			return RushStatus.WAIT;
		}
		final boolean timeout = casting;
		final boolean landed = !casting && ((Math.hypot(x - rushX, y - rushY) >= 100) && distance < 600);
		requestedAt = -1;
		if (!landed)
		{
			nextRushAt = now + 1500;
		}
		return landed ? RushStatus.LANDED : timeout ? RushStatus.TIMED_OUT : RushStatus.FAILED;
	}

    static boolean shouldPressFinish(double hp, double cp, double maxHp, double maxCp, double distance)
    {
        return distance < 220 && maxHp > 0 && (hp + Math.max(0,cp)) / (maxHp + Math.max(0,maxCp)) <= .25;
    }

	boolean canRetreat(long now, double distance)
	{
		return now >= nextRetreatAt && distance > 100 && distance < 220;
	}

	void beginRetreat(long now, double distance)
	{
		retreatAt = now;
		retreatDistance = distance;
	}

	boolean retreatUnproductive(long now, double distance)
	{
		return (distance <= 100) || ((retreatAt >= 0) && ((now - retreatAt >= RETREAT_MAX_MS)
			|| ((now - retreatAt >= RETREAT_PROGRESS_MS) && (distance - retreatDistance < 25))));
	}

	void endRetreat(long now)
	{
		retreatAt = -1;
		nextRetreatAt = now + RETREAT_COOLDOWN_MS;
	}
}
