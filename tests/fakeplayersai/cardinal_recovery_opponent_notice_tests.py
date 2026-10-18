#!/usr/bin/env python3
"""Compile all custom/effect scripts, test recovery filters and arrival notice gates."""
from pathlib import Path
import os
import subprocess
import tempfile
R=Path(__file__).resolve().parents[2]
T=Path(__file__).parent
m=(R/'game/data/scripts/custom/fakeplayersai/BotManager.java').read_text()
n=(R/'game/data/scripts/custom/fakeplayersai/OlympiadOpponentNotice.java').read_text()
a=m[m.index('public static synchronized Player spawnBot('):m.index('private static Player loadBot(')]
assert a.index('BotRosterRecovery.findCardinal(preset)')<a.index('final List<Map.Entry<Integer, Integer>> candidates')<a.index('BotSpawner.generateClassNickname(preset)')
assert 'BOT_CLASSES.putIfAbsent(candidate.objectId(), preset.getClassId())' in a
assert 'OlympiadOpponentNotice.init();' in m and 'OlympiadOpponentNotice.announce(player);' in m
assert 'ON_CREATURE_TELEPORTED' in n and '250, 1000, 2000' in n
assert 'if (!arrivalCandidate(player) || announce(player)) return;' in n
assert 'olympiad.getOlympiadGames().get(id) != game' in n
assert all(x not in n for x in ['setTarget(', 'setOlympiadStart(', 'setIntention', 'broadcastToAll'])
with tempfile.TemporaryDirectory(prefix='arrival51-') as directory:
    d = Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp = os.pathsep.join(map(str, sorted((R / 'libs').glob('*.jar'))))
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'CardinalRecoveryOpponentNoticeTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            result.check_returncode()
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.CardinalRecoveryOpponentNoticeTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile')
