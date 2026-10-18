#!/usr/bin/env python3
"""Actual XML/handlers/targets + production Dominator/Titan decisions; isolated new regression."""
from pathlib import Path
import os,subprocess,tempfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2];SRC=ROOT/'game/data/scripts/custom/fakeplayersai';TEST=Path(__file__).with_name('DominatorCpTitanPressureRegression.java')
def audit():
    skills={int(s.get('id')):s for f in (ROOT/'game/data/stats/skills').glob('*.xml') for s in ET.parse(f).getroot().findall('skill')}
    for id,target,time,power,mp,initial in ((1305,'PARTY_CLAN','2500','687','191','48'),(1306,'ONE','5000','858','96','24')):
        s=skills[id];tables={t.get('name'):t.text.split()[-1] for t in s.findall('table')}
        assert s.findtext('targetType')==target and s.findtext('hitTime')==time and s.findtext('reuseDelay')=='5000'
        assert s.find('./effects/effect').get('name')=='CpHeal' and tables['#amount']==power
        assert tables['#mpConsume']==mp and tables['#mpInitialConsume']==initial
        assert s.findtext('isMagic')=='1' and s.findtext('isDebuff')!='true'
        over=ET.parse(ROOT/'game/data/stats/players/skillTrees/2ndClass/Overlord.xml').getroot().find('skillTree')
        assert any(x.get('skillId')==str(id) for x in over.findall('skill'))
    assert ET.parse(ROOT/'game/data/stats/players/skillTrees/3rdClass/Dominator.xml').getroot().find('skillTree').get('parentClassId')==over.get('classId')
    assert skills[368].find('./selfEffects/effect').get('name')=='ImmobileBuff'
    assert skills[368].find('./enchant2selfEffects/effect').get('name')=='Buff'
    assert skills[368].find('./conditions/using').get('kind')=='SHIELD'
    assert skills[1414].findtext('targetType')=='PARTY_CLAN' and skills[1414].findtext('itemConsumeCount')=='40'
    print('PASS: actual Dominator inherited CP skills, exact costs/casts/effects,1414 real item cost,368 baseImmobile/enchant2slow audit',flush=True)
def guards(s):
    act=s[s.index('private static boolean act('):s.index('private static void stopBasicAttack(')]
    for marker in ('BotSpawner.getTacticalCpSupport(bot, enemy)','TacticsPolicy.dominatorCpNeeded(cpRatio(bot))', 'cpSupport ? 145',
                   's.dominatorCp.choose(options, cpRatio(bot), controlHold)', 'TacticsPolicy.titanPursuit(preset.getClassId(), allowsBasicAttack(bot, preset)',
                   'resources(bot, skill)', 'skill.checkCondition(bot, self ? bot : enemy, false)', 'bot.setTarget(self ? bot : enemy)'):
        assert marker in act,marker
    assert act.index('if (selected >= 0)')<act.index('TacticsPolicy.titanPursuit(')<act.index('if (!defenseOnly && !offenseAvailable)'), 'ready skill first, legal Titan fallback before generic hold'
    assert '&& !TacticsPolicy.selfOnlyTargets(bot, skill.getTargetList(bot, false, bot))) { continue; }' in act
    assert act.index('selfOnlyTargets(bot, skill.getTargetList(bot, false, bot))')<act.index('TacticsPolicy.allowed('),'real full target identity check precedes legal candidate insertion'
    assert act.index('if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow())')<act.index('stopBasicAttack(bot)'), 'no cast retarget/Idle'
    poll=s[s.index('private static void pollAttempt('):s.index('private static boolean hasAppliedStun(')]
    assert 's.dominatorCp.resolved(s.pendingId, paid,' in poll and 'bot.setTarget(enemy)' in poll
    assert poll.index('if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow())')<poll.index('s.dominatorCp.resolved')<poll.index('bot.setTarget(enemy)')
    assert 's.dominatorCp.reset()' in s[s.index('private static void prepare('):s.index('private static boolean act(')]
    assert '(skill.getId() == 1305 || skill.getId() == 1414) && skill.getTargetType() == TargetType.PARTY_CLAN' in s
    support=s[s.index('private static boolean immobilizingSupport('):s.index('private static boolean supportedTarget(')]
    assert support.index('if (skill.getId() == 368) { return false; }')<support.index('EffectScope.GENERAL')
    step=s[s.index('private static boolean step('):]
    assert 'TacticsPolicy.titanRecoveryAllowed(preset.getClassId(), weaponUnavailable(bot))' in step
    assert 'TacticsPolicy.tankRecoveryAllowed(TacticsPolicy.tankClass(preset.getClassId()), weaponUnavailable(bot))' in step
    # Cardinal-only native MP support predates this Dominator regression; never allow it in other code.
    support=s[s.index('private static void restoreCardinalMana('):s.index('private static void stopBasicAttack(')]
    assert 'preset.getClassId() != CARDINAL_CLASS_ID' in support and 'bot.getPlayerClass().getId() != CARDINAL_CLASS_ID' in support
    guarded=s.replace(support,'')
    for forbidden in ('setCurrentCp(', 'setCurrentMp(', 'setCurrentHp(', 'setHeading(', 'setXYZ(', 'abortCast(', 'applyEffects(', 'startAutoPlay(', 'startAutoUseTask('):assert forbidden not in guarded,forbidden

def main():
    audit();guards((SRC/'BotTactics.java').read_text())
    with tempfile.TemporaryDirectory(prefix='dominator-cp-titan-') as d:
        tmp=Path(d);launcher=tmp/'Compile.java';launcher.write_text('import javax.tools.ToolProvider;public class Compile {public static void main(String[] a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        if subprocess.run(['git','cat-file','-e','a1bb787^{commit}'],cwd=ROOT,capture_output=True).returncode==0:
            old=tmp/'old'/'TacticsPolicy.java';old.parent.mkdir();old.write_text(subprocess.check_output(['git','show','a1bb787:game/data/scripts/custom/fakeplayersai/TacticsPolicy.java'],cwd=ROOT,text=True))
            probe=tmp/'TitanCooldownRootProbe.java';probe.write_text('package custom.fakeplayersai;public class TitanCooldownRootProbe{public static void main(String[] a){if(TacticsPolicy.recoveryStepAllowed(false,false,false,100,false))throw new AssertionError("generic cooldown-only recovery backpedals armed Titan");}}')
            subprocess.run(['java',str(launcher),'-d',str(tmp/'oldclasses'),str(old),str(probe)],check=True)
            result=subprocess.run(['java','-cp',str(tmp/'oldclasses'),'custom.fakeplayersai.TitanCooldownRootProbe'],capture_output=True,text=True)
            assert result.returncode!=0 and 'generic cooldown-only recovery backpedals armed Titan' in result.stderr
            print('PASS: baseline a1bb787 behaviorally reproduces generic cooldown-only retreat trigger (not unique toTitan)',flush=True)
        else: print('SKIP: optional historical a1bb787 reproduction (commit absent); current production regressions still run',flush=True)
        # Authoritative target handlers are tracked but not necessarily sparse-checked-out.
        target_sources=[]
        for name in ('PartyClan','One'):
            p=tmp/(name+'.java');p.write_text(subprocess.check_output(['git','show','HEAD:game/data/scripts/handlers/skill/targets/'+name+'.java'],cwd=ROOT,text=True));target_sources.append(p)
        handlers=[ROOT/'game/data/scripts/handlers/skill/effects'/(x+'.java') for x in ('CpHeal','CpHealPercent','Buff','ImmobileBuff')]
        cp=os.pathsep.join(map(str,sorted((ROOT/'libs').glob('*.jar'))))
        subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(tmp/'classes'),*map(str,sorted(SRC.glob('*.java'))),*map(str,handlers),*map(str,target_sources),str(TEST)],check=True,cwd=ROOT)
        print('PASS: all production fakeplayers +actualCP/immobile effects and actual target handlers compile against libs/*.jar',flush=True)
        subprocess.run(['java','-cp',os.pathsep.join([str(tmp/'classes'),cp]),'custom.fakeplayersai.DominatorCpTitanPressureRegression'],check=True,cwd=ROOT/'game')
    print('PASS: CP real-cast weaving/enemy restore,Titan-only cooldown pressure,Vengeance explicit-permission source regressions',flush=True)
if __name__=='__main__':main()
