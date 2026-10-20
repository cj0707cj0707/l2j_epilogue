package custom.fakeplayersai;
import java.util.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Structure78: real command application/native inventory and actual tactics; no live DB/world. */
public class BotGearCardinal78Test extends MeleePursuitBoundaryTest {
 static class QuietStatus extends HeadlessStatus {
  QuietStatus(Player p){super(p);}
  @Override public boolean setCurrentMp(double value,boolean broadcast){try{double old=getCurrentMp();f(org.l2jmobius.gameserver.entity.actor.status.CreatureStatus.class,"_currentMp").setDouble(this,value);return old!=value;}catch(Exception e){throw new RuntimeException(e);}}
 }
 static void quiet(Player p)throws Exception {
  var old=p.getStatus();var status=(QuietStatus)unsafe.allocateInstance(QuietStatus.class);
  for(Class<?> c=org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class;c!=Object.class;c=c.getSuperclass())for(var field:c.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){field.setAccessible(true);field.set(status,field.get(old));}
  f(Creature.class,"_status").set(p,status);
 }

 @SuppressWarnings("unchecked") static void gear78()throws Exception {
  NativeDebuffEpicTest.unsafe=unsafe;var settings=BotManager.getSettings();settings.load();
  verify(settings.getEnchantArmor()==10&&settings.getEnchantJewel()==10&&settings.getEnchantWeapon()==16,"active config10/16/10");
  var defaults=new BotSettings();verify(defaults.getEnchantArmor()==10&&defaults.getEnchantJewel()==10&&defaults.getEnchantWeapon()==16,"fallback defaults match config");
  var roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  var training=(Set<Integer>)f(TrainingArena.class,"BOTS").get(null);
  var sessions=(Map<Integer,Object>)f(TrainingArena.class,"SESSIONS").get(null);
  Class<?> session=Class.forName("custom.fakeplayersai.TrainingArena$Session");
  var apply=handlers.chat.commands.admin.AdminBot.class.getDeclaredMethod("applyEnchantToBots",boolean.class,boolean.class,boolean.class);apply.setAccessible(true);
  List<Player> actors=new ArrayList<>();Map<Player,List<org.l2jmobius.gameserver.entity.item.instance.Item>> snapshots=new HashMap<>();
  try {
   for(var preset:BotPresets.getPresets())for(int mode=0;mode<3;mode++) {
    Player p=NativeDebuffEpicTest.recordingPlayer(preset.getClassId(),mode==1);
    var inv=new NativeDebuffEpicTest.TestInventory(p);f(Inventory.class,"_paperdollListeners").set(inv,new ArrayList<>());f(Player.class,"_inventory").set(p,inv);
    List<Integer> ids=new ArrayList<>();for(int id:BotPresets.getArmorSet(preset,true))ids.add(id);int shield=BotPresets.getShieldId(preset,true);if(shield>0)ids.add(shield);
    ids.add(BotPresets.getWeaponId(preset,true));for(int id:BotPresets.getJewelryIds())ids.add(id);
    for(int id:ids){var item=new NativeDebuffEpicTest.TestItem(id);item.setEnchantLevel(3);inv.own(item);inv.equipItem(item);}
    actors.add(p);snapshots.put(p,new ArrayList<>(inv.getPaperdollItems()));
    if(mode!=2)roster.put(p.getObjectId(),p);else {training.add(p.getObjectId());Object s=unsafe.allocateInstance(session);f(session,"bot").set(s,p);sessions.put(p.getObjectId(),s);}
   }
   verify(TrainingArena.getBots().size()==19&&actors.size()==57,"all19 classes in world/ranked/training registries");
   // Actual same private application called by encharmor/enchweapon/enchjewel/reenchant menu branches.
   verify((int)apply.invoke(null,false,true,false)>0,"armor menu changes all live registries");
   for(Player p:actors)for(var item:p.getInventory().getPaperdollItems())verify(item.getEnchantLevel()==(item.isArmor()&&!BotPresetsIsJewel(item.getId())?10:3),"armor-only leaves weapon/jewels untouched");
   verify((int)apply.invoke(null,false,false,true)>0,"jewel menu includes training");
   verify((int)apply.invoke(null,true,false,false)>0,"weapon menu includes training");
   for(Player p:actors)for(var item:p.getInventory().getPaperdollItems())verify(item.getEnchantLevel()==(item.isWeapon()?16:10),"all equipped armor/shields/jewelry10 weapon16; class="+p.getPlayerClass());
   verify((int)apply.invoke(null,true,true,true)==0,"reenchant idempotent, no DB churn");
   settings.setEnchantArmor(12);settings.setEnchantJewel(14);settings.setEnchantWeapon(18);apply.invoke(null,true,true,true);
   for(Player p:actors)for(var item:p.getInventory().getPaperdollItems())verify(item.getEnchantLevel()==(item.isWeapon()?18:BotPresetsIsJewel(item.getId())?14:12),"menu remains adjustable, no class hardcoding");
   for(Player p:actors)verify(snapshots.get(p).equals(new ArrayList<>(p.getInventory().getPaperdollItems())),"items/slots/identity preserved in-place");
   Object closed=sessions.values().iterator().next();f(session,"closed").setBoolean(closed,true);verify(TrainingArena.getBots().size()==18,"closed training session excluded");
  } finally {for(Player p:actors){roster.remove(p.getObjectId());training.remove(p.getObjectId());sessions.remove(p.getObjectId());}settings.load();}
 }
 static boolean BotPresetsIsJewel(int id){return Arrays.stream(BotPresets.getJewelryIds()).anyMatch(x->x==id);}
 static void healing78()throws Exception {
  var preset=BotPresets.getPresetByClassId(97);int[] rotation=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset),buffs=(int[])f(BotClassPreset.class,"_buffsOverride").get(preset);
  try {
   preset.setRotationOverride(new int[]{1418,1394,1230,1398});preset.setBuffsOverride(new int[]{1418});
   for(var opponent:BotPresets.getPresets())for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(boolean clarity:new boolean[]{false,true}) {
    Player e=actor(opponent.getClassId(),ranked,human),p=cardinal(e,ranked,1218,1394,1230,1418,1398);quiet(p);clear(e,3282);if(clarity)nativeSkill(3282).applyEffects(e,e,true,60);
    hp(p,p.getMaxHp()*.7499);Object s=arena(p,e);verify(CardinalSurvival.urgentHeal(p),"strict under75");
    NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);int[] c=ArenaCombatRules.center(p);ai.setIntentionMoveTo(new org.l2jmobius.gameserver.entity.Location(c[0]+200,c[1],0));int moves=ai.requests;
    verify(run(p,e,s,10000)&&last==1218&&p.getTarget()==p&&!p.isMoving()&&ai.requests==moves,"under75 heal before ready Trance/nuke/kite, self-target even saved lists omit heal");
    int requests=traces.get(p).requests;run(p,e,s,10100);verify(p.isCastingNow()&&traces.get(p).requests==requests,"never cancel/duplicate an in-flight heal");finish(p,e,s,15000);
    hp(p,p.getMaxHp()*.75);verify(!CardinalSurvival.urgentHeal(p),"exact75 not urgent");verify(run(p,e,s,17000)&&last==1394,"exact75 resumes ready Trance");finish(p,e,s,22000);
    hp(p,p.getMaxHp()*.9001);verify(run(p,e,s,24000)&&last==1230,"strict above90 keeps existing ready nuke priority");finish(p,e,s,29000);
    hp(p,p.getMaxHp()*.50);disable(p,1218);verify(run(p,e,s,31000)&&last==1394,"native heal reuse: fallback Trance, never force reuse");finish(p,e,s,36000);disabled(p).clear();
    appliedSleep(p,e,97);verify(run(p,e,s,38000)&&last==1218&&p.getTarget()==p,"urgent heal during enemy sleep, no waking damage");finish(p,e,s,43000);clearSleep(e);
    e.setInvul(true);verify(run(p,e,s,45000)&&last==1218,"urgent heal before escaping an invulnerable target");finish(p,e,s,50000);e.setInvul(false);
    verify(Arrays.stream(BotSpawner.getTacticalRotation(p,e)).noneMatch(id->id==1418),"saved rotation cannot restore own Celestial");
    p.getAutoUseSettings().getAutoSkills().add(1418);p.getAutoUseSettings().getAutoBuffs().add(1418);BotSpawner.trimOlympiadBannedSkillsFromAutoLists(p);
    verify(!p.getAutoUseSettings().getAutoSkills().contains(1418)&&!p.getAutoUseSettings().getAutoBuffs().contains(1418),"stale auto lists pruned");
    verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1418),true),"shared cast veto for own Celestial");
   }
  }finally{preset.setRotationOverride(rotation);preset.setBuffsOverride(buffs);}
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(92,ranked,true),p=cardinal(e,ranked,1218,1394,1418);quiet(p);effects(p);hp(p,5000);Object s=arena(p,e);
   var roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);var training=(Set<Integer>)f(TrainingArena.class,"BOTS").get(null);if(ranked)roster.put(p.getObjectId(),p);else training.add(p.getObjectId());
   try {verify(BotRecoveryRules.guard(castEvent(p,p,1418))!=null,"native event also blocks own Celestial");verify(BotRecoveryRules.guard(castEvent(p,p,1218))==null,"native event permits legal self heal");}finally{roster.remove(p.getObjectId());training.remove(p.getObjectId());}
   f(state,"nextDecision").setLong(s,4999);verify(run(p,e,s,4000)&&last==0&&!p.isMoving(),"urgent heal owns reaction gap, no yield to parent offense/kite");
   mp(p,0);verify(run(p,e,s,5000)&&last==1218,"existing MP restore cannot make stale Trance priority starve urgent heal");finish(p,e,s,9000);
   proc(p);verify(run(p,e,s,10000)&&last==1394,"existing invulnerability self-heal prohibition retained; legal Trance fallback, passive not removed");finish(p,e,s,15000);
   clear(p,5576);p.setInvul(false);flags(p,EffectFlag.MUTED.getMask());verify(run(p,e,s,17000)&&last==0,"native silence blocks both heal and Trance");flags(p,0);
   flags(p,EffectFlag.ROOTED.getMask());verify(run(p,e,s,20000)&&last==1218,"root does not block stationary self heal");
   Player human=actor(97,ranked,true);verify(BotSpawner.tacticalSkillAllowed(human,e,nativeSkill(1418),true),"human Cardinal unchanged");
  }
 }
 public static void main(String[]args){try {
  org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("cardinal75-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();gear78();healing78();System.out.println("PASS: "+checks+" gear78 all19/menu coverage/10-16-10 and Cardinal heal75/no own Celestial checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
