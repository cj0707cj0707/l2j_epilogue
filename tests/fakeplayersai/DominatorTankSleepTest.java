package custom.fakeplayersai;
import java.util.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Structure79: actual public tank-defense hook and native sleep admission; no live world. */
public class DominatorTankSleepTest extends BotGearCardinal78Test {
 static java.lang.reflect.Method support,ready;
 static boolean support(Player p,Player e,Object s,long t)throws Exception {
  last=0;targetEnemy=e;return (boolean)support.invoke(null,p,e,BotPresets.getPresetByClassId(115),s,t,true,true);
 }
 static BuffInfo defense(Player e,int id)throws Exception {BuffInfo b=new BuffInfo(e,e,nativeSkill(id));((Effects)e.getEffectList()).add(b);return b;}
 static boolean ready(Player p,Player e,Object s,long t)throws Exception {return (boolean)ready.invoke(null,p,e,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),s,t);}
 @SuppressWarnings("unchecked") static void public79()throws Exception {
  Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  var preset=BotPresets.getPresetByClassId(115);int[] saved=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);
  try {
   preset.setRotationOverride(new int[]{1245,1306}); // Sleep must not disappear behind an old override.
   for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{90,91,99,106})for(int id:new int[]{110,368,760}) {
    Player e=actor(cid,ranked,human),p=owner(115,ranked);foeReady(p,e);arena(p,e);defense(e,id);
    for(int learned:new int[]{1097,1245,1306})learn(p,nativeSkill(learned));hp(p,5000);f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(p.getStatus(),2000);
    NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);
    if(!human)roster.put(e.getObjectId(),e);if(ranked)roster.put(p.getObjectId(),p);else BotTactics.initializeTraining(p,e);
    try {
     // First enter the real kite phase while sleep is on native cooldown.
     disable(p,1097);disable(p,1245);disable(p,1306);last=0;targetEnemy=e;
     verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&last==0&&p.isMoving(),"unavailable sleep retains native kite");
     Map<Integer,Object> states=(Map<Integer,Object>)f(BotTactics.class,"STATES").get(null);Object s=states.get(p.getObjectId());
     disabled(p).clear();for(String field:new String[]{"nextDecision","observed","hpObserved"})f(state,field).setLong(s,0);
     int moves=ai.requests;last=0;
     verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&last==1097&&p.getTarget()==e&&!p.isMoving()&&ai.requests==moves,"public hook stops old kite and casts sleep instead of heal/drain; tank="+cid+" defense="+id);
     Skill sleep=p.getKnownSkill(1097);verify(sleep.getActivateRate()==20&&sleep.getMinChance()==20&&sleep.getMaxChance()==20,"20/20/20 unchanged");
     int casts=traces.get(p).requests;verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&p.isCastingNow()&&traces.get(p).requests==casts,"in-flight sleep never interrupted/duplicated");
     finish(p,e,s,100000);for(String field:new String[]{"nextDecision","observed","hpObserved"})f(state,field).setLong(s,0);
     last=0;verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&last==1097,"resisted/completed sleep can retry during defense through same public hook");finish(p,e,s,110000);
     appliedSleep(p,e,115);last=0;for(String field:new String[]{"nextDecision","observed","hpObserved"})f(state,field).setLong(s,0);
     verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&last!=1097&&last!=1245,"landed sleep not refreshed or woken by damage");
    }finally {BotTactics.forget(p.getObjectId());roster.remove(p.getObjectId());roster.remove(e.getObjectId());}
   }
  }finally{preset.setRotationOverride(saved);}
 }
 static void gates79()throws Exception {
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(90,ranked,true),p=owner(115,ranked);foeReady(p,e);Object s=arena(p,e);BuffInfo b=defense(e,110);learn(p,nativeSkill(1097));
   verify(ready(p,e,s,10000),"legal awake vulnerable protected tank");
   last=0;targetEnemy=e;verify(!((boolean)support.invoke(null,p,e,BotPresets.getPresetByClassId(115),s,10000L,true,false))&&last!=1097,"ordinary defenseOnly without movingSupport does not admit offense");
   verify(support(p,e,s,10000)&&last==1097,"movingSupport narrowly admits sleep");finish(p,e,s,15000);
   e.setInvul(true);verify(!ready(p,e,s,17000),"invulnerable enemy excluded");support(p,e,s,17000);verify(last!=1097,"no sleep cast on invulnerable tank");e.setInvul(false);
   appliedSleep(p,e,115);verify(!ready(p,e,s,20000),"own sleep excluded");support(p,e,s,20000);verify(last!=1097,"no duplicate own sleep");clearSleep(e);
   Player foreign=actor(115,ranked,true);appliedSleep(foreign,e,115);verify(!ready(p,e,s,22000),"another caster's sleep excluded");clearSleep(e);
   disable(p,1097);verify(!ready(p,e,s,25000)&&!support(p,e,s,25000),"native cooldown falls back to parent kite");disabled(p).clear();
   mp(p,0);verify(!ready(p,e,s,30000)&&!support(p,e,s,30000),"no MP means no free cast");mp(p,10000);
   for(EffectFlag flag:new EffectFlag[]{EffectFlag.MUTED,EffectFlag.STUNNED}) {flags(p,flag.getMask());verify(!ready(p,e,s,35000)&&!support(p,e,s,35000),"native silence/CC retained");flags(p,0);}
   int x=e.getX(),y=e.getY();at(e,x+900,y);verify(!ready(p,e,s,40000)&&!support(p,e,s,40000),"no illegal out-of-range cast or walking cast");at(e,x,y);
   b.setInUse(false);verify(!ready(p,e,s,45000),"inactive defense ignored");b.setInUse(true);((Effects)e.getEffectList()).remove(SkillFinishType.REMOVED,b);verify(!ready(p,e,s,45000),"removed defense ignored");
   defense(e,406);verify(!ready(p,e,s,45000),"icon alone is not strong-defense phase");defense(e,760);
   long clock=46000;for(int cc:new int[]{1208,260}) {
    DominatorControlShadowTest.applied(p,e,cc);verify(DominatorControlRetreat.control(p,e)!=null,"own control-hold fixture active");
    verify(support(p,e,s,clock)&&last==1097,"tank defense sleep allowed even during old root/stun hold restriction");finish(p,e,s,clock+5000);clearSleep(e);clock+=10000;
   }
   Player noSkill=owner(115,ranked);foeReady(noSkill,e);arena(noSkill,e);defense(e,760);verify(!ready(noSkill,e,arena(noSkill,e),50000),"no synthetic learning");
   for(int cid:new int[]{88,93,97,131}) {Player other=owner(cid,ranked);learn(other,nativeSkill(1097));verify(!ready(other,e,s,50000),"other classes excluded");}
   Player human=actor(115,ranked,true);learn(human,nativeSkill(1097));verify(!ready(human,e,s,50000),"human AI unchanged");
   Player nonTank=actor(88,ranked,true);effects(nonTank);defense(nonTank,760);verify(!ready(p,nonTank,s,50000),"non-tank with buff excluded");
   // Rejected native request keeps the standard failure backoff, not a fake landed sleep.
   traces.get(p).accept=false;verify(support(p,e,s,80000)&&last==1097&&f(state,"pendingId").getInt(s)==0,"rejected sleep not marked pending/landed last="+last+" pending="+f(state,"pendingId").getInt(s)+" ready="+ready(p,e,s,80000)+" decision="+f(state,"nextDecision").getLong(s));
   traces.get(p).accept=true;verify(!ready(p,e,s,80001),"ordinary failure backoff retained");verify(ready(p,e,s,90000),"retry after ordinary failure backoff");
  }
 }
 public static void main(String[]args){try {
  org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  support=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class,boolean.class);support.setAccessible(true);
  ready=BotTactics.class.getDeclaredMethod("dominatorTankSleepReady",Player.class,Player.class,BotClassPreset.class,state,long.class);ready.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("dominator79-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();public79();gates79();System.out.println("PASS: "+checks+" Dominator79 public-hook tank-defense sleep/stop-kite/retry/native admission checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
