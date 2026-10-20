package custom.fakeplayersai;
import java.io.File;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Native effects/PlayerAI dispatch and real act/public hooks; only world movement/casts/packets recorded. */
public class CardinalSleepPriorityTest extends ControlPursuitTranceKiteTest {
 static void arrive(Player p,NativeMoveAI ai)throws Exception {at(p,ai.requested.getX(),ai.requested.getY());f(Creature.class,"_move").set(p,null);}
 static void centerSleep()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(boolean clarity:new boolean[]{false,true})
   for(int shield=0;shield<4;shield++)for(int heading=0;heading<360;heading+=45) {
   Player e=actor(108,ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);if(!clarity)clear(e,3282);effects(p);hp(p,10000);
   if((shield&1)!=0){nativeSkill(1418).applyEffects(p,p,true,10);p.setInvul(true);}
   if((shield&2)!=0)proc(p);
   int cx=-20939,cy=-210729;double a=Math.toRadians(heading);
   at(p,cx+(int)Math.round(300*Math.cos(a)),cy+(int)Math.round(300*Math.sin(a)));
   at(e,cx+(int)Math.round(350*Math.cos(a)),cy+(int)Math.round(350*Math.sin(a)));
   Object s=centered();f(state,"arenaCenterX").setInt(s,cx);f(state,"arenaCenterY").setInt(s,cy);
   NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);
   // A previous circular route must be replaced once by the center route.
   ai.setIntentionMoveTo(new org.l2jmobius.gameserver.entity.Location(cx+200,cy+180,0));int oldMoves=ai.requests;
   appliedSleep(p,e,97);verify(run(p,e,s,10000)&&last==0,"sleep preempts shield/proc/Clarity offense");
   verify(ai.requests==oldMoves+1&&ai.requested.getX()==cx&&ai.requested.getY()==cy,"first destination is actual arena center, not another orbit point");
   for(int n=1;n<=4;n++)verify(run(p,e,s,10000+n*100)&&last==0&&ai.requests==oldMoves+1,"own full center leg, no requeue/cast");
   arrive(p,ai);verify(run(p,e,s,12000)&&last==0&&ai.requests==oldMoves+2,"actual center arrival dispatches opposite endpoint");
   verify(Math.hypot(ai.requested.getX()-cx,ai.requested.getY()-cy)<=449,"opposite endpoint inside arena449");
   verify((ai.requested.getX()-cx)*(e.getX()-cx)+(ai.requested.getY()-cy)*(e.getY()-cy)<=0,"opposite half from sleeping opponent");
   double wanted=Math.min(SleepRetreat.distance(p)-1,Math.hypot(e.getX()-cx,e.getY()-cy)+449);
   verify(Math.abs(Math.hypot(ai.requested.getX()-e.getX(),ai.requested.getY()-e.getY())-wanted)<2,"retreat uses native Mana Burn range capped by arena");
   arrive(p,ai);for(int n=0;n<10;n++)verify(run(p,e,s,14000+n*500)&&last==0&&!p.isMoving()&&ai.requests==oldMoves+2,"arrived: HOLD until sleep ends, including own1418 and both shields");
   verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1398),false)&&!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1230),false),"shared candidate guard protects sleep with/without Clarity");
   clearSleep(e);clear(p,1418);p.setInvul((shield&2)!=0);disable(p,1418);
   verify(run(p,e,s,25000)&&last==1398&&!p.isMoving(),"actual wake: Mana Burn before proc-Trance/kite, with or without Clarity");finish(p,e,s,30000);
   verify(run(p,e,s,35000)&&last==1230,"then one ready Prominence, not premature resleep");finish(p,e,s,40000);
   var w=(CardinalSurvival.Window)f(state,"clarityWindow").get(s);verify(!w.pending(),"combo consumed, cannot repeat without another actual sleep");
  }
 }
 @SuppressWarnings("unchecked") static void publicAndGuards()throws Exception {
  Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  Set<Integer> training=(Set<Integer>)f(TrainingArena.class,"BOTS").get(null);
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true}) {
   Player e=actor(108,ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);effects(p);clear(e,3282);hp(p,10000);Object local=arena(p,e);
   if(ranked)roster.put(p.getObjectId(),p);else {training.add(p.getObjectId());BotTactics.initializeTraining(p,e);}
   if(!human)roster.put(e.getObjectId(),e);
   try {
    targetEnemy=e;last=0;proc(p);verify(BotTactics.tickCardinalControl(p,e,BotPresets.getPresetByClassId(97),ranked),"public mandatory proc hook handles active pair");
    Object s=((Map<Integer,?>)f(BotTactics.class,"STATES").get(null)).get(p.getObjectId());
    f(state,"observed").setLong(s,0);f(state,"nextDecision").setLong(s,0);f(state,"hpObserved").setLong(s,0);
    last=0;verify(BotTactics.tickCardinalControl(p,e,BotPresets.getPresetByClassId(97),ranked)&&last==1394,"public hook requests Trance before generic outer kite");
    p.setCastingNow(false);f(state,"pendingId").setInt(s,0);appliedSleep(p,e,97);
    last=0;verify(BotTactics.tick(p,e,BotPresets.getPresetByClassId(97),ranked)&&last==0&&p.isMoving(),"public common tick owns landed sleep before universal kite");
    BotRecoveryRules.init();
    for(int id:new int[]{1394,1398,1230})verify(BotRecoveryRules.guard(castEvent(p,e,id)).terminate(),"native admission rejects hostile skill into sleep "+id);
    var attack=new org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack();attack.setAttacker(p);attack.setTarget(e);
    verify(BotRecoveryRules.bluffAttack(attack).terminate(),"native attack admission protects sleep even from legacy engine");
    var result=org.l2jmobius.gameserver.mechanics.events.EventDispatcher.getInstance().notifyEvent(attack,org.l2jmobius.gameserver.mechanics.events.Containers.Global(),org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn.class);
    verify(result!=null&&result.terminate(),"real synchronous global listener blocks sleep autoattack");
    clearSleep(e);verify(BotRecoveryRules.guard(castEvent(p,e,1398))==null&&BotRecoveryRules.bluffAttack(attack)==null,"wake releases only sleep-specific admission guards");
   } finally {BotTactics.forget(p.getObjectId());roster.remove(p.getObjectId());roster.remove(e.getObjectId());training.remove(p.getObjectId());}
  }
 }
 static void edgeCases()throws Exception {
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(90,ranked,true),p=cardinal(e,ranked,1394,1398,1230,1218,1418);hp(p,10000);effects(p);Object s=arena(p,e);
   effects(e).add(new BuffInfo(e,e,nativeSkill(110)));e.setInvul(true);nativeSkill(1418).applyEffects(p,p,true,10);p.setInvul(true);appliedSleep(p,e,97);
   verify(run(p,e,s,10000)&&last==0&&p.isMoving(),"sleep beats own shield and protected/invulnerable tank kite");
   MoveAI ai=(MoveAI)p.getAI();int[] c=ArenaCombatRules.center(p);verify(ai.requested.getX()==c[0]&&ai.requested.getY()==c[1],"protected tank sleep still retreats through center");
   flags(p,EffectFlag.ROOTED.getMask());int moves=ai.requests;verify(run(p,e,s,12000)&&last==0&&!p.isMoving()&&ai.requests==moves,"root: hold, no alternate route/offense");flags(p,0);
   ai.accept=false;verify(run(p,e,s,14000)&&last==0&&!p.isMoving(),"rejected movement: hold rather than orbit or nuke");
   p.setCastingNow(true);verify(run(p,e,s,16000)&&p.isCastingNow()&&ai.requests==moves+1,"existing cast not aborted by sleep route");p.setCastingNow(false);
   // Another caster's real sleep must also be preserved; ownSleep stays owner-strict for other classes.
   BuffInfo b=SleepRetreat.ownSleep(p,e);f(BuffInfo.class,"_effector").set(b,actor(94,ranked,false));
   verify(SleepRetreat.ownSleep(p,e)==null&&SleepRetreat.cardinalSleep(p,e)==b,"any real sleep protects Cardinal target, owner-strict helper unchanged");
   verify(run(p,e,s,18000)&&last==0,"foreign sleep cannot fall back to kite/damage");
   CardinalSurvival.Window w=new CardinalSurvival.Window();w.observe(p,e);clearSleep(e);clear(e,3282);w.observe(p,e);verify(w.manaBurn(e),"actual wake remains tracked after Clarity expiry");
  }
 }
 public static void main(String[]args){try {
  ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("cardinal74-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();centerSleep();publicAndGuards();edgeCases();System.out.println("PASS: "+checks+" Cardinal native center/sleep-hold/wake/proc/outer-hook checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
