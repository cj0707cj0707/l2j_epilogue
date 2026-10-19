#!/usr/bin/env python3
"""Compile shipped scripts and test root-only Escape Shackle in actual production code."""
from pathlib import Path
import os
import subprocess
import tempfile
R = Path(__file__).resolve().parents[2]
T = Path(__file__).parent
t = (R / 'game/data/scripts/custom/fakeplayersai/BotTactics.java').read_text()
b = (R / 'game/data/scripts/custom/fakeplayersai/BotSpawner.java').read_text()
a = t[t.index('private static boolean act('):t.index('private static void restoreCardinalMana(')]
assert a.index('if (escapeShackle(') < a.index('time < s.nextDecision')
h = t[t.index('private static boolean escapeShackle('):t.index('private static void claim(')]
assert '!bot.isRooted()' in h
assert all(x not in h for x in ['applyEffects(', 'stopSkillEffects(', 'enableSkill(', 'setCurrentMp(', 'abortCast('])
assert 'getAutoSkills().remove(Integer.valueOf(ESCAPE_SHACKLE_ID))' in b
assert 'preset.getBuffs()) if (skillId != ESCAPE_SHACKLE_ID && !TankClassMechanics.skill(skillId))' in b
assert 'autoBuffs.removeIf(skillId -> skillId == ESCAPE_SHACKLE_ID' in b
assert 'if (skillId == ESCAPE_SHACKLE_ID || TankClassMechanics.skill(skillId)) continue;' in b
with tempfile.TemporaryDirectory(prefix='escape49-') as directory:
    d = Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp = os.pathsep.join(map(str, sorted((R / 'libs').glob('*.jar'))))
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'BerserkerSoulCleanseTest.java', 'DaggerEscapeShackleTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            result.check_returncode()
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.DaggerEscapeShackleTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile')
