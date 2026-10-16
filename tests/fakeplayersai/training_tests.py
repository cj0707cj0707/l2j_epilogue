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
for forbidden in ('p.addItem(', 'p.addExpAndSp(', 'p.giveAvailableSkills(', 'p.getInventory().destroy'):
    assert forbidden not in t, forbidden
assert t.index('save(p.getObjectId(), j); // MUST') < t.index('s.bot = createBot(') < t.index('p.teleToLocation(')
assert 'DuelManager.getInstance().addDuel(p, b, 0)' in t
assert 'doSurrender(' not in t, 'cleanup must never reverse a pending natural winner or fabricate a quit loss'
assert 'BotTactics.forget(s.bot.getObjectId())' in t
assert 'if (!BotTactics.tick(b, p, preset, false))' in t and 'OlympiadChase.updateTraining(' in t
assert 'b.getAI().setIntentionAttack(p)' in t
admission = t[t.index('private static String admissionRefusal'):t.index('public static String start(')]
assert 'getCurrentHp' not in admission and 'getCurrentMp' not in admission and 'getCurrentCp' not in admission, 'no resource entry minimum'
assert 'Player.class.getDeclaredField("_startingDuel")' in t and 'if (player == null || field == null) return true;' in t
assert 'startingDuelPending(p, STARTING_DUEL)' in t and 'p.isInBoat()' in admission
assert 'if (!p.canDuel() || !b.canDuel())' in t, 'real nonresource/core validation after acknowledged entry'
assert 'BX = 44112, BY = 188116, HX = 42705, HY = 188869' in t
assert 'CX = 43409, CY = 188493, CZ = -3513' in t and 'RADIUS = 1000' in t
assert 'BotSpawner.enterWorld(s.bot, BX, BY, CZ)' in t and 'p.teleToLocation(HX, HY, CZ' in t
assert t.index('save(p.getObjectId(), j); // MUST') < t.index('p.stopAllEffects(); s.bot.stopAllEffects();') < t.index('restoreResources(p); restoreResources(s.bot);') < t.index('p.teleToLocation(HX, HY, CZ') < t.index('if (!p.canDuel() || !b.canDuel())') < t.index('DuelManager.getInstance().addDuel(p, b, 0)')
assert 'getCurrentCp() <' not in t and 'getMaxHp() - 1' not in t, 'no stricter full-resource gate'
assert 'p.isJailed()' in t and 'p.isAlikeDead()' in t
assert 'ON_CREATURE_ATTACK' in t and 'ON_CREATURE_SKILL_USE' in t and 'FunctionEventListener' in t
assert 'new TerminateReturn(true, true, true)' in t, 'synchronous offensive prep rejection'
assert 'e.getSkill().hasNegativeEffect()' in t and 'e.getSkill().isDebuff()' in t
assert 'e.getSkill().isSuicideAttack()' in t and 'e.getSkill().nextActionIsAttack()' in t
assert 'target != e.getCaster()' in t and 'e.getCaster() == s.player' in t, 'self-only participant casts, no pet/offense'
assert 's.preparationStarted = true' in t and 'preparationSeconds(TacticalSettings.getTrainingPreparationSeconds())' in t
assert 'Math.max(10, Math.min(60, configured))' in t
assert 'applyTrainingOlympiadBuffs(p,' in t and 'applyTrainingOlympiadBuffs(b,' in t
assert 'maintainFullBuffs(' not in t, 'training must not select world buff branch'
assert 'stopSkillEffects(' not in t and t.count('stopAllEffects()') == 2 and 'savePlayerConditions(' not in t, 'only explicit entry stripping, no guessed restoration'
assert t.index('getDuelState() != Duel.DUELSTATE_DUELLING)) return;') < t.index('applyTrainingOlympiadBuffs(p,')
assert t.index('p.enableAllSkills(); b.enableAllSkills(); // both') < t.index('s.preparationUntil = System.nanoTime()') < t.index('s.preparationStarted = true')
prep = t[t.index('if (!s.active)\n        {', t.index('private static void tick')):t.index('if (now < s.nextAction)')]
assert 's.finalizing = true;' in prep and 's.preparationUntil - 1_000_000_000L' in prep
assert 's.preparationStarted && !s.finalizing' in t and 'e.getCaster() == s.player || e.getCaster() == s.bot' in t
assert 'BotSpawner.applyTrainingPreparationSelfBuffs(' in prep
assert prep.index('if (!s.finalizing)') < prep.index('BotSpawner.applyTrainingPreparationSelfBuffs(')
assert 'boolean casting = castsRunning(p) || castsRunning(b);' in prep
assert 'if (!finalSnapshotReady(phaseNow, s.preparationUntil, s.finalDrainUntil, casting)) return;' in prep
assert prep.index('s.finalSnapshotApplied = true;') < prep.index('restoreResources(p); restoreResources(b);') < prep.index('s.combatAt = System.nanoTime() + 1_000_000_000L;') < prep.index('s.active = true')
for forbidden in ('stop(p)', 'stop(b)', 'abortCast(', 'setIntentionIdle(', 'p.stopMove(', 'p.setTarget('):
    assert forbidden not in prep, 'no periodic prep cast interruption or AI stomp: ' + forbidden
assert 'System.nanoTime() < s.combatAt || castsRunning(p) || castsRunning(b)' in prep
stopping = t[t.index('if (s.stopping)\n        {\n            captureOfficialResult(s);'):t.index('if (!s.active)\n        {', t.index('private static void tick'))]
assert 'stopParticipantsOnce(s);' in stopping
for forbidden in ('stop(p)', 'stop(b)', 'setInvul(', 'setImmobilized(', 'setIntention', 'teleToLocation('):
    assert forbidden not in stopping, 'no repeated human mutation during core result drain: ' + forbidden
assert 'if (s.humanStopApplied) return;' in t and 's.humanStopApplied = true;' in t
assert 'if (!s.active) s.player.setImmobilized(false);' in t, 'release only training-owned prep hold'

assert 'BotTactics.allowsBasicAttack(b, preset)' in t and 'if (basicAttack &&' in t
assert 'boolean chaseAttack = basicAttack && preset.getClassId() != 131;' in t, 'training131 must reach strict skill range without attack hysteresis'
assert 'OlympiadChase.updateTraining(b, p, range, chaseAttack,' in t, 'dagger/disarm fallback cannot autoattack'
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

assert 'resetEntryCooldowns' not in t and 'entryCooldownReset' not in t
assert 'p.resetDisabledSkills();' in t and 'p.resetTimeStamps();' in t and 'p.sendPacket(new SkillCoolTime(p));' in t
assert 'getItemReuseTimeStamps' not in t and 'resetPreparationCooldowns' not in t[t.index('private static void finish(Session s)'):], 'no return cooldown reset'
assert t.count('resetPreparationCooldowns(p); resetPreparationCooldowns(b);') == 1, 'exactly one both-participant prep reset call site'
assert t.index('resetPreparationCooldowns(p); resetPreparationCooldowns(b);') < t.index('p.enableAllSkills(); b.enableAllSkills(); // both') < t.index('s.preparationUntil = System.nanoTime()')
assert 'resetPreparationCooldowns' not in prep and 'restoreForCombat' not in t, 'no second reset after selfbuffs'
assert t.count('BotSpawner.applyTrainingClassBuffs(') == 2
assert t.index('s.openersApplied = true;') < t.index('BotSpawner.applyTrainingClassBuffs(') < t.index('BotSpawner.applyPreCombatOpeners(') < t.index('s.active = true')
assert 'ON_CREATURE_TELEPORTED' in t and 'returnAcknowledged' in t
assert 'p.getAI().notifyActionCancel();' in t and 'p.getAI().setNextAction(null);' in t
assert 'j.setProperty("returnMotionSynced", "true"); save(p.getObjectId(), j);' in t
assert 'p.setLastServerPosition(p.getX(), p.getY(), p.getZ());' in t and 'new StopMove(p)' in t
assert t.index('GC ONLY: never alter a returned human again.') < t.index('synchronizeReturnOnce(p, j);'), 'GC-only completed return never mutates human'
assert 'insideJournalArena' in t and 'j.getProperty("arenaX", "630")' in t, 'old arena journals remain recoverable after relocation'
assert 'public static boolean isPreparingBot(Player bot)' in t and 's.bot == bot' in t
assert 'public static boolean isTrainingInstance(int instanceId)' in t
assert 'TrainingPeaceZones.install()' in t and 'if (!TrainingPeaceZones.isInstalled()) return' in t
assert t.index('TrainingPeaceZones.registerInstance(s.instance.getId())') < t.index('s.bot = createBot(') < t.index('BotSpawner.enterWorld(s.bot, BX, BY, CZ)')
assert 'if (!s.stopping && !zoneAuthority(s.instance.getId()))' in t, 'every prep/active pulse revokes stale zone authority'
assert 's.preparationStarted && (blockedZone(p) || blockedZone(b))' in t, 'entering any unauthorized zone stops combat/prep'
assert 'if (!s.duelRequested && (startingDuelPending(p, STARTING_DUEL) || p.isInDuel()' in t, 'a racing external pending core task also quarantines cleanup'

assert 'TrainingPeaceZones.isExcluded(zone, p.getInstanceId())' in t and 'ZoneManager.getInstance().getZones(p.getX(), p.getY(), p.getZ())' in t
assert 'TrainingPeaceZones.unregisterInstance(instanceId)' in t and 'Boolean.parseBoolean(j.getProperty("humanReturned", "false"))' in t
assert 'setInsideZone(' not in t and 'setZone(' not in t, 'no global harbor or raw zone-flag edits'


assert 'f = trainingProfile(profile);' in t and 'int level = 85;' in t
assert 'human.getLevel()' not in t and 'getEnchantLevel()' not in t and 'setEnchantLevel(' not in t, 'no human gear or enchant mirroring'
assert t.index('b.giveAvailableSkills(true, true, true)') < t.index('BotSpawner.applySkillEnchants(b)') < t.index('BotSpawner.equipGear(b, preset)')

assert 'participantSession' in t and '((Summon)actor).getOwner()' in t, 'pet offense follows participant gate'
assert 'GM: выключите invul' in t and 'Уже идёт подготовка другой дуэли' in t
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

assert 'BotSpawner.applyTrainingClassBuffs(p, BotPresets.getPresetByClassId(p.getPlayerClass().getId()));' in prep
assert 'p.getMaxHp() * trainingHpFraction(classId)' in t
assert 'p.getMaxCp() * trainingCpFraction(classId)' in t
assert 'p.setCurrentMp(p.getMaxMp());' in t
