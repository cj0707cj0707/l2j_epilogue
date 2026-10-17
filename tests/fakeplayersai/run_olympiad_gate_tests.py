#!/usr/bin/env python3
"""Isolated gate flag/lifecycle/hold tests and actual-JAR descriptor checks. No live server."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
PKG = 'org.l2jmobius.gameserver.'
STUBS = {
'ai.CreatureAI': '''public class CreatureAI { public boolean idle,saved=true; public void notifyActionCancel(){saved=false;} public void setIntentionIdle(){idle=true;} }''',
'entity.Location': 'public class Location {}',
'entity.actor.Creature': '''public class Creature {
 public boolean attack=true,cast=true,moving=true; public Object target=new Object();
 public boolean throwAbort; public final org.l2jmobius.gameserver.ai.CreatureAI ai=new org.l2jmobius.gameserver.ai.CreatureAI();
 public void abortAttack(){attack=false;if(throwAbort)throw new IllegalStateException("injected");}
 public void abortCast(){cast=false;} public void stopMove(org.l2jmobius.gameserver.entity.Location l){moving=false;}
 public void setTarget(Object o){target=o;} public org.l2jmobius.gameserver.ai.CreatureAI getAI(){return ai;}
}''',
'entity.actor.Player': '''public class Player extends Creature {
 public int id,game,instance; public boolean online=true,mode=true,start=true,dead,teleport,queued=true;
 public Summon summon; public Player(int id){this.id=id;}
 public int getObjectId(){return id;} public int getOlympiadGameId(){return game;} public int getInstanceId(){return instance;}
 public boolean isOnline(){return online;} public boolean isInOlympiadMode(){return mode;} public boolean isOlympiadStart(){return start;}
 public boolean isAlikeDead(){return dead;} public boolean isTeleporting(){return teleport;}
 public void setQueuedSkill(Object s,boolean c,boolean d){if(s!=null||c||d)throw new AssertionError();queued=false;}
 public Summon getSummon(){return summon;}
}''',
'entity.actor.Summon': '''public class Summon extends Creature { public boolean follow=true; public void setFollowStatus(boolean b){follow=b;} }''',
'entity.actor.instance.Door': '''public class Door { public boolean open=true; public boolean isOpen(){return open;} }''',
'data.xml.DoorData': '''public class DoorData {
 public static final DoorData INSTANCE=new DoorData(); public final java.util.Map<Integer,org.l2jmobius.gameserver.entity.actor.instance.Door> doors=new java.util.HashMap<>();
 public static DoorData getInstance(){return INSTANCE;} public org.l2jmobius.gameserver.entity.actor.instance.Door getDoor(int id){return doors.get(id);}
}''',
'mechanics.olympiad.Olympiad': '''public class Olympiad {
 public static final Olympiad INSTANCE=new Olympiad(); public final java.util.Map<Integer,OlympiadGame> games=new java.util.HashMap<>();
 public static Olympiad getInstance(){return INSTANCE;} public java.util.Map<Integer,OlympiadGame> getOlympiadGames(){return games;}
}''',
'mechanics.olympiad.OlympiadGame': '''class OlympiadGame {
 public final int _stadiumID=0; public org.l2jmobius.gameserver.entity.actor.Player _playerOne,_playerTwo;
 protected long _startTime=1; public boolean aborted; public boolean isAborted(){return aborted;}
}''',
'mechanics.olympiad.OlympiadManager': '''public class OlympiadManager {
 protected static final OlympiadStadium[] STADIUMS={new OlympiadStadium()};
}''',
'mechanics.olympiad.OlympiadStadium': '''class OlympiadStadium { public int[] ids={1,2}; public int[] getDoorID(){return ids;} }''',
}
for manager,stop in [('AutoPlayTaskManager','stopAutoPlay'),('AutoUseTaskManager','stopAutoUseTask')]:
    STUBS['taskmanagers.'+manager] = '''public class %s {
 public static boolean running=true; private static final %s INSTANCE=new %s();
 public static %s getInstance(){return INSTANCE;} public void %s(org.l2jmobius.gameserver.entity.actor.Player p){running=false;}
}''' % (manager,manager,manager,manager,stop)

def source(root,name,body):
    path=root/(name.replace('.','/')+'.java');path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text('package '+name.rsplit('.',1)[0]+';\n'+body);return path

def manager_guards():
    text=(ROOT/'game/data/scripts/custom/fakeplayersai/BotManager.java').read_text()
    for forbidden in ('OLY_PENDING_OPENERS', 'OLY_PENDING_ENLIGHTENMENT', 'OLY_OPENER_GATE_TIME',
                      'fireOpeningSelfBuffs', 'fireEnlightenmentOpener', 'T+3', 'T+1'):
        assert forbidden not in text, forbidden
    hook=text[text.index('private static void olympiadPreStart'):text.index('private static void resetOlympiadPoints')]
    assert hook.index('maintainFullBuffs(player, caster)') < hook.index('claimPreStart(player)') < hook.index('applyPreCombatOpeners(player, preset)')
    assert 'ThreadPool.schedule' not in hook and 'applyAutoCombat' not in hook
    for name in ('combatTick', 'retargetTick', 'olympiadChaseTick'):
        chunk=text[text.index('private static void '+name+'()'):]
        chunk=chunk[:chunk.index('\n\tprivate static',30)]
        assert 'holdOlympiadIfNotReady(bot)' in chunk, name
        if name=='combatTick':
            assert chunk.index('bot.onTeleported()') < chunk.index('holdOlympiadIfNotReady(bot)'), 'clientless teleport cannot deadlock gate'
        assert chunk.index('holdOlympiadIfNotReady(bot)') < chunk.index('setIntentionAttack'), name
    # Every direct attack fallback is policy-guarded (including berserker phase fallback).
    for before in text.split('setIntentionAttack(')[:-1]:
        assert 'allowsBasicAttack(bot, preset)' in before[-1400:], 'attack fallback missing policy'
    assert 'Math.max(0, reach - 10)' in text, 'small safety margin inside legal offense range'
    assert 'BotTactics.recoveryOnly(bot, enemy.asPlayer(), preset)' in text, 'berserker recovery yields only movement tick'
    assert 'OLY_GATE_STAYS.remove(bot.getObjectId())' in text and 'OlympiadCombatGate.forget(bot.getObjectId())' in text
    print('PASS: Manager integration source guards (not runtime/thread simulation)',flush=True)

def main():
    manager_guards()
    with tempfile.TemporaryDirectory(prefix='olympiad-gate-') as tmp:
        tmp=Path(tmp); launcher=tmp/'Compile.java'
        launcher.write_text('import javax.tools.ToolProvider; public class Compile {public static void main(String[] a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
        compile=['java',str(launcher)]
        helper=ROOT/'game/data/scripts/custom/fakeplayersai/OlympiadCombatGate.java'
        cp=os.pathsep.join(str(p) for p in sorted((ROOT/'libs').glob('*.jar')))
        desc=ROOT/'tests/fakeplayersai/OlympiadCombatGateDescriptorTest.java'
        subprocess.run(compile+['-cp',cp,'-d',str(tmp/'actual'),str(helper),str(desc)],check=True)
        subprocess.run(['java','-cp',str(tmp/'actual')+os.pathsep+cp,'OlympiadCombatGateDescriptorTest'],check=True)
        sources=[source(tmp/'src',PKG+n,b) for n,b in STUBS.items()]
        sources += [ROOT/'tests/fakeplayersai/OlympiadCombatGateTest.java',helper]
        subprocess.run(compile+['-d',str(tmp/'logic'),*map(str,sources)],check=True)
        subprocess.run(['java','-cp',str(tmp/'logic'),'org.l2jmobius.gameserver.mechanics.olympiad.OlympiadCombatGateTest'],check=True)
        # A changed core reflection contract must reject readiness and claims, not fall back to time.
        broken=tmp/'src/org/l2jmobius/gameserver/mechanics/olympiad/OlympiadGame.java'
        broken.write_text(broken.read_text().replace('_startTime=1','_renamedStartTime=1'))
        failure=source(tmp/'src',PKG+'mechanics.olympiad.GateFailureTest','''public class GateFailureTest {
 public static void main(String[] args){
  var p=new org.l2jmobius.gameserver.entity.actor.Player(1);
  if(custom.fakeplayersai.OlympiadCombatGate.ready(p)||custom.fakeplayersai.OlympiadCombatGate.claimPreStart(p)||custom.fakeplayersai.OlympiadCombatGate.claimSummonPreparation(p))throw new AssertionError("reflection failure opened gate");
  custom.fakeplayersai.OlympiadCombatGate.hold(p);
  if(p.attack||p.cast||p.moving||p.queued)throw new AssertionError("hold unavailable on reflection failure");
  System.out.println("PASS: incompatible core descriptor fails closed; hold still works");
 }}''')
        subprocess.run(compile+['-d',str(tmp/'broken'),*map(str,[p for p in sources if p.name!='OlympiadCombatGateTest.java']),str(failure)],check=True)
        subprocess.run(['java','-cp',str(tmp/'broken'),'org.l2jmobius.gameserver.mechanics.olympiad.GateFailureTest'],check=True)
if __name__=='__main__': main()
