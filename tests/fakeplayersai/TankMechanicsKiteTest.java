package custom.fakeplayersai;
import java.io.File;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Actual act/public hooks/native conditions/effects. Cast, movement, network/timer recording seams. */
public class TankMechanicsKiteTest extends CardinalSurvivalControlChanceTest {
 static void clear(Player p,int...ids) {
  for(int id:ids){BuffInfo b=p.getEffectList().getBuffInfoBySkillId(id);if(b!=null)((Effects)p.getEffectList()).remove(SkillFinishType.REMOVED,b);}
 }
 static void apply(Player p,int id,int level){SkillData.getInstance().getSkill(id,level).applyEffects(p,p,true,0);}
 static Player knight(int cid,boolean ranked,Player e)throws Exception {
  Player p=owner(cid,ranked);effects(p);effects(e);at(p,0,0);at(e,30,0);p.setTarget(e);f(Creature.class,"_zones").set(e,new byte[64]);f(Creature.class,"_ai").set(p,new MoveAI(p));
  for(int id:TankClassMechanics.skills(cid))learn(p,nativeSkill(id));
  var inv=new NativeDebuffEpicTest.TestInventory(p);f(Inventory.class,"_paperdollListeners").set(inv,new ArrayList<>());f(Player.class,"_inventory").set(p,inv);
  var sword=new NativeDebuffEpicTest.TestItem(13457);inv.own(sword);inv.setPaperdollItem(Inventory.PAPERDOLL_RHAND,sword);return p;
 }
 @SuppressWarnings("unchecked") static void defenses()throws Exception {
  Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  for(boolean ranked:new boolean[]{false,true})for(BotClassPreset preset:BotPresets.getPresets())for(int cid:new int[]{90,91,99,106})for(int defense:new int[]{110,368,760})for(boolean human:new boolean[]{false,true}) {
   Player p=owner(preset.getClassId(),ranked),e=actor(cid,ranked,human);Effects list=effects(e);f(Creature.class,"_zones").set(e,new byte[64]);
   // Live native skill identity; no defense-stat simulation is needed for the kite predicate.
   Skill nativeDefense=nativeSkill(defense);BuffInfo info=new BuffInfo(e,e,nativeDefense);
   list.add(info);verify(TankClassMechanics.defendedTank(e),"actual live defense on any human/bot tank including Eva");
   int[] c=ArenaCombatRules.center(p);at(p,c[0]+80,c[1]);at(e,c[0],c[1]);p.setTarget(e);MoveAI ai=new MoveAI(p);f(Creature.class,"_ai").set(p,ai);
   if(!human)roster.put(e.getObjectId(),e);if(ranked)roster.put(p.getObjectId(),p);else BotTactics.initializeTraining(p,e);
   last=0;verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&last==0&&ai.requests==1,"all19 actual public hooks preempt offense and ranked131");
   verify(Math.hypot(ai.requested.getX()-c[0],ai.requested.getY()-c[1])<=450,"inside450 circle");
   int before=ai.requests;verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&ai.requests==before,"route ownership across ticks");
   p.setCastingNow(true);verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&p.isCastingNow()&&ai.requests==before,"never cancel current cast");p.setCastingNow(false);
   flags(p,EffectFlag.STUNNED.getMask());verify(BotTactics.tickInvulnerableMelee(p,e,preset,ranked)&&ai.requests==before,"own CC not bypassed");flags(p,0);
   info.setInUse(false);verify(!TankClassMechanics.defendedTank(e),"inactive buff not protection");info.setInUse(true);list.remove(SkillFinishType.REMOVED,info);
   verify(!TankClassMechanics.defendedTank(e)&&!BotTactics.tickInvulnerableMelee(p,e,preset,ranked),"defense ends: parent ownership restored");
   BotTactics.forget(p.getObjectId());roster.remove(p.getObjectId());roster.remove(e.getObjectId());
  }
  for(int cid:new int[]{88,93,94,97,115,131}) {Player e=actor(cid,false,true);effects(e).add(new BuffInfo(e,e,nativeSkill(760)));verify(!TankClassMechanics.defendedTank(e),"not arbitrary class with defensive buff");}
  Player tank=actor(90,false,true);effects(tank);apply(tank,406,3);verify(!TankClassMechanics.defendedTank(tank),"long class icon is not UD");
 }
 @SuppressWarnings("unchecked") static void classSkills()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{90,91,106}) {
   int seed=cid==90?784:cid==91?761:788,trigger=cid==90?5562:cid==91?5561:5564,finish=cid==90?785:cid==91?762:789;
   Player e=actor(88,ranked,true),p=knight(cid,ranked,e);Object s=centered();
   verify(run(p,e,s,10000)&&last==seed&&p.getTarget()==p,"native seed self-cast at fullHP, even outside preset/overrides cid"+cid+" actual"+last);p.setCastingNow(false);
   apply(p,seed,1);verify(!TankClassMechanics.allowed(p,seed)&&!TankClassMechanics.allowed(p,finish),"seed active cannot refresh or unlock final early");
   for(int level:new int[]{1,2,3}) {
    clear(p,seed,trigger);apply(p,trigger,level);
    verify(!TankClassMechanics.allowed(p,seed),"never overwrite damage-triggered charge stage");
    verify(TankClassMechanics.allowed(p,finish)==(level==3),"only real stage3 unlocks final");
    verify(p.getKnownSkill(finish).checkCondition(p,cid==91?e:p,false)==(level==3),"native active_effect_id_lvl agrees stage"+level);
   }
   verify(run(p,e,centered(),20000)&&last==finish,"actual native final chosen cid"+cid+" expected"+finish+" actual"+last);p.setCastingNow(false);
   if(cid!=91){apply(p,finish,1);verify(!TankClassMechanics.allowed(p,seed)&&!TankClassMechanics.allowed(p,finish),"don't replace or refresh active icon");clear(p,finish);}
   Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);disabled.put(p.getKnownSkill(finish).getReuseHashCode(),System.currentTimeMillis()+60000);
   run(p,e,centered(),30000);verify(last!=finish,"real finisher reuse respected");p.setCastingNow(false);clear(p,seed,trigger);verify(!TankClassMechanics.allowed(p,seed),"don't spend seed while final disabled");disabled.clear();
   verify(TankClassMechanics.allowed(p,seed),"after charges expire setup can be renewed when ready");
   mp(p,0);run(p,e,centered(),40000);verify(last==0,"no free seed MP");mp(p,10000);
   flags(p,EffectFlag.MUTED.getMask());run(p,e,centered(),41000);verify(last==0,"native seed magic silence");flags(p,0);
   verify(!BotSpawner.trainingPreparationSkillAllowed(p.getKnownSkill(seed)),"don't waste seed in countdown");
   verify(!TankClassMechanics.allowed(actor(cid,ranked,true),seed),"no human AI override");
  }
  for(boolean ranked:new boolean[]{false,true})for(double hp:new double[]{3100,3001,3000,2999,2000}) {
   Player e=actor(88,ranked,true),p=knight(90,ranked,e);learn(p,nativeSkill(69));hp(p,hp);
   boolean nativeHp=p.getKnownSkill(406).checkCondition(p,p,false);
   verify(TankClassMechanics.allowed(p,406)==nativeHp,"native HP threshold without integer-percent rounding hp="+hp);
   verify(nativeHp==(hp<=3000),"native HP condition is not the rounded integer display percentage");
   verify(run(p,e,centered(),10000)&&last==(nativeHp?406:69),"Angelic Icon before Sacrifice in legitimate lowHP window hp"+hp+" actual"+last);p.setCastingNow(false);
   if(nativeHp) {apply(p,406,p.getKnownSkill(406).getLevel());verify(!TankClassMechanics.allowed(p,406)&&!TankClassMechanics.allowed(p,784),"active Angelic not overwritten");}
  }
  // No absent skill learning, and no availability spoof through a wrong-class override.
  Player empty=owner(90,false),wrong=owner(115,false);for(int id:new int[]{406,784,785,761,762,788,789}){verify(!TankClassMechanics.allowed(empty,id),"absent stays absent");learn(wrong,nativeSkill(id));verify(!TankClassMechanics.allowed(wrong,id),"wrong class cannot use mechanics");}
 }
 @SuppressWarnings("unchecked") static void phoenixPriorityAndSacrifice()throws Exception {
  Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  for(boolean ranked:new boolean[]{false,true}) {
   for(double health:new double[]{10000,5000,3000,2000}) {
    Player e=actor(88,ranked,true),p=knight(90,ranked,e);learn(p,nativeSkill(69));hp(p,health);apply(p,5562,3);
    verify(run(p,e,centered(),10000)&&last==785,"ready Flame before Angelic/Sacrifice at any legal HP "+health);p.setCastingNow(false);
   }
   for(int stage:new int[]{0,1,2,3}) {
    Player e=actor(88,ranked,true),p=knight(90,ranked,e);learn(p,nativeSkill(69));hp(p,2000);if(stage>0)apply(p,5562,stage);
    Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);
    if(stage==3)disabled.put(p.getKnownSkill(785).getReuseHashCode(),System.currentTimeMillis()+60000);
    verify(run(p,e,centered(),10000)&&last==406,"Angelic emergency when no stage3 or Flame on reuse");p.setCastingNow(false);
   }
   {Player e=actor(88,ranked,true),p=knight(90,ranked,e);hp(p,1000);apply(p,5562,3);
    verify(run(p,e,centered(),10000)&&last==406,"Flame HP cost not bypassed: emergency Angelic");p.setCastingNow(false);}
   for(int icon:new int[]{785,406}) {
    Player e=actor(88,ranked,true),p=knight(90,ranked,e);learn(p,nativeSkill(69));roster.put(p.getObjectId(),p);hp(p,5000);apply(p,icon,p.getKnownSkill(icon).getLevel());Skill heal=p.getKnownSkill(69);
    verify(!BotRecoveryRules.selfHealAllowed(p,heal),"Sacrifice forbidden under active icon "+icon);
    var event=new org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse();event.setCaster(p);event.setSkill(heal);event.setTarget(p);event.setTargets(List.of(p));
    verify(BotRecoveryRules.guard(event).terminate(),"synchronous legacy/AutoUse cast guard");
    verify(BotRecoveryRules.blockedSacrifice(p,p,heal),"execution guard catches icon gained midcast");
    for(var effect:heal.getEffects(EffectScope.GENERAL))effect.onStart(p,p,heal);
    verify(p.getCurrentHp()==5000,"real Heal handler gives no HP under icon");
    run(p,e,centered(),10000);verify(last==0,"actual act cannot select Sacrifice or replace icon, actual="+last);
    verify(!BotRecoveryRules.blockedSacrifice(p,p,nativeSkill(1218)),"other healing skills unchanged");
    BuffInfo info=p.getEffectList().getBuffInfoBySkillId(icon);info.setInUse(false);
    verify(BotRecoveryRules.selfHealAllowed(p,heal),"inactive icon not a healing veto");info.setInUse(true);clear(p,icon);
    verify(BotRecoveryRules.guard(event)==null&&BotRecoveryRules.selfHealAllowed(p,heal),"Sacrifice unlocked on expiration/removal");
    verify(run(p,e,centered(),20000)&&last==69,"normal Sacrifice resumes after icon");p.setCastingNow(false);
    roster.remove(p.getObjectId());
   }
   // A human Phoenix with the same native effects is outside the bot restriction.
   Player human=actor(90,ranked,true);effects(human);apply(human,785,1);
   verify(!TankClassMechanics.sacrificeBlocked(human)&&!BotRecoveryRules.blockedSacrifice(human,human,nativeSkill(69)),"human Phoenix unaffected");
  }
 }
 public static void main(String[]args){try{test60(args);}catch(Throwable t){t.printStackTrace();System.exit(1);}}
 static void test60(String[]args)throws Exception {
  ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);org.l2jmobius.gameserver.handler.TargetHandler.getInstance().registerHandler(new handlers.skill.targets.Party());skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("tank60-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  defenses();classSkills();phoenixPriorityAndSacrifice();System.out.println("PASS: "+checks+" all19 tank-defense circle/ownership, native class mechanics, Flame-first and icon Sacrifice cast/execution veto; no live server");System.exit(0);
 }
}
