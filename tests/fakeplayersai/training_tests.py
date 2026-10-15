#!/usr/bin/env python3
"""Production compile plus isolated helper tests and static safety assertions. NO live test."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'game/data/scripts/custom/fakeplayersai'
t = (SRC / 'TrainingArena.java').read_text()
a = (SRC / 'CombatAnalytics.java').read_text()
for forbidden in ('setInOlympiadMode(', 'setOlympiadGameId(', 'setNoblePoints(', 'registerNoble(', 'resetDisabledSkills(', 'fullRestore(', 'BotSpawner.createBot('):
    assert forbidden not in t, forbidden
for forbidden in ('p.addItem(', 'p.addExpAndSp(', 'p.giveAvailableSkills(', 'p.setCurrentHp(', 'p.setCurrentMp(', 'p.setCurrentCp(', 'p.getInventory().destroy'):
    assert forbidden not in t, forbidden
assert t.index('save(p.getObjectId(), j); // MUST') < t.index('s.bot = createBot(') < t.index('p.teleToLocation(')
assert 'DuelManager.getInstance().addDuel(p, b, 0)' in t
assert 'DuelManager.getInstance().doSurrender(p)' in t
assert 'BotTactics.forget(s.bot.getObjectId())' in t
assert 'if (!BotTactics.tick(b, p, preset, false))' in t and 'OlympiadChase.updateTraining(' in t
assert 'b.getAI().setIntentionAttack(p)' in t
assert 'getCurrentHp() < p.getMaxHp() - 1' in t and 'getCurrentMp() < p.getMaxMp() - 1' in t
assert 'getNoDuelReason().getId()' in t, 'town peace zone must not block trainer; pending duel still rejected'
assert 'out.getFD().sync()' in t and 'StandardCopyOption.ATOMIC_MOVE' in t and 'dir.force(true)' in t
assert 'char_name=? AND account_name=?' in t and 'GameClient.deleteCharByObjId(id)' in t
assert 'DELETE FROM item_elementals WHERE itemId IN' in t and 'DELETE FROM item_variables WHERE id IN' in t
assert 'Files.deleteIfExists(path(p.getObjectId()))' in t
for forbidden in ('DatabaseFactory', 'ON_PLAYER_CHAT', 'OnPlayerChat', 'getEmail', 'getAccountName'):
    assert forbidden not in a, forbidden
assert 'ON_CREATURE_DAMAGE_DEALT' in a and 'ON_CREATURE_DAMAGE_RECEIVED' not in a
assert 'ON_CREATURE_SKILL_USE' in a and 'ON_CREATURE_DEATH' in a
assert 'ON_OLYMPIAD_MATCH_RESULT' in a and 'winner == null ? 0' in a
assert 'escape(r.opponent)' in a and 'escape(r.result)' in a
assert 'size() > 2048' in a and 'rows.size() > 10' in a and 'side.skills.size() < 64' in a
print('PASS: training/analytics source safety and integration guards', flush=True)
with tempfile.TemporaryDirectory(prefix='training-check-') as d:
    tmp = Path(d)
    launcher=tmp / "Compile.java"
    launcher.write_text("import javax.tools.ToolProvider; public class Compile { public static void main(String[] a) { System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a)); } }")
    cp = os.pathsep.join(map(str, sorted((ROOT / 'libs').glob('*.jar'))))
    sources = list(sorted((ROOT / 'game/data/scripts/custom').rglob('*.java')))
    subprocess.run(['java', str(launcher), '-cp', cp, '-d', str(tmp / 'classes'),
                    *map(str, sources), str(ROOT / 'tests/fakeplayersai/trainingPolicyTest.java')], check=True, cwd=ROOT)
    print('PASS: full custom scripts compiled against actual libs/*.jar', flush=True)
    subprocess.run(['java', '-cp', str(tmp / 'classes') + os.pathsep + cp,
                    'custom.fakeplayersai.trainingPolicyTest'], cwd=tmp, check=True)
