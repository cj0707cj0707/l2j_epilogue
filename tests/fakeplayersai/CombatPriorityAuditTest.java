package custom.fakeplayersai;
import java.io.File;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Audit62: real selection/conditions/effects with cast and movement recording seams. */
public class CombatPriorityAuditTest extends TankMechanicsKiteTest {
 @SuppressWarnings("unchecked") static Map<Integer,Long> disabled(Player p)throws Exception{return (Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);}
 static void disable(Player p,int id)throws Exception{disabled(p).put(p.getKnownSkill(id).getReuseHashCode(),System.currentTimeMillis()+60000);}
 static void foeReady(Player p,Player e)throws Exception{effects(p);effects(e);at(p,80,0);at(e,0,0);p.setTarget(e);f(Creature.class,"_zones").set(e,new byte[64]);f(Creature.class,"_ai").set(p,new MoveAI(p));}
 static void rescue()throws Exception{
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(90,ranked,true),p=knight(90,ranked,e);learn(p,nativeSkill(69));hp(p,2000);effects(e).add(new BuffInfo(e,e,nativeSkill(110)));
   int[] c=ArenaCombatRules.center(p);at(p,c[0]+80,c[1]);at(e,c[0],c[1]);
   Object s=centered();verify(run(p,e,s,10000)&&last==406,"emergency Angelic preempts defensive kite");verify(!p.isMoving(),"no movement replaces self cast");
   int requests=traces.get(p).requests;run(p,e,s,10100);verify(traces.get(p).requests==requests&&p.getTarget()==p,"inflight rescue retained, no duplicate");finish(p,e,s,15000);
   disable(p,406);disable(p,69);mp(p,0);run(p,e,s,20000);verify(last==0&&p.isMoving(),"unavailable rescue resumes native kite, no free MP");
   clear(e,110);run(p,e,s,25000);verify(!p.isMoving(),"defense expired: normal act releases its previous kite movement");
   Player h=owner(97,ranked),sleeper=actor(88,ranked,true);foeReady(h,sleeper);learn(h,nativeSkill(1398));learn(h,nativeSkill(1218));hp(h,1000);appliedSleep(h,sleeper,97);
   Object hs=centered();verify(run(h,sleeper,hs,30000)&&last==1218&&!h.isMoving(),"critical Cardinal heal before own-sleep retreat");finish(h,sleeper,hs,35000);hp(h,10000);
   verify(run(h,sleeper,hs,40000)&&h.isMoving()&&last==0,"after rescue continue original sleep retreat inside circle");
   for(int cc:new int[]{1208,260}){
    Player d=owner(115,ranked),melee=actor(88,ranked,true);foeReady(d,melee);at(d,350,0);at(melee,370,0);learn(d,nativeSkill(1306));learn(d,nativeSkill(1245));DominatorControlShadowTest.applied(d,melee,cc);
    f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(d.getStatus(),8400);Object ds=centered();
    verify(run(d,melee,ds,50000)&&last==1306&&!d.isMoving(),"allowed CP before centerward root/stun retreat");finish(d,melee,ds,55000);
    f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(d.getStatus(),8600);
    verify(run(d,melee,ds,60000)&&last==0&&d.isMoving(),"above85%: retreat, not Hammer/curse/CP spam");
   }
  }
 }
 @SuppressWarnings("unchecked") static void rooted()throws Exception{
  Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  for(boolean ranked:new boolean[]{false,true}){
   Player p=owner(131,ranked),e=actor(88,ranked,true);foeReady(p,e);learn(p,nativeSkill(833));hp(p,2000);flags(p,EffectFlag.ROOTED.getMask());disable(p,1510);
   if(!ranked){verify(run(p,e,centered(),10000)&&last==833,"training root with cleanse unavailable still heals");}
   else {
    roster.put(p.getObjectId(),p);Class<?> bs=Class.forName("custom.fakeplayersai.BotManager$BerserkerState");var c=bs.getDeclaredConstructor(int.class,int.class,int.class,int.class);c.setAccessible(true);Object state=c.newInstance(p.getX(),p.getY(),p.getZ(),p.getOlympiadGameId());
    var method=BotManager.class.getDeclaredMethod("driveBerserkerStep",Player.class,BotClassPreset.class,bs,Creature.class,long.class);method.setAccessible(true);last=0;targetEnemy=e;
    method.invoke(null,p,BotPresets.getPresetByClassId(131),state,e,10000L);
    Object shared=((Map<Integer,?>)f(BotTactics.class,"STATES").get(null)).get(p.getObjectId());
    f(CombatPriorityAuditTest.state,"observed").setLong(shared,0);f(CombatPriorityAuditTest.state,"hpObserved").setLong(shared,0);f(CombatPriorityAuditTest.state,"nextDecision").setLong(shared,0);
    verify((boolean)method.invoke(null,p,BotPresets.getPresetByClassId(131),state,e,10200L)&&last==833,"ranked driver root is not blanket cast prohibition");
    verify(!BotTactics.isOwned(p.getObjectId()),"ranked phase driver retains long-term ownership");roster.remove(p.getObjectId());BotTactics.forget(p.getObjectId());
   }
  }
 }
 static void pressureEvidence()throws Exception{
  var policy=new TacticsPolicy.DominatorPressure();var legal=List.of(new TacticsPolicy.Option(1097,TacticsPolicy.Kind.CONTROL,30),new TacticsPolicy.Option(1367,TacticsPolicy.Kind.SLOW,20),new TacticsPolicy.Option(1245,TacticsPolicy.Kind.DAMAGE,100));
  policy.requested(1097,true,false);policy.resolved(1097,false,false,true,false,false);verify(!policy.opened()&&!policy.controlFinished(),"accepted/started/interrupted is not landed or completed");
  policy.requested(1097,true,false);policy.resolved(1097,true,false,true,false,false);verify(!policy.opened()&&policy.controlFinished(),"resisted/budget exhausted distinct from landed control");
  verify(policy.choose(legal,List.of(1097),List.of(1367),false)==1367,"bounded control retries then curses");policy.resolved(1367,false,false,false,true,false);verify(!policy.curseDone(1367),"interrupted curse not recorded as applied");
  policy.requested(1367,false,true);policy.requested(1367,false,true);verify(policy.choose(legal,List.of(1097),List.of(1367),false)==1245,"bounded failed curse attempts then attack");
  policy.resolved(1245,false,false,false,false,true);verify(policy.controlFinished(),"interrupted damage cannot rearm sleep");policy.resolved(1245,true,false,false,false,true);verify(!policy.controlFinished(),"completed damage rearms next cycle");
  Player p=owner(115,false),e=actor(94,false,true);foeReady(p,e);at(p,0,0);at(e,30,0);learn(p,nativeSkill(1097));Object s=centered();run(p,e,s,10000);verify(last==1097,"native opener requested");p.setCastingNow(false);poll.invoke(null,p,e,s,15000L);
  verify(!((TacticsPolicy.DominatorPressure)f(state,"dominatorPressure").get(s)).opened(),"production sawCast alone cannot open control phase");
  // Native direct hit confirms zero-MP attacks too (Maximum Clarity), without a new tactical lock.
  p.setCastingNow(false);clearSleep(e);learn(p,nativeSkill(1245));
  Object ds=centered();TacticsPolicy.DominatorPressure phase=(TacticsPolicy.DominatorPressure)f(state,"dominatorPressure").get(ds);
  phase.requested(1097,true,false);phase.requested(1097,true,false);phase.requested(1367,false,true);phase.requested(1367,false,true);
  ((Map<Integer,Skill>)f(Creature.class,"_skills").get(p)).remove(1097);run(p,e,ds,20000);verify(last==1245,"direct attack after control budget");
  Object evidence=f(state,"pendingEvidence").get(ds);verify(evidence!=null,"one pending native damage observer");
  var event=new org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageDealt();event.setAttacker(p);event.setTarget(e);event.setSkill(p.getKnownSkill(1245));event.setDamage(100);
  var observe=evidence.getClass().getDeclaredMethod("observe",event.getClass());observe.setAccessible(true);observe.invoke(evidence,event);
  f(state,"pendingCompletionMp").setDouble(ds,0);p.setCastingNow(false);poll.invoke(null,p,e,ds,25000L);
  verify(!phase.controlFinished()&&f(state,"pendingEvidence").get(ds)==null,"native hit rearms cycle at zeroMP; observer detached on resolution");

 }
 public static void main(String[]args){try{
  ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);org.l2jmobius.gameserver.handler.TargetHandler.getInstance().registerHandler(new handlers.skill.targets.Party());skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("audit62-geodata-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  rescue();rooted();pressureEvidence();System.out.println("PASS: "+checks+" audit62 native rescue/root/bounded-control checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
