#!/usr/bin/env python3
"""Actual native Chance-route audit and epic jewelry migration; no live world/DB."""
from pathlib import Path
import os,subprocess,tempfile
R=Path(__file__).resolve().parents[2];T=Path(__file__).parent;S=R/'game/data/scripts/custom/fakeplayersai'
def run(cmd,cwd):
 p=subprocess.run(cmd,cwd=cwd,capture_output=True,text=True,timeout=240)
 if p.returncode:print(p.stdout[-4000:]);print(p.stderr[-6000:]);raise SystemExit(p.returncode)
 return p.stdout.strip()
# Structure67: broad fixed gates stay removed; scoped Trance25%, Dominator controls15%. Dagger/tank-defense rules stay untouched.
m=(S/'BotManager.java').read_text();tr=(S/'CardinalTrance.java').read_text();sp=(S/'BotSpawner.java').read_text();tc=(R/'game/data/scripts/handlers/skill/effects/TargetCancel.java').read_text()
assert 'FIXED_DEBUFF_RATE_PLAYERS.add' not in m and 'isTankBotDebuffSuccess' not in m+tc
assert 'BotControlChance.apply(bot)' in tr
assert 'minChance' not in tr and 'maxChance' not in tr and 'new Skill(' not in tr
assert 'return Formulas.calcProbability(_chance, effector, effected, skill);' in tc
assert sp.index('if (debuffChance > 0) targetLevel')<sp.index('else if (damage) targetLevel')
assert 'ensurePresetJewelry(player);' in sp and 'item.updateDatabase();' in sp
with tempfile.TemporaryDirectory(prefix='native-debuff-epic-') as temp:
 d=Path(temp); compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
 cp=os.pathsep.join(map(str,sorted((R/'libs').glob('*.jar'))))
 sources=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java')),*[T/n for n in ['LowManaAutoAttackTest.java','CombatRecoveryRulesTest.java','ArenaOrbitTranceManaTest.java','NativeDebuffEpicTest.java']]]
 run(['java',str(compiler),'-cp',cp,'-d',str(d/'classes'),*map(str,sources)],R)
 print(run(['java','-cp',str(d/'classes')+os.pathsep+cp,'custom.fakeplayersai.NativeDebuffEpicTest',str(R/'game'),str(d/'audit.md')],R/'game'))
 print('PASS: all custom/effect scripts compile; native skill/class audit generated')
 if os.environ.get('NATIVE_DEBUFF_AUDIT'):
  Path(os.environ['NATIVE_DEBUFF_AUDIT']).write_text((d/'audit.md').read_text())
  Path(os.environ['NATIVE_DEBUFF_AUDIT']+'.changes.md').write_text((d/'audit.md.changes.md').read_text())
