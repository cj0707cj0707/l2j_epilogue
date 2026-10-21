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
        verify(p.getTarget()==((s.getId()==69||s.getId()==833||s.getId()==1305||s.getId()==1306||s.getId()==1510||s.getId()==453||s.getId()==1218||s.getId()==1418||TankClassMechanics.support(s.getId()))?p:targetEnemy),"correct self/enemy cast target");
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
    static void finish(Player p,Player e,Object s,long now)throws Exception {p.setCastingNow(false);
        Skill skill=p.getKnownSkill(f(state,"pendingId").getInt(s));
        if(skill!=null) mp(p,f(state,"pendingMp").getDouble(s)-p.getStat().getMpInitialConsume(skill)-p.getStat().getMpConsume(skill));
        poll.invoke(null,p,e,s,now);
    }
    static void actAt(Player p,Player e,Object s,long now)throws Exception {last=0;act.invoke(null,p,e,BotPresets.getPresetByClassId(115),s,now,false);}
    static void policies(){
        var d=new TacticsPolicy.DominatorPressure();
        var legal=List.of(new TacticsPolicy.Option(1245,TacticsPolicy.Kind.DAMAGE,100),new TacticsPolicy.Option(1097,TacticsPolicy.Kind.CONTROL,30),new TacticsPolicy.Option(1367,TacticsPolicy.Kind.SLOW,25),new TacticsPolicy.Option(1246,TacticsPolicy.Kind.CONTROL,25));
        for(int i=0;i<100;i++){
            d.reset();verify(d.choose(legal,List.of(1097),List.of(1367,1246),false)==1097,"control before high score damage");
            d.resolved(1097,false,true,false,false);verify(!d.opened(),"rejected request cannot advance");
            d.resolved(1097,true,true,false,false);verify(d.opened(),"compatibility overload explicitly represents a completed landed control");
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
            Object s=fresh();verify(BotTactics.tacticalApproachRange(p,e,BotPresets.getPresetByClassId(115))==900,"rule88 native Steal approach vs ranged "+cid);
            actAt(p,e,s,10000);verify(last==1245,"rule88 starts with Steal, not sleep/curse");finish(p,e,s,15000);
            s=fresh();traces.get(p).accept=false;actAt(p,e,s,20000);verify(last==1245&&f(state,"pendingId").getInt(s)==0,"rejected Steal never creates pending or advances cycle");
            traces.get(p).accept=true;actAt(p,e,s,25000);verify(last==1245,"native failure backoff allows later retry");finish(p,e,s,30000);
            s=fresh();mp(p,0);actAt(p,e,s,35000);verify(last==0,"zeroMP never bypassed");mp(p,10000);
            @SuppressWarnings("unchecked") Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);
            disabled.put(p.getKnownSkill(1245).getReuseHashCode(),System.currentTimeMillis()+60000);
            actAt(p,e,s,40000);verify(last==0,"Steal cooldown never falls back to unrequested curse/early sleep");disabled.clear();
            s=fresh();learn(p,nativeSkill(1306));f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(p.getStatus(),2000);
            actAt(p,e,s,45000);verify(last==1245&&p.getTarget()==e,"rule88 awake ranged opponent: only Steal despite lowCP");finish(p,e,s,50000);
            Player melee=actor(131,ranked,true);verify(BotTactics.tacticalApproachRange(p,melee,BotPresets.getPresetByClassId(115))>40,"melee opponent keeps existing ranged approach");
        }
        // Native SkillData sees Bluff base reuse +50% (8s ->12s), including every Chance level.
        for(int level:new int[]{1,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115}){
            Skill bluff=SkillData.getInstance().getSkill(358,level);verify(bluff!=null&&bluff.getReuseDelay()==12000,"native358 base reuse12s (+50%) level"+level);
            for(int cid:new int[]{93,101,108})for(boolean human:new boolean[]{true,false}){Player p=actor(cid,true,human);learn(p,bluff);verify(p.getKnownSkill(358).getReuseDelay()==12000,"player and bot use same358 template");}
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
            // Rule75 also enters the normal reuse-gap kite vs ranged targets: record, never run world movement.
            f(Creature.class,"_ai").set(p,new DaggerRearApproachTest.RecordingAI(p));
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
    // Preserve human/bot identity, suppress only packets to a nonexistent connection.
    static class HeadlessClient extends org.l2jmobius.gameserver.network.GameClient {
        HeadlessClient(){super(null);}
        @Override public void sendPacket(org.l2jmobius.gameserver.network.serverpackets.ServerPacket packet){}
        @Override public void sendPacket(org.l2jmobius.gameserver.network.SystemMessageId id){}
    }
    static Player chanceActor(int cid,boolean ranked,boolean human)throws Exception {
        Player p=actor(cid,ranked,human);
        Field access=f(Player.class,"_accessLevel");access.set(p,unsafe.allocateInstance(access.getType()));
        if(human)p.setClient((HeadlessClient)unsafe.allocateInstance(HeadlessClient.class));
        return p;
    }
    static class StunCapture extends EffectList {
        int additions;
        StunCapture(Player p){super(p);}
        @Override public void add(BuffInfo info){
            verify(info.getEffects().size()==1&&info.getEffects().get(0).getEffectType()==org.l2jmobius.gameserver.mechanics.effects.EffectType.STUN,"one native continuous Stun, independent of instant80% effects");
            additions++; // no world scheduler, packets or stat/paperdoll callbacks
        }
    }
    static void fixedBluffStun()throws Exception {
        int previous=org.l2jmobius.gameserver.mechanics.stats.Formulas.FIXED_DEBUFF_RATE_PERCENT;
        try {
            org.l2jmobius.gameserver.mechanics.stats.Formulas.FIXED_DEBUFF_RATE_PERCENT=100;
            for(int cid:new int[]{93,101,108})for(boolean ranked:new boolean[]{false,true})for(boolean humanCaster:new boolean[]{false,true})for(boolean humanTarget:new boolean[]{false,true})
                for(int level:new int[]{1,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115}) {
                    Skill nativeBluff=SkillData.getInstance().getSkill(358,level);
                    verify(nativeBluff.getMinChance()==40&&nativeBluff.getMaxChance()==40&&nativeBluff.getActivateRate()<100&&nativeBluff.getBasicProperty()!=org.l2jmobius.gameserver.mechanics.stats.BaseStat.NONE,"all Bluff/enchant levels clamp40, no guaranteed-success path");
                    verify(nativeBluff.getReuseDelay()==12000&&nativeBluff.getAbnormalTime()==9&&nativeBluff.getMpConsume()==35,"Bluff reuse12s/duration9s/MP unchanged");
                    if(level!=1&&level!=101&&level!=108&&level!=115)continue;
                    Player p=chanceActor(cid,ranked,humanCaster),e=chanceActor(94,ranked,humanTarget);learn(p,nativeBluff);
                    if(!humanCaster)org.l2jmobius.gameserver.mechanics.stats.Formulas.FIXED_DEBUFF_RATE_PLAYERS.add(p.getObjectId());
                    BotControlChance.apply(p);
                    verify(!org.l2jmobius.gameserver.mechanics.stats.Formulas.FIXED_DEBUFF_RATE_PLAYERS.contains(p.getObjectId()),"dagger bot clears stale early100% gate");
                    verify(p.getKnownSkill(358)==nativeBluff,"same native Bluff for player/bot, no private clone or condition loss");
                    for(int profile:new int[]{0,1,2}) {
                        Object owner=new Object();
                        e.addStatFunc(new org.l2jmobius.gameserver.mechanics.stats.functions.FuncSet(org.l2jmobius.gameserver.mechanics.stats.Stat.DEBUFF_VULN,0x70,owner,profile==1?10000:profile==2?-100:1,null));
                        int trait=nativeBluff.getTraitType().ordinal();
                        p.getStat().getAttackTraits()[trait]=profile==1?3:1;e.getStat().getDefenceTraits()[trait]=profile==2?10:1;
                        int passed=0;for(int n=0;n<2000;n++)if(org.l2jmobius.gameserver.mechanics.stats.Formulas.calcEffectSuccess(p,e,nativeBluff))passed++;
                        verify(passed>640&&passed<960,"fixed40% native formula despite enchant/trait/vulnerability profile="+profile+" level="+level+" passed="+passed);
                        e.removeStatsOwner(owner);
                    }
                    Object immunity=new Object();
                    e.addStatFunc(new org.l2jmobius.gameserver.mechanics.stats.functions.FuncSet(org.l2jmobius.gameserver.mechanics.stats.Stat.DEBUFF_IMMUNITY,0x70,immunity,1,null));
                    for(int n=0;n<10;n++)verify(!org.l2jmobius.gameserver.mechanics.stats.Formulas.calcEffectSuccess(p,e,nativeBluff),"debuff immunity is not bypassed by fixed40");
                    e.removeStatsOwner(immunity);e.getStat().getTraitsInvul()[nativeBluff.getTraitType().ordinal()]=1;
                    for(int n=0;n<10;n++)verify(!org.l2jmobius.gameserver.mechanics.stats.Formulas.calcEffectSuccess(p,e,nativeBluff),"full trait immunity stays0");
                }
            // Actual Skill.applyEffects continuous gate, with instant actions disabled only in fixture.
            for(int level:new int[]{1,115}) {
                Player p=chanceActor(108,true,false),e=chanceActor(97,true,true);Skill bluff=SkillData.getInstance().getSkill(358,level);
                f(Creature.class,"_invulAgainst").set(e,new java.util.concurrent.ConcurrentHashMap<>());
                StunCapture list=new StunCapture(e);f(Creature.class,"_effectList").set(e,list);
                for(int n=0;n<4000;n++)bluff.applyEffects(p,e,false,9);
                verify(list.additions>1360&&list.additions<1840,"native continuous apply remains40%, not40%x80%, enchant level="+level+" passed="+list.additions);
            }
        } finally {org.l2jmobius.gameserver.mechanics.stats.Formulas.FIXED_DEBUFF_RATE_PERCENT=previous;}
    }
    public static void main(String[]args)throws Exception {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("pressure53-geodata-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        policies();integration();tranceDefaults();fixedBluffStun();System.out.println("PASS: "+checks+" Bluff fixed40% stun/native reuse and Dominator actual act/poll/rotation pressure checks; no live server");
    }
}
