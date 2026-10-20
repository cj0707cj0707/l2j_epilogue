package custom.fakeplayersai;
import java.io.File;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.*;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Native candidates/effects and actual act; recording casts/movement, no live server. */
public class ControlPursuitTranceKiteTest extends ClarityBluffPhaseTest {
 static Object arena(Player p,Player e)throws Exception {
  int[] c=ArenaCombatRules.center(p);at(p,c[0]+80,c[1]);at(e,c[0],c[1]);Object s=centered();
  f(state,"arenaCenterX").setInt(s,c[0]);f(state,"arenaCenterY").setInt(s,c[1]);return s;
 }
 static void dominatorTargets()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true}) {
   for(int cid:new int[]{92,94,95,97,102,103,109,110,115,134}) {
    Player e=actor(cid,ranked,human),p=owner(115,ranked);foeReady(p,e);at(p,0,0);at(e,500,0);
    if(cid==134)e.addStatFunc(new org.l2jmobius.gameserver.mechanics.stats.functions.FuncAdd(org.l2jmobius.gameserver.mechanics.stats.Stat.POWER_ATTACK_RANGE,0x10,new Object(),500,null));
    for(int id:new int[]{1097,260,1367,1245})learn(p,nativeSkill(id));Object s=centered();
    verify(!BotSpawner.tacticalSkillAllowed(p,e,p.getKnownSkill(260),false),"bot115 common guard rejects any STUN vs ranged human/bot cid"+cid);
    verify(BotTactics.tacticalApproachRange(p,e,BotPresets.getPresetByClassId(115))==600,"no40u melee pursuit vs ranged cid"+cid);
    verify(run(p,e,s,10000)&&last==1097,"actual sleep at500u without melee chase cid"+cid);finish(p,e,s,15000);
    disable(p,1097);at(e,30,0);s=centered();verify(run(p,e,s,20000)&&last==1367,"sleep unavailable: curse, never Hammer even at30u");finish(p,e,s,25000);
    disable(p,1367);s=centered();verify(run(p,e,s,30000)&&last==1245,"control/curses unavailable: ranged drain, not stun wait");finish(p,e,s,35000);
   }
   Player e=actor(88,ranked,human),p=owner(115,ranked);foeReady(p,e);at(p,0,0);at(e,30,0);learn(p,nativeSkill(260));
   f(Weapon.class,"_type").set(p.getActiveWeaponItem(),WeaponType.BLUNT);f(Inventory.class,"_wearedMask").setInt(p.getInventory(),WeaponType.BLUNT.mask());
   verify(BotSpawner.tacticalSkillAllowed(p,e,p.getKnownSkill(260),false),"melee opponent retains Hammer Crush");
   verify(run(p,e,centered(),40000)&&last==260,"native legal Hammer still cast vs melee: last="+last+" cond="+p.getKnownSkill(260).checkCondition(p,e,false)+" phys="+p.getKnownSkill(260).isPhysical()+" weapon="+p.getActiveWeaponItem().getItemType());
   Player humanD=actor(115,ranked,true),caster=actor(97,ranked,human);
   verify(BotTactics.dominatorStunAllowed(humanD,caster,nativeSkill(260)),"human Dominator skills are not restricted by bot policy");
  }
 }
 static void tranceGaps()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(boolean clarity:new boolean[]{false,true}) {
   Player e=actor(88,ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);Object s=arena(p,e);disable(p,1230);if(!clarity)clear(e,3282);
   hp(p,10000);disable(p,1418);disable(p,1394);
   verify(run(p,e,s,10000)&&last==0&&p.isMoving(),"Trance reuse gap: ordinary ring movement, not stationary nuke spam");
   MoveAI ai=(MoveAI)p.getAI();int moves=ai.requests;int[] c=ArenaCombatRules.center(p);
   verify(Math.hypot(ai.requested.getX()-c[0],ai.requested.getY()-c[1])<=450,"gap route stays inside450");
   // Route is still in progress: ready Trance must take control, not wait for arrival.
   disabled(p).remove(p.getKnownSkill(1394).getReuseHashCode());
   verify(run(p,e,s,12000)&&last==1394&&!p.isMoving()&&ai.requests==moves,"ready Trance interrupts kite route, before ready Prominence");
   int requests=traces.get(p).requests;run(p,e,s,12100);verify(traces.get(p).requests==requests&&p.isCastingNow(),"in-flight Trance preserved, no movement/duplicate cast");finish(p,e,s,17000);
   verify(run(p,e,s,19000)&&last==1394,"resisted Trance: retry when native-ready, not another full orbit");finish(p,e,s,24000);
   appliedSleep(p,e,97);verify(run(p,e,s,26000)&&last==0&&p.isMoving(),"actual landed Trance switches to existing long retreat, no refreshing sleep");
   clearSleep(e);f(Creature.class,"_move").set(p,null);s=arena(p,e);hp(p,5000);
   verify(run(p,e,s,30000)&&last==1218,"rule78 below75: ready self heal precedes Trance/route");finish(p,e,s,35000);hp(p,10000);
   e.setInvul(true);s=arena(p,e);verify(run(p,e,s,40000)&&last==0&&p.isMoving(),"invulnerable melee: pure defense kite, no Trance attempts");e.setInvul(false);
   run(p,e,s,45000);verify(last==1394,"invulnerability ended: ready Trance resumes immediately");
  }
 }
 static void closeWakeCombo()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true}) {
   Player e=actor(88,ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);Object s=arena(p,e);disable(p,1230);disable(p,1418);hp(p,10000);
   appliedSleep(p,e,97);run(p,e,s,10000);disable(p,1394);clearSleep(e);f(Creature.class,"_move").set(p,null);int[] c=ArenaCombatRules.center(p);at(p,c[0]+80,c[1]);at(e,c[0],c[1]);
   verify(run(p,e,s,15000)&&last==1398,"Trance cooldown: close wake Mana Burn before kite");finish(p,e,s,20000);disabled(p).remove(p.getKnownSkill(1230).getReuseHashCode());
   verify(run(p,e,s,25000)&&last==1230,"Trance cooldown: close wake Prominence before kite");finish(p,e,s,30000);
   disable(p,1230);disabled(p).remove(p.getKnownSkill(1394).getReuseHashCode());verify(run(p,e,s,35000)&&last==1394,"close waking melee: after combo resleep, not movement");
  }
 }
 static void proc(Player p)throws Exception {nativeSkill(5576).applyEffects(p,p,true,5);p.setInvul(true);}
 static void ownInvulnerability()throws Exception {
  verify(nativeSkill(5576).getAbnormalTime()==5&&!nativeSkill(5576).isRemovedOnAnyActionExceptMove(),"native proc duration and no action removal");
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(boolean clarity:new boolean[]{false,true})for(int cid:new int[]{108,93,101,94}) {
   Player e=actor(cid,ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);Object s=arena(p,e);disable(p,1230);if(!clarity)clear(e,3282);hp(p,1000);
   effects(p);nativeSkill(1418).applyEffects(p,p,true,10);p.setInvul(true);
   verify(run(p,e,s,10000)&&last==1394&&!p.isMoving()&&f(state,"cardinalTrancePriority").getBoolean(s),"own1418: Trance, no kite, with/without Clarity");finish(p,e,s,11000);
   proc(p);verify(run(p,e,s,12000)&&last==1394&&!p.isMoving(),"overlapping own1418+proc5576: Trance, no kite");finish(p,e,s,13000);clear(p,1418);
   verify(run(p,e,s,14000)&&last==1394&&!p.isMoving(),"live5576: Trance before lowHP defense, kite or damage");
   int requests=traces.get(p).requests;run(p,e,s,14100);verify(traces.get(p).requests==requests&&p.isCastingNow(),"preserve real cast");finish(p,e,s,19000);
   verify(run(p,e,s,21000)&&last==1394,"resist: retry while5576 is still live");finish(p,e,s,26000);
   disable(p,1394);verify(run(p,e,s,28000)&&last==0&&!p.isMoving(),"proc reuse gap: no kite or filler");
   clear(p,5576);p.setInvul(false);disable(p,1418);hp(p,5000);
   verify(run(p,e,s,30000)&&last==1218&&!f(state,"cardinalTrancePriority").getBoolean(s),"proc expiry before sleep: immediately restore normal heal, no latch");finish(p,e,s,35000);
   disabled(p).remove(p.getKnownSkill(1394).getReuseHashCode());proc(p);
   verify(run(p,e,s,37000)&&last==1394,"new proc independently enables Trance phase");finish(p,e,s,42000);
   appliedSleep(p,e,97);verify(run(p,e,s,44000)&&last!=1394&&!f(state,"cardinalTrancePriority").getBoolean(s),"actual sleep ends phase while proc remains, never refresh sleep");
  }
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(108,ranked,true),p=cardinal(e,ranked,1394,1398,1230,1218,1418);clear(e,3282);Object s=arena(p,e);disable(p,1230);hp(p,10000);disable(p,1394);disable(p,1418);effects(p);
   verify(run(p,e,s,10000)&&p.isMoving(),"ordinary reuse-gap kite before proc");proc(p);
   verify(run(p,e,s,12000)&&last==0&&!p.isMoving(),"proc interrupts existing kite even on reuse");
   disabled(p).remove(p.getKnownSkill(1394).getReuseHashCode());verify(run(p,e,s,14000)&&last==1394,"ready Trance after stopped route");finish(p,e,s,19000);
   p=cardinal(e,ranked,1394);effects(p);s=arena(p,e);int[] c=ArenaCombatRules.center(p);at(p,c[0]+449,c[1]);at(e,c[0]-449,c[1]);proc(p);
   verify(run(p,e,s,25000)&&last==0&&p.isMoving(),"distant target: bounded approach, no out-of-range cast");
   MoveAI ai=(MoveAI)p.getAI();verify(ai.requested.getX()<p.getX()&&Math.hypot(ai.requested.getX()-c[0],ai.requested.getY()-c[1])<=449,"approach inside circle");
  }
  Player e=actor(108,true,true),p=cardinal(e,true,1394,1398,1218);clear(e,3282);effects(p);Object s=arena(p,e);p.setInvul(true);hp(p,10000);
  verify(!CardinalSurvival.passiveTrance(p,e),"generic invul without actual5576 is not a proc");proc(p);
  flags(p,org.l2jmobius.gameserver.mechanics.effects.EffectFlag.MUTED.getMask());verify(run(p,e,s,10000)&&last==0&&!p.isMoving(),"silence: no cast/kite");flags(p,0);
  e.setInvul(true);verify(run(p,e,s,15000)&&last!=1394&&!p.isMoving(),"target immunity forbids Trance and protected kite");if(p.isCastingNow())finish(p,e,s,19000);e.setInvul(false);
  verify(run(p,e,s,20000)&&last==1394,"target immunity expired while proc live: Trance resumes");finish(p,e,s,25000);
 }
 public static void main(String[]args){try{
  ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("control65-geodata-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();dominatorTargets();tranceGaps();closeWakeCombo();ownInvulnerability();System.out.println("PASS: "+checks+" native Dominator ranged-stun guard/600u pursuit and Cardinal ready-Trance/kite-gap/wake-combo checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
