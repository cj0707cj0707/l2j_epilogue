#!/usr/bin/env python3
"""Structure65 Dominator ranged pursuit and Cardinal Trance/kite gap regressions, no live server."""
from pathlib import Path
import os, subprocess, tempfile, shutil
R=Path(__file__).resolve().parents[2];T=Path(__file__).parent
with tempfile.TemporaryDirectory(prefix='tank60-') as directory:
    d = Path(directory)
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
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.ControlPursuitTranceKiteTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile')
