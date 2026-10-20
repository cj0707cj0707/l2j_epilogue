package custom.fakeplayersai;
import java.io.File;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Native templates/conditions/applied effects, recording cast/move only, no live server. */
public class ClarityBluffPhaseTest extends CombatPriorityAuditTest {
 static void asleep(Player p,Player e,Object s,long time)throws Exception{
  appliedSleep(p,e,97);run(p,e,s,time);verify(last!=1398&&last!=1230&&last!=1396,"Clarity: never burn/nuke/backfire a sleeping target");p.setCastingNow(false);f(state,"pendingId").setInt(s,0);
 }
 static void clarity()throws Exception{
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true}){
   Player e=actor(94,ranked,human),p=cardinal(e,ranked,1394,1398,1218,1418,1230,1396);Object s=centered();disable(p,1230);
   verify(run(p,e,s,10000)&&last==1394,"Clarity opener with Prominence on reuse: Trance, not Mana Burn");finish(p,e,s,15000);
   disable(p,1394);disable(p,1418);disable(p,1230);hp(p,10000);asleep(p,e,s,20000);
   // Already retreated: stay asleep at range, still no burn on the landed Trance.
   at(p,-440,0);f(Creature.class,"_move").set(p,null);run(p,e,s,25000);verify(last==0,"arrived after Trance: preserve sleep, no Mana Burn");
   clearSleep(e);f(Creature.class,"_move").set(p,null);at(p,-449,0);at(e,30,0);
   verify(run(p,e,s,30000)&&last==1398,"after actual Trance ended, legal Mana Burn with Clarity still active: actual="+last);finish(p,e,s,35000);
   // Survival recovery wins even in the open Mana Burn window.
   hp(p,5000);verify(run(p,e,s,40000)&&last==1218,"waking target: recovery before Mana Burn");finish(p,e,s,45000);hp(p,10000);
   asleep(p,e,s,50000);verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1398),false),"shared candidate guard also protects live sleep");
   verify(!((CardinalSurvival.Window)f(state,"clarityWindow").get(s)).manaBurn(e),"new real Trance closes previous burn window");
   clearSleep(e);clear(e,3282);f(Creature.class,"_move").set(p,null);at(p,0,0);at(e,30,0);disabled(p).clear();
   @SuppressWarnings("unchecked") Map<Integer,Skill> skills=(Map<Integer,Skill>)f(Creature.class,"_skills").get(p);skills.remove(1394);skills.remove(1398);skills.remove(1218);skills.remove(1418);
   verify(run(p,e,s,60000)&&last==1230,"Clarity gone: ordinary Prominence resumes");finish(p,e,s,65000);
   Player onlyBurn=cardinal(actor(94,ranked,human),ranked,1398);Player target=(Player)onlyBurn.getTarget();
   verify(run(onlyBurn,target,centered(),10000)&&last==0,"no observed successful Trance: no fabricated wake window");
   CardinalSurvival.Window w=new CardinalSurvival.Window();w.observe(onlyBurn,target);verify(!w.manaBurn(target),"no actual sleep history on a fresh pair");
   BuffInfo sleep=appliedSleep(onlyBurn,target,97);w.observe(onlyBurn,target);clearSleep(target);w.observe(onlyBurn,target);verify(w.manaBurn(target),"real removal opens wake window");
   clear(target,3282);w.observe(onlyBurn,target);nativeSkill(3282).applyEffects(target,target,true,60);w.observe(onlyBurn,target);verify(w.manaBurn(target),"Clarity replacement cannot discard the actually observed sleep/wake cycle");
  }
 }
 static void wakeSequence()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int mode:new int[]{0,1,2,3}) {
   Player e=actor(94,ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418,1396);Object s=centered();
   hp(p,10000);disable(p,1418);at(p,-449,0);at(e,30,0);
   appliedSleep(p,e,97);run(p,e,s,10000);disable(p,1394);disable(p,1230);clearSleep(e);f(Creature.class,"_move").set(p,null);
   verify(run(p,e,s,15000)&&last==1398,"wake with Trance on reuse: Mana Burn, both arenas/target kinds");
   int requests=traces.get(p).requests;run(p,e,s,15100);verify(traces.get(p).requests==requests&&p.isCastingNow(),"pending Mana Burn: no duplicate/premature Prominence/Trance");finish(p,e,s,20000);disabled(p).remove(p.getKnownSkill(1230).getReuseHashCode());
   if(mode==1)disable(p,1230);
   if(mode==2)((TacticsPolicy.CardinalProminence)f(state,"cardinalProminence").get(s)).accepted(23000);
   if(mode==3){hp(p,5000);verify(run(p,e,s,25000)&&last==1218,"recovery preempts owed Prominence");finish(p,e,s,26000);hp(p,10000);}
   if(mode==0||mode==3){verify(run(p,e,s,27000)&&last==1230,"exactly one ready Prominence after Mana Burn, before Trance");finish(p,e,s,28000);}
   at(p,0,0);at(e,30,0);f(Creature.class,"_move").set(p,null);
   disabled(p).remove(p.getKnownSkill(1394).getReuseHashCode());verify(run(p,e,s,30000)&&last==1394,"after Prominence or cooldown skip: return to Trance, never repeated burn/nuke");finish(p,e,s,35000);
   var w=(CardinalSurvival.Window)f(state,"clarityWindow").get(s);verify(!w.manaBurn(e)&&!w.prominence(e),"resleep request cannot reuse previous wake window");
  }
  // Pure phase policy: rejected requests do not advance; heals never consume an owed nuke.
  Player e=actor(94,false,true),p=cardinal(e,false,1394,1398,1230);var w=new CardinalSurvival.Window();
  appliedSleep(p,e,97);w.observe(p,e);clearSleep(e);w.observe(p,e);
  var legal=List.of(new TacticsPolicy.Option(1394,TacticsPolicy.Kind.CONTROL,54),new TacticsPolicy.Option(1398,TacticsPolicy.Kind.DAMAGE,62),new TacticsPolicy.Option(1230,TacticsPolicy.Kind.DAMAGE,62));
  verify(w.choose(legal,1)==1398&&w.choose(legal,1)==1398,"unaccepted/rejected burn leaves first phase intact");w.requested(1398);verify(w.choose(legal,1)==1230,"accepted burn owes nuke, not another sleep");w.requested(1230);verify(w.choose(legal,1)==1394,"accepted nuke leaves only resleep");
  clear(e,3282);w.observe(p,e);verify(!w.manaBurn(e)&&!w.prominence(e),"completed combo stays consumed across Clarity removal");
 }
 static BuffInfo bluff(Player p,Player e)throws Exception{
  clearSleep(e);Skill spell=nativeSkill(358);learn(p,spell);BuffInfo b=new BuffInfo(p,e,spell);
  for(var effect:spell.getEffects(EffectScope.GENERAL))if(!effect.isInstant())b.addEffect(effect);
  e.getEffectList().getDebuffs().add(b);
  @SuppressWarnings("unchecked") Map<AbnormalType,BuffInfo> slots=(Map<AbnormalType,BuffInfo>)f(org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList.class,"_stackedEffects").get(e.getEffectList());slots.put(spell.getAbnormalType(),b);
  flags(e,EffectFlag.STUNNED.getMask());return b;
 }
 static org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse castEvent(Player p,Player e,int id){
  var event=new org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse();event.setCaster(p);event.setTarget(e);event.setTargets(List.of(e));event.setSkill(nativeSkill(id));return event;
 }
 @SuppressWarnings("unchecked") static void guards(Player p,Player e,boolean ranked)throws Exception{
  Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  Set<Integer> training=(Set<Integer>)f(TrainingArena.class,"BOTS").get(null);
  if(ranked)roster.put(p.getObjectId(),p);else training.add(p.getObjectId());
  try{
   BotRecoveryRules.init();BotRecoveryRules.init();
   var event=new org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack();event.setAttacker(p);event.setTarget(e);
   verify(BotRecoveryRules.bluffAttack(event).terminate(),"native autoattack admission blocked throughout own Bluff");
   var result=org.l2jmobius.gameserver.mechanics.events.EventDispatcher.getInstance().notifyEvent(event,org.l2jmobius.gameserver.mechanics.events.Containers.Global(),org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn.class);
   verify(result!=null&&result.terminate(),"real global attack listener veto, not just AI preference");
   boolean rear=BluffRear.ready(p,e);
   verify((BotRecoveryRules.guard(castEvent(p,e,30))==null)==rear,"native cast admission requires actual rear and range");
   verify((!BotRecoveryRules.blockedBluffStrike(p,e,nativeSkill(30)))==rear,"effect-time facing guard agrees with admission");
   verify(BotRecoveryRules.guard(castEvent(p,e,821)).terminate(),"Shadow Step is not a rear-phase damaging strike");
   verify((BotRecoveryRules.guard(castEvent(p,e,531))==null)==rear,"Critical Wound also waits for rear arrival");
   if(!rear){
    double before=e.getCurrentHp();int handlers=0;
    for(int id:new int[]{30,16})for(var effect:nativeSkill(id).getEffects(EffectScope.GENERAL))
     if(effect.getName().equals("Backstab")||effect.getName().equals("FatalBlow")){effect.onStart(p,e,nativeSkill(id));handlers++;}
    verify(handlers==2&&e.getCurrentHp()==before,"both native blow handlers reject front execution without HP damage");
   }
   f(Player.class,"_client").set(p,unsafe.allocateInstance(org.l2jmobius.gameserver.network.GameClient.class));
   verify(BotRecoveryRules.bluffAttack(event)==null&&!BotRecoveryRules.blockedBluffStrike(p,e,nativeSkill(30)),"human character excluded even if mistakenly listed as managed");
   f(Player.class,"_client").set(p,null);
   if(ranked)f(Player.class,"_olympiadStart").setBoolean(p,false);else{training.remove(p.getObjectId());roster.put(p.getObjectId(),p);}
   verify(BotRecoveryRules.bluffAttack(event)==null&&!BotRecoveryRules.blockedBluffStrike(p,e,nativeSkill(30)),"inactive/world bot not subject to arena-only new guards");
   if(ranked)f(Player.class,"_olympiadStart").setBoolean(p,true);else{roster.remove(p.getObjectId());training.add(p.getObjectId());}
  }finally{if(ranked)roster.remove(p.getObjectId());else training.remove(p.getObjectId());}
 }
 @SuppressWarnings("unchecked") static void daggers()throws Exception{
  for(int cid:new int[]{93,101,108})for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true}){
   Player p=owner(cid,ranked),e=actor(94,ranked,human);foeReady(p,e);at(p,80,0);at(e,0,0);f(Weapon.class,"_type").set(p.getActiveWeaponItem(),WeaponType.DAGGER);
   // Native using-kind condition reads inventory mask, not the fists fallback alone.
   var item=(org.l2jmobius.gameserver.entity.item.instance.Item)unsafe.allocateInstance(org.l2jmobius.gameserver.entity.item.instance.Item.class);
   f(item.getClass(),"_itemTemplate").set(item,p.getActiveWeaponItem());
   ((org.l2jmobius.gameserver.entity.item.instance.Item[])f(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.class,"_paperdoll").get(p.getInventory()))[org.l2jmobius.gameserver.entity.itemcontainer.Inventory.PAPERDOLL_RHAND]=item;
   f(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.class,"_wearedMask").setInt(p.getInventory(),WeaponType.DAGGER.mask());
   for(int id:new int[]{358,30,531,821,412,453})learn(p,nativeSkill(id));BuffInfo b=bluff(p,e);Object s=centered();MoveAI ai=(MoveAI)p.getAI();
   verify(BluffRear.stun(p,e)==b,"only real current own358 STUN enters the phase");guards(p,e,ranked);
   // Even a stale ATTACK intention must not issue Idle while a native cast is already running.
   for(boolean simultaneous:new boolean[]{false,true}){
    f(org.l2jmobius.gameserver.ai.AbstractAI.class,"_intention").set(ai,org.l2jmobius.gameserver.ai.Intention.ATTACK);
    if(simultaneous)p.setCastingSimultaneouslyNow(true);else p.setCastingNow(true);
    verify(run(p,e,s,9000)&&last==0&&ai.getIntention()==org.l2jmobius.gameserver.ai.Intention.ATTACK&&p.getTarget()==e,"own Bluff: no Idle/retarget/duplicate during existing cast");
    p.setCastingNow(false);p.setCastingSimultaneouslyNow(false);
   }
   ai.setIntentionIdle();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
   verify(run(p,e,s,10000)&&last==0&&p.isMoving(),"rear movement preempts ready blows/Shadow Step and reaction gate");
   int requests=ai.requests;int gx=ai.requested.getX(),gy=ai.requested.getY();verify(gx>0&&Math.abs(gy)>30&&Math.hypot(gx,gy)<=85,"first waypoint goes around victim, not a chord through its center");
   at(p,50,0);verify(run(p,e,s,10100)&&last==0&&ai.requests==requests,"partial movement is NOT a completed rear approach");
   at(p,gx,gy);f(Creature.class,"_move").set(p,null);
   for(int n=0;n<8&&!BluffRear.ready(p,e);n++) {
    run(p,e,s,11000+n*200);verify(last==0&&p.isMoving(),"no strike before actual rear arrival");
    at(p,ai.requested.getX(),ai.requested.getY());f(Creature.class,"_move").set(p,null);
   }
   verify(BluffRear.ready(p,e),"multi-waypoint ordinary movement reaches actual rear and40u reach");guards(p,e,ranked);f(state,"nextDecision").setLong(s,0);
   verify(run(p,e,s,12000)&&(last==30||last==531),"only after actual rear arrival: blow or Critical Wound actual="+last+" cid="+cid+" ranked="+ranked+" behind="+TacticsPolicy.behind(e.getX(),e.getY(),e.getHeading(),p.getX(),p.getY())+" mp="+p.getCurrentMp()+" condition30="+nativeSkill(30).checkCondition(p,e,false)+" condition531="+nativeSkill(531).checkCondition(p,e,false));finish(p,e,s,17000);
   Map<Integer,Skill> known=(Map<Integer,Skill>)f(Creature.class,"_skills").get(p);known.remove(30);
   verify(run(p,e,s,20000)&&last==531,"only Critical Wound remains legal, not Bluff/Shadow/selfbuff");finish(p,e,s,25000);
   mp(p,0);verify(!BotTactics.allowsBasicAttack(p,BotPresets.getPresetByClassId(cid)),"Bluff STUN forbids even zero-MP autoattack fallback");
   verify(run(p,e,s,30000)&&last==0,"no MP: hold, never yield an autoattack or fake skill");
   b.setInUse(false);verify(BluffRear.stun(p,e)==null,"inactive358 not a live stun phase");b.setInUse(true);
   flags(e,0);verify(BluffRear.stun(p,e)==null,"BuffInfo alone without native stun flag is insufficient");flags(e,EffectFlag.STUNNED.getMask());
   f(BuffInfo.class,"_effector").set(b,actor(cid,ranked,false));verify(BluffRear.stun(p,e)==null,"another caster's Bluff is not ours");f(BuffInfo.class,"_effector").set(b,p);
   f(BuffInfo.class,"_skill").set(b,nativeSkill(260));verify(BluffRear.stun(p,e)==null,"another own stun is not Bluff");f(BuffInfo.class,"_skill").set(b,nativeSkill(358));
   clearSleep(e);verify(BluffRear.stun(p,e)==null&&BotTactics.allowsBasicAttack(p,BotPresets.getPresetByClassId(cid)),"stun ended: ordinary low-MP fallback restored");
   // Being in the rear sector alone is not enough: close to actual native strike reach.
   mp(p,10000);at(p,-120,0);bluff(p,e);s=centered();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
   verify(run(p,e,s,35000)&&last==0&&p.isMoving(),"rear but out of native40 reach: finish ordinary approach before strikes");
   clearSleep(e);f(Creature.class,"_move").set(p,null);
   // Expiry midway through rear movement releases just this phase's route.
   mp(p,10000);at(p,80,0);bluff(p,e);s=centered();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);run(p,e,s,40000);verify(p.isMoving(),"new actual Bluff can start a new approach");
   clearSleep(e);f(state,"nextStep").setLong(s,Long.MAX_VALUE);run(p,e,s,40100);verify(!p.isMoving(),"expired Bluff releases owned route before ordinary tactics");
   // A genuine movement prohibition cannot be turned into a free rear teleport/cast.
   at(p,80,0);bluff(p,e);flags(p,EffectFlag.ROOTED.getMask());s=centered();verify(run(p,e,s,50000)&&last==0&&!p.isMoving(),"root prevents rear approach; no premature blows or unrelated cast");flags(p,0);
   clearSleep(e);
  }
 }

 // Keep native PlayerAI.setIntentionMoveTo admission/queuing; only world movement is recorded.
 static class NativeMoveAI extends org.l2jmobius.gameserver.ai.PlayerAI {
  int requests;org.l2jmobius.gameserver.entity.Location requested;
  NativeMoveAI(Player p){super(p);}
  @Override public void stopFollow(){} // no follow scheduler in the isolated fixture
  @Override public void clientStopAutoAttack(){} // no attack-stance scheduler in the isolated fixture
  @Override protected void moveTo(int x,int y,int z){
   requests++;requested=new org.l2jmobius.gameserver.entity.Location(x,y,z);
   try {
    Class<?> c=Class.forName("org.l2jmobius.gameserver.entity.actor.Creature$MoveData");var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);Object m=ctor.newInstance();
    c.getField("xDestination").setInt(m,x);c.getField("yDestination").setInt(m,y);c.getField("zDestination").setInt(m,z);f(Creature.class,"_move").set(_actor,m);
   }catch(Exception e){throw new RuntimeException(e);}
  }
  @Override public void clientStopMoving(org.l2jmobius.gameserver.entity.Location loc){try{f(Creature.class,"_move").set(_actor,null);}catch(Exception e){throw new RuntimeException(e);}}
 }
 static void edgeRear()throws Exception {
  for(int cid:new int[]{93,101,108})for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})
   for(int h:new int[]{0,8192,16384,24576,32768,40960,49152,57344,65535})for(int edge:new int[]{430,445}) {
   int cx=-20939,cy=-210729;double angle=h*2*Math.PI/65536.0;
   Player p=owner(cid,ranked),e=actor(94,ranked,human);foeReady(p,e);
   at(p,cx+(int)Math.round(350*Math.cos(angle)),cy+(int)Math.round(350*Math.sin(angle)));
   at(e,cx+(int)Math.round(edge*Math.cos(angle)),cy+(int)Math.round(edge*Math.sin(angle)));e.setHeading((h+32768)%65536);
   int ex=e.getX(),ey=e.getY(),eh=e.getHeading();
   NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);BuffInfo applied=bluff(p,e);BluffRear route=new BluffRear();
   for(int n=0;n<24&&!BluffRear.ready(p,e);n++) {
    route.tick(p,e,applied,10000+n*200,new int[]{cx,cy,0});
    if(p.isMoving()) {
     verify(Math.hypot(ai.requested.getX()-cx,ai.requested.getY()-cy)<=449,"edge route stays inside449");
     double length=Math.hypot(ai.requested.getX()-p.getX(),ai.requested.getY()-p.getY());
     verify(length<=161,"ordinary bounded movement, never teleport");
     at(p,ai.requested.getX(),ai.requested.getY());f(Creature.class,"_move").set(p,null);
    }
   }
   verify(BluffRear.ready(p,e),"edge: reach a feasible rear-sector point, not only exact180deg; cid="+cid+" edge="+edge+" h="+h+" pos="+p.getX()+","+p.getY());
   verify(ai.requests>0&&p.calculateDistance2D(e)<=40,"native PlayerAI dispatch reaches rear within strike range");
   verify(e.getX()==ex&&e.getY()==ey&&e.getHeading()==eh,"no victim movement or facing forgery");
   // From this actual rear point the same production act can issue a skill.
   // Native using-kind condition must see a real dagger paperdoll/mask.
   f(Weapon.class,"_type").set(p.getActiveWeaponItem(),WeaponType.DAGGER);
   var item=(org.l2jmobius.gameserver.entity.item.instance.Item)unsafe.allocateInstance(org.l2jmobius.gameserver.entity.item.instance.Item.class);
   f(item.getClass(),"_itemTemplate").set(item,p.getActiveWeaponItem());
   ((org.l2jmobius.gameserver.entity.item.instance.Item[])f(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.class,"_paperdoll").get(p.getInventory()))[org.l2jmobius.gameserver.entity.itemcontainer.Inventory.PAPERDOLL_RHAND]=item;
   f(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.class,"_wearedMask").setInt(p.getInventory(),WeaponType.DAGGER.mask());
   learn(p,nativeSkill(30));Object s=centered();f(state,"arenaCenterX").setInt(s,cx);f(state,"arenaCenterY").setInt(s,cy);
   f(state,"nextDecision").setLong(s,0);verify(run(p,e,s,20000)&&last==30,"edge arrival releases production Backstab, not autoattack; actual="+last);
   verify(!BotTactics.allowsBasicAttack(p,BotPresets.getPresetByClassId(cid)),"autoattack stays forbidden during own Bluff even behind");
   p.setCastingNow(false);clearSleep(e);
  }
 }
 public static void main(String[]args){try{
  ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("phase63-geodata-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  clarity();wakeSequence();daggers();edgeRear();System.out.println("PASS: "+checks+" native Clarity/Trance/wake-ManaBurn and all3 dagger own-Bluff rear/skills-only checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
