#!/usr/bin/env python3
"""Latest three fixes: real overrides/skills/effects + source wiring, no server/DB."""
from pathlib import Path
import os,subprocess,tempfile
R=Path(__file__).resolve().parents[2];S=R/'game/data/scripts/custom/fakeplayersai';T=Path(__file__).parent
m=(S/'BotManager.java').read_text();t=(S/'BotTactics.java').read_text();sp=(S/'BotSpawner.java').read_text()
a=sp[sp.index('public static int[] getTacticalCpSupport'):sp.index('private static int[][] buildOlympiadProfile')]
assert 'dominatorPresetAllows' not in a and 'getKnownSkill' in a
for text in [m,(S/'TrainingArena.java').read_text()]:assert 'BotTactics.strictMeleePressure(' in text
assert 'if (chargeClosing && !self(kind)) { continue; }' in t
assert 'chargeClosing && !bot.isMovementDisabled() && !bot.isOutOfControl()) return false' in t
assert 'TacticsPolicy.chargeMeleeRecoveryAllowed(strictMeleePressure(bot, enemy, preset)' in t
assert 'if (trainingRushActive && skill.getId() != 793)' in t
assert t.index('if (trainingRushActive && selected < 0)')<t.index('if (manaFallback && selected < 0')
for name in ['enumaDone','crushingDone','disarmDone']:
    assert 'if (state.'+name+' && ' in m, 'burst latch must rearm on native reuse readiness'
with tempfile.TemporaryDirectory(prefix='cp-melee-rush-') as d:
    d=Path(d);compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}');cp=os.pathsep.join(map(str,sorted((R/'libs').glob('*.jar'))));classes=d/'classes'
    targets=[]
    for name in ('PartyClan','One'):
        path=d/(name+'.java');path.write_text(subprocess.check_output(['git','show','HEAD:game/data/scripts/handlers/skill/targets/'+name+'.java'],cwd=R,text=True));targets.append(path)
    sources=[*targets,T/'DominatorCpTitanPressureRegression.java',R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java')),*[T/n for n in ['LowManaAutoAttackTest.java','CombatRecoveryRulesTest.java','ArenaOrbitTranceManaTest.java','CpMeleeRushRecoveryTest.java']]]
    p=subprocess.run(['java',str(compiler),'-cp',cp,'-d',str(classes),*map(str,sources)],capture_output=True,text=True,timeout=180,cwd=R)
    if p.returncode:print(p.stderr[-6000:]);raise SystemExit(p.returncode)
    p=subprocess.run(['java','-cp',str(classes)+os.pathsep+cp,'custom.fakeplayersai.CpMeleeRushRecoveryTest',str(R/'game')],capture_output=True,text=True,timeout=120,cwd=R/'game')
    if p.returncode:print(p.stdout[-2000:]);print(p.stderr[-6000:]);raise SystemExit(p.returncode)
    print(p.stdout.strip())
    p=subprocess.run(['java','-cp',str(classes)+os.pathsep+cp,'custom.fakeplayersai.DominatorCpTitanPressureRegression'],capture_output=True,text=True,timeout=120,cwd=R/'game')
    if p.returncode:print(p.stdout[-2000:]);print(p.stderr[-6000:]);raise SystemExit(p.returncode)
    print(p.stdout.strip());print('PASS: all custom scripts compile; shared and both parent-driver wiring checked')
