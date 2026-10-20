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
   Player e=actor(88,ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);Object s=arena(p,e);if(!clarity)clear(e,3282);
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
   verify(run(p,e,s,30000)&&last==1218,"legal heal remains ahead of Trance/route");finish(p,e,s,35000);hp(p,10000);
   e.setInvul(true);s=arena(p,e);verify(run(p,e,s,40000)&&last==0&&p.isMoving(),"invulnerable melee: pure defense kite, no Trance attempts");e.setInvul(false);
   run(p,e,s,45000);verify(last==1394,"invulnerability ended: ready Trance resumes immediately");
  }
 }
 static void closeWakeCombo()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true}) {
   Player e=actor(88,ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);Object s=arena(p,e);disable(p,1418);hp(p,10000);
   appliedSleep(p,e,97);run(p,e,s,10000);clearSleep(e);f(Creature.class,"_move").set(p,null);int[] c=ArenaCombatRules.center(p);at(p,c[0]+80,c[1]);at(e,c[0],c[1]);
   verify(run(p,e,s,15000)&&last==1398,"close waking melee with Clarity: owed Mana Burn before kite/Trance");finish(p,e,s,20000);
   verify(run(p,e,s,25000)&&last==1230,"close waking melee: owed ready Prominence before kite/Trance");finish(p,e,s,30000);
   verify(run(p,e,s,35000)&&last==1394,"close waking melee: after combo resleep, not movement");
  }
 }
 public static void main(String[]args){try{
  ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("control65-geodata-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();dominatorTargets();tranceGaps();closeWakeCombo();System.out.println("PASS: "+checks+" native Dominator ranged-stun guard/600u pursuit and Cardinal ready-Trance/kite-gap/wake-combo checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
