#!/usr/bin/env python3
"""Production Cardinal methods/policy + real core skill grant; offline, no DB/network/server."""
from pathlib import Path
import os
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
S = ROOT / 'game/data/scripts/custom/fakeplayersai'
spawner = (S / 'BotSpawner.java').read_text()
tactics = (S / 'BotTactics.java').read_text()
manager = (S / 'BotManager.java').read_text()
training = (S / 'TrainingArena.java').read_text()
# Wiring assertions complement (not replace) executing the actual production methods.
assert 'fixedClassNickname(desired, player.getObjectId(), CharInfoTable.getInstance()::getIdByName)' in spawner
assert 'fixedClassNickname(base, 0, CharInfoTable.getInstance()::getIdByName)' in spawner
assert 'applyBotOnlySkills(player, preset);' in spawner
assert 'REGISTRY_NAMES.put(bot.getObjectId(), bot.getName());' in manager
assert manager.index('BotSpawner.applyClassSkills(bot);') < manager.index('BotSpawner.applyBotOnlySkills(bot, preset);') < manager.index('BotSpawner.applySkillEnchants(bot);')
assert training.index('b.giveAvailableSkills(true, true, true);') < training.index('BotSpawner.applyBotOnlySkills(b, preset);') < training.index('BotSpawner.applySkillEnchants(b);')
assert 'applyBotOnlySkills(human' not in training
assert 'final boolean protectedSleep = sleeping && !dominator && !cardinalMbException && !trainingRushActive;' in tactics
assert 'TacticsPolicy.cardinalSleepException(preset.getClassId(), skill.getId())' in tactics
assert 'skill.getId() == CARDINAL_PROMINENCE_ID && !s.cardinalProminence.ready(time)' in tactics
assert 's.cardinalProminence.reset();' in tactics
assert 'final int selected = recoveryChoice >= 0 ? recoveryChoice : cardinalChoice >= 0 ? cardinalChoice' in tactics
assert 'shadowChoice >= 0 || cardinalChoice >= 0 || chosen == TacticsPolicy.Kind.HEAL' in tactics
act = tactics[tactics.index('private static boolean act('):tactics.index('private static void stopBasicAttack(')]
assert act.index('TacticsPolicy.allowed(kind, ready, inRange') < act.index('s.cardinalProminence.choose(') < act.index('step(bot, enemy') < act.index('bot.useMagic(skill, true, true)')
assert act.index('bot.useMagic(skill, true, true)') < act.index('if (accepted)') < act.index('s.cardinalProminence.accepted(time)')
for guard in ['resources(bot, skill)', 'skill.checkCondition(bot, self ? bot : enemy, false)', '!bot.isSkillDisabled(skill)', 'GeoEngine.getInstance().canSeeTarget(bot, enemy)', 'bot.isCastingNow() || bot.isCastingSimultaneouslyNow()']:
    assert guard in act, guard
for path in (ROOT / 'game/data/stats/players/skillTrees').rglob('*.xml'):
    for tree in ET.parse(path).getroot().findall('skillTree'):
        if tree.get('classId') in {'97','16','15','10','0'}:
            assert not any(x.get('skillId')=='1230' for x in tree.findall('skill')), 'Human Cardinal tree must not acquire Prominence'
print('PASS: new/restored/training grant wiring; sleep/10s priority wiring; native cast guards; human skill trees unchanged', flush=True)
with tempfile.TemporaryDirectory(prefix='cardinal-regression-') as d:
    temp = Path(d)
    compiler = temp / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp = os.pathsep.join(str(p) for p in sorted((ROOT / 'libs').glob('*.jar')))
    sources = [ROOT / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((ROOT / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((ROOT / 'game/data/scripts/custom').rglob('*.java')),
               Path(__file__).with_name('CardinalNameProminenceTest.java')]
    subprocess.run(['java', str(compiler), '-cp', cp, '-d', str(temp), *map(str, sources)], cwd=ROOT, check=True, timeout=180)
    run = subprocess.run(['java', '-cp', str(temp)+os.pathsep+cp, 'custom.fakeplayersai.CardinalNameProminenceTest', str(ROOT/'game')], cwd=ROOT/'game', capture_output=True, text=True, timeout=120)
    if run.returncode:
        print(run.stdout); print(run.stderr); run.check_returncode()
    assert 'SEVERE:' not in run.stderr and 'No skill info found' not in run.stderr, run.stderr
    print(run.stdout.strip())
