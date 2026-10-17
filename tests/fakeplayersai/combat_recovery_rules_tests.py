#!/usr/bin/env python3
"""Five combat requests: actual JAR/effect/event execution plus production wiring."""
from pathlib import Path
import os,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2];SRC=ROOT/'game/data/scripts/custom/fakeplayersai'
def body(source,signature):
    p=source.index(signature);a=source.index('{',p);b=a+1;depth=1
    while depth:depth+=(source[b]=='{')-(source[b]=='}');b+=1
    return source[p:b]
t=(SRC/'BotTactics.java').read_text();m=(SRC/'BotManager.java').read_text()
s=(SRC/'BotSpawner.java').read_text();r=(SRC/'BotRecoveryRules.java').read_text()
act=body(t,'private static boolean act(');step=body(t,'private static boolean step(')
assert 'TacticsPolicy.soultakerKite(meleeOpponent(enemy)' in step and 'preset.getClassId() == 95 ? 800' in step
assert 'BotRecoveryRules.selfHealAllowed(bot, skill)' in act
assert 'hp <= .40' in act and '!recoveryReady ? trainingRushAction' in act
assert act.index('recoveryChoice >= 0 ? recoveryChoice')<act.index('if (selected >= 0)')
assert 'enemy.isMageClass())' in act[act.index('final int tankSelected'):act.index('final int tankChoice')]
defense=body(t,'public static synchronized boolean tickDefense(')
assert defense.index('pollAttempt(')<defense.index('getCurrentHpPercent() >= 65')
driver=body(m,'private static boolean driveBerserkerStep(')
assert driver.index('!state.movement.rushPending()')<driver.index('BotTactics.tickDefense(')<driver.index('driveOwnRushStunLoop(')
assert 'BotRecoveryRules.applyPhoenixSacrifice(player);' in body(s,'public static int applySkillEnchants(')
assert '760, VENGEANCE_ID' in s and 'set("reuseDelay", 0)' in r
assert 'BotRecoveryRules.init()' in m and 'BotRecoveryRules.init()' in (SRC/'TrainingArena.java').read_text()
for name in ['HealPercent','HealOverTime','CpHeal','CpHealPercent','CpHealOverTime','HpByLevel','RebalanceHP','HpDrain']:
    effect=(ROOT/f'game/data/scripts/handlers/skill/effects/{name}.java').read_text()
    assert 'BotRecoveryRules.blockedSelfHeal' in effect,name
print('PASS: all five production paths wired; common+ranked/training recovery; native no-overlap/cast/resource gates retained',flush=True)
with tempfile.TemporaryDirectory(prefix='combat-recovery-') as d:
    tmp=Path(d);compiler=tmp/'Compile.java';javap=tmp/'Inspect.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    javap.write_text('class Inspect{public static void main(String[]a){System.exit(java.util.spi.ToolProvider.findFirst("javap").orElseThrow().run(System.out,System.err,a));}}')
    cp=os.pathsep.join(str(p) for p in sorted((ROOT/'libs').glob('*.jar')))
    core=subprocess.check_output(['java',str(javap),'-classpath',cp,'-c','-p','org.l2jmobius.gameserver.entity.actor.Creature'],text=True)
    invul=core.split('public boolean isInvul();',1)[1].split('public void setMortal(',1)[0]
    assert 'EffectFlag.INVUL:' in invul
    assert 'EffectFlag.INVUL.getMask()' in (ROOT/'game/data/scripts/handlers/skill/effects/Invincible.java').read_text()
    begin=core[core.index('Field org/l2jmobius/gameserver/mechanics/events/EventType.ON_CREATURE_SKILL_USE:'):]
    assert begin.index('EventDispatcher.notifyEvent:')<begin.index('TerminateReturn.terminate:')<begin.index('getMpInitialConsume:')
    sources=[ROOT/'game/data/scripts/handlers/EffectMasterHandler.java',
             *sorted((ROOT/'game/data/scripts/handlers/skill/effects').glob('*.java')),
             *sorted((ROOT/'game/data/scripts/custom').rglob('*.java')),
             Path(__file__).with_name('LowManaAutoAttackTest.java'),Path(__file__).with_name('CombatRecoveryRulesTest.java')]
    compile=subprocess.run(['java',str(compiler),'-cp',cp,'-d',str(tmp),*map(str,sources)],cwd=ROOT,capture_output=True,text=True,timeout=180)
    if compile.returncode:print(compile.stdout);print(compile.stderr);raise SystemExit(compile.returncode)
    run=subprocess.run(['java','-cp',str(tmp)+os.pathsep+cp,'custom.fakeplayersai.CombatRecoveryRulesTest',str(ROOT/'game')],cwd=ROOT/'game',capture_output=True,text=True,timeout=120)
    if run.returncode:print(run.stdout[-3000:]);print(run.stderr[-5000:]);raise SystemExit(run.returncode)
    assert 'SEVERE:' not in run.stderr and 'No skill info found' not in run.stderr,run.stderr[-3000:]
    print(run.stdout.strip())