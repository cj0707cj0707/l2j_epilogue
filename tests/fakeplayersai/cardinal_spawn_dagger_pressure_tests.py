#!/usr/bin/env python3
"""Exact shipped spawnAll body against inert actor creation; real dagger step denial.
No production DB, world entry or geodata movement.
"""
from pathlib import Path
import os,subprocess,tempfile
R=Path(__file__).resolve().parents[2];S=R/'game/data/scripts/custom/fakeplayersai';T=Path(__file__).parent
m=(S/'BotManager.java').read_text();t=(S/'BotTactics.java').read_text();sp=(S/'BotSpawner.java').read_text()
def method(text,signature):
    a=text.index(signature);b=text.index('{',a);level=1;c=b+1
    while level:
        if text[c]=='{':level+=1
        elif text[c]=='}':level-=1
        c+=1
    return text[a:c]
assert 'BotCount=19' in (R/'game/config/Custom/FakePlayersAI.ini').read_text()
step=method(t,'private static boolean step(')
assert step.index('TacticsPolicy.daggerStepAllowed(')<step.index('final boolean recovery')<step.index('ArenaCombatRules.kitePoint(')
assert 'enemy.getHeading()' not in step and '85 * Math.cos' not in step and '2500' not in step
assert 'fixedClassNames(desired).stream().anyMatch(n -> n.equalsIgnoreCase(current))' in sp
assert 'owners.contains(e.getKey()) ? 0 : 1' in m
registration=method(m,'private static Player registerBot(')
assert registration.index('BOTS.put(')<registration.index('BOT_CLASSES.put(')<registration.index('REGISTRY_NAMES.put(')<registration.index('saveRegistry();')
assert registration.count('saveRegistry();')==1 and 'if (renamed)' not in registration
assert 'saveRegistry();' not in method(m,'public static synchronized Player spawnBot(')
with tempfile.TemporaryDirectory(prefix='cardinal-dagger46-') as d:
    d=Path(d);compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    test=d/'CardinalSpawnDaggerPressureTest.java'
    test.write_text((T/test.name).read_text().replace('// INSERT_SPAWN_ALL',method(m,'public static synchronized int spawnAll()')).replace('// INSERT_MISSING',method(m,'private static List<BotClassPreset> missingRosterPresets()')).replace('// INSERT_SAVE',method(m,'public static void saveRegistry()')))
    cp=os.pathsep.join(map(str,sorted((R/'libs').glob('*.jar'))))
    sources=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java')),*[T/n for n in ['LowManaAutoAttackTest.java','CombatRecoveryRulesTest.java','ArenaOrbitTranceManaTest.java']],test]
    def run(cmd,cwd):
        p=subprocess.run(cmd,cwd=cwd,capture_output=True,text=True,timeout=180)
        if p.returncode:print(p.stdout[-4000:]);print(p.stderr[-6000:]);p.check_returncode()
        return p.stdout.strip()
    run(['java',str(compiler),'-cp',cp,'-d',str(d/'classes'),*map(str,sources)],R)
    print(run(['java','-cp',str(d/'classes')+os.pathsep+cp,'custom.fakeplayersai.CardinalSpawnDaggerPressureTest',str(R/'game')],R/'game'))
    print('PASS: all custom/effect scripts compile; no live DB/world spawn; shared ranked/training step gate precedes all optional retreat paths')
