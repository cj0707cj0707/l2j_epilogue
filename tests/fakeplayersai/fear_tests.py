#!/usr/bin/env python3
"""Compile the REAL Fear against shipped jars, then test it with isolated API doubles.

No server, DB, scripts loader, networking, production writes or JAR patching.
Doubles model only called APIs; these checks are not a live gameplay guarantee.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
FEAR = ROOT / 'game/data/scripts/handlers/skill/effects/Fear.java'
TEST = Path(__file__).with_name('BoundedFearTest.java')
BASE = 'org.l2jmobius.gameserver.'
STUBS = {
BASE + 'ai.Intention': '''public enum Intention { IDLE, MOVE_TO, CAST, ATTACK, REST }''',
BASE + 'entity.Location': '''public class Location {
 private final int x,y,z;
 public Location(int a,int b,int c){x=a;y=b;z=c;}
 public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}
}''',
BASE + 'entity.actor.enums.creature.Race': 'public enum Race { HUMAN, SIEGE_WEAPON }',
BASE + 'mechanics.conditions.Condition': 'public class Condition {}',
BASE + 'util.StatSet': 'public class StatSet {}',
BASE + 'mechanics.effects.EffectType': 'public enum EffectType { FEAR }',
BASE + 'mechanics.effects.EffectFlag': '''public enum EffectFlag { FEAR;
 public int getMask(){return 1;}
}''',
BASE + 'mechanics.effects.AbstractEffect': '''
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.util.StatSet;
public class AbstractEffect {
 public AbstractEffect(Condition a,Condition b,StatSet c,StatSet d){}
 public boolean canStart(Creature a,Creature b,Skill s){return true;}
 public int getEffectFlags(){return 0;} public EffectType getEffectType(){return null;}
 public int getTicks(){return 0;}
 public void onStart(Creature a,Creature b,Skill s){}
 public boolean onActionTime(Creature a,Creature b,Skill s){return false;}
 public void onExit(Creature a,Creature b,Skill s){}
}''',
BASE + 'mechanics.skill.Skill': '''public class Skill {
 private final int id; public Skill(int i){id=i;} public int getId(){return id;}
}''',
BASE + 'entity.actor.Creature': '''
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
public class Creature {
 public int x=110000,y=-77000,z=-3000,instance=7,id;
 public int dx,dy,dz,getAICalls,createdAI,abortAttacks,abortCasts,stopMoves,runCalls;
 public boolean aiPresent=true,spawned=true,teleport,dead,root,sleep,stun,paralyzed,casting;
 public boolean canAbort=true,attack,allSkillsDisabled,pet,attackable,queued;
 public Object castTarget=new Object();
 public Race race=Race.HUMAN;
 public final CreatureAI ai=new CreatureAI(this);
 public Creature(int i){id=i;}
 public int getObjectId(){return id;} public int getX(){return x;} public int getY(){return y;}
 public int getZ(){return z;} public int getInstanceId(){return instance;}
 public boolean isPlayer(){return false;} public boolean isSummon(){return false;}
 public boolean isAttackable(){return attackable;} public boolean isPet(){return pet;}
 public boolean hasAI(){return aiPresent;}
 public CreatureAI getAI(){getAICalls++;if(!aiPresent){createdAI++;aiPresent=true;}return ai;}
 public boolean isSpawned(){return spawned;} public boolean isTeleporting(){return teleport;}
 public boolean isAlikeDead(){return dead;} public boolean isDead(){return dead;}
 public boolean isMovementDisabled(){return root||sleep||stun||paralyzed||dead||teleport;}
 public boolean isAllSkillsDisabled(){return allSkillsDisabled||sleep||stun||paralyzed;}
 public boolean isCastingNow(){return casting;} public boolean canAbortCast(){return canAbort;}
 public void abortCast(){abortCasts++;casting=false;} public void abortAttack(){attack=false;abortAttacks++;}
 public void stopMove(Location p){stopMoves++;ai.moving=false;}
 public void setRunning(){runCalls++;}
 public int getXdestination(){return dx;} public int getYdestination(){return dy;} public int getZdestination(){return dz;}
 public Player asPlayer(){return (Player)this;}
 public Template getTemplate(){return new Template(race);}
 public static class Template { final Race race; public Template(Race r){race=r;} public Race getRace(){return race;} }
 public void arrive(){x=dx;y=dy;z=dz;ai.moving=false;}
}''',
BASE + 'entity.actor.Player': '''public class Player extends Creature {
 public Player(int id){super(id);} public boolean isPlayer(){return true;}
 public void setQueuedSkill(Object s,boolean a,boolean b){queued=s!=null;}
}''',
BASE + 'entity.actor.Summon': '''public class Summon extends Creature {
 public Summon(int id){super(id);} public boolean isSummon(){return true;}
}''',
BASE + 'ai.CreatureAI': '''
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.Location;
public class CreatureAI {
 public final Creature actor;
 public Intention intention=Intention.IDLE;
 public int moves,cancels,idles,afraidStart,afraidTick;
 public boolean moving;
 public CreatureAI(Creature c){actor=c;}
 public Intention getIntention(){return intention;}
 public void notifyActionCancel(){cancels++;actor.abortCast();}
 public void setIntentionIdle(){idles++;intention=Intention.IDLE;actor.castTarget=null;}
 public void setIntentionMoveTo(Location p){
  if(actor.isMovementDisabled()||actor.isAllSkillsDisabled()||actor.casting)return;
  intention=Intention.MOVE_TO;actor.dx=p.getX();actor.dy=p.getY();actor.dz=p.getZ();moves++;moving=true;
 }
 public void notifyActionAfraid(Creature effector,boolean initial){if(initial)afraidStart++;else afraidTick++;}
}''',
BASE + 'geoengine.GeoEngine': '''
import org.l2jmobius.gameserver.entity.Location;
public class GeoEngine {
 public static final GeoEngine INSTANCE=new GeoEngine();
 public static String mode="normal"; public static int projections,paths;
 public static GeoEngine getInstance(){return INSTANCE;}
 public Location getValidLocation(int x,int y,int z,int dx,int dy,int dz,int i){
  projections++;
  if(mode.equals("null"))return null;
  if(mode.equals("outside"))return new Location(x+10000,y+10000,z);
  if(mode.equals("farZ"))return new Location(dx,dy,z+1000);
  if(mode.equals("zero"))return new Location(x,y,z);
  return new Location(dx,dy,dz);
 }
 public boolean canMoveToTarget(int a,int b,int c,int d,int e,int f,int i){paths++;return !mode.equals("blocked");}
}''',
}
for name in ('Defender', 'FortCommander', 'SiegeFlag'):
    STUBS[BASE + 'entity.actor.instance.' + name] = (
        'import org.l2jmobius.gameserver.entity.actor.Creature;\n'
        f'public class {name} extends Creature {{ public {name}(){{super(999);attackable=true;}} }}'
    )


def main():
    before = FEAR.read_bytes()
    with tempfile.TemporaryDirectory(prefix='bounded-fear-test-') as d:
        tmp = Path(d)
        compiler = tmp / 'Compile.java'
        compiler.write_text('import javax.tools.ToolProvider; public class Compile { public static void main(String[] a) { System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a)); } }')
        jars = os.pathsep.join(str(p) for p in sorted((ROOT / 'libs').glob('*.jar')))
        real = tmp / 'real-classes'
        real.mkdir()
        subprocess.run(['java', str(compiler), '-cp', jars, '-d', str(real), str(FEAR)], check=True, cwd=tmp)
        print('PASS: actual Fear.java compiled against shipped libs/*.jar', flush=True)
        sources = []
        for qualified, body in STUBS.items():
            package, name = qualified.rsplit('.', 1)
            path = tmp / 'stubs' / Path(*package.split('.')) / (name + '.java')
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('package ' + package + ';\n' + body + '\n')
            sources.append(str(path))
        isolated = tmp / 'isolated-classes'
        isolated.mkdir()
        subprocess.run(['java', str(compiler), '-cp', str(isolated), '-d', str(isolated), *sources, str(FEAR), str(TEST)], check=True, cwd=tmp)
        result = subprocess.run(['java', '-ea', '-cp', str(isolated), 'handlers.skill.effects.BoundedFearTest'], cwd=tmp)
        assert FEAR.read_bytes() == before, 'Fear changed during tests (possibly concurrent parent edit); rerun on stable snapshot'
        if result.returncode:
            raise SystemExit(result.returncode)


if __name__ == '__main__':
    main()
