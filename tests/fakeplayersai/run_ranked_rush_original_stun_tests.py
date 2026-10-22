#!/usr/bin/env python3
"""Compile real production + real Stun handler; test inert applied effects, identity and safety."""
from pathlib import Path
import os
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
SCRIPTS=ROOT/'game/data/scripts/custom/fakeplayersai'

def guards():
    text=(SCRIPTS/'BotManager.java').read_text()
    loop=text.split('private static boolean driveOwnRushStunLoop(',1)[1].split('private static boolean rushStunRepeatAllowed(',1)[0]
    assert 'getOlympiadOpponent(bot) != enemy' in loop and 'preset.getClassId() != CLASS_DOOMBRINGER' in loop
    assert 'AutoPlayTaskManager.getInstance().stopAutoPlay(bot)' in loop and 'AutoUseTaskManager.getInstance().stopAutoUseTask(bot)' in loop
    assert loop.count('if (bot.isCastingNow() || bot.isCastingSimultaneouslyNow()) return true') == 2
    assert 'bot.isCastingNow() || bot.isCastingSimultaneouslyNow() || bot.isMovementDisabled()' in loop
    stop=text.split('private static void stopRushStunMove(',1)[1].split('private static List<Location>',1)[0]
    assert '!bot.isCastingNow() && !bot.isCastingSimultaneouslyNow()' in stop
    assert 'ownRushStun(bot, enemy) != state.rushStun.original' in loop
    for forbidden in ('abortCast(', 'applyEffects(', 'setAbnormalTime(', 'setCurrentMp(', 'enableSkill(', 'teleToLocation(', 'setXYZ(', 'setStunned('):
        assert forbidden not in loop,forbidden
    driver=text.split('private static boolean driveBerserkerStep(',1)[1]
    assert driver.index('driveOwnRushStunLoop(')<driver.index('state.movement.rushPending()')
    assert 'final BuffInfo previousStun = ownRushStun(bot, enemy);' in driver
    assert 'state.rushStun.requested(enemy, previousStun, now);' in driver
    for path in (ROOT/'game/data/stats/skills').glob('*.xml'):
        rush=next((s for s in ET.parse(path).getroot().findall('skill') if s.get('id')=='793'),None)
        if rush is not None:break
    assert rush is not None and rush.find('.//target').get('mindistance')=='200'
    assert any(e.get('name')=='Stun' for e in rush.findall('./effects/effect'))
    assert int(rush.findtext('castRange'))>=200
    print('PASS: ranked Rush integration/source + actual793 XML guards',flush=True)

def main():
    guards()
    with tempfile.TemporaryDirectory(prefix='ranked-rush-original-') as tmp:
        tmp=Path(tmp); launcher=tmp/'Compile.java'
        launcher.write_text('import javax.tools.ToolProvider; public class Compile {public static void main(String[]a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        cp=os.pathsep.join(map(str,sorted((ROOT/'libs').glob('*.jar'))))
        sources=list(SCRIPTS.glob('*.java'))+[ROOT/'game/data/scripts/custom/GiranServices/Preparation.java']+[ROOT/'game/data/scripts/handlers/skill/effects/Stun.java',ROOT/'tests/fakeplayersai/RankedRushOriginalStunRegression.java']
        subprocess.run(['java',str(launcher),'-cp',cp,'-d',str(tmp/'classes'),*map(str,sources)],check=True)
        subprocess.run(['java','-cp',str(tmp/'classes')+os.pathsep+cp,'custom.fakeplayersai.RankedRushOriginalStunRegression'],check=True,timeout=60,cwd=ROOT/'game')
if __name__=='__main__':main()
