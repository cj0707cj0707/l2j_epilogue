#!/usr/bin/env python3
"""Actual zero-CHARGE baseline reproduction plus production cyclic lease regressions."""
from pathlib import Path
import os
import subprocess
import tempfile
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'game/data/scripts/custom/fakeplayersai'
TEST=Path(__file__).with_name('CyclicTrainingRushRegression.java')

def guards(source):
    act=source[source.index('private static boolean act('):source.index('private static void stopBasicAttack(')]
    move=source[source.index('private static int trainingRushAction('):source.index('private static void failure(')]
    poll=source[source.index('private static void pollAttempt('):source.index('private static boolean hasAppliedStun(')]
    assert 'distance < 1' not in move,'core exact-zero landing must not clear original lease'
    assert 's.rushLoop.clear()' not in move,'temporary inability must hold original until actualend/replacement'
    assert 's.rushDirectionX, s.rushDirectionY' in move
    assert 'attempt < 8' in move and 'TrainingArena.inside(x, y)' in move and 'canMoveToTarget(' in move
    assert 's.rushLoop.retryRetreat(time)' in move and 'return -2' in move
    assert 's.rushLoop.resolvedLoopLanding(original, original != null, paid,' in poll
    assert 's.pendingId == 793 && s.pendingRushLoop' in poll
    assert 's.nextRushStep = time;' in poll and 's.rushDirectionX = s.pendingX - bot.getX()' in poll
    assert '!s.pendingRushLoop' in poll,'normal fresh own793 can begin; loop repeats never reanchor'
    assert 'if (trainingRushActive && selected == 793) { s.rushLoop.loopRequestAccepted(); }' in act
    assert 'trainingBerserker && !trainingRushActive && !s.trainingBurst.allowsSkill' in act
    assert 'options.stream().anyMatch(option -> option.id() == 793) ? 793 : -1' in act
    assert 'if (trainingRushActive) { stopBasicAttack(bot); return true; }' in act
    assert act.index('s.pendingX = bot.getX(); s.pendingY = bot.getY();')<act.index('bot.useMagic(skill, true, true)')
    assert 'if (ranked && preset.getClassId() == 131) { return false; }' in source
    for required in ('resources(bot, skill)', 'skill.checkCondition(bot, self ? bot : enemy, false)',
                     'weaponUnavailable(bot)', 'effect.getEffector() == bot', 'hasAppliedStun(effect)',
                     '!effect.isRemoved() && effect.isInUse()', 'bot.isCastingNow() || bot.isCastingSimultaneouslyNow()'):
        assert required in source,required
    for forbidden in ('setXYZ(', 'setHeading(', 'setAbnormalTime(', 'setCurrentMp(', 'abortCast(', 'applyEffects(', 'startAutoPlay(', 'startAutoUseTask('):
        assert forbidden not in source,forbidden

def main():
    guards((SRC/'BotTactics.java').read_text())
    # Confirm actual checked-in XML, not guessed skill reuse or a synthetic charge.
    skill=next(s for p in (ROOT/'game/data/stats/skills').glob('*.xml') for s in ET.parse(p).getroot().findall('skill') if s.get('id')=='793')
    assert skill.findtext('flyType')=='CHARGE' and skill.findtext('castRange')=='600'
    assert skill.find('.//target').get('mindistance')=='200'
    with tempfile.TemporaryDirectory(prefix='cyclic-training-rush-') as d:
        tmp=Path(d); launcher=tmp/'Compile.java'
        launcher.write_text('import javax.tools.ToolProvider; public class Compile {public static void main(String[] a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        # This exact assertion RUNS against baseline and fails on its zero-distance
        # null waypoint. The failure is behavioral, not merely a new-method check.
        probe=tmp/'ZeroTrainingRushProbe.java'
        probe.write_text('package custom.fakeplayersai; public class ZeroTrainingRushProbe {public static void main(String[] a){if(TacticsPolicy.OriginalRushStun.retreatPoint(42705,188869,42705,188869,0)==null)throw new AssertionError("actual CHARGE zero landing has no retreat direction");}}')
        baseline=tmp/'baseline'; baseline.mkdir(); old=baseline/'TacticsPolicy.java'
        old.write_text(subprocess.check_output(['git','show','bc857da:game/data/scripts/custom/fakeplayersai/TacticsPolicy.java'],cwd=ROOT,text=True))
        subprocess.run(['java',str(launcher),'-d',str(baseline/'classes'),str(old),str(probe)],check=True)
        result=subprocess.run(['java','-cp',str(baseline/'classes'),'custom.fakeplayersai.ZeroTrainingRushProbe'],capture_output=True,text=True)
        assert result.returncode!=0 and 'actual CHARGE zero landing has no retreat direction' in result.stderr
        print('PASS: baseline bc857da behaviorally reproduces zero-distance CHARGE/null-retreat failure',flush=True)
        jars=sorted((ROOT/'libs').glob('*.jar')); cp=os.pathsep.join(map(str,jars))
        subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(tmp/'classes'),*map(str,sorted(SRC.glob('*.java'))),str(TEST),str(probe)],check=True,cwd=ROOT)
        print('PASS: ALL production fake-player scripts compile against actual libs/*.jar',flush=True)
        runtime=os.pathsep.join([str(tmp/'classes'),cp])
        subprocess.run(['java','-cp',runtime,'custom.fakeplayersai.ZeroTrainingRushProbe'],check=True,cwd=ROOT/'game')
        subprocess.run(['java','-cp',runtime,'custom.fakeplayersai.CyclicTrainingRushRegression'],check=True,cwd=ROOT/'game')
    print('PASS: cyclic training Rush actual-core-root, production-seam and no-bypass source regressions',flush=True)
if __name__=='__main__': main()
