package custom.fakeplayersai;

/** Pure phase-policy tests; no server, player objects or database are constructed. */
public class BerserkerCycleTest
{
	private static int checks;
	private static void check(boolean condition, String message)
	{
		checks++;
		if (!condition)
		{
			throw new AssertionError(message);
		}
	}

	public static void main(String[] args)
	{
		BerserkerCycle c = new BerserkerCycle();
		check(c.canRush(0), "opening rush available");
		check(c.pollRush(0, false, 500, 0, 0) == BerserkerCycle.RushStatus.NONE, "no phantom pending rush");
		c.requested(100, 0, 0);
		check(!c.canRush(200), "one request at a time");
		check(c.pollRush(500, true, 30, 470, 0) == BerserkerCycle.RushStatus.WAIT, "even a moved rush must finish casting before burst");
		check(c.pollRush(1100, false, 30, 470, 0) == BerserkerCycle.RushStatus.LANDED, "finished displaced rush confirmed");
		check(!c.rushPending(), "confirmation clears pending request");

		c = new BerserkerCycle();
		c.requested(0, 0, 0);
		check(c.pollRush(100, false, 500, 0, 0) == BerserkerCycle.RushStatus.FAILED, "accepted but not executed does not count as landed");
		check(!c.canRush(1000), "failed cast has retry backoff");
		check(c.canRush(1600), "retry backoff expires");
		c.requested(2000, 0, 0);
		check(c.pollRush(2500, false, 40, 0, 0) == BerserkerCycle.RushStatus.FAILED, "enemy approaching stationary bot is not a successful rush");
		c.requested(4000, 0, 0);
		check(c.pollRush(10000, true, 500, 0, 0) == BerserkerCycle.RushStatus.TIMED_OUT, "stuck owned cast watchdog");
		check(!c.rushPending() && !c.canRush(10500), "timeout releases phase with retry backoff");

		c = new BerserkerCycle();
		check(!c.canRetreat(0, 50), "no running away under point-blank pressure");
		check(c.canRetreat(0, 150), "short gap can be opened");
		check(!c.canRetreat(0, 300), "already in rush range: no pointless retreat");
		c.beginRetreat(0, 150);
		check(!c.retreatUnproductive(500, 160), "retreat gets a brief opportunity");
		check(c.retreatUnproductive(750, 160), "no meaningful progress stops the run");
		c.endRetreat(750);
		check(!c.canRetreat(1000, 150), "no rapid retreat/fight oscillation");
		check(c.canRetreat(5750, 150), "bounded retreat cooldown expires");
		c.beginRetreat(6000, 150);
		check(!c.retreatUnproductive(6750, 200), "productive gap opening may continue");
		check(c.retreatUnproductive(7500, 210), "absolute 1.5s retreat limit");
		c.beginRetreat(8000, 180);
		check(c.retreatUnproductive(8250, 90), "pressed retreat turns to fight immediately");
		check(!c.canRetreat(9000, 220), "220 is engagement/rush threshold, not retreat");
		System.out.println("PASS: " + checks + " Doombringer cycle-policy checks (no live server).");
	}
}
