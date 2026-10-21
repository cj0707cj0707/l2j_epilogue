package custom.fakeplayersai;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Structure76: actual act, real skills/effects/guards; world casts and movement recorded. */
public class CardinalProminencePriorityTest extends CardinalAlwaysTranceTest {
 static void priority76()throws Exception {
  var preset=BotPresets.getPresetByClassId(97);int[] saved=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);
  try {
   preset.setRotationOverride(new int[]{1394,1398,1218,1418}); // Mandatory learned1230 must survive saved rotation overrides.
   for(var opponent:BotPresets.getPresets())for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})
    for(boolean clarity:new boolean[]{false,true})for(int protection:new int[]{0}) {
    Player e=actor(opponent.getClassId(),ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);clear(e,3282);shield(p,protection);if(clarity)nativeSkill(3282).applyEffects(e,e,true,60);
    hp(p,p.getMaxHp()*.91);Object s=arena(p,e);
    verify(run(p,e,s,10000)&&last==1230&&!p.isMoving(),">90%: ready Prominence before Trance/heal/kite, every class/arena/target/Clarity/shield");
    int casts=traces.get(p).requests;run(p,e,s,10100);verify(p.isCastingNow()&&traces.get(p).requests==casts,"no duplicate while nuke casts");finish(p,e,s,15000);
    verify(run(p,e,s,16000)&&last==1394,"10s nuke gap: Trance resumes, not another Prominence");finish(p,e,s,19500);
    verify(run(p,e,s,20000)&&last==1230,"exact10s boundary reopens ready Prominence");finish(p,e,s,25000);
    hp(p,p.getMaxHp()*.90);verify(run(p,e,s,30000)&&last==1394,"exact90% blocks ready nuke; Trance remains first");finish(p,e,s,35000);
    hp(p,p.getMaxHp()*.9001);verify(run(p,e,s,40000)&&last==1230,"strictly over90%, no rounded threshold");finish(p,e,s,45000);
   }
  }finally{preset.setRotationOverride(saved);}
 }
 static void gates76()throws Exception {
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(92,ranked,true),p=cardinal(e,ranked,1394,1230);hp(p,p.getMaxHp());effects(p);Object s=arena(p,e);proc(p);
   disable(p,1230);verify(run(p,e,s,10000)&&last==1394,"native nuke reuse respected, no wait instead of Trance");finish(p,e,s,15000);disabled(p).clear();
   flags(p,EffectFlag.MUTED.getMask());verify(run(p,e,s,17000)&&last==0&&!p.isMoving(),"silence blocks nuke and sleep, no protected kite");flags(p,0);
   e.setInvul(true);verify(run(p,e,s,20000)&&last==0&&p.isMoving(),"rule87: invulnerable ranged target has no nuke/Trance and takes kite priority");e.setInvul(false);
   appliedSleep(p,e,97);verify(run(p,e,s,25000)&&last==0&&!p.isMoving(),"sleeping target: no nuke even if10s ready, protected HOLD");
   verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1230),false),"shared sleep guard");clearSleep(e);
   p.getEffectList().getBuffInfoBySkillId(5576).setInUse(false);p.setInvul(false);
   hp(p,p.getMaxHp()*.90);verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1230),false),"shared exact90% veto");hp(p,p.getMaxHp()*.91);
   verify(BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(1230),false),"shared Clarity target allowed at91%");
   var roster=(java.util.Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);var training=(java.util.Set<Integer>)f(TrainingArena.class,"BOTS").get(null);
   if(ranked)roster.put(p.getObjectId(),p);else training.add(p.getObjectId());
   try {
    hp(p,p.getMaxHp()*.90);verify(BotRecoveryRules.guard(castEvent(p,e,1230))!=null,"native admission exact90% veto");hp(p,p.getMaxHp()*.91);
    e.setInvul(true);verify(BotRecoveryRules.guard(castEvent(p,e,1230))!=null,"native admission invul veto");e.setInvul(false);
    verify(BotRecoveryRules.guard(castEvent(p,e,1230))==null,"native admission permits awake Clarity target above90%");
   }finally{roster.remove(p.getObjectId());training.remove(p.getObjectId());}
   verify(run(p,e,s,30000)&&last==1230,"wake: ready healthy nuke precedes previous wake combo and Trance");finish(p,e,s,35000);
   // Timer boundary with no alternate skill to cast while waiting.
   p=cardinal(e,ranked,1230);hp(p,p.getMaxHp());s=arena(p,e);verify(run(p,e,s,40000)&&last==1230,"fresh ready nuke");finish(p,e,s,45000);
   run(p,e,s,49999);verify(last!=1230,"9999ms never bypasses10s interval");verify(run(p,e,s,50000)&&last==1230,"10000ms ready");
  }
 }
 public static void main(String[]args){try {
  org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("cardinal75-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();priority76();gates76();System.out.println("PASS: "+checks+" Cardinal76 healthy Prominence priority/strict90/10s/Clarity/native-guard checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
