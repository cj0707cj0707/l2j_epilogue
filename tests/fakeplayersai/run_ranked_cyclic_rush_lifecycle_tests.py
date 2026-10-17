#!/usr/bin/env python3
"""Ranked full-loop cyclic regression, source-exact bodies + real-JAR compile/bytecode audit.
No production edits, existing-test edits, core changes, resource/reuse hacks or server startup.
"""
from pathlib import Path
import os
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'game/data/scripts/custom/fakeplayersai'
TEST=Path(__file__).with_name('RankedCyclicRushLifecycleRegression.java')

def block(source,signature):
    a=source.index(signature); brace=source.index('{',a); depth=1; i=brace+1
    while depth:
        if source[i]=='{':depth+=1
        elif source[i]=='}':depth-=1
        i+=1
    return source[a:i]

def main():
    source=(SRC/'BotManager.java').read_text()
    # Preserve and exercise the shipped exact implementation, not a handwritten
    # duplicate of the policy. Seam doubles only substitute server runtime services.
    signatures=['private static final class BerserkerState','private static final class RushStunLoop',
        'private static boolean isOwnRushStun(', 'private static BuffInfo ownRushStun(',
        'private static boolean driveOwnRushStunLoop(', 'private static boolean rushStunRepeatAllowed(',
        'private static void stopRushStunMove(', 'private static List<Location> rushStunRetreatCandidates(',
        'private static boolean castSkill(', 'private static boolean canBerserkerCast(']
    bodies='\n\n'.join(block(source,s) for s in signatures)
    driver=block(source,'private static boolean driveBerserkerStep(')
    # Emergency recovery may run before the loop, but never during a pending Rush/cast.
    assert driver.index('!state.movement.rushPending()')<driver.index('BotTactics.tickDefense(')<driver.index('driveOwnRushStunLoop(')
    assert driver.index('driveOwnRushStunLoop(')<driver.index('if (state.movement.rushPending())')
    assert 'state.rushStun.requested(enemy, previousStun, now);' in driver
    outer=block(source,'private static boolean driveBerserker(')
    assert outer.index('OlympiadCombatGate.ready(bot)')<outer.index('driveBerserkerStep(')
    loop=block(source,'private static boolean driveOwnRushStunLoop(')
    assert loop.count('if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) return true')==2
    for forbidden in ('abortCast(', 'setXYZ(', 'setCurrentMp(', 'setAbnormalTime(', 'enableSkill(', 'applyEffects(', 'teleToLocation('):
        assert forbidden not in loop,forbidden
    for p in (ROOT/'game/data/stats/skills').glob('*.xml'):
        rush=next((s for s in ET.parse(p).getroot().findall('skill') if s.get('id')=='793'),None)
        if rush is not None:break
    assert rush.findtext('flyType')=='CHARGE' and rush.find('.//target').get('mindistance')=='200'
    assert rush.findtext('nextActionAttack')=='true' and rush.findtext('castRange')=='600'
    with tempfile.TemporaryDirectory(prefix='ranked-cyclic-lifecycle-') as d:
        tmp=Path(d); launcher=tmp/'Compile.java'; javap=tmp/'InspectClass.java'
        launcher.write_text('import javax.tools.ToolProvider;public class Compile{public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        javap.write_text('public class InspectClass{public static void main(String[]a){System.exit(java.util.spi.ToolProvider.findFirst("javap").orElseThrow().run(System.out,System.err,a));}}')
        cp=os.pathsep.join(map(str,sorted((ROOT/'libs').glob('*.jar'))))
        subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(tmp/'actual'),*map(str,sorted(SRC.glob('*.java')))],check=True,cwd=ROOT)
        print('PASS: all production fake-player scripts compile against actual libs/*.jar',flush=True)
        creature=subprocess.check_output(['java',str(javap),'-classpath',cp,'-p','-c','org.l2jmobius.gameserver.entity.actor.Creature'],text=True)
        begin=creature.split('private void beginCast(',2)[2].split('\n  public ',1)[0]
        assert 'Skill.getFlyType:' in begin and 'Method setXYZ:(III)V' in begin
        for getter in ('getX:()I','getY:()I','getZ:()I'): assert getter in begin
        finalizer=creature.split('public void onMagicFinalizer(',1)[1].split('\n  private void completeMagicFinalizer',1)[0]
        assert 'setCastingNow:(Z)V' in finalizer and 'setCastingSimultaneouslyNow:(Z)V' in finalizer
        assert 'long 333l' in finalizer and 'Intention.MOVE_TO:' in finalizer
        ai=subprocess.check_output(['java',str(javap),'-classpath',cp,'-p','-c','org.l2jmobius.gameserver.ai.PlayerAI'],text=True)
        finish=ai.split('public void notifyActionFinishCasting(',1)[1].split('\n  public ',1)[0]
        assert 'replaySavedIntention:()V' in finish and 'setIntentionIdle:()V' in finish
        print('PASS: actual JAR verifies synchronous XYZ=target CHARGE, async finalizer and saved/delayed intentions',flush=True)
        generated=tmp/TEST.name
        generated.write_text(TEST.read_text().replace('// INSERT_UNCHANGED_PRODUCTION_BODIES',bodies))
        subprocess.run(['java',str(launcher),'-d',str(tmp/'isolated'),str(SRC/'BerserkerCycle.java'),str(generated)],check=True,cwd=ROOT)
        subprocess.run(['java','-cp',str(tmp/'isolated'),'custom.fakeplayersai.RankedCyclicRushLifecycleRegression'],check=True,cwd=ROOT)
    print('PASS: ranked current zero-distance path completes >=3 cycles; no ranked one-shot defect assumed',flush=True)

if __name__=='__main__':main()
