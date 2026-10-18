#!/usr/bin/env python3
"""Compile all shipped custom/effect scripts; test the actual Soul Cleanse emergency path via real core actors.
The cast/target recording seams are inert; no production DB, client or native cast scheduler is started.
"""
from pathlib import Path
import os
import subprocess
import tempfile

R = Path(__file__).resolve().parents[2]
T = Path(__file__).parent
# Source wiring: urgent check must precede both reaction/phase and root early-outs.
t = (R / 'game/data/scripts/custom/fakeplayersai/BotTactics.java').read_text()
m = (R / 'game/data/scripts/custom/fakeplayersai/BotManager.java').read_text()
a = t[t.index('private static boolean act('):t.index('private static void restoreCardinalMana(')]
assert a.index('if (soulCleanse(') < a.index('time < s.nextDecision') < a.index('trainingRushAction(')
drive = m[m.index('private static boolean driveBerserker('):m.index('private static boolean driveBerserkerStep(')]
assert drive.index('OlympiadCombatGate.ready(bot)') < drive.index('BotTactics.tickSoulCleanse(') < drive.index('now < state.nextDecisionAt')
assert 'bot.isMovementDisabled()' in m[m.index('private static boolean driveBerserkerStep('):]
assert '1510, // Soul Cleanse:' in m
assert '!bot.isCastingSimultaneouslyNow() && !BotTactics.isOwned(bot.getObjectId())' in m
assert 'if (skill.getId() == SOUL_CLEANSE_ID) return null;' in t
helper = t[t.index('private static boolean soulCleanse('):t.index('private static void claim(')]
assert helper.index('bot.setTarget(bot);') < helper.index('skill.checkCondition(bot, bot, false)') < helper.index('bot.useMagic(skill, true, true)')
assert 'bot.isRooted() || bot.isPhysicalMuted()' in helper and 'bot.isMovementDisabled()' not in helper
assert all(x not in helper for x in ['applyEffects(', 'stopSkillEffects(', 'enableSkill(', 'setCurrentMp(', 'abortCast('])

with tempfile.TemporaryDirectory(prefix='cleanse48-') as directory:
    d = Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    cp = os.pathsep.join(map(str, sorted((R / 'libs').glob('*.jar'))))
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'BerserkerSoulCleanseTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            result.check_returncode()
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.BerserkerSoulCleanseTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile')
