#!/usr/bin/env python3
"""Structure60 real native Skill/stat behavior; scheduling/network/AI recording seams."""
from pathlib import Path
import os, subprocess, tempfile, shutil
R=Path(__file__).resolve().parents[2];T=Path(__file__).parent
S=R/'game/data/scripts/custom/fakeplayersai'
tactics=(S/'BotTactics.java').read_text();manager=(S/'BotManager.java').read_text()
support=(S/'CardinalArenaSupport.java').read_text()
def body(source,signature):
    a=source.index('{',source.index(signature));b=a+1;depth=1
    while depth:
        depth+=(source[b]=='{')-(source[b]=='}');b+=1
    return source[a:b]
common=body(tactics,'public static synchronized boolean tick(')
assert common.index('tickInvulnerableMelee(')<common.index('preset.getClassId() == 131')<common.index('return act(')
slow=body(manager,'private static void combatTick(')
assert slow.index('BotTactics.tickInvulnerableMelee(')<slow.index('BotSpawner.applyOpeningCurse(')<slow.index('driveBerserker(')
fast=body(manager,'private static void olympiadChaseTick(')
assert fast.index('holdOlympiadIfNotReady(')<fast.index('BotTactics.tickInvulnerableMelee(')<fast.index('driveBerserker(')
assert 'System.nanoTime()' in tactics and 's.cardinalSupport.start(bot, enemy, s.combatStartedAt, time)' in tactics
assert 'REGENERATE_MP_RATE' not in support and '0.10' not in support
assert 'getSkill(3282,1)' in support and 'applyEffects(enemy,enemy,true,seconds)' in support
assert 'time + 400' not in support and 'if(attempted)return;' in support
assert 'SETTINGS.isRestoreMp() && bot.getPlayerClass().getId() == 97' in manager
print('PASS: active authority, common/legacy131/opener priority, native3282 once-only and normal-MP wiring',flush=True)
# The restrictive mode must win before candidate scoring and parent fall-through.
survival=(S/'CardinalSurvival.java').read_text();chance=(S/'BotControlChance.java').read_text()
assert 'getBuffInfoBySkillId(3282)' in survival and '!b.isRemoved()&&b.isInUse()' in survival
assert 'CardinalSurvival.active(bot, opponent)' in manager
assert tactics.index('if (survival && !CardinalSurvival.allowed(skill.getId()))') < tactics.index('final boolean cardinalHealUrgent')
assert 'if (survival) { stopBasicAttack(bot); return true; }' in tactics
assert 'if (survival && bot.isAffectedBySkill(1418))' in tactics
assert 'CardinalSurvival.active(player, enemy)' in (S/'BotSpawner.java').read_text()
assert 'classId==97&&skillId==1394' in chance and 'classId==115&&(skillId==1097||skillId==1092)' in chance
assert all(f's.set("{key}",15)' in chance for key in ['activateRate','minChance','maxChance'])
assert 'bot.addSkill(copy,false)' in chance and 'getClient()!=null' in chance
mechanics=(S/'TankClassMechanics.java').read_text()
assert 'TankClassMechanics.defendedTank(enemy)' in tactics
assert 'TankClassMechanics.support(skill.getId()) ? 230' in tactics
assert '(tankMechanicChoice == 785 || tankMechanicChoice == 406) ? tankMechanicChoice' in tactics
assert 'TankClassMechanics.sacrificeBlocked(bot)' in (S/'BotRecoveryRules.java').read_text()
assert 'BotRecoveryRules.blockedSacrifice(effector, effected, skill)' in (R/'game/data/scripts/handlers/skill/effects/Heal.java').read_text()
assert 'TankClassMechanics::skill' in (S/'BotSpawner.java').read_text()
assert 'TankClassMechanics.skill(skill.getId())' in (S/'BotSpawner.java').read_text()
for forbidden in ['setCurrentHp(', 'applyEffects(', 'setReuseDelay(', 'makeTriggerCast(', 'setCharges(', 'addSkill(']:
    assert forbidden not in mechanics,forbidden
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
                                        'ArenaOrbitTranceManaTest.java', 'BerserkerSoulCleanseTest.java', 'DominatorPressureBluffTest.java', 'DaggerRearApproachTest.java', 'DominatorCpTitanPressureRegression.java', 'CpMeleeRushRecoveryTest.java', 'PostSleepSummonTest.java', 'DominatorControlShadowTest.java', 'ArenaInvulnerabilityClarityTest.java', 'CardinalSurvivalControlChanceTest.java', 'NativeDebuffEpicTest.java', 'TankMechanicsKiteTest.java']]]

    def run(command, cwd):
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True, timeout=180)
        if result.returncode:
            print(result.stdout[-4000:])
            print(result.stderr[-6000:])
            raise SystemExit(result.returncode)
        return result.stdout.strip()

    run(['java', str(compiler), '-cp', cp, '-d', str(d / 'classes'), *map(str, sources)], R)
    print(run(['java', '-cp', str(d / 'classes') + os.pathsep + cp,
               'custom.fakeplayersai.TankMechanicsKiteTest', str(R / 'game')], R / 'game'))
    print('PASS: all custom/effect scripts compile')
