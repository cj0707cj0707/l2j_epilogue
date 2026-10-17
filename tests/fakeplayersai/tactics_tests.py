#!/usr/bin/env python3
"""Actual XML + inherited learning + real-handler tactics regressions; no DB/server."""
from pathlib import Path
import os
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / 'game/data/scripts/custom/fakeplayersai'
EFFECTS = ROOT / 'game/data/scripts/handlers/skill/effects'
ACTUAL_HANDLERS = ('Backstab', 'FatalBlow', 'EnergyDamage', 'PhysicalSoulDamage',
                   'PhysicalMute', 'Disarm', 'CpDamPercent', 'HealPercent')


def presets():
    text = re.sub(r'/\*.*?\*/|//[^\n]*', '', (SCRIPTS / 'BotPresets.java').read_text(), flags=re.S)
    matches = re.finditer(r'addPreset\((\d+), "([^"]+)".*?new int\[\]\s*\{([^}]+)\}.*?new int\[\]\s*\{([^}]+)\}', text, re.S)
    result = [(int(m[1]), m[2], list(map(int, re.findall(r'\d+', m[3]))),
               list(map(int, re.findall(r'\d+', m[4])))) for m in matches]
    assert len(result) == 18, 'audit must cover the full eighteen-class roster'
    return result


def xml_audit(output):
    trees = {}
    for path in (ROOT / 'game/data/stats/players/skillTrees').rglob('*.xml'):
        for tree in ET.parse(path).getroot().iter('skillTree'):
            if tree.get('type') == 'classSkillTree':
                trees[int(tree.get('classId'))] = (int(tree.get('parentClassId', '-1')),
                    {int(s.get('skillId')) for s in tree.findall('skill')})
    assert trees, 'actual inherited skill trees required: sparse-checkout stats/players/skillTrees'

    def learned(class_id):
        parent, own = trees[class_id]
        return own | learned(parent) if parent >= 0 else own

    skills = {int(s.get('id')): s for path in (ROOT / 'game/data/stats/skills').glob('*.xml')
              for s in ET.parse(path).getroot().findall('skill')}
    assert skills, 'actual skill XML required'
    enums = {}
    for path in EFFECTS.glob('*.java'):
        text = path.read_text()
        match = re.search(r'EffectType getEffectType\(\)\s*\{\s*return EffectType\.(\w+);', text)
        enums[path.stem] = match[1] if match else 'NONE'  # AbstractEffect default, not a guessed flag.
    assert all(name in enums for name in ACTUAL_HANDLERS), 'actual handler sources required'
    assert all(enums[name] == 'PHYSICAL_ATTACK' for name in ACTUAL_HANDLERS[:4])
    assert enums['PhysicalMute'] == enums['Disarm'] == 'NONE'
    entries, unlearned = [], []
    for cid, name, rotation, buffs in presets():
        known = learned(cid)
        for role, ids in [('R', rotation), ('B', buffs)]:
            for sid in ids:
                assert sid in skills, f'XML missing for {cid}/{sid}'
                node = skills[sid]
                tables = {t.get('name'): (t.text or '').split() for t in node.findall('table')}

                def value(tag):
                    raw = node.findtext(tag)
                    if raw is None:
                        return None
                    raw = raw.strip()
                    if raw.startswith('#'):
                        values = tables[raw]
                        raw = values[min(int(node.get('levels', '1')), len(values)) - 1]
                    return raw

                attrs = []
                for tag in ('operateType', 'targetType', 'castRange', 'affectRange', 'isDebuff',
                            'isMagic', 'abnormalType', 'abnormalTime', 'effectPoint'):
                    val = value(tag)
                    if val is not None:
                        attrs.append(f'{tag}={val}')
                effect_names = sorted({e.get('name') for group in ('effects', 'pvpEffects')
                                       for container in node.findall(group) for e in container.findall('effect')})
                assert all(e in enums for e in effect_names), f'unresolved actual effect handler {cid}/{sid}'
                flags = ';'.join(f'{e}:{enums[e]}' for e in effect_names)
                entries.append('\t'.join([str(cid), str(sid), role, str(int(sid in known)), ';'.join(attrs), flags]))
                if sid not in known:
                    unlearned.append((cid, sid))
        # A usable normal damage family must actually be learned by each class.
        damage = [sid for sid in rotation if sid in known and any(
            enums.get(e.get('name')) in {'PHYSICAL_ATTACK', 'MAGICAL_ATTACK', 'HP_DRAIN', 'PHYSICAL_ATTACK_HP_LINK'}
            for group in skills[sid].findall('effects') for e in group.findall('effect'))]
        assert damage, f'{cid} {name}: default rotation contains no learned direct attack'
        print(f'AUDIT: {cid} {name}: {len(rotation)+len(buffs)} entries, {len(damage)} learned direct attacks', flush=True)
    # Every default entry must be learned by its own inherited class tree.
    assert not unlearned, f'unexpected unlearned defaults: {unlearned}'
    if unlearned:
        print(f'AUDIT WARNING: parent-owned preset correction pending: {unlearned}', flush=True)
    output.write_text('\n'.join(entries) + '\n')
    print(f'PASS: exhaustive XML/learning/target/range/effect audit, {len(entries)} entries', flush=True)


def source_guards():
    src = (SCRIPTS / 'BotTactics.java').read_text()
    # Explicit user-requested Cardinal-only arena MP exception, covered by
    # cardinal_arena_parity_tests.py; keep every other direct resource write banned.
    assert src.count('bot.setCurrentMp(maximum);') == 1
    guarded_src = src.replace('bot.setCurrentMp(maximum);', '')
    for forbidden in ('setHeading(', 'setXYZ(', 'teleToLocation(', 'MagicSkillUse(', 'applyEffects(',
                      'setCurrentHp(', 'setCurrentMp(', 'increaseCharges(', 'setMoveSpeed(', 'setPAtk(', 'abortCast('):
        assert forbidden not in guarded_src, 'No tactical cheats outside Cardinal MP exception: ' + forbidden
    for required in ('bot.useMagic(skill, true, true)', 'bot.getClient() != null',
                     'BotSpawner.getTacticalRotation(bot, enemy)', 'BotSpawner.getTacticalDefenses(bot, enemy)',
                     'BotSpawner.isOpenerOnlySkill(skill.getId())', 'preset.getBuffs()',
                     'AutoUseTaskManager.getInstance().stopAutoUseTask(bot)',
                     'AutoPlayTaskManager.getInstance().stopAutoPlay(bot)',
                     'bot.isCastingNow() || bot.isOutOfControl() || bot.isAllSkillsDisabled()',
                     'canMoveToTarget(', 'canSeeTarget(', 'MAX_BOTS = 2048', 's.failures.size() >= 64',
                     'TacticsPolicy.attemptResolved(s.sawCast, paid, time - s.pendingAt)',
                     'TacticsPolicy.effectiveRange(', 'TacticsPolicy.recoveryStepAllowed(',
                     'TacticsPolicy.defenseScore(', 'TacticsPolicy.sleepOffenseAllowed(', 'bot.getActiveWeaponItem() == null', 'public static synchronized boolean recoveryOnly(',
                     'skill.checkCondition(bot, self ? bot : enemy, false)',
                     'public static boolean allowsBasicAttack(Player bot, BotClassPreset preset)',
                     'public static synchronized int tacticalApproachRange(Player bot, Player enemy, BotClassPreset preset)'):
        assert required in src, 'Missing tactical contract: ' + required
    assert 'getAllSkills()' not in src, 'effective lists/overrides, no indiscriminate learned-skill scan'
    assert 'skill.getId() == 344 &&' not in src, 'Lethal Blow is NOT positional Backstab'
    assert not re.search(r'if\s*\(bot\.isAttackingNow\(\)\)\s*\{[^{}]*return', src), 'auto swings must never starve tactic selection'
    assert 's.sleepObserved = -1;' in src and 'TacticsPolicy.observeSleep(s.sleepObserved, enemy.isSleeping(), time)' in src, 'observed sleep cycle reset and edge detection'
    assert 's.moveUntil = time + (recovery ? 450 : 200)' in src
    assert 'if (!defenseOnly && !offenseAvailable) { stopBasicAttack(bot); return true; }' in src
    assert 'ranked && preset.getClassId() == 131' in src, 'ranked phase ownership preserved'
    # Shift=true is essential: Creature.onMagicFinalizer checks it before the
    # nextActionAttack follow-up. Never mutate the shared XML Skill to suppress it.
    assert 'setCurrentSkill(' not in src and 'setNextIntentionAttack' not in src
    print('PASS: safety, no-starvation, no-auto and bounded movement source regressions', flush=True)


def main():
    source_guards()
    with tempfile.TemporaryDirectory(prefix='tactics-tests-') as directory:
        tmp = Path(directory)
        fixtures = tmp / 'actual-presets.tsv'
        xml_audit(fixtures)
        launcher = tmp / 'Compile.java'
        launcher.write_text('import javax.tools.ToolProvider; public class Compile { public static void main(String[] a) { System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a)); } }')
        subprocess.run(['java', str(launcher), '-d', str(tmp / 'policy'), str(SCRIPTS / 'TacticsPolicy.java'),
                        str(ROOT / 'tests/fakeplayersai/tacticsPolicyTest.java')], check=True)
        subprocess.run(['java', '-cp', str(tmp / 'policy'), 'custom.fakeplayersai.tacticsPolicyTest'], check=True)
        jars = sorted((ROOT / 'libs').glob('*.jar'))
        subprocess.run(['java', str(launcher), '-cp', os.pathsep.join(map(str, jars)), '-d', str(tmp / 'production'),
                        *map(str, sorted(SCRIPTS.glob('*.java'))),
                        *[str(EFFECTS / (name + '.java')) for name in ACTUAL_HANDLERS],
                        str(ROOT / 'tests/fakeplayersai/tacticsStateTest.java')], check=True)
        print('PASS: ALL production fake-player scripts + actual regression handlers compiled against libs/*.jar', flush=True)
        subprocess.run(['java', '-cp', os.pathsep.join([str(tmp / 'production'), *map(str, jars)]),
                        'custom.fakeplayersai.tacticsStateTest', str(fixtures), str(ROOT / 'game')], check=True, cwd=ROOT / 'game')


if __name__ == '__main__':
    main()
