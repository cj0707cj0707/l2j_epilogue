#!/usr/bin/env python3
"""Compile all shipped custom/effect scripts; test the actual dagger step via real core actors.
The recording AI is inert; no production DB, client or world movement is started.
"""
from pathlib import Path
import os
import subprocess
import tempfile

R = Path(__file__).resolve().parents[2]
T = Path(__file__).parent
code = (R / 'game/data/scripts/custom/fakeplayersai/BotTactics.java').read_text()
step = code[code.index('private static boolean step('):]
assert step.index('s.nextStep = time + 2000;') < step.index('canMoveToTarget(') < step.index('setIntentionMoveTo(')
assert 'if (!armedDagger) s.nextStep = time + 1500;' in step
assert 'time < s.nextStep' in step and 'if (length > 160)' in step
assert 'if (!defenseOnly && (!sleeping' in code and '&& !urgent && time >= s.nextStep' in code
assert 'bot.isCastingNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()' in code
assert 'if (s.pendingId != 0)' in code and 'if (!controlHold && manaFallback && selected < 0 && !sleeping)' in code
assert not any(x in step for x in ['teleToLocation(', 'setXYZ(', 'setHeading('])

with tempfile.TemporaryDirectory(prefix='dagger47-') as directory:
    d = Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp = os.pathsep.join(map(str, sorted((R / 'libs').glob('*.jar'))))
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'DaggerRearApproachTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            result.check_returncode()
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.DaggerRearApproachTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile; source guards preserve cast/CC/sleep/heal/low-MP priority and geodata checks')
