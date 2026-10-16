#!/usr/bin/env python3
"""Unique actual-library tank combat+5/live-removal and original793-identity regressions."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'game/data/scripts/custom/fakeplayersai'
TEST=Path(__file__).with_name('TankDefenseRushIdentityRegression.java')

def guards(source):
    assert 'public static synchronized boolean beginCombat(Player bot, Player enemy)' in source
    assert 's.tankDefense.reset(time); s.rushLoop.clear();' in source
    assert 'if (timedPool && !s.tankDefense.due(time)) { continue; }' in source
    assert 'final double base = timedPool ? 210' in source
    assert 's.tankDefense.choose(options, time, activeStrong != 0)' in source
    assert 'BotSpawner.isStrongTankDefenseSkill(s.pendingId)' in source
    assert 's.tankDefense.observed(activeStrong)' in source
    assert 'BotPresets.getPresetByClassId(enemy.getPlayerClass().getId())' in source
    assert 'effect.getSkill().getId() == 793 && effect.getEffector() == bot && effect.getEffected() == enemy' in source
    assert '!effect.isRemoved() && effect.isInUse() && enemy.isStunned() && hasAppliedStun(effect)' in source
    assert 'applied.getEffectType() == EffectType.STUN && (applied.getEffectFlags() & EffectFlag.STUNNED.getMask()) != 0' in source
    assert 's.pendingId == 793 && paid && !s.pendingRushLoop' in source
    assert 's.pendingRushLoop = trainingRushActive;' in source
    assert 'if (trainingRushActive && skill.getId() != 793) { continue; }' in source
    assert 'if (ranked && preset.getClassId() == 131) { return false; }' in source
    move=source[source.index('private static int trainingRushAction('):source.index('private static void failure(')]
    for value in ('s.rushLoop.active(stun, stun != null)', 'distance >= 220', 'TrainingArena.inside(x, y)',
                  's.rushAnchorX','> 650','> 1600','canMoveToTarget(', 's.nextRushStep = time + 250', 'stopRushMovement(bot, s)'):
        assert value in move,value
    assert 's.anchorX' not in move,'old1596-separated spawn anchor must not reject legitimate near-human retreats'
    assert move.index('s.rushLoop.active')<move.index('setIntentionMoveTo(')
    act=source[source.index('private static boolean act('):source.index('private static void stopBasicAttack(')]
    assert act.index('if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow())')<act.index('stopBasicAttack(bot)')
    assert act.index('s.pendingX = bot.getX(); s.pendingY = bot.getY();')<act.index('bot.useMagic(skill, true, true)')
    for forbidden in ('setAbnormalTime(', 'setXYZ(', 'setHeading(', 'teleToLocation(', 'setCurrentMp(', 'applyEffects(', 'abortCast(', 'startAutoPlay(', 'startAutoUseTask('):
        assert forbidden not in source,forbidden

def main():
    guards((SRC/'BotTactics.java').read_text())
    baseline=subprocess.check_output(['git','show','c4ba349:game/data/scripts/custom/fakeplayersai/BotTactics.java'],cwd=ROOT,text=True)
    try: guards(baseline)
    except (AssertionError,ValueError): print('PASS: baseline c4ba349 fails tank timing/attribution/movement integration regressions',flush=True)
    else: raise AssertionError('baseline unexpectedly passes')
    with tempfile.TemporaryDirectory(prefix='tank-defense-rush-identity-') as d:
        temp=Path(d); launcher=temp/'Compile.java'
        launcher.write_text('import javax.tools.ToolProvider; public class Compile {public static void main(String[] a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        jars=sorted((ROOT/'libs').glob('*.jar')); cp=os.pathsep.join(map(str,jars))
        subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(temp/'classes'),*map(str,sorted(SRC.glob('*.java'))),str(ROOT/'game/data/scripts/handlers/skill/effects/Stun.java'),str(TEST)],check=True,cwd=ROOT)
        print('PASS: all production scripts compile against actual libs/*.jar',flush=True)
        subprocess.run(['java','-cp',os.pathsep.join([str(temp/'classes'),cp]),'custom.fakeplayersai.TankDefenseRushIdentityRegression',str(ROOT/'game')],check=True,cwd=ROOT/'game')
    print('PASS: tank real-combat clock/live-effect removal and original-own-stun integration guards',flush=True)

if __name__=='__main__': main()
