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
		for (boolean dagger : new boolean[] {false, true}) for (boolean caster : new boolean[] {false, true}) for (boolean disarmed : new boolean[] {false, true})
			check(basicAttackAllowed(dagger, caster, disarmed) == (!dagger && !caster && !disarmed), "exhaustive basic-attack gate");
		check(effectiveRange(false,40,0,80,8)==48, "targeted melee includes actual bot collision radius");
		check(effectiveRange(false,-1,0,80,8)==88, "negative physical cast range uses weapon reach");
		check(effectiveRange(true,-1,150,80,8)==158, "negative aura cast range uses affect radius");
		check(effectiveRange(true,-1,0,80,8)==0, "missing aura radius never grants collision-only attack");
		check(defenseScore(.95,.2,true)>score(Kind.DAMAGE,healthy), "CP collapse under threat triggers defense before HP damage");
		check(defenseScore(.95,.2,false)<0, "no CP panic without observable threat");
		check(defenseScore(.2,1,false)>=150, "HP emergency independent of CP");
		for (int id : new int[] {110,368,350,439,527,112,82,86,760,913,351,72,916,111,139,447,761,788,499})
			check(defenseSkill(id), "explicit physical/magic and non-tank defense ID " + id);
		for (int id : new int[] {30,344,36,917,769,1532}) check(!defenseSkill(id), "attack/opener never mistaken for defense " + id);
		check(recoveryStepAllowed(true,true,false,200,false), "disarmed melee retreats at real speed");
		check(recoveryStepAllowed(false,false,false,200,false), "no usable offensive skill retreats");
		check(!recoveryStepAllowed(false,true,false,200,false), "out-of-range ready offense pursues rather than perpetual kite");
		check(!recoveryStepAllowed(false,true,true,80,false), "ready melee offense precedes kite");
		check(!recoveryStepAllowed(true,false,false,350,false), "bounded recovery distance");
		check(!recoveryStepAllowed(true,false,false,80,true), "no retreat from controlled threat");
		for (boolean dominator : new boolean[] {false,true}) for (boolean sleeping : new boolean[] {false,true})
			for (long elapsed : new long[] {0,4999,5000,10000,60000}) for (boolean direct : new boolean[] {false,true}) for (boolean sleepSkill : new boolean[] {false,true})
			{
				boolean expected = !sleeping || dominator && !sleepSkill && (elapsed<5000 ? !direct : direct);
				check(sleepOffenseAllowed(dominator,sleeping,elapsed,direct,sleepSkill)==expected,"bounded class-specific sleep phase matrix");
			}
		check(observeSleep(-1,false,100)==-1,"awake keeps sleep phase reset");
		check(observeSleep(-1,true,100)==100,"sleep observation starts phase once");
		check(observeSleep(100,true,5000)==100,"continuous sleep never refreshes five-second phase");
		check(observeSleep(100,false,5100)==-1,"sleep end resets bounded phase");
		check(observeSleep(-1,true,5200)==5200,"next sleep cycle gets a fresh curse phase");
		check(defensePriority(110)>defensePriority(82),"UD emergency precedes low value Majesty");
		check(defensePriority(760)>defensePriority(72),"anti magic armor precedes Iron Will");
		check(!recoveryEnemyControlled(true,true,true,false),"Dominator may kite when sleeping foe leaves no legal curses/damage");
		check(recoveryEnemyControlled(false,true,true,false),"other classes retain sleep positioning hold");
		check(recoveryEnemyControlled(true,false,true,false),"rooted non-sleeping enemy remains controlled");
		check(boundedApproachRange(48.8)==48,"pursuit rounds down within legal blow collision radius");
		check(boundedApproachRange(-1)==20,"minimum pursuit range bounded");
		check(boundedApproachRange(5000)==1000,"maximum pursuit range bounded");
        System.out.println("PASS: " + checks + " tactical policy assertions");
	}
}
