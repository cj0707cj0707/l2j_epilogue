#!/usr/bin/env python3
"""Unique training131 regression harness. No existing tests or server data modified."""
from pathlib import Path
import os
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'game/data/scripts/custom/fakeplayersai'
TEST=Path(__file__).with_name('TrainingBerserkerPressureRegression.java')

def integration_guards(source):
    act=source[source.index('private static boolean act('):source.index('private static void stopBasicAttack(')]
    assert 's.trainingBerserkerMode = trainingBerserker;' in act
    assert 'preset.getClassId() == 131 && !bot.isInOlympiadMode()' in act
    assert 's.trainingEnemy == enemy.getObjectId() && s.trainingInstance == bot.getInstanceId()' in act
    assert 'if (ranked && preset.getClassId() == 131) { return false; }' in source
    assert 's.trainingBurst.choose(options, distance, skills.containsKey(793)' in act
    assert act.index('s.trainingBurst.allowsSkill(')<act.index('offenseAvailable = true')
    assert act.index('s.pendingX = bot.getX(); s.pendingY = bot.getY();')<act.index('bot.useMagic(skill, true, true)')
    assert act.index('if (selected >= 0)')<act.index('if (trainingPressure) { return false; }')
    assert act.index('if (trainingPressure) { return false; }')<act.index('if (!defenseOnly && !offenseAvailable) { stopBasicAttack(bot); return true; }')
    assert '!trainingPressure && step(' in act
    busy=act[act.index('if (trainingBerserker && (bot.isCastingNow()'):act.index('if (!defenseOnly && !allowsBasicAttack')]
    assert 'return true;' in busy and 'stopBasicAttack' not in busy and 'setIntentionIdle' not in busy
    poll=source[source.index('private static void pollAttempt('):source.index('private static void failure(')]
    assert 'if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) { s.sawCast = true; return; }' in poll
    assert 's.trainingBurst.resolved(s.pendingId, paid, Math.hypot(' in poll
    assert poll.index('s.trainingBurst.resolved')<poll.index('s.pendingId = 0;')
    assert 's.trainingBurst.reset(); s.trainingBerserkerMode = false;' in source
    assert 'MAX_BOTS = 2048' in source and 'TTL_MS = 30 * 60_000L' in source
    for forbidden in ('abortCast(', 'setXYZ(', 'setHeading(', 'setCurrentMp(', 'startAutoPlay(', 'startAutoUseTask(', 'setOlympiadMode(', 'setOlympiadStart('):
        assert forbidden not in source, forbidden

def xml_guards():
    skills={int(s.get('id')):s for p in (ROOT/'game/data/stats/skills').glob('*.xml') for s in ET.parse(p).getroot().findall('skill')}
    for sid in (485,526,497,498,477):
        assert skills[sid].findtext('castRange')=='40', f'actual melee XML {sid}'
    assert skills[526].findtext('targetType')=='FRONT_AREA' and skills[526].findtext('affectRange')=='400'
    assert skills[793].findtext('flyType')=='CHARGE' and skills[793].findtext('castRange')=='600'
    assert skills[793].find('.//target').get('mindistance')=='200'
    assert skills[498].find('.//target').get('abnormal')=='1'
    assert skills[501].findtext('castRange')=='600' and skills[501].findtext('reuseDelay')=='2000'
    print('PASS: actual training131 XML range/charge/conditional/filler audit',flush=True)

def main():
    integration_guards((SRC/'BotTactics.java').read_text()); xml_guards()
    baseline=subprocess.check_output(['git','show','b8d608d:game/data/scripts/custom/fakeplayersai/BotTactics.java'],cwd=ROOT,text=True)
    try: integration_guards(baseline)
    except (AssertionError,ValueError): print('PASS: baseline b8d608d fails production-wiring regressions',flush=True)
    else: raise AssertionError('baseline unexpectedly passes new regression')
    with tempfile.TemporaryDirectory(prefix='training-berserker-pressure-') as d:
        temp=Path(d); launcher=temp/'Compile.java'
        launcher.write_text('import javax.tools.ToolProvider; public class Compile {public static void main(String[] a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        jars=sorted((ROOT/'libs').glob('*.jar')); cp=os.pathsep.join(map(str,jars))
        subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(temp/'classes'),*map(str,sorted(SRC.glob('*.java'))),str(TEST)],check=True,cwd=ROOT)
        print('PASS: all production fake-player scripts compile against actual libs/*.jar',flush=True)
        subprocess.run(['java','-cp',os.pathsep.join([str(temp/'classes'),cp]),'custom.fakeplayersai.TrainingBerserkerPressureRegression'],check=True,cwd=ROOT/'game')
    print('PASS: training berserker post-rush pressure integration/source safety checks',flush=True)

if __name__=='__main__': main()
