#!/usr/bin/env python3
"""Structure68: passive barrier/25% Trance and mandatory rear-only Bluff regressions."""
from pathlib import Path
import os, subprocess, tempfile, shutil
R=Path(__file__).resolve().parents[2];T=Path(__file__).parent
S=R/'game/data/scripts/custom/fakeplayersai'
tactics=(S/'BotTactics.java').read_text();manager=(S/'BotManager.java').read_text()
survival=(S/'CardinalSurvival.java').read_text();chance=(S/'BotControlChance.java').read_text()
assert 'live(bot,5576)' in survival and '!ownCelestial(bot)' in survival and 'live(bot,1418)' in survival
assert 'CardinalSurvival.passiveTrance(bot, enemy)' in tactics
assert 'int chance=id==1394 ? 25 : 15;' in chance and 'ThreadLocalRandom' not in chance
for signature in ['private static void combatTick(', 'private static void olympiadChaseTick(']:
    section=manager[manager.index(signature):]
    assert section.index('BotTactics.tickBluffRear(')<section.index('BotTactics.tickInvulnerableMelee(')
assert 'TacticalSettings.isEnabled() || BotTactics.isOwned(bot.getObjectId())' in manager
recovery=(S/'BotRecoveryRules.java').read_text()
assert 'EventType.ON_CREATURE_ATTACK' in recovery and 'arenaManaged(event.getAttacker())' in recovery
for name in ['Backstab', 'FatalBlow']:
    effect=(R/f'game/data/scripts/handlers/skill/effects/{name}.java').read_text()
    assert effect.index('blockedBluffStrike(')<effect.index('Formulas.calcBackstabDamage(' if name=='Backstab' else 'Formulas.calcBlowDamage(')
print('PASS: passive/own shield distinction, 25% policy, Bluff priority/owner and native event/execution guard wiring',flush=True)
with tempfile.TemporaryDirectory(prefix='passive-bluff68-') as directory:
    d=Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    jar=d/'GameServer.jar';shutil.copyfile(R/'libs/GameServer.jar',jar)
    for kind,installer,helper in [('debuff-no-refresh','InstallDebuffGuard','DebuffCategoryGuard'),('arena-boundary','InstallArenaBoundary','ArenaBoundaryGuard')]:
        base=R/'game/core-patches'/kind
        subprocess.run(['java','--class-path','.',str(base/(installer+'.java')),str(jar),str(base/(helper+'.java'))],cwd=R,check=True,capture_output=True,text=True,timeout=180)
    cp = os.pathsep.join([str(jar),*map(str, sorted((R / 'libs').glob('*.jar')))])
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/handlers/skill/targets').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'BerserkerSoulCleanseTest.java', 'DominatorPressureBluffTest.java', 'DaggerRearApproachTest.java', 'DominatorCpTitanPressureRegression.java', 'CpMeleeRushRecoveryTest.java', 'PostSleepSummonTest.java', 'DominatorControlShadowTest.java', 'ArenaInvulnerabilityClarityTest.java', 'CardinalSurvivalControlChanceTest.java', 'NativeDebuffEpicTest.java', 'TankMechanicsKiteTest.java', 'CombatPriorityAuditTest.java', 'ClarityBluffPhaseTest.java', 'ControlPursuitTranceKiteTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            raise SystemExit(result.returncode)
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    for name in ['ControlPursuitTranceKiteTest','CardinalSurvivalControlChanceTest','ArenaOrbitTranceManaTest','NativeDebuffEpicTest','ClarityBluffPhaseTest','TankMechanicsKiteTest','CombatPriorityAuditTest']:
        print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp, 'custom.fakeplayersai.'+name, str(R / 'game'), *([str(d/'audit68.md')] if name=='NativeDebuffEpicTest' else [])], R / 'game'), flush=True)
    print('PASS: all custom/effect scripts compile')
