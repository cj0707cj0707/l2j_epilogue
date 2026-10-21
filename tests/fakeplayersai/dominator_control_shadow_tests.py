#!/usr/bin/env python3
"""Actual native combat selection and guards for structure55; includes structure54 regression."""
from pathlib import Path
import os, subprocess, tempfile
R=Path(__file__).resolve().parents[2];T=Path(__file__).parent
S=R/'game/data/scripts/custom/fakeplayersai'
t=(S/'BotTactics.java').read_text();m=(S/'BotManager.java').read_text();sp=(S/'BotSpawner.java').read_text()
a=t[t.index('private static boolean act('):t.index('private static void restoreCardinalMana(')]
assert a.index('s.pendingId != 0')<a.index('s.sleepRetreat.tick(')<a.index('time < s.nextDecision')
assert 'if (controlHold) return true;' in a and 'shadowChoice >= 0 ? shadowChoice' in a
reset=t[t.index('private static void prepare('):t.index('private static boolean act(')]
assert 's.sleepRetreat.reset();' in reset and 's.summonAttack.reset();' in reset and 's.rangedCycle.reset();' in reset
assert 's.summonAttack.tick(bot, enemy, time);' in t
assert 'BotTactics.tickSummonAttack(bot, activeOpponent, true);' in m
assert 'BotTactics.tickSummonAttack(player, target.asPlayer(), true);' in sp
assert 'summon.getTarget() != enemy' not in sp
pulse=(S/'SummonAttackPulse.java').read_text()
assert 'time + 3000' in pulse and 'summon.doSummonAttack(enemy)' in pulse
assert all(x not in pulse for x in ['applyEffects(', 'useMagic(', 'fullRestore(', 'setPet(', 'revive(', 'owner.setTarget('])
retreat=(S/'SleepRetreat.java').read_text()
assert all(x not in retreat for x in ['teleToLocation(', 'setXYZ(', 'setHeading(', 'abortCast(', 'setCurrentMp('])
with tempfile.TemporaryDirectory(prefix='sleep54-') as directory:
    d = Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp = os.pathsep.join(map(str, sorted((R / 'libs').glob('*.jar'))))
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/handlers/skill/targets').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'BerserkerSoulCleanseTest.java', 'DominatorPressureBluffTest.java', 'DaggerRearApproachTest.java', 'DominatorCpTitanPressureRegression.java', 'CpMeleeRushRecoveryTest.java', 'PostSleepSummonTest.java', 'DominatorControlShadowTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            raise SystemExit(result.returncode)
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.DominatorControlShadowTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile')
