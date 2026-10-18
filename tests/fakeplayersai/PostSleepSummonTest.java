package custom.fakeplayersai;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.ai.*;
import org.l2jmobius.gameserver.entity.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.mechanics.effects.*;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.stats.*;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncAdd;
import org.l2jmobius.gameserver.util.StatSet;
import sun.misc.Unsafe;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Actual act/effects/native doSummonAttack. Movement/cast/AI recording only; no world/DB. */
public class PostSleepSummonTest extends DominatorPressureBluffTest
{
    static class MoveAI extends DaggerRearApproachTest.RecordingAI {
        MoveAI(Player p){super(p);accept=true;}
        @Override public void setIntentionIdle(){try{f(Creature.class,"_move").set(_actor,null);_intention=Intention.IDLE;}catch(Exception e){throw new RuntimeException(e);}}
    }
    static class PetAI extends SummonAI {
        int requests;WorldObject requested;
        PetAI(Summon s){super(s);}
        @Override public void setIntentionAttack(WorldObject target){requests++;requested=target;_intention=Intention.ATTACK;}
    }
    static void at(Player p,int x,int y)throws Exception{f(WorldObject.class,"_location").set(p,new Location(x,y,0,0,10));}
    static Player owner(int cid,boolean ranked)throws Exception {
        Player p=bot(cid,ranked,false);f(Player.class,"_mountType").set(p,org.l2jmobius.gameserver.entity.actor.enums.player.MountType.NONE);
        float[] pool=new float[256];Arrays.fill(pool,10000);
        f(p.getTemplate().getClass(),"_baseCp").set(p.getTemplate(),pool);f(p.getTemplate().getClass(),"_baseMp").set(p.getTemplate(),pool);
        f(org.l2jmobius.gameserver.entity.actor.stat.PlayerStat.class,"_oldMaxCp").setInt(p.getStat(),10000);
        f(org.l2jmobius.gameserver.entity.actor.stat.PlayerStat.class,"_oldMaxMp").setInt(p.getStat(),10000);
        f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(p.getStatus(),10000);
        hp(p,10000);at(p,80,0);return p;
    }
    static BuffInfo appliedSleep(Player p,Player e,int cid)throws Exception {
        Skill spell=nativeSkill(cid==97?1394:1097);learn(p,spell);
        StatSet props=new StatSet();props.set("name","Sleep");var effect=new handlers.skill.effects.Sleep(null,null,props,new StatSet());
        BuffInfo b=CpMeleeRushRecoveryTest.info(p,e,spell,effect);
        e.getEffectList().getDebuffs().clear();e.getEffectList().getDebuffs().add(b);
        @SuppressWarnings("unchecked") Map<AbnormalType,BuffInfo> slots=(Map<AbnormalType,BuffInfo>)f(EffectList.class,"_stackedEffects").get(e.getEffectList());slots.clear();slots.put(spell.getAbnormalType(),b);
        flags(e,EffectFlag.SLEEP.getMask());return b;
    }
    static void clearSleep(Player e)throws Exception {flags(e,0);e.getEffectList().getDebuffs().clear();@SuppressWarnings("unchecked") Map<AbnormalType,BuffInfo> slots=(Map<AbnormalType,BuffInfo>)f(EffectList.class,"_stackedEffects").get(e.getEffectList());slots.clear();}
    static boolean run(Player p,Player e,Object s,long t)throws Exception {last=0;targetEnemy=e;return (boolean)act.invoke(null,p,e,BotPresets.getPresetByClassId(p.getPlayerClass().getId()),s,t,false);}
    static Object centered()throws Exception{Object s=fresh();f(state,"hasArenaCenter").setBoolean(s,true);f(state,"arenaCenterX").setInt(s,0);f(state,"arenaCenterY").setInt(s,0);return s;}
    static void geometry(){
        for(int ea=0;ea<360;ea+=15)for(int er:new int[]{0,100,300,449,450,600})for(int ba=0;ba<360;ba+=30)for(int br:new int[]{0,80,300,449})for(int want:new int[]{300,900,1000}) {
            int ex=(int)Math.round(er*Math.cos(Math.toRadians(ea))),ey=(int)Math.round(er*Math.sin(Math.toRadians(ea)));
            int x=(int)Math.round(br*Math.cos(Math.toRadians(ba))),y=(int)Math.round(br*Math.sin(Math.toRadians(ba)));
            Location p=SleepRetreat.goal(x,y,7,ex,ey,0,0,want);
            verify(p!=null&&p.getZ()==7&&Math.hypot(p.getX(),p.getY())<=450,"every planned goal inside450 circle");
            double maximum=Math.hypot(ex,ey)+449,need=Math.min(want-1,maximum),actual=Math.hypot(p.getX()-ex,p.getY()-ey);
            verify(Math.abs(actual-need)<1,"best feasible requested separation (integer rounding only)");
            for(int k=0;k<=10;k++)verify(Math.hypot(x+(p.getX()-x)*k/10.0,y+(p.getY()-y)*k/10.0)<=450,"entire straight route inside convex kite circle");
        }
        verify(SleepRetreat.goal(0,0,0,0,0,0,0,Double.NaN)==null,"invalid range rejected");
        Location across=SleepRetreat.goal(350,0,0,400,0,0,0,900);verify(across.getX()<-440,"ordinary straight run may cross arena center to far edge");
    }
    static void retreats()throws Exception {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{97,115}) {
            Player p=owner(cid,ranked),e=actor(94,ranked,true);at(e,0,0);f(Creature.class,"_zones").set(e,new byte[64]);p.setTarget(e);
            if(cid==97)learn(p,nativeSkill(1398));else for(int id:new int[]{1245,1367})learn(p,nativeSkill(id));
            MoveAI ai=new MoveAI(p);f(Creature.class,"_ai").set(p,ai);Object s=centered();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            BuffInfo sleep=appliedSleep(p,e,cid);verify(SleepRetreat.ownSleep(p,e)==sleep,"real current own applied SLEEP");
            verify(run(p,e,s,10000)&&ai.requests==1&&last==0,"actual retreat preempts reaction and offensive cast");
            double wanted=cid==97?449:299;
            verify(Math.abs(Math.hypot(ai.requested.getX()-e.getX(),ai.requested.getY()-e.getY())-wanted)<1,"Cardinal range capped by circle; Dominator300");
            for(int i=1;i<=8;i++)verify(run(p,e,s,10000+i*100)&&ai.requests==1&&last==0,"entire running route owned, not a200ms step");
            int tx=ai.requested.getX(),ty=ai.requested.getY();at(p,tx,ty);f(Creature.class,"_move").set(p,null);
            f(state,"nextDecision").setLong(s,0);verify(run(p,e,s,12000)&&last==(cid==97?1398:1367),"after arrival ordinary legal attack resumes;115 does not chase back40 into sleep cid="+cid+" last="+last+" casting="+p.isCastingNow());
            finish(p,e,s,17000);verify(ai.requests==1,"one retreat for one effect, no repeated circle walking");
            // Removed sleep stops ONLY the owned route; another AI move is never cancelled.
            at(p,80,0);clearSleep(e);appliedSleep(p,e,cid);s=centered();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);run(p,e,s,20000);
            verify(p.isMoving(),"second distinct actual sleep starts new retreat");clearSleep(e);run(p,e,s,20100);verify(!p.isMoving(),"sleep ended: owned route released immediately");
            appliedSleep(p,e,cid);s=centered();run(p,e,s,22000);ai.setIntentionMoveTo(new Location(10,20,0));clearSleep(e);run(p,e,s,22100);verify(p.isMoving()&&p.getXdestination()==10,"foreign route not stopped on sleep expiry");f(Creature.class,"_move").set(p,null);
            // Root/CC/cast are genuine prohibitions, never abort/teleport bypasses.
            appliedSleep(p,e,cid);s=centered();flags(p,EffectFlag.ROOTED.getMask());int before=ai.requests;f(state,"nextDecision").setLong(s,Long.MAX_VALUE);run(p,e,s,24000);verify(ai.requests==before,"own root never bypassed");flags(p,0);
            p.setCastingNow(true);run(p,e,s,25000);verify(ai.requests==before&&p.isCastingNow(),"real cast retained");p.setCastingNow(false);
            flags(e,0);verify(SleepRetreat.ownSleep(p,e)==null,"template alone/failed sleep cannot trigger retreat");flags(e,EffectFlag.SLEEP.getMask());
            f(BuffInfo.class,"_effector").set(e.getEffectList().getDebuffs().iterator().next(),actor(94,ranked,false));verify(SleepRetreat.ownSleep(p,e)==null,"another caster sleep cannot trigger own retreat");clearSleep(e);
            // At the edge and already far enough: no extra retreat. Range stat is real.
            at(p,449,0);at(e,-449,0);appliedSleep(p,e,cid);s=centered();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);before=ai.requests;run(p,e,s,30000);verify(ai.requests==before,"already safely distant: no needless movement");
            if(cid==97){verify(SleepRetreat.distance(p)==900,"native Mana Burn maximum900 not invented600");p.addStatFunc(new FuncAdd(Stat.MAGIC_ATTACK_RANGE,0x10,new Object(),100,null));verify(SleepRetreat.distance(p)==1000,"actual range modifier respected");}
        }
    }
    static void edgeCases()throws Exception {
        for(int cid:new int[]{97,115}) {
            Player p=owner(cid,false),e=actor(94,false,true);at(e,0,0);if(cid==97)learn(p,nativeSkill(1398));
            MoveAI ai=new MoveAI(p);f(Creature.class,"_ai").set(p,ai);ai.accept=false;
            BuffInfo sleep=appliedSleep(p,e,cid);SleepRetreat r=new SleepRetreat();int[] center={0,0};
            verify(!r.tick(p,e,1000,center)&&ai.requests>0,"refused movement releases ordinary rotation");int before=ai.requests;
            for(long t:new long[]{1001,1200,1499})verify(!r.tick(p,e,t,center)&&ai.requests==before,"refused route has500ms backoff");
            ai.accept=true;verify(r.tick(p,e,1500,center)&&ai.requests==before+1,"accepted retry owns whole route");
            sleep.setInUse(false);verify(SleepRetreat.ownSleep(p,e)==null&&!r.tick(p,e,1600,center)&&!p.isMoving(),"inactive sleep releases owned route");sleep.setInUse(true);
            @SuppressWarnings("unchecked") Map<AbnormalType,BuffInfo> stack=(Map<AbnormalType,BuffInfo>)f(EffectList.class,"_stackedEffects").get(e.getEffectList());stack.clear();
            verify(SleepRetreat.ownSleep(p,e)==null,"non-current stacked sleep cannot trigger");stack.put(sleep.getSkill().getAbnormalType(),sleep);
            verify(!new SleepRetreat().tick(p,e,2000,null),"unknown arena center: never invent world-space route");
            Player human=bot(cid,false,true);appliedSleep(human,e,cid);verify(SleepRetreat.ownSleep(human,e)==null,"human character never controlled");
            if(cid==97){Player missing=owner(cid,false);appliedSleep(missing,e,cid);verify(SleepRetreat.distance(missing)==0&&!new SleepRetreat().tick(missing,e,3000,center),"unlearned Mana Burn gives no invented range");}
        }
    }
    static Servitor pet(Player owner,PetAI[] out)throws Exception {
        Servitor s=(Servitor)unsafe.allocateInstance(Servitor.class);f(Summon.class,"_owner").set(s,owner);f(Creature.class,"_effectList").set(s,new EffectList(s));f(WorldObject.class,"_location").set(s,new Location(0,0,0,0,10));
        PetAI ai=new PetAI(s);f(Creature.class,"_ai").set(s,ai);f(Player.class,"_summon").set(owner,s);out[0]=ai;return s;
    }
    @SuppressWarnings("unchecked") static void pulses()throws Exception {
        Map<Integer,Player> roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{90,91,95}) {
            Player p=owner(cid,ranked),e=actor(94,ranked,true);p.setTarget(p);f(WorldObject.class,"_isSpawned").setBoolean(e,true);
            PetAI[] holder=new PetAI[1];Servitor summon=pet(p,holder);PetAI ai=holder[0];SummonAttackPulse pulse=new SummonAttackPulse();
            for(int i=0;i<100;i++){
                long now=1000+i*3000L;pulse.tick(p,e,now);verify(ai.requests==i+1&&ai.requested==e&&summon.getTarget()==e,"native doSummonAttack repeated with UNCHANGED summon target");
                pulse.tick(p,e,now);pulse.tick(p,e,now+2999);verify(ai.requests==i+1,"no duplicate before exact3seconds even across callers");verify(p.getTarget()==p,"owner self-cast target untouched");
            }
            f(Creature.class,"_isDead").setBoolean(summon,true);int before=ai.requests;
            for(int i=0;i<10;i++)pulse.tick(p,e,400000+i*3000L);verify(ai.requests==before&&p.getSummon()==summon,"dead summon: no attack, replacement or resurrection");f(Creature.class,"_isDead").setBoolean(summon,false);
            f(Player.class,"_summon").set(p,null);pulse.tick(p,e,500000);verify(ai.requests==before&&p.getSummon()==null,"missing summon stays absent");f(Player.class,"_summon").set(p,summon);
            summon.setCastingNow(true);pulse.tick(p,e,500000);verify(ai.requests==before&&summon.isCastingNow(),"own summon cast not interrupted");summon.setCastingNow(false);
            f(Summon.class,"_owner").set(summon,e);pulse.tick(p,e,500000);verify(ai.requests==before,"foreign summon refused");f(Summon.class,"_owner").set(summon,p);
            f(WorldObject.class,"_location").set(summon,new Location(0,0,0,0,11));pulse.tick(p,e,500000);verify(ai.requests==before,"foreign instance refused");f(WorldObject.class,"_location").set(summon,new Location(0,0,0,0,10));
            if(ranked)roster.put(p.getObjectId(),p);else verify(BotTactics.initializeTraining(p,e),"training pair explicitly authorized");
            BotTactics.tickSummonAttack(p,e,ranked);verify(ai.requests==before+1,"public authority-validated hook commands existing live summon");
            BotTactics.tickSummonAttack(p,e,ranked);verify(ai.requests==before+1&&p.getTarget()==p,"public callers share cadence without owner retarget");
            BotTactics.forget(p.getObjectId());roster.remove(p.getObjectId());
            if(ranked)f(Player.class,"_olympiadStart").setBoolean(p,false);
            BotTactics.tickSummonAttack(p,e,ranked);verify(ai.requests==before+1,"inactive/unauthorized pair never attacks");
        }
    }
    public static void main(String[]args)throws Exception {
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
        handlers.EffectMasterHandler.main(new String[0]);skill=SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("sleep54-geodata-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        geometry();retreats();edgeCases();pulses();System.out.println("PASS: "+checks+" post-sleep circle/range, actual act/effect ownership and native summon attack cadence checks; no live world/DB");
    }
}
