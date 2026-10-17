#!/usr/bin/env python3
"""Actual saved overrides and exact shipped skill-pool statements, before/after833 fix."""
from pathlib import Path
import os,subprocess,tempfile
R=Path(__file__).resolve().parents[2];T=Path(__file__).parent;S=R/'game/data/scripts/custom/fakeplayersai'
source=(S/'BotTactics.java').read_text()
def pool(s):
    a=s.index('final Map<Integer, Skill> skills = new LinkedHashMap<>();',s.index('private static boolean act('));b=s.index('int chargeDemand = 4;',a)
    return s[a:b]
previous=subprocess.check_output(['git','show','7a93669bdf6ce7952795b4714da1fd858c8d90f4:game/data/scripts/custom/fakeplayersai/BotTactics.java'],text=True,cwd=R)
assert 'skills.put(833' not in pool(previous) and 'skills.put(833' in pool(source)
# Inclusion is unconditional on HP; the existing native eligibility/priority gates
# must remain AFTER insertion, and current STUN keeps its expressly requested Rush-only priority.
a=source[source.index('private static boolean act('):source.index('private static void stopBasicAttack(')]
for marker in ['BotRecoveryRules.selfHealAllowed(bot, skill)','resources(bot, skill)','!bot.isSkillDisabled(skill)','skill.checkCondition(bot, self ? bot : enemy, false)','if (trainingRushActive && skill.getId() != 793)','preset.getClassId() == 131 && o.id() == 833','bot.setTarget(self ? bot : enemy)','bot.useMagic(skill, true, true)']:assert marker in a,marker
m=(S/'BotManager.java').read_text();driver=m[m.index('private static boolean driveBerserkerStep('):m.index('private static boolean isOwnRushStun(')]
assert driver.index('driveOwnRushStunLoop(')<driver.index('BotTactics.tickDefense(')<driver.index('final boolean manaFallback')
with tempfile.TemporaryDirectory(prefix='berserker-body833-') as d:
    d=Path(d);compiler=d/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
    test=d/'BerserkerBodyReconstructionTest.java';test.write_text((T/test.name).read_text().replace('// INSERT_CURRENT_POOL',pool(source)).replace('// INSERT_PREVIOUS_POOL',pool(previous)))
    cp=os.pathsep.join(map(str,sorted((R/'libs').glob('*.jar'))))
    sources=[R/'game/data/scripts/handlers/EffectMasterHandler.java',*sorted((R/'game/data/scripts/handlers/skill/effects').glob('*.java')),*sorted((R/'game/data/scripts/custom').rglob('*.java')),*[T/n for n in ['LowManaAutoAttackTest.java','CombatRecoveryRulesTest.java','ArenaOrbitTranceManaTest.java']],test]
    p=subprocess.run(['java',str(compiler),'-cp',cp,'-d',str(d/'classes'),*map(str,sources)],capture_output=True,text=True,timeout=180,cwd=R)
    if p.returncode:print(p.stderr[-6000:]);raise SystemExit(p.returncode)
    p=subprocess.run(['java','-cp',str(d/'classes')+os.pathsep+cp,'custom.fakeplayersai.BerserkerBodyReconstructionTest',str(R/'game')],capture_output=True,text=True,timeout=120,cwd=R/'game')
    if p.returncode:print(p.stdout[-2000:]);print(p.stderr[-5000:]);raise SystemExit(p.returncode)
    print(p.stdout.strip());print('PASS: all custom scripts compile; native legality and ranked/training STUN-only priority wiring preserved')
