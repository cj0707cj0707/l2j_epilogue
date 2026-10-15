#!/usr/bin/env python3
"""Compile production bot scripts, then test pursuit with isolated API doubles.

Run from any directory: python3 tests/fakeplayersai/run_olympiad_chase_tests.py
Requires Python 3 and a Java 25 JDK (including jdk.compiler); no server or DB.
The API doubles test logic only, not the real AI, geodata or networking.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = "org.l2jmobius.gameserver."
STUBS = {
    "ai.Intention": "public enum Intention { IDLE, ATTACK, CAST, MOVE_TO }",
    "entity.Location": """public class Location {
        private final int x,y,z;
        public Location(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
        public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}
    }""",
    "entity.actor.Player": """public class Player {
        public int id, x,y,z,dx,dy,dz,game=0,instance=0;
        public double speed=240;
        public boolean mode=true,start=true,dead,teleport,casting,disabled,outOfControl,moving,attacking,allDisabled;
        public boolean attackable=true,autoPlaying,rejectMove;
        public Object client; public Object getClient(){return client;}
        public int itemTicks,rotationTicks;
        public final Settings settings=new Settings();
        public static class Settings {
            public final java.util.Collection<Integer> buffs=new java.util.concurrent.CopyOnWriteArrayList<>();
            public java.util.Collection<Integer> getAutoBuffs(){return buffs;}
        }
        public Settings getAutoUseSettings(){return settings;}
        public boolean isAutoPlaying(){return autoPlaying;} public void setAutoPlaying(boolean b){autoPlaying=b;}
        public Player target;
        public final AI ai=new AI();
        public Player(int id,int x,int y){this.id=id;this.x=x;this.y=y;}
        public int getObjectId(){return id;} public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}
        public int getXdestination(){return moving?dx:x;} public int getYdestination(){return moving?dy:y;} public int getZdestination(){return moving?dz:z;}
        public int getOlympiadGameId(){return game;} public int getInstanceId(){return instance;}
        public boolean isInOlympiadMode(){return mode;} public boolean isOlympiadStart(){return start;}
        public boolean isAlikeDead(){return dead;} public boolean isTeleporting(){return teleport;} public boolean isCastingNow(){return casting;}
        public boolean isMovementDisabled(){return disabled;} public boolean isOutOfControl(){return outOfControl;}
        public boolean isAttackingNow(){return attacking;} public boolean isAllSkillsDisabled(){return allDisabled;}
        public boolean isMoving(){return moving;} public boolean isAutoAttackable(Player p){return attackable;}
        public double getMoveSpeed(){return speed;} public Player getTarget(){return target;}
        public AI getAI(){return ai;} public double calculateDistance2D(Player p){return Math.hypot((double)x-p.x,(double)y-p.y);}
        public class AI {
            public org.l2jmobius.gameserver.ai.Intention intention=org.l2jmobius.gameserver.ai.Intention.IDLE;
            public int moves=0,attacks=0,idles=0;
            public org.l2jmobius.gameserver.ai.Intention getIntention(){return intention;}
            public void setIntentionMoveTo(org.l2jmobius.gameserver.entity.Location p){moves++;if(rejectMove)return;dx=p.getX();dy=p.getY();dz=p.getZ();moving=true;intention=org.l2jmobius.gameserver.ai.Intention.MOVE_TO;}
            public void setIntentionIdle(){idles++;moving=false;intention=org.l2jmobius.gameserver.ai.Intention.IDLE;}
            public void setIntentionAttack(Player p){attacks++;moving=false;intention=org.l2jmobius.gameserver.ai.Intention.ATTACK;}
        }
    }""",
    "taskmanagers.AutoPlayTaskManager": """public class AutoPlayTaskManager {
        private static final AutoPlayTaskManager INSTANCE=new AutoPlayTaskManager();
        public static final java.util.Set<org.l2jmobius.gameserver.entity.actor.Player> pool=new java.util.HashSet<>();
        public static int starts,stops;
        public static AutoPlayTaskManager getInstance(){return INSTANCE;}
        public synchronized void startAutoPlay(org.l2jmobius.gameserver.entity.actor.Player p){if(pool.contains(p))return;starts++;p.setAutoPlaying(true);pool.add(p);}
        public void stopAutoPlay(org.l2jmobius.gameserver.entity.actor.Player p){if(pool.remove(p)){stops++;p.setAutoPlaying(false);}}
        // Mimic the retarget branch, which can replace an existing move. Like the core,
        // the task checks pool membership, NOT isAutoPlaying(). No thread timing claim.
        public void retargetTick(org.l2jmobius.gameserver.entity.actor.Player p){if(pool.contains(p)&&!p.casting&&p.target!=null)p.ai.setIntentionAttack(p.target);}
    }""",
    "geoengine.GeoEngine": """public class GeoEngine {
        private static final GeoEngine INSTANCE=new GeoEngine();
        public static boolean blocked=false,visible=true; public static int minAllowedY=-100000;
        public static GeoEngine getInstance(){return INSTANCE;}
        public boolean canSeeTarget(org.l2jmobius.gameserver.entity.actor.Player b,org.l2jmobius.gameserver.entity.actor.Player e){return visible;}
        public boolean canMoveToTarget(int x,int y,int z,int px,int py,int pz,int instance){return !blocked && py>=minAllowedY;}
    }""",
}
TEST = r'''
package custom.fakeplayersai;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
public class OlympiadChaseTest {
    static int checks;
    static void check(boolean b,String name){checks++;if(!b)throw new AssertionError(name);}
    static Location point(double bx,double by,double bs,double ex,double ey,double dx,double dy,double es,double hold){
        return OlympiadChase.intercept(bx,by,bs,ex,ey,0,dx,dy,es,hold);
    }
    static void update(Player b,Player e,int range,boolean attack){OlympiadChase.update(b,e,range,attack,0,0,2000);}
    static Player bot(){OlympiadChase.forget(1);GeoEngine.blocked=false;GeoEngine.visible=true;GeoEngine.minAllowedY=-100000;var p=new Player(1,0,0);org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.pool.clear();org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.getInstance().startAutoPlay(p);return p;}
    static Player enemy(){return new Player(2,800,0);}
    static int stateCount()throws Exception {
        var f=OlympiadChase.class.getDeclaredField("STATES");f.setAccessible(true);return ((java.util.Map<?,?>)f.get(null)).size();
    }
    static double simulate(int mode){
        double bx=0,by=0,ex=500,ey=0,px=500,py=0;
        double dt=0.05;
        for(int tick=0;tick<300;tick++){
            if(Math.hypot(ex-bx,ey-by)<=150)return tick*dt;
            if(tick % (mode==0?40:5)==0){
                Location p=(mode==2)?point(bx,by,240,ex,ey,ex,5000,200,100):new Location((int)ex,(int)ey,0);
                px=p.getX();py=p.getY();
            }
            double d=Math.hypot(px-bx,py-by), step=Math.min(240*dt,d);
            if(d>0){bx+=(px-bx)*step/d;by+=(py-by)*step/d;}
            ey+=200*dt;
        }
        return Double.POSITIVE_INFINITY;
    }
    public static void main(String[] args)throws Exception {
        Location p=point(0,0,240,800,0,800,0,0,100);
        check(p.getX()==700 && p.getY()==0,"stationary opponent: direct approach with standoff");
        p=point(0,0,240,800,0,800,2000,200,100);
        check(p.getY()>0 && p.getX()<800,"lateral runner: aim ahead, not at old position");
        p=point(0,0,240,800,0,0,0,200,100);
        check(p.getX()<700,"oncoming runner: no run to old far endpoint");
        p=point(0,0,200,800,0,2000,0,200,0);
        check(p.getX()==1050,"equal-speed escape: bounded fallback, no infinity");
        p=point(0,0,240,800,0,3000,0,1000,0);
        check(p.getX()==1100,"lead capped at 300 units");
        p=point(0,0,240,800,0,810,0,200,0);
        check(p.getX()==810,"prediction never exceeds server movement endpoint");
        p=point(0,0,240,800,0,800,0,999,100);
        check(p.getX()==700,"zero-length route: no division by zero");
        p=point(0,0,0,800,0,800,2000,200,100);
        check(p.getX()==700 && p.getY()==0,"zero pursuer speed: no invalid quadratic");
        p=point(0,0,240,300,0,1000,0,200,450);
        check(p.getX()==0,"already in standoff: never step backward");
        p=point(0,0,240,800,0,800,2000,200,450);
        check(Math.abs(Math.hypot(800-p.getX(),250-p.getY())-450)<1,"ranged standoff from prediction");
        double predictedTime=simulate(2),oldTime=simulate(0),reactiveTime=simulate(1);
        check(predictedTime<oldTime,"deterministic lateral pursuit improves old capture time");
        check(predictedTime<reactiveTime,"prediction improves capture vs equally frequent direct pursuit");
        System.out.printf("Simulation (not a live match): predictive %.2fs, 250ms direct %.2fs; old 2s direct %s%n",predictedTime,reactiveTime,Double.isFinite(oldTime)?(oldTime+"s"):"not caught within 15s");

        Player b=bot(),e=enemy();e.moving=true;e.dx=800;e.dy=1800;e.speed=200;b.target=e;
        update(b,e,150,true);check(b.ai.moves==1 && b.dy>0,"server destination drives prediction");
        update(b,e,150,true);check(b.ai.moves==1,"shared callers do not spam identical move commands");
        int north=b.dy;Thread.sleep(270);e.dy=-1800;update(b,e,150,true);
        check(b.ai.moves==2 && b.dy<0 && north>0,"direction reversal updates intercept");
        Thread.sleep(270);e.moving=false;update(b,e,150,true);
        check(b.dy==0,"stopped target discards old directional lead");
        e.x=100;update(b,e,150,true);check(b.ai.attacks==1 && b.ai.intention==Intention.ATTACK,"melee in range returns to attacking");
        check(stateCount()==0,"in-range route state cleared");

        b=bot();e=enemy();b.target=e;update(b,e,500,false);
        check(b.dx==350,"caster/bow far approach holds range, not enemy position");
        e.x=400;update(b,e,500,false);check(b.ai.idles==1 && b.ai.attacks==0,"ranged stops owned approach without autoattack");
        b=bot();e=enemy();update(b,e,500,false);
        b.ai.setIntentionMoveTo(new Location(-300,0,0));e.x=400;
        update(b,e,500,false);check(b.ai.idles==0 && b.dx==-300,"never stop an unrelated kite retreat");
        b=bot();e=enemy();b.casting=true;update(b,e,150,true);check(b.ai.moves==0,"active cast untouched");
        b=bot();e=enemy();b.ai.intention=Intention.CAST;update(b,e,150,true);check(b.ai.moves==1 && b.ai.intention==Intention.MOVE_TO,"CAST walk-into-range is replaced, not mistaken for a live cast");
        b=bot();e=enemy();b.disabled=true;update(b,e,150,true);check(b.ai.moves==0,"root/stun/immobilization respected");
        b=bot();e=enemy();b.outOfControl=true;update(b,e,150,true);check(b.ai.moves==0,"fear/confusion respected");
        b=bot();e=enemy();b.attacking=true;update(b,e,150,true);check(b.ai.moves==0,"attack animation not replaced with a saved move");
        b=bot();e=enemy();b.allDisabled=true;update(b,e,150,true);check(b.ai.moves==0,"all-skills-disabled move gate respected");

        for(String field:new String[]{"mode","start","dead","teleport","attackable"}){
            b=bot();e=enemy();Player.class.getField(field).setBoolean(e,field.equals("dead")||field.equals("teleport"));
            update(b,e,150,true);check(b.ai.moves==0,"opponent invalid: "+field);
        }
        for(String field:new String[]{"mode","start","dead","teleport"}){
            b=bot();e=enemy();Player.class.getField(field).setBoolean(b,field.equals("dead")||field.equals("teleport"));
            update(b,e,150,true);check(b.ai.moves==0,"bot invalid: "+field);
        }
        b=bot();e=enemy();e.game=1;update(b,e,150,true);check(b.ai.moves==0,"other match ignored");
        b=bot();e=enemy();e.instance=1;update(b,e,150,true);check(b.ai.moves==0,"other instance ignored");
        b=bot();e=enemy();b.game=-1;e.game=-1;update(b,e,150,true);check(b.ai.moves==0,"no game ignored");
        b=bot();update(b,null,150,true);check(b.ai.moves==0,"missing opponent ignored");
        b=bot();update(b,b,150,true);check(b.ai.moves==0,"self ignored");
        b=bot();e=enemy();e.x=2100;update(b,e,150,true);check(b.ai.moves==0,"opponent outside stadium ignored");
        b=bot();e=enemy();b.x=-2100;update(b,e,150,true);check(b.ai.moves==0,"bot outside stadium ignored");
        b=bot();e=enemy();GeoEngine.blocked=true;update(b,e,150,true);check(b.ai.moves==0,"blocked predicted AND direct routes issue no unsafe move");
        b=bot();e=enemy();e.moving=true;e.dx=800;e.dy=-1800;e.speed=200;GeoEngine.minAllowedY=0;
        update(b,e,150,true);check(b.ai.moves==1 && b.dy==0,"blocked shortcut falls back to walkable direct approach");
        b=bot();e=enemy();update(b,e,150,true);GeoEngine.blocked=true;update(b,e,150,true);
        check(b.ai.idles==1 && stateCount()==0,"invalidated owned route stops safely");
        b=bot();e=enemy();update(b,e,150,true);b.start=false;update(b,e,150,true);
        check(stateCount()==0,"end of match clears state");
        b=bot();e=enemy();update(b,e,150,true);OlympiadChase.forget(b.id);
        check(stateCount()==0,"despawn cleanup clears state");

        b=bot();e=enemy();e.x=400;update(b,e,500,true);e.x=530;update(b,e,500,true);
        check(b.ai.moves==0,"basic-attack range hysteresis prevents edge shuffling");
        e.x=570;update(b,e,500,true);check(b.ai.moves==1,"range opens beyond hysteresis: pursue again");
        b=bot();e=enemy();e.x=40;update(b,e,50,false);e.x=70;update(b,e,50,false);
        check(b.ai.moves==1,"skill-only pursuit never stops outside its real cast radius");
        b=bot();e=enemy();e.x=400;update(b,e,500,false);e.id=3;e.x=530;update(b,e,500,false);
        check(b.ai.moves==1,"engagement hysteresis never leaks into another opponent");
        b=bot();e=enemy();e.x=400;GeoEngine.visible=false;update(b,e,500,false);
        check(b.ai.moves==1,"occluded opponent inside hold range still gets an approach");
        b=bot();e=enemy();update(b,e,150,true);int initialX=b.dx;Thread.sleep(1550);update(b,e,150,true);
        check(b.ai.moves==2 && b.dx<initialX && b.dy!=0,"no server progress for 1.5s triggers a checked sidestep");
        b=bot();e=enemy();update(b,e,150,true);Thread.sleep(1550);b.casting=true;update(b,e,150,true);b.casting=false;update(b,e,150,true);
        check(b.ai.moves==1 && b.dy==0,"cast time does not count as a stuck run");
        OlympiadChase.forget(b.id);
        for(String mapName:new String[]{"STATES","PROGRESS","ENGAGED","ENGINE_HOLDS"}){var f=OlympiadChase.class.getDeclaredField(mapName);f.setAccessible(true);check(((java.util.Map<?,?>)f.get(null)).isEmpty(),"all state cleaned: "+mapName);}

        // Movement ownership, including setup/repair and intentional engine stops.
        b=bot();e=enemy();b.target=e;b.settings.buffs.add(100);update(b,e,150,true);
        check(OlympiadChase.isPursuing(b.id) && !b.autoPlaying,"far approach leases movement and pauses offensive rotation");
        check(b.settings.buffs.isEmpty(),"pending self-buffs cleared, not active effects");
        var tasks=org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.getInstance();
        check(!org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.pool.contains(b),"must remove AutoPlay pool member, not just clear a flag");
        int starts=org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.starts;
        OlympiadChase.startAutoPlayUnlessPursuing(b);
        check(!b.autoPlaying && starts==org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.starts,"setup cannot re-arm while pursuit owns movement");
        tasks.retargetTick(b);check(b.ai.intention==Intention.MOVE_TO,"pooled retarget cannot overwrite leased intercept");
        e.x=100;update(b,e,150,true);
        check(b.autoPlaying && !OlympiadChase.isPursuing(b.id),"range reached releases rotation");
        check(b.ai.intention==Intention.ATTACK,"range release returns melee attack");
        starts=org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.starts;update(b,e,150,true);
        check(starts==org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.starts,"release is idempotent");
        b=bot();e=enemy();b.casting=true;update(b,e,150,true);
        check(b.casting && b.ai.moves==0 && !b.autoPlaying,"live cast completes while NEW casts are gated");
        b.casting=false;update(b,e,150,true);check(b.ai.moves==1,"intercept begins after real cast finishes");
        b.casting=true;e.x=100;update(b,e,150,true);
        check(b.casting && b.autoPlaying,"in-range lease releases even while real cast finishes");
        b=bot();e=enemy();update(b,e,150,true);starts=org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.starts;
        b.dead=true;update(b,e,150,true);
        check(!OlympiadChase.isPursuing(b.id) && !b.autoPlaying && starts==org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.starts,"death drops lease without engine restart");
        b=bot();e=enemy();update(b,e,150,true);b.teleport=true;update(b,e,150,true);
        check(!OlympiadChase.isPursuing(b.id) && !b.autoPlaying,"teleport does not re-arm old pursuit");
        b=bot();e=enemy();update(b,e,150,true);b.start=false;update(b,e,150,true);
        check(!OlympiadChase.isPursuing(b.id) && !b.autoPlaying,"match end drops lease for normal lifecycle repair");
        b=bot();e=enemy();update(b,e,150,true);GeoEngine.blocked=true;update(b,e,150,true);
        check(b.autoPlaying && !OlympiadChase.isPursuing(b.id),"route failure restores normal combat engines");
        b=bot();e=enemy();tasks.stopAutoPlay(b);OlympiadChase.update(b,e,150,true,0,0,2000,false);
        check(!OlympiadChase.isPursuing(b.id) && !b.autoPlaying && b.ai.moves==1,"berserker engine ownership is not stolen");
        e.x=100;OlympiadChase.update(b,e,150,true,0,0,2000,false);
        check(!b.autoPlaying,"berserker intentional stop is not resumed at range");
        b=bot();e=enemy();b.rejectMove=true;update(b,e,150,true);
        check(stateCount()==0,"a refused MOVE_TO must not be recorded as accepted");
        b.rejectMove=false;update(b,e,150,true);
        check(stateCount()==1,"refused order is retried without a phantom throttle");
        OlympiadChase.forget(b.id);check(!OlympiadChase.isPursuing(b.id),"despawn drops movement lease");

        b=bot();e=enemy();b.mode=e.mode=false;b.instance=e.instance=11;
        org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager.getInstance().stopAutoPlay(b);
        OlympiadChase.registerTrainingPair(b,e);OlympiadChase.updateTraining(b,e,150,true,0,0,2000);
        check(b.ai.moves==1 && !b.autoPlaying,"isolated training intercept needs no Olympiad flags or engines");
        OlympiadChase.unregisterTrainingPair(b.id);b.ai.moves=0;OlympiadChase.updateTraining(b,e,150,true,0,0,2000);
        check(b.ai.moves==0,"unregistered training pair cannot move an actor");
        b.client=new Object();boolean rejected=false;try{OlympiadChase.registerTrainingPair(b,e);}catch(IllegalArgumentException ex){rejected=true;}
        check(rejected,"client-backed human can never register as chase bot");
        b=bot();e=enemy();update(b,e,150,true);BotTactics.owned=true;e.x=100;update(b,e,150,true);
        check(!b.autoPlaying,"range release cannot rearm AutoPlay over tactical owner");
        OlympiadChase.startAutoPlayUnlessPursuing(b);check(!b.autoPlaying,"setup respects tactical owner too");BotTactics.owned=false;
        OlympiadChase.forget(b.id);

        java.util.Random random=new java.util.Random(15);
        for(int i=0;i<5000;i++){
            double bx=random.nextDouble()*3000-1500,by=random.nextDouble()*3000-1500;
            double ex=random.nextDouble()*3000-1500,ey=random.nextDouble()*3000-1500;
            double dx=random.nextDouble()*3000-1500,dy=random.nextDouble()*3000-1500;
            p=point(bx,by,random.nextDouble()*500,ex,ey,dx,dy,random.nextDouble()*500,0);
            if(Math.hypot(p.getX()-ex,p.getY()-ey)>301)throw new AssertionError("unbounded lead");
            if((p.getX() < Math.min(ex,dx)-1)||(p.getX()>Math.max(ex,dx)+1)||(p.getY()<Math.min(ey,dy)-1)||(p.getY()>Math.max(ey,dy)+1))throw new AssertionError("prediction beyond endpoint");
        }
        check(true,"5000 deterministic geometry property cases");
        System.out.println("PASS: "+checks+" checks, including 5000 geometry cases. API doubles only; not a running GameServer.");
    }
}
'''

def write_java(root, full_name, content):
    path = root / (full_name.replace(".", "/") + ".java")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return path


def main():
    with tempfile.TemporaryDirectory(prefix="olympiad-chase-") as tmp:
        tmp = Path(tmp)
        launcher = tmp / "Compile.java"
        launcher.write_text("""import javax.tools.ToolProvider;
public class Compile { public static void main(String[] args) {
    System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,args));
} }""", encoding="utf-8")
        scripts = ROOT / "game/data/scripts/custom/fakeplayersai"
        jars = sorted((ROOT / "libs").glob("*.jar"))
        if not jars:
            raise SystemExit("Missing libs/*.jar: run from a checkout containing server libraries")
        subprocess.run(["java", str(launcher), "-cp", os.pathsep.join(map(str, jars)), "-d", str(tmp / "production"),
                        *map(str, sorted(scripts.glob("*.java")))], check=True)
        print("Production scripts: compiled against repository JARs", flush=True)
        sources = []
        for name, body in STUBS.items():
            full = PACKAGE + name
            sources.append(write_java(tmp / "src", full, "package " + full.rsplit(".", 1)[0] + ";\n" + body))
        sources.append(write_java(tmp / "src", "custom.fakeplayersai.OlympiadCombatGate", "package custom.fakeplayersai; import org.l2jmobius.gameserver.entity.actor.Player; public class OlympiadCombatGate { public static boolean ready(Player p){return true;} public static void hold(Player p){} }"))
        sources.append(write_java(tmp / "src", "custom.fakeplayersai.BotTactics", "package custom.fakeplayersai; public class BotTactics { public static boolean owned; public static boolean isOwned(int id){return owned;} }"))
        sources.append(write_java(tmp / "src", "custom.fakeplayersai.OlympiadChaseTest", TEST))
        sources.append(ROOT / "tests/fakeplayersai/PursuitOwnershipRegression.java")
        subprocess.run(["java", str(launcher), "-d", str(tmp / "tests"), *map(str, sources), str(scripts / "OlympiadChase.java")], check=True)
        subprocess.run(["java", "-cp", str(tmp / "tests"), "custom.fakeplayersai.OlympiadChaseTest"], check=True)
        subprocess.run(["java", "-cp", str(tmp / "tests"), "custom.fakeplayersai.PursuitOwnershipRegression"], check=True)


if __name__ == "__main__":
    main()
