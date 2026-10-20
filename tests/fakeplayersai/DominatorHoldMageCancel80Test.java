package custom.fakeplayersai;
import java.util.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Structure80: real public hooks, native templates and PlayerAI; requests recorded, no live world. */
public class DominatorHoldMageCancel80Test extends DominatorTankSleepTest {
 static java.lang.reflect.Method cancelReady;
 static long clock(){return System.nanoTime()/1000000L;}
 static void prompt(Object s)throws Exception {for(String field:new String[]{"nextDecision","observed","hpObserved"})f(state,field).setLong(s,0);}
 static boolean cancelReady(Player p,Player e,Object s,long t)throws Exception{return (boolean)cancelReady.invoke(null,p,e,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),s,t);}
 static void cp(Player p,double value)throws Exception {f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(p.getStatus(),value);}
 static boolean publicCancel(Player p,Player e,boolean ranked){last=0;targetEnemy=e;return BotTactics.tickMageCancel(p,e,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),ranked);}
 static boolean publicTank(Player p,Player e,boolean ranked){last=0;targetEnemy=e;return BotTactics.tickInvulnerableMelee(p,e,BotPresets.getPresetByClassId(115),ranked);}
 @SuppressWarnings("unchecked") static Object publicState(Player p)throws Exception {return ((Map<Integer,Object>)f(BotTactics.class,"STATES").get(null)).get(p.getObjectId());}
 @SuppressWarnings("unchecked") static void sleepCycle80()throws Exception {
  var roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{90,91,99,106})for(int id:new int[]{110,368,760}) {
   Player e=actor(cid,ranked,human),p=owner(115,ranked);foeReady(p,e);arena(p,e);BuffInfo defense=defense(e,id);
   for(int skill:new int[]{1097,1245,1306})learn(p,nativeSkill(skill));cp(p,10000);
   NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);
   if(!human)roster.put(e.getObjectId(),e);if(ranked)roster.put(p.getObjectId(),p);else BotTactics.initializeTraining(p,e);
   try {
    // An accepted request alone must not trigger sleep retreat.
    publicTank(p,e,ranked);Object s=publicState(p);prompt(s);
    if(last!=1097){verify(publicTank(p,e,ranked)&&last==1097,"ready sleep requested");}
    finish(p,e,s,clock()+5000);prompt(s);
    appliedSleep(p,e,115);int casts=traces.get(p).requests;
    verify(publicTank(p,e,ranked)&&last==0&&p.isMoving(),"actual own sleep starts bounded retreat, not CP/nuke");
    verify(ai.requested!=null&&p.calculateDistance2D(ai.requested)>10,"real native movement not teleport");
    int moves=ai.requests;for(int n=0;n<5;n++)verify(publicTank(p,e,ranked)&&ai.requests==moves&&traces.get(p).requests==casts,"owns complete retreat without orbit or cast spam");
    at(p,ai.requested.getX(),ai.requested.getY());f(Creature.class,"_move").set(p,null);
    verify(Math.abs(p.calculateDistance2D(e)-299)<2,"Dominator sleep separation300, not endless circle");
    for(int n=0;n<10;n++)verify(publicTank(p,e,ranked)&&!p.isMoving()&&ai.requests==moves&&last==0,"arrived: HOLD until actual wake, no ordinary rotation after5s");
    verify(run(p,e,s,clock()+20000)&&last==0&&!p.isMoving()&&ai.requests==moves,"still actually asleep after20s: no old5s damage fallback or new orbit");
    cp(p,2000);prompt(s);verify(publicTank(p,e,ranked)&&last==1306&&p.getTarget()==p&&!p.isMoving(),"legal self CP recovery allowed while holding sleep");
    finish(p,e,s,clock()+5000);disable(p,1306);cp(p,10000);prompt(s);
    verify(publicTank(p,e,ranked)&&last==0&&ai.requests==moves&&!p.isMoving(),"after CP no orbit/damage resumes while asleep");
    clearSleep(e);disable(p,1097);prompt(s);
    verify(publicTank(p,e,ranked)&&last==0&&p.isMoving()&&ai.requests>moves,"wake with sleep reuse: resume defensive kite");
    disabled(p).remove(p.getKnownSkill(1097).getReuseHashCode());prompt(s);
    // Finish's simulated future result time also sets ordinary failure backoff: expire it explicitly.
    ((Map<?,?>)f(state,"failures").get(s)).clear();
    verify(publicTank(p,e,ranked)&&last==1097&&!p.isMoving(),"wake ready sleep interrupts resumed kite");finish(p,e,s,clock()+5000);prompt(s);
    appliedSleep(p,e,115);int before=ai.requests;
    // Still at299: a new sleep may HOLD immediately; move close to verify a newly armed retreat.
    at(p,e.getX()+80,e.getY());
    verify(publicTank(p,e,ranked)&&last==0&&p.isMoving()&&ai.requests>before,"second landed sleep rearms retreat, no stale finished flag");
    defense.setInUse(false);before=ai.requests;
    verify(!publicTank(p,e,ranked)&&!p.isMoving()&&ai.requests==before,"defense ends mid-retreat: release movement/lease, no extra orbit");
    clearSleep(e);disable(p,1097);prompt(s);
    verify(run(p,e,s,clock()+10000)&&last==1245,"after all protection expires ordinary drain rotation restored");
   }finally{BotTactics.forget(p.getObjectId());roster.remove(p.getObjectId());roster.remove(e.getObjectId());}
  }
 }
 static void holdSafety80()throws Exception {
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(90,ranked,true),p=owner(115,ranked);foeReady(p,e);Object s=arena(p,e);defense(e,760);learn(p,nativeSkill(1245));appliedSleep(p,e,115);
   flags(p,EffectFlag.ROOTED.getMask());verify(run(p,e,s,10000)&&last==0&&!p.isMoving(),"root during sleep: HOLD, no damage fallback");flags(p,0);
   verify(run(p,e,s,11000)&&last==0&&p.isMoving(),"root ends: ordinary retreat resumes");
   flags(p,EffectFlag.STUNNED.getMask());f(Creature.class,"_move").set(p,null);verify(run(p,e,s,12000)&&last==0&&!p.isMoving(),"stun never bypassed");flags(p,0);
   Player other=actor(115,ranked,true);appliedSleep(other,e,115);verify(run(p,e,s,15000)&&last==0&&!p.isMoving(),"foreign actual sleep: safe HOLD without inventing own landed effect");
   flags(e,EffectFlag.SLEEP.getMask());e.getEffectList().getDebuffs().clear();verify(run(p,e,s,16000)&&last==0&&!p.isMoving(),"missing sleep BuffInfo never falls back to orbit/damage");
  }
 }
 static boolean priorityTarget(int cid){return cid==97||cid==99||List.of(88,90,91,93,101,106,108,113,114,131).contains(cid);}
 static void cancel80()throws Exception {
  List<Integer> targets=new ArrayList<>();for(var preset:BotPresets.getPresets())targets.add(preset.getClassId());targets.add(99);
  for(int cid:new int[]{94,103}) {
   var preset=BotPresets.getPresetByClassId(cid);int[] saved=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);int nuke=cid==94?1230:1235;
   try {
    preset.setRotationOverride(new int[]{nuke});
    for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int target:targets) {
     Player e=actor(target,ranked,human),p=owner(cid,ranked);foeReady(p,e);Object s=arena(p,e);learn(p,nativeSkill(1056));learn(p,nativeSkill(nuke));
     boolean priority=priorityTarget(target);verify(cancelReady(p,e,s,10000)==priority,"Cancel scope: melee+Cardinal only, class="+target);
     NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);int[] c=ArenaCombatRules.center(p);ai.setIntentionMoveTo(new org.l2jmobius.gameserver.entity.Location(c[0]+200,c[1],0));int moves=ai.requests;
     run(p,e,s,10000);
     if(priority) {
      verify(last==1056&&p.getTarget()==e&&!p.isMoving()&&ai.requests==moves,"Cancel injected despite override, before nuke/kite, correct target");
      int casts=traces.get(p).requests;run(p,e,s,10100);verify(p.isCastingNow()&&traces.get(p).requests==casts,"never duplicate/cancel an actual cast");finish(p,e,s,17000);
      disable(p,1056);verify(run(p,e,s,20000)&&last==nuke,"native Cancel reuse: normal damage rotation, no idle wait");finish(p,e,s,25000);
      disabled(p).clear();verify(run(p,e,s,40000)&&last==1056,"each newly native-ready Cancel is first again");finish(p,e,s,47000);
     }else verify(last!=1056,"non-targeted ranged class does not gain injected Cancel");
    }
   }finally{preset.setRotationOverride(saved);}
  }
 }
 @SuppressWarnings("unchecked") static void publicCancel80()throws Exception {
  var roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  for(int cid:new int[]{94,103})for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int target:new int[]{93,97,90}) {
   Player e=actor(target,ranked,human),p=owner(cid,ranked);foeReady(p,e);arena(p,e);learn(p,nativeSkill(1056));learn(p,nativeSkill(cid==94?1230:1235));if(target==90)defense(e,110);
   if(!human)roster.put(e.getObjectId(),e);if(ranked)roster.put(p.getObjectId(),p);else BotTactics.initializeTraining(p,e);
   try {
    publicCancel(p,e,ranked);Object s=publicState(p);prompt(s);
    verify(publicCancel(p,e,ranked)&&last==1056,"mandatory hook chooses ready Cancel before generic escape/nuke, even without optional tactical driver");
    int casts=traces.get(p).requests;verify(publicCancel(p,e,ranked)&&p.isCastingNow()&&traces.get(p).requests==casts,"public in-flight Cancel retained");
    finish(p,e,s,clock()+7000);disable(p,1056);verify(!publicCancel(p,e,ranked),"native reuse releases mandatory hook to normal driver");
   }finally{BotTactics.forget(p.getObjectId());roster.remove(p.getObjectId());roster.remove(e.getObjectId());}
  }
 }
 static void cancelGates80()throws Exception {
  for(int cid:new int[]{94,103})for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(97,ranked,true),p=owner(cid,ranked);foeReady(p,e);Object s=arena(p,e);learn(p,nativeSkill(1056));Skill nativeCancel=nativeSkill(1056);
   verify(nativeCancel.getReuseDelay()==30000&&nativeCancel.getCastRange()==600&&nativeCancel.hasEffectType(org.l2jmobius.gameserver.mechanics.effects.EffectType.DISPEL),"native Cancel30s/600/Dispel unchanged");
   verify(cancelReady(p,e,s,10000),"native ready Cancel");e.setInvul(true);verify(!cancelReady(p,e,s,10000),"invulnerability respected");e.setInvul(false);
   appliedSleep(actor(115,ranked,true),e,115);verify(!cancelReady(p,e,s,10000),"sleep gate retained");clearSleep(e);
   for(EffectFlag flag:new EffectFlag[]{EffectFlag.MUTED,EffectFlag.STUNNED}){flags(p,flag.getMask());verify(!cancelReady(p,e,s,10000),"native mute/stun respected");flags(p,0);}
   mp(p,0);verify(!cancelReady(p,e,s,10000),"no freeMP");mp(p,10000);disable(p,1056);verify(!cancelReady(p,e,s,10000),"native reuse respected");disabled(p).clear();
   int x=e.getX(),y=e.getY();at(e,x+900,y);verify(!cancelReady(p,e,s,10000),"out-of-range unavailable");at(e,x,y);
   flags(p,EffectFlag.ROOTED.getMask());verify(cancelReady(p,e,s,10000),"root permits stationary native magical Cancel");flags(p,0);
   Player human=actor(cid,ranked,true);learn(human,nativeCancel);verify(!cancelReady(human,e,s,10000),"humans untouched");Player empty=owner(cid,ranked);verify(!cancelReady(empty,e,s,10000),"no synthetic learning");
   p.setCastingNow(true);int casts=traces.get(p).requests;verify(run(p,e,s,10000)&&last==0&&p.isCastingNow()&&traces.get(p).requests==casts,"other native cast never interrupted for Cancel");p.setCastingNow(false);
   traces.get(p).accept=false;verify(run(p,e,s,20000)&&last==1056&&f(state,"pendingId").getInt(s)==0,"rejected request uses ordinary failure processing");traces.get(p).accept=true;
   verify(!cancelReady(p,e,s,20001)&&cancelReady(p,e,s,30000),"no immediate retry through backoff, then ready again");
   Player tank=actor(90,ranked,true);foeReady(p,tank);arena(p,tank);defense(tank,760);
   Object movingState=fresh();last=0;targetEnemy=tank;verify((boolean)support.invoke(null,p,tank,BotPresets.getPresetByClassId(cid),movingState,40000L,true,true)&&last==1056,"moving-support escape admits only legal Cancel for these mages");
   finish(p,tank,movingState,47000);last=0;targetEnemy=tank;verify(!(boolean)support.invoke(null,p,tank,BotPresets.getPresetByClassId(cid),fresh(),50000L,true,false)&&last==0,"pure defense-only does not become generic offensive mode");
  }
 }
 public static void main(String[]args){try {
  org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  support=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class,boolean.class);support.setAccessible(true);
  cancelReady=BotTactics.class.getDeclaredMethod("mageCancelReady",Player.class,Player.class,BotClassPreset.class,state,long.class);cancelReady.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("hold-cancel80-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();sleepCycle80();holdSafety80();cancel80();publicCancel80();cancelGates80();System.out.println("PASS: "+checks+" structure80 Dominator sleep retreat/HOLD/wake/CP/defense-end and Archmage/Mystic Muse ready-Cancel priority/native-gate checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
