package custom.fakeplayersai;
import java.util.*;
import org.l2jmobius.gameserver.entity.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Structure81: native ROOT identity, actual public/act rotation and native movement admission. */
public class TankRootStationary81Test extends DominatorHoldMageCancel80Test {
 static class AttackAI extends NativeMoveAI {
  int attacks;WorldObject victim;
  AttackAI(Player p){super(p);}
  @Override public void setIntentionAttack(WorldObject target){attacks++;victim=target;_intention=org.l2jmobius.gameserver.ai.Intention.ATTACK;}
 }
 static boolean hook(Player p,Player e,boolean ranked){last=0;targetEnemy=e;return BotTactics.tickInvulnerableMelee(p,e,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),ranked);}
 static BuffInfo root(Player tank,Player bot)throws Exception{return DominatorControlShadowTest.applied(tank,bot,403);}
 @SuppressWarnings("unchecked") static void cycle81()throws Exception {
  var roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  // A common legal learned probe isolates routing for all19 classes. It is test-only:
  // production does not learn skills, change presets, or add a new rotation.
  for(var preset:BotPresets.getPresets()) {
   // Tank mirrors no longer enter defense-kite/root leases (86). Stationary tank
   // auto-attack under root remains covered by auto81; mirror offense by test60.
   if(TankClassMechanics.tank(owner(preset.getClassId(),false)))continue;
   int[] saved=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);preset.setRotationOverride(new int[]{1230});
   try{for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{90,91,99,106})for(int id:new int[]{110,368,760}) {
    Player e=actor(cid,ranked,human),p=owner(preset.getClassId(),ranked);foeReady(p,e);quiet(p);arena(p,e);quiet(p);BuffInfo shield=defense(e,id);learn(p,nativeSkill(1230));if(preset.getClassId()==131)disable(p,1510);
    NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);
    if(!human)roster.put(e.getObjectId(),e);if(ranked)roster.put(p.getObjectId(),p);else BotTactics.initializeTraining(p,e);
    try{
     // Cardinal has its own mandatory no-kite/Trance priority. Its direct root
     // behavior is checked below; other18 enter the actual escape route first.
     disable(p,1230);hook(p,e,ranked);Object s=publicState(p);prompt(s);
     int moves=ai.requests;BuffInfo landed=root(e,p);verify(BotTactics.rootedByTank(p,e),"actual tank root detected all classes/tanks/modes");disabled(p).clear();if(preset.getClassId()==131)disable(p,1510);
     verify(hook(p,e,ranked)&&last==1230&&!p.isMoving()&&ai.requests==moves,"root: normal legal skill, no move/chase; class="+preset.getClassId());
     verify(p.isRooted()&&landed.isInUse()&&p.getTarget()==e,"root/effect/target retained, no fake cleanse");
     int casts=traces.get(p).requests;verify(hook(p,e,ranked)&&p.isCastingNow()&&traces.get(p).requests==casts,"root never cancels/duplicates native cast");
     finish(p,e,s,clock()+5000);disable(p,1230);prompt(s);
     verify(hook(p,e,ranked)&&last==0&&ai.requests==moves&&!p.isMoving(),"native reuse under root: hold rather than walk/free cast");
     clearSleep(p);verify(!BotTactics.rootedByTank(p,e),"actual root expiry recognized immediately");
     verify(hook(p,e,ranked)&&last==0,"protection remains: escape phase still owns bot");
     if(preset.getClassId()!=97)verify(p.isMoving()&&ai.requests>moves,"after root resume kite when still necessary");
     root(e,p);prompt(s);disabled(p).clear();if(preset.getClassId()==131)disable(p,1510);((Map<?,?>)f(state,"failures").get(s)).clear();
     // Prominence has its existing10s AI interval for Cardinal: use a fresh normal spell there.
     if(preset.getClassId()!=97)verify(hook(p,e,ranked)&&last==1230&&!p.isMoving(),"reapplied root reenters stationary rotation");
     p.setCastingNow(false);f(state,"pendingId").setInt(s,0);shield.setInUse(false);disable(p,1230);prompt(s);
     verify(hook(p,e,ranked)&&last==0&&!p.isMoving(),"defense expires first: remain stationary until ROOT ends");
     clearSleep(p);
     verify(!hook(p,e,ranked)&&!p.isMoving(),"root and defense expired: release lease, no stale escape");
    }finally{BotTactics.forget(p.getObjectId());roster.remove(p.getObjectId());roster.remove(e.getObjectId());}
   }}finally{preset.setRotationOverride(saved);}
  }
 }
 static void identity81()throws Exception {
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(90,ranked,true),p=owner(94,ranked);foeReady(p,e);quiet(p);arena(p,e);defense(e,110);
   flags(p,EffectFlag.ROOTED.getMask());verify(!BotTactics.rootedByTank(p,e),"root flag alone is insufficient");
   BuffInfo b=root(e,p);verify(BotTactics.rootedByTank(p,e),"live opponent effect required");
   b.setInUse(false);verify(!BotTactics.rootedByTank(p,e),"inactive ROOT excluded");b.setInUse(true);
   f(BuffInfo.class,"_effector").set(b,actor(90,ranked,true));verify(!BotTactics.rootedByTank(p,e),"another tank caster excluded");
   f(BuffInfo.class,"_effector").set(b,e);f(BuffInfo.class,"_effected").set(b,e);verify(!BotTactics.rootedByTank(p,e),"wrong victim excluded");f(BuffInfo.class,"_effected").set(b,p);
   p.getEffectList().getDebuffs().clear();verify(!BotTactics.rootedByTank(p,e),"stale stack slot excluded");
   root(actor(115,ranked,true),p);verify(!BotTactics.rootedByTank(p,e),"non-tank root is not tank-root escape exception");
   root(p,p);verify(!BotTactics.rootedByTank(p,e),"self root/UD excluded");
   DominatorControlShadowTest.applied(e,p,260);verify(!BotTactics.rootedByTank(p,e),"stun is not root");
   learn(p,nativeSkill(1230));verify(run(p,e,arena(p,e),10000)&&last==0,"stun still blocks ordinary rotation");
   root(e,p);flags(p,0);verify(!BotTactics.rootedByTank(p,e),"no ROOTED flag: no active exception");
   Player human=actor(94,ranked,true);effects(human);root(e,human);verify(!BotTactics.rootedByTank(human,e),"human victim unaffected");
  }
 }
 static void gates81()throws Exception {
  for(boolean ranked:new boolean[]{false,true}) {
   for(int cid:new int[]{94,103,115,97}) {
    Player e=actor(90,ranked,true),p=owner(cid,ranked);foeReady(p,e);quiet(p);Object s=arena(p,e);defense(e,760);root(e,p);
    int spell=cid==94?1230:cid==103?1235:cid==115?1245:1394;learn(p,nativeSkill(spell));
    NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);
    verify(run(p,e,s,10000)&&last==spell&&!p.isMoving()&&ai.requests==0,"real class rotation under root cid="+cid);finish(p,e,s,17000);
    disable(p,spell);verify(run(p,e,s,20000)&&last==0&&ai.requests==0,"reuse respected, no movement");disabled(p).clear();
    flags(p,EffectFlag.ROOTED.getMask()|EffectFlag.MUTED.getMask());verify(run(p,e,s,30000)&&last==0&&ai.requests==0,"magic silence respected while rooted");flags(p,EffectFlag.ROOTED.getMask());
    mp(p,0);if(cid!=97)verify(run(p,e,s,40000)&&last==0&&ai.requests==0,"nativeMP gate retained");mp(p,10000);
    int x=e.getX(),y=e.getY();at(e,x+1600,y);verify(run(p,e,s,50000)&&last==0&&ai.requests==0,"out-of-range: no cast/chase/teleport cid="+cid+" last="+last);at(e,x,y);
    p.setCastingNow(true);int casts=traces.get(p).requests;verify(run(p,e,s,60000)&&last==0&&traces.get(p).requests==casts&&p.isCastingNow(),"in-flight cast untouched");p.setCastingNow(false);
   }
   Player e=actor(90,ranked,true),p=owner(115,ranked);foeReady(p,e);quiet(p);Object s=arena(p,e);defense(e,110);root(e,p);learn(p,nativeSkill(1245));appliedSleep(p,e,115);
   verify(run(p,e,s,10000)&&last==0&&!p.isMoving(),"tank asleep: retain HOLD, do not wake despite rooted exception");
   clearSleep(e);disable(p,1097);verify(run(p,e,s,20000)&&last==1245&&!p.isMoving(),"wake while rooted: normal stationary drain");
   for(int cid:new int[]{94,103}) {p=owner(cid,ranked);foeReady(p,e);quiet(p);s=arena(p,e);root(e,p);learn(p,nativeSkill(1056));learn(p,nativeSkill(cid==94?1230:1235));verify(run(p,e,s,30000)&&last==1056,"Cancel priority retained under tank root");}
  }
 }
 static void auto81()throws Exception {
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(90,ranked,true),p=TankMechanicsKiteTest.knight(90,ranked,e);foeReady(p,e);quiet(p);Object s=arena(p,e);defense(e,110);root(e,p);mp(p,0);
   if(!ranked)for(Player a:List.of(p,e)){f(Player.class,"_isInDuel").setBoolean(a,true);f(Player.class,"_duelId").setInt(a,50);f(Player.class,"_duelState").setInt(a,org.l2jmobius.gameserver.entity.actor.holders.player.Duel.DUELSTATE_DUELLING);}
   AttackAI ai=new AttackAI(p);f(Creature.class,"_ai").set(p,ai);at(p,e.getX()+10,e.getY());
   verify(run(p,e,s,10000)&&ai.attacks==1&&ai.victim==e&&ai.requests==0,"ordinary MP-starved physical fallback swings already in range without movement attacks="+ai.attacks+" disabled="+p.isAttackDisabled()+" auto="+e.isAutoAttackable(p)+" basic="+BotTactics.allowsBasicAttack(p,BotPresets.getPresetByClassId(90))+" root="+BotTactics.rootedByTank(p,e)+" mana="+BotTactics.needsManaAutoAttack(p,BotPresets.getPresetByClassId(90))+" pending="+f(state,"pendingId").getInt(s));
   at(p,e.getX()+500,e.getY());verify(run(p,e,s,20000)&&ai.attacks==1&&ai.requests==0&&!p.isMoving(),"out-of-range auto fallback holds, never chases");
   at(p,e.getX()+10,e.getY());e.setInvul(true);verify(run(p,e,s,30000)&&ai.attacks==1,"no stationary auto through immunity");e.setInvul(false);
   appliedSleep(actor(115,ranked,true),e,115);verify(run(p,e,s,40000)&&ai.attacks==1,"auto fallback cannot wake sleeping target");
  }
 }
 public static void main(String[]args){try {
  org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("root81-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();cycle81();identity81();gates81();auto81();System.out.println("PASS: "+checks+" structure81 tank-root stationary rotation / no movement / root-end kite / native gates checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
