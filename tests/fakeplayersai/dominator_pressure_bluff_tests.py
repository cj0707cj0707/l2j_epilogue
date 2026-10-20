#!/usr/bin/env python3
"""Compile all custom/effect scripts and execute native SkillData + actual tactical selection."""
from pathlib import Path
import os, subprocess, tempfile, xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[2];T=Path(__file__).parent
path='game/data/stats/skills/00300-00399.xml'
old=ET.fromstring(subprocess.check_output(['git','show','HEAD:'+path],cwd=R,text=True));new=ET.parse(R/path).getroot()
# Baseline-independent invariant: changing358 reuse must not touch any other XML value.
assert new.findtext("skill[@id='358']/reuseDelay")=='12000'
old.find("skill[@id='358']/reuseDelay").text='12000'
assert ET.tostring(old)==ET.tostring(new)
# Native Trance XML is already restored; do not rewrite its full 99-skill document.
trance=ET.parse(R/'game/data/stats/skills/01300-01399.xml').getroot().find("skill[@id='1394']")
assert trance.findtext('reuseDelay')=='1000'
source=R/'game/data/scripts/custom/fakeplayersai'
t=(source/'BotTactics.java').read_text()
assert 'nextCardinalTrance' not in t
assert 's.dominatorPressure.reset();' in t and 's.dominatorPressure.resolved(s.pendingId, completed, ownEffect,' in t
for driver in ['BotManager.java','TrainingArena.java']:
    text=(source/driver).read_text()
    assert 'BotTactics.tick(' in text and 'BotTactics.tacticalApproachRange(' in text
assert 'if (dominatorRangedPressure(bot, enemy, preset)) return false;' in t
with tempfile.TemporaryDirectory(prefix='pressure53-') as directory:
    d = Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp = os.pathsep.join(map(str, sorted((R / 'libs').glob('*.jar'))))
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'BerserkerSoulCleanseTest.java', 'DominatorPressureBluffTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            result.check_returncode()
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.DominatorPressureBluffTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile')
