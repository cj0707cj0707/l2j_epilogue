#!/usr/bin/env python3
"""Low-MP physical fallback wiring plus real JAR predicates/stats; no DB/GameServer."""
from pathlib import Path
import os, subprocess, tempfile
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'game/data/scripts/custom/fakeplayersai'
def body(source,signature):
    p=source.index(signature);a=source.index('{',p);b=a+1;depth=1
    while depth:
        depth+=(source[b]=='{')-(source[b]=='}');b+=1
    return source[p:b]
t=(SRC/'BotTactics.java').read_text();m=(SRC/'BotManager.java').read_text()
training=(SRC/'TrainingArena.java').read_text();support=(SRC/'ManaAutoAttackSupport.java').read_text()
act=body(t,'private static boolean act(')
assert act.index('pollAttempt(')<act.index('final boolean manaFallback')
assert act.index('bot.isOutOfControl() || bot.isAllSkillsDisabled()')<act.index('final boolean manaFallback')
assert act.index('s.pendingId != 0) { return')<act.index('final boolean manaFallback')
assert 'trainingBerserker && !manaFallback && !recoveryReady ? trainingRushAction' in act
assert act.index('if (manaFallback && selected < 0 && !sleeping)')<act.index('step(bot, enemy')
assert 's.moveUntil = 0; return false;' in act
for method in ['public static synchronized boolean tick(','public static synchronized boolean tickDefense(']:
    b=body(t,method);assert b.index('!valid(')<b.index('s.manaAutoAttack.update(bot, preset)')
assert 'removed.manaAutoAttack.clear()' in body(t,'public static synchronized void forget(')
assert 'e.getValue().manaAutoAttack.clear()' in body(t,'private static State state(')
assert 's.manaAutoAttack.clear()' in body(t,'private static void prepare(')
assert 'needsManaAutoAttack(bot, preset) && allowsBasicAttack(bot, preset)' in body(t,'public static synchronized boolean recoveryOnly(')
assert 'bot.getStat().getMpInitialConsume(skill) + bot.getStat().getMpConsume(skill)' in body(t,'public static boolean needsManaAutoAttack(')
assert 'Stat.BOW_MP_CONSUME_RATE, 0x70, this, 0, null' in support
for forbidden in ['setCurrentMp','MP_CONSUME,','setCurrentHp','setXYZ','applyEffects','enableSkill']:
    assert forbidden not in support,forbidden
assert 'BotTactics.maintainWorldManaFallback(bot, preset)' in body(m,'private static void retargetTick(')
retarget=body(m,'private static void retargetTick(')
assert '!bot.isCastingSimultaneouslyNow()' in retarget
assert 'intention != Intention.CAST || BotTactics.needsManaAutoAttack(bot, preset)' in retarget
world=body(t,'public static synchronized void maintainWorldManaFallback(')
for guard in ['!BotManager.isBot(bot)','bot.isInOlympiadMode()','isAutoAttackable(bot)','getInstanceId() == bot.getInstanceId()']:
    assert guard in world,guard
assert 'BotTactics.allowsBasicAttack(b, preset)' in training
assert 'GeoEngine.getInstance().canSeeTarget(b, p)' in training and '!p.isSleeping()' in training
fast=body(m,'private static void olympiadChaseTick(')
for guard in ['OlympiadCombatGate.ready(bot)','opponent.isAutoAttackable(bot)','!opponent.isSleeping()','GeoEngine.getInstance().canSeeTarget(bot, opponent)']:
    assert guard in fast,guard
driver=body(m,'private static boolean driveBerserkerStep(')
assert 'if (!manaFallback && driveOwnRushStunLoop(' in driver
assert driver.index('state.movement.rushPending()')<driver.index('if (manaFallback)')
assert driver.index('if (bot.isCastingSimultaneouslyNow()) return true;')<driver.index('if (manaFallback)')<driver.index('BotTactics.recoveryOnly(')
assert 'return false; // Parent owns ordinary armed pursuit/attack' in driver
assert 'if (bot.getAI().getIntention() == Intention.CAST) bot.getAI().setIntentionIdle();' in driver
print('PASS: common fallback before recovery/kite; real cast/pending/sleep/LOS gates; trained/ranked/world wiring; physical floor exclusion; scoped bow lifecycle',flush=True)
with tempfile.TemporaryDirectory(prefix='low-mana-') as d:
    tmp=Path(d);compiler=tmp/'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp=os.pathsep.join(str(p) for p in sorted((ROOT/'libs').glob('*.jar')))
    sources=[ROOT/'game/data/scripts/handlers/EffectMasterHandler.java',
             *sorted((ROOT/'game/data/scripts/handlers/skill/effects').glob('*.java')),
             *sorted((ROOT/'game/data/scripts/custom').rglob('*.java')),
             Path(__file__).with_name('LowManaAutoAttackTest.java')]
    subprocess.run(['java',str(compiler),'-cp',cp,'-d',str(tmp),*map(str,sources)],cwd=ROOT,check=True,timeout=180)
    run=subprocess.run(['java','-cp',str(tmp)+os.pathsep+cp,'custom.fakeplayersai.LowManaAutoAttackTest',str(ROOT/'game')],cwd=ROOT/'game',capture_output=True,text=True,timeout=120)
    if run.returncode:
        print(run.stdout);print(run.stderr);run.check_returncode()
    assert 'SEVERE:' not in run.stderr and 'No skill info found' not in run.stderr,run.stderr
    print(run.stdout.strip())