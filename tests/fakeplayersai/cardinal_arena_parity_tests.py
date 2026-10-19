#!/usr/bin/env python3
"""Ranked/training common Cardinal path, real stat API/authority/cleanup, no live DB/client."""
from pathlib import Path
import os
import subprocess
import tempfile
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'game/data/scripts/custom/fakeplayersai'
t=(SRC/'BotTactics.java').read_text();m=(SRC/'BotManager.java').read_text();a=(SRC/'TrainingArena.java').read_text()
support=(SRC/'CardinalArenaSupport.java').read_text()
def body(source,signature):
    pos=source.index(signature);left=source.index('{',pos);right=left+1;depth=1
    while depth:
        depth+=(source[right]=='{')-(source[right]=='}');right+=1
    return source[pos:right]
tick=body(t,'public static synchronized boolean tick(')
assert tick.index('!valid(')<tick.index('return act(bot, enemy, preset, s, time, false)')
assert 's.cardinalSupport.start(bot, enemy, s.combatStartedAt, time)' in body(t,'private static void prepare(')
assert 'BotTactics.tick(b, p, preset, false)' in a and 'BotTactics.tick(bot, opponent, preset, true)' in m
assert 'getCurrentMp' not in support and 'setCurrentMp' not in support
assert 'REGENERATE_MP_RATE' not in support and '0.10' not in support
assert 'skill.applyEffects(enemy,enemy,true,seconds)' in support
assert 'old.removeStatsOwner(this)' in support
assert 's.cardinalSupport.clear();' in body(t,'private static void prepare(')
assert 'removed.cardinalSupport.clear();' in body(t,'public static synchronized void forget(')
assert 'e.getValue().cardinalSupport.clear();' in body(t,'private static State state(')
assert 'forget(STATES.keySet().iterator().next())' in t
assert 'BotTactics.forget' in body(a,'private static void stopParticipantsOnce(')
assert 'BotTactics.forget' in body(m,'public static synchronized void despawnBot(')
assert 'BotTactics.forget' in body(m,'private static boolean holdOlympiadIfNotReady(')
combat=body(m,'private static void combatTick(')
assert combat.index('BotTactics.isOwned(bot.getObjectId()))\n\t\t\t\t{\n\t\t\t\t\tBotSpawner.maintainOlympiadSummon')<combat.index('WorldObject target = bot.getTarget()'), 'ownership yield must precede all target repairs'
fast=body(m,'private static void olympiadChaseTick(')
assert 'if (!BotTactics.isOwned(bot.getObjectId()) && !bot.isCastingNow() && !bot.isCastingSimultaneouslyNow()) bot.setTarget(opponent);' in fast
assert 'holdOlympiadIfNotReady(bot)' in fast and fast.index('holdOlympiadIfNotReady(bot)')<fast.index('BotTactics.tick(')
refill=body(m,'private static boolean olympiadMpFloorAllowed(')
assert 'return false;' in refill
assert 'SETTINGS.isRestoreMp() && bot.getPlayerClass().getId() == 97' in m
reach=body(t,'public static synchronized int tacticalApproachRange(')
assert reach.index('!resources(bot, skill)')<reach.index('tranceReach = range')
assert 'skill.checkCondition(bot, enemy, false)' in reach # only ordinary readiness/conditions contribute
assert 'TacticsPolicy.cardinalSleepException(preset.getClassId(), id)' in reach
assert 'TacticsPolicy.cardinalApproachRange(preset.getClassId(), reach, tranceReach)' in reach
assert 'nextCardinalTrance' not in t, 'Trance availability must use native cooldown, not a second bot-only timer'
act=body(t,'private static boolean act(')
assert act.count('restoreCardinalMana(bot, preset);') == 2, 'refill both casting and normal ticks'
casting=act[act.index('if (bot.isCastingNow()'):act.index('if (!defenseOnly && !allowsBasicAttack')]
assert casting.index('pollAttempt(')<casting.index('restoreCardinalMana(')<casting.index('return true;')
normal=act[act.index('if (!defenseOnly && !allowsBasicAttack'):]
assert normal.index('pollAttempt(')<normal.index('restoreCardinalMana(')<normal.index('time < s.nextDecision')<normal.index('resources(bot, skill)')
mana=body(t,'private static void restoreCardinalMana(')
for guard in ['preset.getClassId() != CARDINAL_CLASS_ID','bot.getClient() != null',
              'bot.getPlayerClass().getId() != CARDINAL_CLASS_ID','!bot.isOnline()','bot.isAlikeDead()',
              'maximum > 0 && bot.getCurrentMp() < maximum','bot.setCurrentMp(maximum)']:
    assert guard in mana,guard
assert 'ranked' not in mana and 'setCurrentHp' not in mana and 'setCurrentCp' not in mana
for guard in ['bot.isCastingNow() || bot.isCastingSimultaneouslyNow()','resources(bot, skill)','skill.checkCondition(bot, self ? bot : enemy, false)','!bot.isSkillDisabled(skill)','bot.useMagic(skill, true, true)']:
    assert guard in act,guard
print('PASS: common ranked/training driver and bound effect; parent target ownership; actual reuse/resource/LOS checks; every cleanup path wired',flush=True)
with tempfile.TemporaryDirectory(prefix='cardinal-parity-') as d:
    tmp=Path(d);compiler=tmp/'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp=os.pathsep.join(str(p) for p in sorted((ROOT/'libs').glob('*.jar')))
    sources=[ROOT/'game/data/scripts/handlers/EffectMasterHandler.java',
             *sorted((ROOT/'game/data/scripts/handlers/skill/effects').glob('*.java')),
             *sorted((ROOT/'game/data/scripts/custom').rglob('*.java')),
             Path(__file__).with_name('CardinalArenaParityTest.java')]
    subprocess.run(['java',str(compiler),'-cp',cp,'-d',str(tmp),*map(str,sources)],cwd=ROOT,check=True,timeout=180)
    run=subprocess.run(['java','-cp',str(tmp)+os.pathsep+cp,'custom.fakeplayersai.CardinalArenaParityTest',str(ROOT/'game')],cwd=ROOT/'game',capture_output=True,text=True,timeout=120)
    if run.returncode:
        print(run.stdout);print(run.stderr);run.check_returncode()
    assert 'SEVERE:' not in run.stderr and 'No skill info found' not in run.stderr,run.stderr
    print(run.stdout.strip())
