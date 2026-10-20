package custom.fakeplayersai;
import java.util.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
import java.lang.invoke.*;
import org.l2jmobius.gameserver.entity.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
/** Installed native boundary wrappers; no live world, database or client. */
public class MeleePursuitBoundaryTest extends CardinalProminencePriorityTest {
 @SuppressWarnings("unchecked") static void pursuit77()throws Throwable {
  var guard=Class.forName("org.l2jmobius.gameserver.mechanics.skill.ArenaBoundaryGuard");
  var bind=guard.getMethod("bind",Player.class,Player.class,int.class,int.class);
  var clamp=guard.getMethod("clamp",WorldObject.class,int.class,int.class);
  Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
  Set<Integer> melee=Set.of(88,90,91,93,101,106,108,113,114,131);
  verify(BotPresets.getPresets().size()==19,"current nineteen-class pool");
  for(var preset:BotPresets.getPresets())for(boolean ranked:new boolean[]{false,true})for(boolean human:new boolean[]{false,true}) {
   Player p=actor(preset.getClassId(),ranked,false),e=actor(95,ranked,human);
   if(!ranked)for(Player a:List.of(p,e)){f(Player.class,"_isInDuel").setBoolean(a,true);f(Player.class,"_duelId").setInt(a,50);f(Player.class,"_duelState").setInt(a,Duel.DUELSTATE_DUELLING);}
   int[] c=ArenaCombatRules.center(p);at(p,c[0]+80,c[1]);at(e,c[0],c[1]);
   boolean isMelee=melee.contains(preset.getClassId());verify(ArenaCombatRules.meleeClass(p)==isMelee,"role classification cid="+preset.getClassId());
   verify(ArenaCombatRules.attackRadius(p)==(ranked?f(BotManager.class,"OLYMPIAD_ARENA_MAX_DISTANCE").getDouble(null):TrainingArena.arenaRadius()),"same existing outer chase envelope");
   var inside=BluffRear.class.getDeclaredMethod("inside",Player.class,Location.class,int[].class);inside.setAccessible(true);
   verify((boolean)inside.invoke(null,p,new Location(c[0]+600,c[1],0),c),"Bluff offensive goal outside kite ring admitted");
   verify(!(boolean)inside.invoke(null,p,new Location(c[0]+(int)ArenaCombatRules.attackRadius(p)+1,c[1],0),c),"Bluff still rejects outside existing whole-arena envelope");
   roster.put(p.getObjectId(),p);if(!human)roster.put(e.getObjectId(),e);
   try {
    // A stale binding must be removed, not retained after an earlier opponent tick.
    bind.invoke(null,p,e,c[0],c[1]);
    if(isMelee)at(p,c[0]+600,c[1]);
    if(!human)verify(!ArenaCombatRules.update(e,p,BotPresets.getPresetByClassId(95)),"opponent driver admitted first");
    verify(!ArenaCombatRules.update(p,e,preset),"own driver admitted");
    if(isMelee)verify(p.getX()==c[0]+600,"no snap/normalization of pursuing melee outside kite ring");
    var commit=MethodHandles.privateLookupIn(WorldObject.class,MethodHandles.lookup()).findSpecial(WorldObject.class,"setXYZ",MethodType.methodType(void.class,int.class,int.class,int.class),WorldObject.class).bindTo(p);
    for(int radius:new int[]{450,460,461,600,900})for(int angle=0;angle<360;angle+=15) {
     int x=c[0]+(int)Math.round(radius*Math.cos(Math.toRadians(angle))),y=c[1]+(int)Math.round(radius*Math.sin(Math.toRadians(angle)));
     int[] xy=(int[])clamp.invoke(null,p,x,y);boolean unrestricted=isMelee||human;
     verify(unrestricted?xy[0]==x&&xy[1]==y:Math.hypot(xy[0]-c[0],xy[1]-c[1])<=460,"real core clamp follows role, not enemy driver");
     commit.invoke(x,y,p.getZ());verify(unrestricted?p.getX()==x&&p.getY()==y:Math.hypot(p.getX()-c[0],p.getY()-c[1])<=460,"actual native setXYZ wrapper");
     p.setLocation(new Location(x,y,p.getZ(),0,10));verify(unrestricted?p.getX()==x&&p.getY()==y:Math.hypot(p.getX()-c[0],p.getY()-c[1])<=460,"actual native setLocation wrapper");
    }
    if(ranked&&isMelee){commit.invoke(c[0]+1500,c[1],p.getZ());verify(p.getX()==c[0]+1500,"ranked pursuit beyond900, still inside2000 envelope");}
    if(!human)verify(ArenaCombatRules.retreatRadius(p,e,2000)==450,"voluntary kite radius unchanged even for melee");
    for(int angle=0;angle<360;angle+=15){double a=Math.toRadians(angle);Location n=ArenaCombatRules.orbit(c[0]+(int)(440*Math.cos(a)),c[1]+(int)(440*Math.sin(a)),0,e.getX(),e.getY(),c[0],c[1],120,1);verify(Math.hypot(n.getX()-c[0],n.getY()-c[1])<=451,"kite planner remains450, not outer attack envelope");}
    Location n=ArenaCombatRules.orbit(c[0]+900,c[1],0,e.getX(),e.getY(),c[0],c[1],120,1);verify(Math.hypot(n.getX()-c[0],n.getY()-c[1])<900&&Math.hypot(n.getX()-c[0]-900,n.getY()-c[1])<=121,"return from chase is ordinary bounded movement, not teleport");
   }finally{ArenaCombatRules.clear(p.getObjectId());ArenaCombatRules.clear(e.getObjectId());roster.remove(p.getObjectId());roster.remove(e.getObjectId());}
  }
 }
 public static void main(String[]args){try {
  org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
  handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
  act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
  var geo=java.nio.file.Files.createTempDirectory("cardinal75-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
  BotPresets.loadOverrides();pursuit77();System.out.println("PASS: "+checks+" all19 melee pursuit/native position wrappers/opponent-first/unchanged kite checks; no live server");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
