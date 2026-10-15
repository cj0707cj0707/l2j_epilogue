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
for forbidden in ('setInOlympiadMode(', 'setOlympiadGameId(', 'setNoblePoints(', 'registerNoble(', 'fullRestore(', 'BotSpawner.createBot('):
    assert forbidden not in t, forbidden
for forbidden in ('p.addItem(', 'p.addExpAndSp(', 'p.giveAvailableSkills(', 'p.setCurrentHp(', 'p.setCurrentMp(', 'p.setCurrentCp(', 'p.getInventory().destroy'):
    assert forbidden not in t, forbidden
assert t.index('save(p.getObjectId(), j); // MUST') < t.index('s.bot = createBot(') < t.index('p.teleToLocation(')
assert 'DuelManager.getInstance().addDuel(p, b, 0)' in t
assert 'doSurrender(' not in t, 'cleanup must never reverse a pending natural winner or fabricate a quit loss'
assert 'BotTactics.forget(s.bot.getObjectId())' in t
assert 'if (!BotTactics.tick(b, p, preset, false))' in t and 'OlympiadChase.updateTraining(' in t
assert 'b.getAI().setIntentionAttack(p)' in t
assert 'getCurrentHp() < p.getMaxHp() / 2' in t and 'getCurrentMp() < p.getMaxMp() / 2' in t
assert 'getCurrentCp() <' not in t and 'getMaxHp() - 1' not in t, 'no stricter full-resource gate'
assert 'p.isJailed()' in t and 'p.isAlikeDead()' in t
assert 'ON_CREATURE_ATTACK' in t and 'ON_CREATURE_SKILL_USE' in t and 'FunctionEventListener' in t
assert 'new TerminateReturn(true, true, true)' in t, 'synchronous offensive prep rejection'
assert 'e.getSkill().hasNegativeEffect()' in t and 'e.getSkill().isDebuff()' in t
assert 'e.getSkill().isSuicideAttack()' in t and 'e.getSkill().nextActionIsAttack()' in t
assert 'target != e.getCaster()' in t and 'e.getCaster() == s.player' in t, 'self-only human casts, no bot/pet offense'
assert 's.preparationStarted = true' in t and 'preparationSeconds(TacticalSettings.getTrainingPreparationSeconds())' in t
assert 'Math.max(10, Math.min(60, configured))' in t
assert 'applyTrainingOlympiadBuffs(p,' in t and 'applyTrainingOlympiadBuffs(b,' in t
assert 'maintainFullBuffs(' not in t, 'training must not select world buff branch'
assert 'stopSkillEffects(' not in t and 'stopAllEffects(' not in t and 'savePlayerConditions(' not in t
assert t.index('getDuelState() != Duel.DUELSTATE_DUELLING)) return;') < t.index('applyTrainingOlympiadBuffs(p,')
assert t.index('p.enableAllSkills(); // human') < t.index('s.preparationUntil = System.nanoTime()') < t.index('s.preparationStarted = true')
assert t.index('if (remaining > 0)') < t.index('s.active = true') < t.index('BotTactics.tick(b, p, preset, false)')
prep = t[t.index('if (remaining > 0)'):t.index('// A selfbuff begun near the deadline')]
assert 'p.abortCast()' not in prep and 'stop(p)' not in prep, 'do not cancel human selfbuffs each prep pulse'
assert 'p.setInvul(true); b.setInvul(true)' in prep and 'stop(b); p.abortAttack()' in prep
transition = t[t.index('// A selfbuff begun near the deadline'):t.index('s.active = true')]
assert 'if (p.isCastingNow() || p.isCastingSimultaneouslyNow())' in transition
casting_wait = transition[transition.index('if (p.isCastingNow()'):transition.index('// Never abortCast')]
assert 'return;' in casting_wait and 'stop(b)' in casting_wait, 'finish human cast before combat activation'
assert 'p.abortAttack(); p.stopMove(null)' in transition
for forbidden in ('stop(p)', 'p.abortCast(', 'p.setTarget(', 'p.getAI()'):
    assert forbidden not in transition, 'deadline transition must not interrupt a racing selfbuff: ' + forbidden
assert t.index('if (s.stopping)\n        {\n            captureOfficialResult(s);') < t.index('// A selfbuff begun near the deadline'), 'core timeout/leave precede cast extension'

assert 'BotTactics.allowsBasicAttack(b, preset)' in t and 'if (basicAttack &&' in t
assert 'OlympiadChase.updateTraining(b, p, range, basicAttack,' in t, 'dagger/disarm fallback cannot autoattack'
assert t.count('BotSpawner.applyPreCombatOpeners(') == 1, 'exactly one explicit training opener call site'
assert t.index('s.openersApplied = true;') < t.index('BotSpawner.applyPreCombatOpeners(') < t.index('s.active = true')
assert 'Math.max(s.preparationUntil, System.nanoTime() + 3_000_000_000L)' in t
assert 's.pendingWinnerId = naturalWinnerId(' in t and 's.pendingWinnerId == 0' in t
assert 'if (!s.preparationStarted && !s.stopping && s.pendingWinnerId == 0 && !s.duel.getFinished())' in t, 'no baseline buffs or skill unlock after terminal defeat'
assert 'if (s.officialResultCaptured || !s.duel.getFinished()) return;' in t
assert 's.duel.checkEndDuelCondition()' in t and 's.duel.getWinner()' in t
assert 's.officialResultCaptured = true;' in t, 'capture before restore only once'
assert 'officialWinnerId(true, result,' in t and 's.officialReason' in t
assert 'Unknown: core result was not observed before restore' in t
assert 'до остатка общего лимита 120 сек.' in t, 'quit explains potentially full core timeout wait'
assert 'resultLabel(b.training, own.id, winnerId, reason)' in a

assert t.count('resetEntryCooldowns(p);') == 1, 'one entry reset call site only'
entry = t[t.index('if (!s.duelRequested)'):t.index('if (s.duel == null && p.isInDuel())')]
assert entry.index('if (s.stopping)') < entry.index('if (p.isTeleporting() || b.isTeleporting()) return;') < entry.index('resetEntryCooldowns(p);') < entry.index('addDuel(p, b, 0)')
assert entry.index('s.entryCooldownReset = true;') < entry.index('resetEntryCooldowns(p);')
assert t.index('p.getInstanceId() != s.instance.getId()') < t.index('resetEntryCooldowns(p);'), 'verify actual instance before reset'
assert 'p.resetDisabledSkills();' in t and 'p.resetTimeStamps();' in t and 'p.sendPacket(new SkillCoolTime(p));' in t
assert 'getItemReuseTimeStamps' not in t and 'resetEntryCooldowns' not in t[t.index('private static void finish(Session s)'):], 'no end/recovery cooldown reset'
assert 'f = trainingProfile(profile);' in t and 'int level = 85;' in t
assert 'human.getLevel()' not in t and 'getEnchantLevel()' not in t and 'setEnchantLevel(' not in t, 'no human gear or enchant mirroring'
assert t.index('b.giveAvailableSkills(true, true, true)') < t.index('BotSpawner.applySkillEnchants(b)') < t.index('BotSpawner.equipGear(b, preset)')

assert 'participantSession' in t and '((Summon)actor).getOwner()' in t, 'pet offense follows participant gate'
assert 'до 50%' in t and 'GM: выключите invul' in t and 'Уже идёт подготовка другой дуэли' in t
assert 'Все 8 тренировочных арен заняты' in t and 'accounts/__training_arena__' in t
assert 'return !requested || hasDuel && finished && !playerInDuel && !botInDuel && !managerHasDuel' in t
assert 'ThreadPool.scheduleAtFixedRate(TrainingArena::pulse, 100, 100)' in t

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
