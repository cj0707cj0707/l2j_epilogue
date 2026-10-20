package custom.fakeplayersai;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.*;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncSet;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Real native Formulas, skills/effects and actual act; recorded movement/cast, no world/DB. */
public class CardinalSurvivalControlChanceTest extends ArenaInvulnerabilityClarityTest {
 static void chance()throws Exception {
  for(int[] pair:new int[][]{{97,1394},{115,1097},{115,1092}})for(int level:new int[]{1,SkillData.getInstance().getMaxLevel(pair[1]),130,230}) {
   Skill original=SkillData.getInstance().getSkill(pair[1],level);if(original==null)continue;
   int rate=original.getActivateRate();Player p=actor(pair[0],true,false),e=actor(94,true,true);learn(p,original);BotControlChance.apply(p);Skill copy=p.getKnownSkill(pair[1]);
   verify(copy!=original&&copy.getActivateRate()==(pair[0]==97?25:15)&&copy.getMinChance()==copy.getActivateRate()&&copy.getMaxChance()==copy.getActivateRate(),"scoped chance fields");
   for(Field field:Skill.class.getDeclaredFields()) {
    if(Modifier.isStatic(field.getModifiers())||Set.of("_activateRate","_minChance","_maxChance","_effectLists","_effectTypes").contains(field.getName()))continue;
    field.setAccessible(true);verify(Objects.deepEquals(field.get(original),field.get(copy)),"native field preserved "+pair[1]+"/"+level+" "+field.getName()+" original="+field.get(original)+" copy="+field.get(copy));
   }
   for(EffectScope scope:EffectScope.values())verify(Objects.equals(original.getEffects(scope),copy.getEffects(scope)),"all original effects preserved");
   BotControlChance.apply(p);verify(p.getKnownSkill(pair[1]).getLevel()==level&&original.getActivateRate()==rate,"idempotent, shared SkillData unchanged");
   BotControlChance.apply(p);verify(p.getKnownSkill(pair[1])==copy,"cached fixed25/15 copy is idempotent");
   for(boolean human:new boolean[]{false,true}) {Player wrong=actor(human?pair[0]:94,true,human);learn(wrong,original);BotControlChance.apply(wrong);verify(wrong.getKnownSkill(pair[1])==original,"human and other class unchanged");}
   Player empty=actor(pair[0],false,false);BotControlChance.apply(empty);verify(empty.getKnownSkill(pair[1])==null,"does not learn absent skills");
   for(int profile:new int[]{100,10000}) {
    Object owner=new Object();e.addStatFunc(new FuncSet(Stat.MAGIC_DEFENCE,0x70,owner,profile,null));int successes=0;
    for(int i=0;i<8000;i++){BotControlChance.apply(p);if(Formulas.calcEffectSuccess(p,e,p.getKnownSkill(pair[1])))successes++;}
    verify(successes>(pair[0]==97?1760:960)&&successes<(pair[0]==97?2240:1440),"real formula stays near scoped25/15% across stats "+pair[1]+" level"+level+" successes"+successes);e.removeStatsOwner(owner);
   }
   Object immune=new Object();e.addStatFunc(new FuncSet(Stat.DEBUFF_IMMUNITY,0x70,immune,1,null));for(int i=0;i<100;i++)verify(!Formulas.calcEffectSuccess(p,e,copy),"native debuff immunity wins");e.removeStatsOwner(immune);
   e.getStat().getTraitsInvul()[copy.getTraitType().ordinal()]=1;for(int i=0;i<100;i++)verify(!Formulas.calcEffectSuccess(p,e,copy),"native trait immunity wins");
  }
 }
 static Player cardinal(Player e,boolean ranked,int...ids)throws Exception {
  Player p=owner(97,ranked);effects(e);SkillData.getInstance().getSkill(3282,1).applyEffects(e,e,true,60);f(Creature.class,"_zones").set(e,new byte[64]);p.setTarget(e);at(p,0,0);at(e,30,0);f(Creature.class,"_ai").set(p,new MoveAI(p));for(int id:ids)learn(p,nativeSkill(id));return p;
 }
 static void rotation()throws Exception {
  for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true})for(int cid:new int[]{88,94,97,115}) {
   for(int[] scenario:new int[][]{{10000,30,1394},{5000,30,1418},{9000,30,1218},{10000,800,0}}) {
    Player e=actor(cid,ranked,human),p=cardinal(e,ranked,1394,1418,1230,1218,1398,1396,1399,1307,1217);hp(p,scenario[0]);at(e,scenario[1],0);
    verify(CardinalSurvival.active(p,e),"actual3282 triggers both arenas/all targets");
    Object s=centered();verify(run(p,e,s,10000)&&last==scenario[2],"survival priority expected"+scenario[2]+" actual"+last+" hp"+scenario[0]);p.setCastingNow(false);
   }
   Player e=actor(cid,ranked,human),p=cardinal(e,ranked,1398,1396,1399,1307,1217);
   verify(run(p,e,centered(),10000)&&last==0,"no forbidden cast or parent attack when allowed skills absent");
   BuffInfo b=e.getEffectList().getBuffInfoBySkillId(3282);((Effects)e.getEffectList()).remove(SkillFinishType.REMOVED,b);
   verify(!CardinalSurvival.active(p,e),"dispel immediately disables mode");
   p=owner(97,ranked);learn(p,nativeSkill(1398));p.setTarget(e);at(p,0,0);at(e,30,0);f(Creature.class,"_ai").set(p,new MoveAI(p));verify(run(p,e,centered(),20000)&&last==1398,"normal Mana Burn resumes");p.setCastingNow(false);
  }
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(88,ranked,true),p=cardinal(e,ranked,1394,1418,1230,1218,1398,1396);hp(p,5000);e.setInvul(true);
   verify(run(p,e,centered(),10000)&&last==1418,"shield before invulnerable melee kiting");p.setCastingNow(false);
   effects(p);nativeSkill(1418).applyEffects(p,p,true,10);p.setInvul(true);hp(p,1000);MoveAI ai=(MoveAI)p.getAI();
   verify(run(p,e,centered(),20000)&&last==0&&ai.requests>0,"preserve native Celestial: movement only, no action cancels it");int[] c=ArenaCombatRules.center(p);verify(Math.hypot(ai.requested.getX()-c[0],ai.requested.getY()-c[1])<=450,"survival kite inside450 circle");
   p.setCastingNow(true);int before=ai.requests;verify(run(p,e,centered(),21000)&&p.isCastingNow()&&ai.requests==before,"never abort real cast");p.setCastingNow(false);
   Player healthy=cardinal(actor(88,ranked,true),ranked,1230,1398);Player victim=(Player)healthy.getTarget();victim.setInvul(true);verify(run(healthy,victim,centered(),30000)&&last==0,"no offense against invulnerable target");
  }
  for(int id:new int[]{1396,1399,1307,1217,1510})verify(!CardinalSurvival.allowed(id),"strict phase whitelist "+id);
 }
 @SuppressWarnings("unchecked") static void guards()throws Exception {
  for(boolean ranked:new boolean[]{false,true}) {
   Player e=actor(88,ranked,true),p=cardinal(e,ranked,1394,1418,1230,1218,1398,1396);hp(p,5000);
   Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);
   disabled.put(p.getKnownSkill(1418).getReuseHashCode(),System.currentTimeMillis()+60000);
   verify(run(p,e,centered(),10000)&&last==1218,"shield cooldown: heal instead, never bypass reuse");p.setCastingNow(false);
   for(int id:new int[]{1394,1418,1230,1218})disabled.put(p.getKnownSkill(id).getReuseHashCode(),System.currentTimeMillis()+60000);
   hp(p,10000);verify(run(p,e,centered(),20000)&&last==0,"all allowed on cooldown: kite, not forbidden skill");
   MoveAI ai=(MoveAI)p.getAI();int before=ai.requests;flags(p,org.l2jmobius.gameserver.mechanics.effects.EffectFlag.ROOTED.getMask());verify(run(p,e,centered(),21000)&&last==0&&ai.requests==before,"own root prevents survival movement");flags(p,0);disabled.clear();
   flags(p,org.l2jmobius.gameserver.mechanics.effects.EffectFlag.MUTED.getMask());verify(run(p,e,centered(),22000)&&last==0,"native magic silence prevents four casts");flags(p,0);
   for(int id:new int[]{1396,1399,1307})verify(!BotSpawner.tacticalSkillAllowed(p,e,nativeSkill(id),false),"shared guard rejects "+id);
   SkillData.getInstance().getSkill(3282,1).applyEffects(e,e,true,90);verify(CardinalSurvival.active(p,e),"later independent3282 remains a live mode trigger");
   BuffInfo b=e.getEffectList().getBuffInfoBySkillId(3282);b.setInUse(false);verify(!CardinalSurvival.active(p,e),"inactive BuffInfo not sufficient");b.setInUse(true);
   verify(!CardinalSurvival.active(actor(97,ranked,true),e)&&!CardinalSurvival.active(owner(115,ranked),e),"not humans or other bot classes");
   Player healer=cardinal(actor(94,ranked,true),ranked,1394,1218);Player sleeping=(Player)healer.getTarget();appliedSleep(healer,sleeping,97);hp(healer,5000);verify(run(healer,sleeping,centered(),30000)&&last==1218,"heal preempts own-sleep retreat");healer.setCastingNow(false);
  }
 }
 public static void main(String[]args){try{test59(args);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
 static void test59(String[]args)throws Exception {
  ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
  unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("cardinal59-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  chance();rotation();guards();nativeClarity();kiting();System.out.println("PASS: "+checks+" Cardinal survival, 25/15% native-formula, immutable templates, previous Clarity/kiting checks; no live server");System.exit(0);
 }
}
