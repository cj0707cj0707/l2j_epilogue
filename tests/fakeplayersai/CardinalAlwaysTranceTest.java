package custom.fakeplayersai;
import java.io.File;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Structure75: native skill/effect gates, actual act and native PlayerAI movement dispatch. No live world. */
public class CardinalAlwaysTranceTest extends CardinalSleepPriorityTest {
 static void shield(Player p,int mode)throws Exception {
  effects(p);
  if((mode&1)!=0){nativeSkill(1418).applyEffects(p,p,true,10);p.setInvul(true);}
  if((mode&2)!=0)proc(p);
  if(mode==4)p.setInvul(true);
 }
 static void allOpponents()throws Exception {
  var preset=BotPresets.getPresetByClassId(97);
  int[] rotation=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);
  try {
   preset.setRotationOverride(new int[]{1398,1230,1218,1418}); // Deliberately no1394: mandatory learned injection.
   for(var opponent:BotPresets.getPresets())for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})
    for(int clarity=0;clarity<4;clarity++)for(int protection=0;protection<5;protection++) {
    Player e=actor(opponent.getClassId(),ranked,human),p=cardinal(e,ranked,1394,1398,1230,1218,1418);
    clear(e,3282);shield(p,protection);if((clarity&1)!=0)nativeSkill(3282).applyEffects(p,p,true,60);if((clarity&2)!=0)nativeSkill(3282).applyEffects(e,e,true,60);
    hp(p,8000);Object s=arena(p,e);NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);
    int[] c=ArenaCombatRules.center(p);ai.setIntentionMoveTo(new org.l2jmobius.gameserver.entity.Location(c[0]+300,c[1]+200,0));int moves=ai.requests;
    verify(run(p,e,s,10000)&&last==1394&&!p.isMoving()&&ai.requests==moves,"all classes incl Sagittarius, both arenas/targets, self/enemy/both/no Clarity, all shield states: ready Trance before heal/kite");
    verify(p.getKnownSkill(1394).getActivateRate()==20&&p.getKnownSkill(1394).getMinChance()==20&&p.getKnownSkill(1394).getMaxChance()==20,"priority never increases fixed20 chance");
    int casts=traces.get(p).requests;run(p,e,s,10100);verify(p.isCastingNow()&&traces.get(p).requests==casts&&ai.requests==moves,"no duplicate cast or movement during native cast");finish(p,e,s,15000);
    verify(run(p,e,s,17000)&&last==1394&&!p.isMoving(),"resisted sleep retried, never replace with damage or heal");finish(p,e,s,22000);
    appliedSleep(p,e,97);verify(run(p,e,s,24000)&&last!=1394,"actual sleep blocks Trance regardless of protection/Clarity");
    if(protection!=0)verify(!p.isMoving(),"sleep plus protection forbids retreat");
   }
  } finally {preset.setRotationOverride(rotation);}
 }
 static void wakePriority()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(int protection=0;protection<5;protection++) {
   Player e=actor(92,ranked,true),p=cardinal(e,ranked,1394,1398,1230,1218,1418);hp(p,10000);clear(e,3282);shield(p,protection);Object s=arena(p,e);
   appliedSleep(p,e,97);run(p,e,s,10000);if(last==1398)finish(p,e,s,11000);clearSleep(e);f(Creature.class,"_move").set(p,null);
   verify(run(p,e,s,15000)&&last==((protection&3)!=0?1394:1230)&&!p.isMoving(),"rule94: own Celestial keeps Trance; otherwise healthy Prominence resumes");finish(p,e,s,20000);
  }
 }
 static void gatesAndApproach()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(int protection=1;protection<5;protection++) {
   Player e=actor(92,ranked,true),p=cardinal(e,ranked,1394,1398,1230,1218,1418);hp(p,10000);clear(e,3282);shield(p,protection);Object s=arena(p,e);
   disable(p,1230);NativeMoveAI ai=new NativeMoveAI(p);f(Creature.class,"_ai").set(p,ai);int[] c=ArenaCombatRules.center(p);
   disable(p,1394);verify(run(p,e,s,10000)&&last==0&&!p.isMoving()&&ai.requests==0,"protected reuse: hold, no filler or orbit");
   disabled(p).clear();disable(p,1230);flags(p,EffectFlag.MUTED.getMask());verify(run(p,e,s,15000)&&last==0&&!p.isMoving(),"protected silence: no cast or kite");flags(p,0);
   e.setInvul(true);verify(run(p,e,s,20000)&&last==0&&p.isMoving(),"rule87: invulnerable ranged target blocks offense and requires kite even under own shield");if(p.isCastingNow())finish(p,e,s,24000);e.setInvul(false);
   verify(run(p,e,s,27000)&&last==1394&&!p.isMoving(),"target vulnerability resumes Trance");finish(p,e,s,32000);
   at(p,c[0]+449,c[1]);at(e,c[0]-449,c[1]);verify(run(p,e,s,35000)&&last==0&&p.isMoving(),"outside600: bounded approach, never out-of-range cast");
   verify(ai.requested.getX()<p.getX()&&Math.hypot(ai.requested.getX()-c[0],ai.requested.getY()-c[1])<=449,"approach toward target inside449, not escape");
   arrive(p,ai);flags(p,EffectFlag.ROOTED.getMask());int before=ai.requests;verify(run(p,e,s,37000)&&last==0&&ai.requests==before,"root prevents approach, no teleport");flags(p,0);
   at(p,c[0]+80,c[1]);at(e,c[0],c[1]);f(Creature.class,"_move").set(p,null);verify(run(p,e,s,40000)&&last==1394,"range restored: Trance");finish(p,e,s,45000);
   disable(p,1394);verify(!new InvulnerableMeleeKite().tick(p,e,50000,true),"hard universal-kite shield guard");
  }
  verify(nativeSkill(1418).isRemovedOnAnyActionExceptMove(),"own Celestial native action cancellation unchanged");
  verify(!nativeSkill(5576).isRemovedOnAnyActionExceptMove(),"passive Celestial native noncancellation unchanged");
 }
 public static void main(String[]args){try {
  ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("cardinal75-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();allOpponents();wakePriority();gatesAndApproach();System.out.println("PASS: "+checks+" Cardinal75 all-pool/Clarity/shield/ready-Trance/wake/native-gate/approach checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
