#!/usr/bin/env python3
"""Compile all custom/effect scripts and exercise both production server-buff profiles."""
from pathlib import Path
import os
import subprocess
import tempfile
R=Path(__file__).resolve().parents[2]
T=Path(__file__).parent
spawner=(R/'game/data/scripts/custom/fakeplayersai/BotSpawner.java').read_text()
training=(R/'game/data/scripts/custom/fakeplayersai/TrainingArena.java').read_text()
a=training.index('if (!s.preparationStarted && !s.stopping && s.pendingWinnerId == 0 && !s.duel.getFinished())')
b=training.index('s.preparationStarted = true',a)
assert training.index('BotSpawner.applyTrainingOlympiadBuffs(p,',a)<b
assert training.index('BotSpawner.applyTrainingOlympiadBuffs(b,',a)<b
assert training.count('BotSpawner.applyTrainingOlympiadBuffs(')==2
helper=spawner[spawner.index('private static void applyDominatorArenaBuff('):spawner.index('public static int[] getTacticalCpSupport(')]
assert 'dominatorPresetAllows' not in spawner
assert 'getSkill(1414, 1)' in helper and 'skill.applyEffects(player, player)' in helper
assert 'addSkill(' not in helper and 'useMagic(' not in helper
with tempfile.TemporaryDirectory(prefix='victory50-') as directory:
    d = Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp = os.pathsep.join(map(str, sorted((R / 'libs').glob('*.jar'))))
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'DominatorServerVictoryTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            result.check_returncode()
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.DominatorServerVictoryTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile')
