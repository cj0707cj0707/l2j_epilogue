#!/usr/bin/env python3
"""Three-tank actual XML/learned control audit, production decisions, and full real-JAR compilation."""
from pathlib import Path
import os, subprocess, tempfile, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'game/data/scripts/custom/fakeplayersai'
TEST=Path(__file__).with_name('TankCloseControlRegression.java')

def audit():
    skills={int(s.get('id')):s for f in (ROOT/'game/data/stats/skills').glob('*.xml') for s in ET.parse(f).getroot().findall('skill')}
    trees={int(t.get('classId')):t for f in (ROOT/'game/data/stats/players/skillTrees').rglob('*.xml') for t in ET.parse(f).getroot().findall('skillTree') if t.get('type')=='classSkillTree' and t.get('classId')}
    for cls,expected in {90:{92,403},91:{92,403},106:{352,402}}.items():
        learned=set(); seen=set(); c=cls
        while c in trees and c not in seen:
            seen.add(c); t=trees[c]; learned.update(int(s.get('skillId')) for s in t.findall('skill')); c=int(t.get('parentClassId','-1'))
        assert expected <= learned,(cls,expected-learned)
        for id in expected:
            s=skills[id]; effects={e.get('name') for e in s.findall('./effects/effect')}
            assert s.findtext('targetType')=='ONE' and s.findtext('isDebuff')=='true'
            assert s.findtext('reuseDelay') in ('3000','4000')
            if id in (92,352):
                assert 'Stun' in effects and s.findtext('castRange')=='40' and s.find('.//using').get('kind')=='SHIELD'
            else:
                assert 'Root' in effects and s.findtext('castRange')==('600' if id==403 else '150')
        print(f'PASS: actual class{cls} inherited learned catch skills{sorted(expected)}',flush=True)
    assert skills[110].find('./effects/effect').get('name')=='ImmobileBuff'
    assert skills[368].find('./selfEffects/effect').get('name')=='ImmobileBuff'
    assert skills[760].find('./effects/effect').get('name')=='Buff'
    assert not any(e.get('name')=='ImmobileBuff' for e in skills[760].iter('effect'))
    print('PASS: actual control cast40/150/600 and 110GENERAL/368SELF immobility;760 plainBuff audit',flush=True)

def guards(s):
    approach=s[s.index('public static synchronized int tacticalApproachRange('):s.index('/** Explicit teardown;')]
    assert approach.index('TacticsPolicy.tankApproachRange(')<approach.index('isSkillDisabled('),'tank reach not expanded by cooldown/mana filtering'
    act=s[s.index('private static boolean act('):s.index('private static void stopBasicAttack(')]
    for marker in ('TacticsPolicy.tankClass(preset.getClassId())','final boolean timedTank = tank;', 'TacticsPolicy.tankClosingSkillAllowed(',
                   'skill.hasEffectType(EffectType.STUN)','skill.hasEffectType(EffectType.ROOT)', 'tankRoot && enemy.isMovementDisabled()',
                   'TacticsPolicy.tankSupportAllowed(tankClosing, immobilizingSupport(skill))',
                   'TacticsPolicy.tankActionChoice(options, tankStuns, tankRoots, tankMelee, tankSelected,',
                   '!enemy.isMovementDisabled() && !enemy.isOutOfControl()', 'TacticsPolicy.tankPursuit(tank, allowsBasicAttack(bot, preset)',
                   's.tankDefense.observed(activeStrong)', 's.tankDefense.choose(options, time, activeStrong != 0)',
                   'resources(bot, skill)', 'skill.checkCondition(bot, self ? bot : enemy, false)',
                   'BotSpawner.getTacticalRotation(bot, enemy)', 'BotSpawner.getTacticalDefenses(bot, enemy)'):
        assert marker in act,marker
    assert act.index('if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow())')<act.index('stopBasicAttack(bot)')
    assert act.index('if (score > 0)')<act.index('tankStuns.add(skill.getId())'),'only fully legal options enter priority subsets'
    assert 'final boolean tankClosing = tank && (!visible || distance > TacticsPolicy.tankApproachRange(' in act,'near blind obstacle must defer immobile options BEFORE selection, not merely yield after it'
    assert act.index('TacticsPolicy.tankPursuit(')<act.index('if (!defenseOnly && !offenseAvailable)')
    assert 'sleeping, enemy.isInvul(), enemy.isAutoAttackable(bot)' in act,'near/far blind yield is movement-only; normal skill legality still requires visible'
    support=s[s.index('private static boolean immobilizingSupport('):s.index('private static boolean supportedTarget(')]
    assert 'EffectScope.SELF' in support and '"ImmobileBuff".equals(effect.getName())' in support
    assert '760' not in support,'plain mage armor must not be falsely self-root classified'
    step=s[s.index('private static boolean step('):]
    assert 'TacticsPolicy.tankRecoveryAllowed(TacticsPolicy.tankClass(preset.getClassId()), weaponUnavailable(bot))' in step
    for bad in ('setCurrentMp(', 'setXYZ(', 'setHeading(', 'abortCast(', 'applyEffects(', 'setAbnormalTime('): assert bad not in s,bad

def main():
    audit(); guards((SRC/'BotTactics.java').read_text())
    with tempfile.TemporaryDirectory(prefix='tank-close-control-') as d:
        tmp=Path(d); launcher=tmp/'Compile.java'; launcher.write_text('import javax.tools.ToolProvider; public class Compile {public static void main(String[] a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        probe=tmp/'VengeanceFirstProbe.java'; probe.write_text('package custom.fakeplayersai; import java.util.List; public class VengeanceFirstProbe { public static void main(String[] a) {var s=new TacticsPolicy.TankDefenseSequence(); s.reset(10000);var legal=List.of(new TacticsPolicy.Option(110,TacticsPolicy.Kind.DEFENSE,230),new TacticsPolicy.Option(368,TacticsPolicy.Kind.DEFENSE,234));if(s.choose(legal,15000,false)!=368)throw new AssertionError("Vengeance must be first legal strong defense");}}')
        old=tmp/'baseline'/'TacticsPolicy.java'; old.parent.mkdir(); old.write_text(subprocess.check_output(['git','show','c09ddae:game/data/scripts/custom/fakeplayersai/TacticsPolicy.java'],cwd=ROOT,text=True))
        subprocess.run(['java',str(launcher),'-d',str(tmp/'oldclasses'),str(old),str(probe)],check=True)
        result=subprocess.run(['java','-cp',str(tmp/'oldclasses'),'custom.fakeplayersai.VengeanceFirstProbe'],capture_output=True,text=True)
        assert result.returncode!=0 and 'Vengeance must be first legal strong defense' in result.stderr
        print('PASS: baseline c09ddae behaviorally fails actual production Vengeance-first decision',flush=True)
        cp=os.pathsep.join(map(str,sorted((ROOT/'libs').glob('*.jar'))))
        subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(tmp/'classes'),*map(str,sorted(SRC.glob('*.java'))),str(TEST),str(probe)],check=True,cwd=ROOT)
        print('PASS: ALL production fake-player scripts compile against actual libs/*.jar',flush=True)
        runtime=os.pathsep.join([str(tmp/'classes'),cp])
        for cls in ('VengeanceFirstProbe','TankCloseControlRegression'):
            subprocess.run(['java','-cp',runtime,'custom.fakeplayersai.'+cls],check=True,cwd=ROOT/'game')
    print('PASS: three-tank close/control/immobile support/source integration regressions',flush=True)
if __name__=='__main__': main()
