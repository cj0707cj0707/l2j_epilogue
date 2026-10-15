package custom.fakeplayersai;

import java.lang.reflect.Field;
import java.util.Map;

/** Exercises real production bookkeeping without constructing a GameServer/DB. */
public final class tacticsStateTest
{
	private static void check(boolean condition, String message)
	{
		if (!condition) { throw new AssertionError(message); }
	}
	public static void main(String[] args) throws Exception
	{
		check(BotTactics.getDefaultDifficulty().equals("strong"), "initial ranked default is strong");
		BotTactics.setDefaultDifficulty("strong");
		check(BotTactics.getDifficulty(77).equals("strong"), "missing state follows default");
		BotTactics.setDifficulty(77, "normal");
		BotTactics.setDefaultDifficulty("experienced");
		check(BotTactics.getDifficulty(77).equals("normal"), "explicit training override survives global changes");
		check(!BotTactics.isOwned(77), "configuration alone cannot claim a real player");
		BotTactics.forget(77);
		check(BotTactics.getDifficulty(77).equals("experienced"), "forget drops all per-bot settings");
		check(!BotTactics.tick(null, null, null, true), "null tick safe");
		check(!BotTactics.initializeTraining(null, null), "null registration denied");
		check(!BotTactics.tickDefense(null, null, null, false), "null defense hook safe");
		for (int id = 1; id <= 3000; id++) { BotTactics.setDifficulty(id, "strong"); }
		Field field = BotTactics.class.getDeclaredField("STATES");
		field.setAccessible(true);
		Map<?, ?> states = (Map<?, ?>) field.get(null);
		check(states.size() == 2048, "hard bounded bot state");
		check(BotTactics.getDifficulty(1).equals("experienced"), "oldest state evicted");
		Object sample = states.values().iterator().next();
		for (Field f : sample.getClass().getDeclaredFields())
		{
			check(!f.getType().getName().contains("actor.Player"), "state never retains Player references");
		}
		for (int id = 1; id <= 3000; id++) { BotTactics.forget(id); }
		check(states.isEmpty(), "complete teardown");
		BotTactics.setDifficulty(77, "invalid");
		check(BotTactics.getDifficulty(77).equals("experienced"), "invalid difficulty safe");
		BotTactics.forget(77);
		System.out.println("PASS: production tactics bookkeeping, cleanup and 2048-state cap");
	}
}
