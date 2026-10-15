#!/usr/bin/env python3
"""Tactics policy + source safety guards + compile ALL fake-player scripts. No DB."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / 'game/data/scripts/custom/fakeplayersai'


def main():
    src = (SCRIPTS / 'BotTactics.java').read_text()
    for forbidden in ('setHeading(', 'setXYZ(', 'teleToLocation(', 'MagicSkillUse(', 'applyEffects(',
                      'setCurrentHp(', 'setCurrentMp(', 'increaseCharges(', 'setMoveSpeed(', 'setPAtk('):
        assert forbidden not in src, 'No tactical cheats: ' + forbidden
    assert 'bot.useMagic(skill, true, true)' in src
    assert 'bot.getClient() != null' in src
    assert 'ranked && preset.getClassId() == 131' in src
    assert 'getRotation()' in src and 'getBuffs()' in src and 'getAllSkills()' not in src
    assert 'AutoUseTaskManager.getInstance().stopAutoUseTask(bot)' in src
    assert 'AutoPlayTaskManager.getInstance().stopAutoPlay(bot)' in src
    assert 'bot.isCastingNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()' in src
    assert 'if (sleeping && !defenseOnly)' in src
    assert 'skill.isDebuff() || kind != TacticsPolicy.Kind.DAMAGE' in src
    assert 'canMoveToTarget(' in src and 'canSeeTarget(' in src
    assert 'MAX_BOTS = 2048' in src and 's.failures.size() >= 64' in src
    print('PASS: tactical safety source guards', flush=True)
    with tempfile.TemporaryDirectory(prefix='tactics-tests-') as directory:
        tmp = Path(directory)
        launcher=tmp / "Compile.java"
        launcher.write_text("import javax.tools.ToolProvider; public class Compile { public static void main(String[] a) { System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a)); } }")
        subprocess.run(['java', str(launcher), '-d', str(tmp / 'policy'),
                        str(SCRIPTS / 'TacticsPolicy.java'),
                        str(ROOT / 'tests/fakeplayersai/tacticsPolicyTest.java')], check=True)
        subprocess.run(['java', '-cp', str(tmp / 'policy'), 'custom.fakeplayersai.tacticsPolicyTest'], check=True)
        jars = sorted((ROOT / 'libs').glob('*.jar'))
        subprocess.run(['java', str(launcher), '-cp', os.pathsep.join(map(str, jars)),
                        '-d', str(tmp / 'production'), *map(str, sorted(SCRIPTS.glob('*.java'))),
                        str(ROOT / 'tests/fakeplayersai/tacticsStateTest.java')], check=True)
        print('PASS: all production fake-player scripts compiled against libs/*.jar', flush=True)
        subprocess.run(['java', '-cp', os.pathsep.join([str(tmp / 'production'), *map(str, jars)]),
                        'custom.fakeplayersai.tacticsStateTest'], check=True)


if __name__ == '__main__':
    main()
