package custom.fakeplayersai;
import java.io.File;
import java.lang.classfile.ClassFile;
import java.lang.constant.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
/** Real native skills and actual act/poll selection. Only cast/UI are recording seams. */
public class DominatorPressureBluffTest extends BerserkerSoulCleanseTest {
    static int last;
    public static boolean request(Player p,Skill s,boolean force,boolean shift) {
        Trace t=traces.get(p); t.requests++;t.castTarget=p.getTarget();last=s.getId();
        verify(force&&shift,"ordinary forced shift cast, never native walk-to-cast");
        verify(p.getTarget()==((s.getId()==1306||s.getId()==1510||s.getId()==453)?p:targetEnemy),"correct self/enemy cast target");
        if(t.accept)p.setCastingNow(true);return t.accept;
    }
    static Player targetEnemy;
    static void recordingType()throws Exception
    {
        ClassDesc player=ClassDesc.of("org.l2jmobius.gameserver.entity.actor.Player"),world=ClassDesc.of("org.l2jmobius.gameserver.entity.WorldObject"),sk=ClassDesc.of("org.l2jmobius.gameserver.mechanics.skill.Skill"),test=ClassDesc.of(DominatorPressureBluffTest.class.getName());
        var bytes=ClassFile.of().build(ClassDesc.of("custom.fakeplayersai.PressureRecordingPlayer"),c->c.withSuperclass(player)
            .withMethodBody("setTarget",MethodTypeDesc.of(ConstantDescs.CD_void,world),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).invokestatic(test,"target",MethodTypeDesc.of(ConstantDescs.CD_void,player,world)).return_())
            .withMethodBody("getTarget",MethodTypeDesc.of(world),ClassFile.ACC_PUBLIC,b->b.aload(0).invokestatic(test,"target",MethodTypeDesc.of(world,player)).areturn())
            .withMethodBody("useMagic",MethodTypeDesc.of(ConstantDescs.CD_boolean,sk,ConstantDescs.CD_boolean,ConstantDescs.CD_boolean),ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).iload(2).iload(3).invokestatic(test,"request",MethodTypeDesc.of(ConstantDescs.CD_boolean,player,sk,ConstantDescs.CD_boolean,ConstantDescs.CD_boolean)).ireturn()));
        recording=MethodHandles.lookup().defineClass(bytes);
    }

    static Skill nativeSkill(int id){return SkillData.getInstance().getSkill(id,SkillData.getInstance().getMaxLevel(id));}
    static void location(Player p,int x)throws Exception {f(org.l2jmobius.gameserver.entity.WorldObject.class,"_location").set(p,new Location(x,0,0,0,10));}
    static void finish(Player p,Player e,Object s,long now)throws Exception {p.setCastingNow(false);poll.invoke(null,p,e,s,now);}
    static void actAt(Player p,Player e,Object s,long now)throws Exception {last=0;act.invoke(null,p,e,BotPresets.getPresetByClassId(115),s,now,false);}
    static void policies(){
        var d=new TacticsPolicy.DominatorPressure();
        var legal=List.of(new TacticsPolicy.Option(1245,TacticsPolicy.Kind.DAMAGE,100),new TacticsPolicy.Option(1097,TacticsPolicy.Kind.CONTROL,30),new TacticsPolicy.Option(1367,TacticsPolicy.Kind.SLOW,25),new TacticsPolicy.Option(1246,TacticsPolicy.Kind.CONTROL,25));
        for(int i=0;i<100;i++){
            d.reset();verify(d.choose(legal,List.of(1097),List.of(1367,1246),false)==1097,"control before high score damage");
            d.resolved(1097,false,true,false,false);verify(!d.opened(),"rejected request cannot advance");
            d.resolved(1097,true,true,false,false);verify(d.opened(),"executed resisted control advances, no indefinite sleep retry");
            verify(d.choose(legal,List.of(1097),List.of(1367,1246),false)==1367,"first effective rotation curse");
            d.resolved(1367,true,false,true,false);verify(d.choose(legal,List.of(1097),List.of(1367,1246),true)==1246,"remaining curse even during sleep");
            d.resolved(1246,true,false,true,false);verify(d.choose(legal,List.of(1097),List.of(1367,1246),true)==1245,"damage after curses");
            d.resolved(1245,true,false,false,true);verify(!d.opened()&&!d.curseDone(1367),"damage rearms next cycle");
            verify(d.choose(legal,List.of(1097),List.of(1367,1246),true)==1367,"do not refresh existing sleep/stun");
            verify(d.choose(List.of(legal.get(0)),List.of(),List.of(),false)==1245,"unavailable control and curses never stall damage");
        }
    }
    static void integration()throws Exception {
        BotPresets.loadOverrides();verify(BotPresets.getPresetByClassId(115).hasRotationOverride(),"actual persisted115 rotation");
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{92,94,95,97,102,103,109,110,115,134}){
            Player p=bot(115,ranked,false),e=actor(cid,ranked,true);targetEnemy=e;location(e,900);location(p,0);
            float[] cps=new float[256];Arrays.fill(cps,10000);f(p.getTemplate().getClass(),"_baseCp").set(p.getTemplate(),cps);
            f(Creature.class,"_zones").set(e,new byte[64]);
            f(org.l2jmobius.gameserver.entity.actor.stat.PlayerStat.class,"_oldMaxCp").setInt(p.getStat(),10000);
            if(cid==134)e.addStatFunc(new org.l2jmobius.gameserver.mechanics.stats.functions.FuncAdd(org.l2jmobius.gameserver.mechanics.stats.Stat.POWER_ATTACK_RANGE,0x10,new Object(),500,null));
            f(Player.class,"_mountType").set(p,org.l2jmobius.gameserver.entity.actor.enums.player.MountType.NONE);p.setTarget(e);hp(p,10000);
            for(int id:new int[]{1245,1097,1367})learn(p,nativeSkill(id));
            Object s=fresh();verify(BotTactics.tacticalApproachRange(p,e,BotPresets.getPresetByClassId(115))<=40,"close range vs mage/archer "+cid);
            actAt(p,e,s,10000);verify(last==0,"no ranged drain/sleep before closing");
            location(e,30);actAt(p,e,s,20000);            verify(last==1097,"actual act prioritizes sleep over earlier1245 in effective rotation");
            finish(p,e,s,25000);actAt(p,e,s,30000);verify(last==1367,"actual completed control -> first curse, even resisted control");
            finish(p,e,s,35000);actAt(p,e,s,40000);verify(last==1245,"actual curse -> damage, never repeated curse/sleep");
            finish(p,e,s,45000);flags(e,EffectFlag.SLEEP.getMask());actAt(p,e,s,50000);verify(last==1367,"sleep kept for curses regardless old5s timer");
            finish(p,e,s,55000);actAt(p,e,s,60000);verify(last==1245,"damage after available curses breaks sleep");finish(p,e,s,65000);flags(e,0);
            // A rejected opener is not treated as an executed stage.
            s=fresh();traces.get(p).accept=false;actAt(p,e,s,70000);verify(last==1097&&f(state,"pendingId").getInt(s)==0,"rejection stays out of pending phase");
            traces.get(p).accept=true;actAt(p,e,s,75000);verify(last==1097,"retry opener after ordinary failure backoff");finish(p,e,s,80000);
            // Native silence/resources/reuse still gate both opening and rotation.
            s=fresh();mp(p,0);actAt(p,e,s,90000);verify(last==0,"zeroMP never bypassed");mp(p,10000);
            @SuppressWarnings("unchecked") Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);
            disabled.put(p.getKnownSkill(1097).getReuseHashCode(),System.currentTimeMillis()+60000);
            actAt(p,e,s,95000);verify(last==1367,"sleep on native cooldown -> available curse without waiting");finish(p,e,s,100000);disabled.clear();
            s=fresh();learn(p,nativeSkill(260));
            f(org.l2jmobius.gameserver.entity.item.Weapon.class,"_type").set(p.getActiveWeaponItem(),org.l2jmobius.gameserver.entity.item.type.WeaponType.BLUNT);
            f(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.class,"_wearedMask").setInt(p.getInventory(),org.l2jmobius.gameserver.entity.item.type.WeaponType.BLUNT.mask());
            disabled.put(p.getKnownSkill(1097).getReuseHashCode(),System.currentTimeMillis()+60000);
            actAt(p,e,s,101000);verify(last==260,"ready legal native Hammer Crush opens when sleep is unavailable");finish(p,e,s,106000);disabled.clear();
            @SuppressWarnings("unchecked") Map<Integer,Skill> known=(Map<Integer,Skill>)f(Creature.class,"_skills").get(p);known.remove(260);
            s=fresh();learn(p,nativeSkill(1306));f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(p.getStatus(),2000);
            actAt(p,e,s,110000);verify(last==1306&&p.getTarget()==p,"lowCP support interrupts opening, real self target");finish(p,e,s,115000);
            actAt(p,e,s,120000);verify(last==1097,"afterCP follow control phase, not CP-weave damage override");finish(p,e,s,125000);
            actAt(p,e,s,130000);verify(last==1306,"maintain lowCP between control and curse");finish(p,e,s,135000);
            actAt(p,e,s,140000);verify(last==1367,"CP support did not reset completed opener");finish(p,e,s,145000);
            f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(p.getStatus(),6000);
            actAt(p,e,s,150000);verify(last==1245,"adequateCP: damage after curse, no unnecessary CP cast");finish(p,e,s,155000);
            Player melee=actor(131,ranked,true);verify(BotTactics.tacticalApproachRange(p,melee,BotPresets.getPresetByClassId(115))>40,"melee opponent keeps existing ranged approach");
        }
        // Native general SkillData sees restored default reuse for all skill levels, actors share it.
        for(int level:new int[]{1,101,105,110,115}){
            Skill bluff=SkillData.getInstance().getSkill(358,level);verify(bluff!=null&&bluff.getReuseDelay()==8000,"native358 default reuse8s level"+level);
            for(int cid:new int[]{93,101,108})for(boolean human:new boolean[]{true,false}){Player p=actor(cid,true,human);learn(p,bluff);verify(p.getKnownSkill(358).getReuseDelay()==8000,"player and bot use same358 template");}
        }
    }
    static void tranceDefaults()throws Exception {
        for(int level:new int[]{1,10,101,115,130,201,215,230}){
            Skill trance=SkillData.getInstance().getSkill(1394,level);
            verify(trance!=null&&trance.getReuseDelay()==1000,"native1394 default reuse1s level"+level);
            verify(trance.getHitTime()==3000,"cast time is not cooldown and remains3s");
        }
        // Actual act/poll in both arenas: completed/resisted cast is retried before the
        // removed3s AI delay, but native disabled-skill state still blocks selection.
        Skill trance=SkillData.getInstance().getSkill(1394,230);
        for(boolean ranked:new boolean[]{false,true}){
            Player p=bot(97,ranked,false),e=actor(94,ranked,true);targetEnemy=e;location(p,0);location(e,200);
            f(Creature.class,"_zones").set(e,new byte[64]);
            f(Player.class,"_mountType").set(p,org.l2jmobius.gameserver.entity.actor.enums.player.MountType.NONE);
            p.setPrivateStoreType(org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType.NONE);
            float[] mana=new float[256];Arrays.fill(mana,1000);f(p.getTemplate().getClass(),"_baseMp").set(p.getTemplate(),mana);
            f(org.l2jmobius.gameserver.entity.actor.stat.PlayerStat.class,"_oldMaxMp").setInt(p.getStat(),1000);
            p.setTarget(e);hp(p,10000);learn(p,trance);Object s=fresh();f(state,"nextStep").setLong(s,Long.MAX_VALUE);
            var preset=BotPresets.getPresetByClassId(97);
            last=0;act.invoke(null,p,e,preset,s,10000L,false);
            verify(last==1394,"ready native Trance requested");finish(p,e,s,10500);
            @SuppressWarnings("unchecked") Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);
            disabled.put(trance.getReuseHashCode(),System.currentTimeMillis()+60000);
            verify(BotTactics.tacticalApproachRange(p,e,preset)!=600,"native cooldown excludes Trance from short approach");
            last=0;act.invoke(null,p,e,preset,s,11100L,false);verify(last==0,"native reuse still blocks Trance");
            disabled.put(trance.getReuseHashCode(),System.currentTimeMillis()-1);
            verify(BotTactics.tacticalApproachRange(p,e,preset)==600,"expired native cooldown enables Trance approach");
            last=0;act.invoke(null,p,e,preset,s,11500L,false);
            verify(last==1394,"no extra3s timer after accepted Trance; ordinary native readiness only");finish(p,e,s,12000);
        }
    }
    public static void main(String[]args)throws Exception {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("pressure53-geodata-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        policies();integration();tranceDefaults();System.out.println("PASS: "+checks+" Bluff native reuse and Dominator actual act/poll/rotation pressure checks; no live server");
    }
}
