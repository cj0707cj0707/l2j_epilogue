package custom.fakeplayersai;

import java.util.List;
import static custom.fakeplayersai.TacticsPolicy.*;

public final class tacticsPolicyTest
{
	private static int checks;
	private static void check(boolean value, String message)
	{
		checks++;
		if (!value) { throw new AssertionError(message); }
	}
	public static void main(String[] args)
	{
		check(Difficulty.parse(null) == Difficulty.EXPERIENCED, "default experienced");
		check(Difficulty.parse("STRONG") == Difficulty.STRONG, "case tolerant");
		check(Difficulty.parse("invalid") == Difficulty.EXPERIENCED, "safe invalid");
		check(Difficulty.STRONG.reactionMs < Difficulty.EXPERIENCED.reactionMs && Difficulty.EXPERIENCED.reactionMs < Difficulty.NORMAL.reactionMs, "reaction ordering");
		for (Difficulty d : Difficulty.values())
		{
			check(!reactionReady(d, 100, 100, false), "no zero-latency reaction");
			check(!reactionReady(d, 100 + d.reactionMs - 1, 100, false), "wait full latency");
			check(reactionReady(d, 100 + d.reactionMs, 100, false), "ready exact boundary");
			long urgentDelay = Math.max(100,d.reactionMs/2);
            check(!reactionReady(d, 100+urgentDelay-1, 100, true), "urgent retains configured minimum");
            check(reactionReady(d, 100+urgentDelay, 100, true), "urgent exact boundary");
			for (Kind k : Kind.values())
			{
				check(!allowed(k, false, true, true, false, false, true, true), "cooldown gate");
				check(!allowed(k, true, false, true, false, false, true, true), "range gate");
				check(!allowed(k, true, true, true, false, false, false, true), "resource gate");
				check(!allowed(k, true, true, true, true, false, true, true), "active-effect gate");
				check(allowed(k, true, true, true, false, false, true, true), "legal candidate");
			}
			for (Kind k : new Kind[] {Kind.DAMAGE, Kind.CONTROL, Kind.SLOW})
			{
				check(!allowed(k, true, true, true, false, true, true, true), "NEVER break sleep");
				check(!allowed(k, true, true, false, false, false, true, true), "LOS offense gate");
			}
		}
		check(!allowed(Kind.CONTROL, true, true, true, false, false, true, false), "interrupt cue reaction");
		Context danger = new Context(.2, .7, 120, false, false, false, false, false, false);
		Context healthy = new Context(.95, .7, 120, false, false, false, false, false, false);
		Context casting = new Context(.7, .7, 300, false, true, true, true, false, false);
		Context window = new Context(.7, .7, 120, false, false, false, false, true, false);
		Context chasing = new Context(.7, .7, 300, false, false, false, true, false, true);
		check(score(Kind.HEAL, danger) > score(Kind.DAMAGE, danger), "urgent heal");
		check(score(Kind.DEFENSE, danger) > score(Kind.DAMAGE, danger), "urgent defense");
		check(score(Kind.HEAL, healthy) < 0 && score(Kind.DEFENSE, healthy) < 0, "no waste at full HP");
		check(score(Kind.CONTROL, casting) > score(Kind.DAMAGE, casting), "interrupt real cast");
		check(score(Kind.DAMAGE, window) > score(Kind.DAMAGE, healthy), "damage window priority");
		check(score(Kind.SLOW, chasing) > score(Kind.DAMAGE, chasing), "useful slow while chasing");
		check(score(Kind.CHARGE, chasing) > score(Kind.DAMAGE, chasing), "build needed charges");
		check(select(List.of(), Difficulty.NORMAL, 0, 0) == -1, "empty selection");
		List<Option> emergency = List.of(new Option(1, Kind.DAMAGE, 62), new Option(2, Kind.HEAL, 160));
		for (Difficulty d : Difficulty.values())
		{
			for (int i = 0; i < 100; i++)
			{
				check(select(emergency, d, 0, i / 100.0) == 2, "mistakes never ignore emergency");
			}
		}
		List<Option> choices = List.of(new Option(7, Kind.DAMAGE, 62), new Option(8, Kind.SLOW, 92));
		int normalBest = 0, strongBest = 0;
		for (int i = 0; i < 1000; i++)
		{
			final double roll = (i + .5) / 1000;
			if (select(choices, Difficulty.NORMAL, 1, roll) == 8) { normalBest++; }
			if (select(choices, Difficulty.STRONG, 1, roll) == 8) { strongBest++; }
		}
		check(strongBest > normalBest && strongBest < 1000, "weighted quality, not stats or perfect play");
		check(select(choices, Difficulty.NORMAL, 0, 0) == 7, "bounded mistake chooses a legal alternative");
		check(failureBackoff(1) == 500 && failureBackoff(2) == 1000, "exponential backoff");
		check(failureBackoff(100) == 8000, "backoff bounded");
		check(behind(0, 0, 0, -100, 0), "west rear east facing");
		check(!behind(0, 0, 0, 100, 0), "not in front");
		check(behind(0, 0, 32768, 100, 0), "heading reversed");
		check(!behind(0, 0, 0, 0, 0), "overlap not rear");
		check(TacticsPolicy.adapt(92,TacticsPolicy.Kind.SLOW,true,false)==112,"fleeing memory raises slow preference");
        check(TacticsPolicy.adapt(62,TacticsPolicy.Kind.DAMAGE,true,true)==82,"fleeing memory raises legal movement charge");
        check(TacticsPolicy.adapt(-1,TacticsPolicy.Kind.BUFF,true,false)==-1,"habit never enables disallowed choice");
        check(Difficulty.STRONG.reactionMs == 100, "strong uses 100ms observation cadence, not zero latency");
        check(attemptResolved(true,false,10), "completed observed cast has no fake 750ms lock");
        check(attemptResolved(false,true,100), "paid instant cast resumes next tick");
        check(!attemptResolved(false,false,349), "unstarted request has bounded grace");
        check(attemptResolved(false,false,350), "unstarted request releases on timeout");
        check(!rangedStepAllowed(true,true,120,.9,false,true,3), "healthy caster attacks instead of stepping");
        check(!rangedStepAllowed(false,true,90,.9,false,true,3), "healthy bow attacks instead of stepping");
        check(!rangedStepAllowed(true,true,120,.6,false,true,1), "must attack between retreats");
        check(rangedStepAllowed(true,true,120,.6,false,true,2), "bounded retreat after offensive sequence");
        check(rangedStepAllowed(true,true,80,.2,false,true,0), "low HP pointblank emergency remains");
        check(!rangedStepAllowed(true,false,80,.2,false,true,3), "do not kite casters or bows as melee");
        check(!rangedStepAllowed(true,true,80,.2,true,true,3), "no retreat from already controlled enemy");
        check(!rangedStepAllowed(true,true,145,.4,false,false,3), "caster range boundary");
        check(!rangedStepAllowed(false,true,110,.4,false,false,3), "bow range boundary");
        check(rangedStepAllowed(true,true,140,.9,false,false,0), "cooldown permits positioning without sacrificing ready offense");
        System.out.println("PASS: " + checks + " tactical policy assertions");
	}
}
