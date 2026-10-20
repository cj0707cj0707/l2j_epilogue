#!/usr/bin/env python3
"""Structure79: Dominator tank-defense sleep; gear/menu coverage, Cardinal recovery and melee/control/Bluff regressions."""
from pathlib import Path
import os, subprocess, tempfile, shutil, argparse
parser=argparse.ArgumentParser()
parser.add_argument("--bluff-only",action="store_true",help="Run only the native rear-phase fixture after compiling all scripts")
parser.add_argument("--cardinal-only",action="store_true",help="Run Cardinal center/hold/proc public-hook regression only")
parser.add_argument("--trance-only",action="store_true",help="Run Cardinal75 all-opponent/Clarity/shield priority fixture only")
parser.add_argument("--gear-heal-only",action="store_true",help="Run structure78 gear/menu and Cardinal heal75 fixture only")
parser.add_argument("--pursuit-only",action="store_true",help="Run structure77 melee pursuit/native boundary fixture only")
parser.add_argument("--prominence-only",action="store_true",help="Run Cardinal76 strict90/10s healthy nuke priority fixture only")
parser.add_argument("--tank-sleep-only",action="store_true",help="Run structure79 Dominator tank-defense sleep fixture only")
options=parser.parse_args()
R=Path(__file__).resolve().parents[2];T=Path(__file__).parent
S=R/'game/data/scripts/custom/fakeplayersai'
tactics=(S/'BotTactics.java').read_text();manager=(S/'BotManager.java').read_text()
survival=(S/'CardinalSurvival.java').read_text();chance=(S/'BotControlChance.java').read_text()
assert 'live(bot,5576)' in survival and 'live(bot,1418)' in survival and 'static boolean noKite' in survival
assert 'cardinalTranceReady(bot,enemy,s,time)' in tactics
assert 'if(CardinalSurvival.noKite(bot))' in (S/'InvulnerableMeleeKite.java').read_text()
assert 'CardinalSurvival.tranceTarget(bot,enemy)' in tactics
assert 'int chance=id==1092 ? 15 : 20;' in chance and 'ThreadLocalRandom' not in chance
assert 'Formulas.FIXED_DEBUFF_RATE_PLAYERS.remove(bot.getObjectId())' in chance
assert all(f's.set("{key}",chance)' in chance for key in ['activateRate','minChance','maxChance'])
for signature in ['private static void combatTick(', 'private static void olympiadChaseTick(']:
    section=manager[manager.index(signature):]
    assert section.index('BotTactics.tickBluffRear(')<section.index('BotTactics.tickCardinalControl(')<section.index('BotTactics.tickInvulnerableMelee(')
assert 'TacticalSettings.isEnabled() || BotTactics.isOwned(bot.getObjectId())' in manager
recovery=(S/'BotRecoveryRules.java').read_text()
assert 'EventType.ON_CREATURE_ATTACK' in recovery and 'arenaManaged(event.getAttacker())' in recovery
for name in ['Backstab', 'FatalBlow']:
    effect=(R/f'game/data/scripts/handlers/skill/effects/{name}.java').read_text()
    assert effect.index('blockedBluffStrike(')<effect.index('Formulas.calcBackstabDamage(' if name=='Backstab' else 'Formulas.calcBlowDamage(')
print('PASS: both shield no-kite guards, all-opponent Trance priority, 20% sleep policy, Bluff priority/owner and native event/execution wiring',flush=True)
with tempfile.TemporaryDirectory(prefix='passive-bluff68-') as directory:
    d=Path(directory)
    compiler = d / 'Compile.java'
    compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    jar=d/'GameServer.jar';shutil.copyfile(R/'libs/GameServer.jar',jar)
    for kind,installer,helper in [('debuff-no-refresh','InstallDebuffGuard','DebuffCategoryGuard'),('arena-boundary','InstallArenaBoundary','ArenaBoundaryGuard')]:
        base=R/'game/core-patches'/kind
        subprocess.run(['java','--class-path','.',str(base/(installer+'.java')),str(jar),str(base/(helper+'.java'))],cwd=R,check=True,capture_output=True,text=True,timeout=180)
    cp = os.pathsep.join([str(jar),*map(str, sorted((R / 'libs').glob('*.jar')))])
    sources = [R / 'game/data/scripts/handlers/EffectMasterHandler.java', R / 'game/data/scripts/handlers/chat/commands/admin/AdminBot.java',
               *sorted((R / 'game/data/scripts/handlers/skill/effects').glob('*.java')),
               *sorted((R / 'game/data/scripts/handlers/skill/targets').glob('*.java')),
               *sorted((R / 'game/data/scripts/custom').rglob('*.java')),
               *[T / name for name in ['LowManaAutoAttackTest.java', 'CombatRecoveryRulesTest.java',
                                        'ArenaOrbitTranceManaTest.java', 'BerserkerSoulCleanseTest.java', 'DominatorPressureBluffTest.java', 'DaggerRearApproachTest.java', 'DominatorCpTitanPressureRegression.java', 'CpMeleeRushRecoveryTest.java', 'PostSleepSummonTest.java', 'DominatorControlShadowTest.java', 'ArenaInvulnerabilityClarityTest.java', 'CardinalSurvivalControlChanceTest.java', 'NativeDebuffEpicTest.java', 'TankMechanicsKiteTest.java', 'CombatPriorityAuditTest.java', 'ClarityBluffPhaseTest.java', 'ControlPursuitTranceKiteTest.java', 'CardinalSleepPriorityTest.java', 'CardinalAlwaysTranceTest.java', 'CardinalProminencePriorityTest.java', 'MeleePursuitBoundaryTest.java', 'BotGearCardinal78Test.java', 'DominatorTankSleepTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            raise SystemExit(result.returncode)
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    suites=['DominatorTankSleepTest'] if options.tank_sleep_only else ['BotGearCardinal78Test'] if options.gear_heal_only else ['MeleePursuitBoundaryTest'] if options.pursuit_only else ['CardinalProminencePriorityTest'] if options.prominence_only else ['CardinalAlwaysTranceTest'] if options.trance_only else ['CardinalSleepPriorityTest'] if options.cardinal_only else ['ClarityBluffPhaseTest'] if options.bluff_only else ['ControlPursuitTranceKiteTest','CardinalSurvivalControlChanceTest','ArenaOrbitTranceManaTest','NativeDebuffEpicTest','ClarityBluffPhaseTest','TankMechanicsKiteTest','CombatPriorityAuditTest','CardinalSleepPriorityTest','DominatorPressureBluffTest','PostSleepSummonTest','CardinalAlwaysTranceTest','CardinalProminencePriorityTest','MeleePursuitBoundaryTest','BotGearCardinal78Test','DominatorTankSleepTest']
    for name in suites:
        print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp, 'custom.fakeplayersai.'+name, str(R / 'game'), *([str(d/'audit68.md')] if name=='NativeDebuffEpicTest' else [])], R / 'game'), flush=True)
    print('PASS: all custom/effect scripts compile')
