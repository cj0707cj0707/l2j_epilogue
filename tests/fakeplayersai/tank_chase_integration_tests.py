#!/usr/bin/env python3
"""Run exact ranked/training caller bodies with inert services and the real Chase range condition.
No live AI/geodata/DB simulation; proves tank callers cannot retain the +60 no-route band.
"""
from pathlib import Path
import subprocess,tempfile,re
R=Path(__file__).resolve().parents[2];S=R/'game/data/scripts/custom/fakeplayersai'
def block(text,signature):
 start=text.index(signature);a=text.index('{',start);depth=1;i=a+1
 while depth:
  depth+=(text[i]=='{')-(text[i]=='}');i+=1
 return text[start:i]
manager=(S/'BotManager.java').read_text();training=(S/'TrainingArena.java').read_text();chase=(S/'OlympiadChase.java').read_text();spawner=(S/'BotSpawner.java').read_text()
ranked=block(manager,'private static void moveForOlympiadChase(')
fallback=block(training,'if (!BotTactics.tick(b, p, preset, false))')
a=chase.index('if (visible && ((distanceToEnemy <= holdRange)');a=chase.index('(',a);depth=1;i=a+1
while depth:
 depth+=(chase[i]=='(')-(chase[i]==')');i+=1
condition=chase[a+1:i-1]
pool=block(spawner,'private static final int[] TANK_DEFENSE_PHYSICAL =')
clean=re.sub(r'//[^\n]*','',pool).split('{',1)[1].split('}',1)[0]
assert [v.strip() for v in clean.split(',') if v.strip()][:2]==['VENGEANCE_ID','110'],'both shared/legacy defenses must prefer368 before110'
assert 'attackInRange && !closeControlTank && BotTactics.allowsBasicAttack(bot, preset)' in ranked
assert 'preset.getClassId() != 131 && !closeControlTank' in fallback
assert 'if (basicAttack && !b.isAttackingOrCastingNow()' in fallback,'caller must retain guarded ordinary tank attack'
assert '(!inOlympiad || (!isTankBot(bot) && getBotClassId(bot) != 113) || GeoEngine.getInstance().canSeeTarget(bot, target))' in manager,'main ranked tank fallback must not attack blindly'
assert '((isTankBot(bot) || getBotClassId(bot) == 113) && !GeoEngine.getInstance().canSeeTarget(bot, target))' in manager,'main tank route must also handle blocked LoS at close distance'
text='''import java.util.*;
public class TankChaseWiringRegression {
 static int checks, attacks;static boolean captured,ready=true,sight=true;static int held;static Player foe=new Player();
 static final int CLASS_DOOMBRINGER=131,CX=0,CY=0;static final double RADIUS=1000,OLYMPIAD_ARENA_MAX_DISTANCE=1000;
 static final int[][] OLYMPIAD_STADIUM_CENTERS={{0,0}};static final Map<Integer,Object> BERSERKER_STATES=new HashMap<>();
 static final Map<Integer,Boolean> ENGAGED=new HashMap<>();
 static class Player {int id=90;double distance=60;AI ai=new AI();int getOlympiadGameId(){return 0;}int getObjectId(){return id;}
  void setTarget(Player p){}AI getAI(){return ai;}boolean isAttackingOrCastingNow(){return false;}boolean isMovementDisabled(){return false;}
  boolean isSleeping(){return false;}boolean isAutoAttackable(Player b){return true;}double calculateDistance2D(Player p){return distance;}}
 static class AI {void setIntentionAttack(Player p){attacks++;}}
 static class BotClassPreset {int id;BotClassPreset(int i){id=i;}int getClassId(){return id;}}
 static class BotPresets {static BotClassPreset getPresetByClassId(int i){return new BotClassPreset(i);}}
 static class BotTactics {static boolean basic=true;static boolean strictMeleePressure(Player b,Player p,BotClassPreset c){return c.id==88||c.id==114;}static int tacticalApproachRange(Player b,Player p,BotClassPreset c){return 50;}
  static boolean allowsBasicAttack(Player b,BotClassPreset p){return basic;}static boolean tick(Player b,Player p,BotClassPreset c,boolean rank){return false;}}
 static class OlympiadCombatGate {static boolean ready(Player p){return ready;}static void hold(Player p){}}
 static class OlympiadChase {static void forget(int id){}static void update(Player b,Player p,int r,boolean a,int x,int y,double rad,boolean manage){held=r;captured=a;}
  static void updateTraining(Player b,Player p,int r,boolean a,int x,int y,double rad){held=r;captured=a;}}
 static class GeoEngine {static GeoEngine getInstance(){return new GeoEngine();}boolean canSeeTarget(Player b,Player p){return sight;}}
 static Player getOlympiadOpponent(Player p){return foe;}static int getBotClassId(Player p){return p.id;}
 static int approachWithMargin(int range){return Math.max(10,range-10);}
 static void check(boolean ok,String m){checks++;if(!ok)throw new AssertionError(m);}
 static boolean rangeHolds(double distanceToEnemy,int holdRange,boolean attackInRange,boolean engaged){
  boolean visible=sight;int id=1;ENGAGED.clear();if(engaged)ENGAGED.put(id,true);return CONDITION;
 }
 RANKED
 static void training(Player b,Player p,BotClassPreset preset) { FALLBACK }
 public static void main(String[] args){
  // The original branch demonstrably admits a +60 no-route dead band when attackInRange=true.
  check(rangeHolds(60,40,true,true),"baseline engaged60/hold40 retains no-route band");
  check(!rangeHolds(60,40,false,true),"strict caller must keep pursuing beyond actual40 range");
  for(int id:new int[]{88,90,91,106,113,114})for(boolean basic:new boolean[]{false,true}){
   Player bot=new Player();bot.id=id;BotTactics.basic=basic;
   captured=true;held=-1;moveForOlympiadChase(bot,500,true);
   check(!captured,"ranked tank disables attack hysteresis id="+id);check(held==40,"ranked uses actual close envelope, not500");
   check(!rangeHolds(60,held,captured,true),"ranked engaged tank cannot stop early without old route");
   attacks=0;captured=true;held=-1;training(bot,foe,new BotClassPreset(id));
   check(!captured,"training tank disables attack hysteresis id="+id);check(held==40,"training close margin");
   check(!rangeHolds(60,held,captured,true),"training engaged tank keeps approaching");
   check(attacks==0,"no fake out-of-range autoattack at60");bot.distance=30;training(bot,foe,new BotClassPreset(id));
   check(attacks==(basic?1:0),"legal close ordinary auto remains owned by training caller");
  }
  BotTactics.basic=true;
  for(int id:new int[]{92,108,131}){
   Player bot=new Player();bot.id=id;moveForOlympiadChase(bot,500,true);
   check(captured,"ranked non-tank behavior remains unchanged id="+id);
   attacks=0;training(bot,foe,new BotClassPreset(id));check(captured==(id!=131),"training previous131-only exception preserved id="+id);
  }
  sight=false;BotTactics.basic=true;
  for(int id:new int[]{88,90,91,106,113,114})for(double distance:new double[]{30,60}){
   Player bot=new Player();bot.id=id;bot.distance=distance;attacks=0;
   training(bot,foe,new BotClassPreset(id));check(attacks==0,"blocked sight never permits training manual hit, near or far");
   check(!captured,"blocked-sight tank remains strict movement-only");
   check(!rangeHolds(distance,held,captured,true),"even near blocked target must not be retained as in-range by Chase");
   moveForOlympiadChase(bot,500,true);check(!captured,"ranked blocked-sight tank does not enable Chase blind attacks");
   check(!rangeHolds(distance,held,captured,true),"ranked no-LoS route remains available near or far");
  }
  sight=true;
  System.out.println("PASS: "+checks+" exact caller/hysteresis/ordinary-attack integration checks (inert services)");
 }
}
'''.replace('CONDITION',condition).replace('RANKED',ranked).replace('FALLBACK',fallback)
with tempfile.TemporaryDirectory(prefix='tank-chase-wiring-') as d:
 t=Path(d);src=t/'TankChaseWiringRegression.java';src.write_text(text)
 subprocess.run(['java',str(src)],check=True)
print('PASS: actual shared physical pool368-first, strict tank chase in both callers, non-tank and armed fallback unchanged')
