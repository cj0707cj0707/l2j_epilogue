package custom.fakeplayersai;

import java.util.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Real native stages/conditions and actual act/public hooks; recording cast/movement seams. */
public class TankChargeFinisher95Test extends ArenaClassRules94Test
{
    static int trigger(int cid){return cid==90?5562:cid==91?5561:5564;}
    static int finisher(int cid){return cid==90?785:cid==91?762:789;}
    static void immediate95()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{90,91,106})
        {
            Player e=actor(88,ranked,true),p=knight(cid,ranked,e);apply(p,trigger(cid),3);
            Object s=centered();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            f(state,"hpObserved").setLong(s,10000);f(state,"observed").setLong(s,10000);
            verify(run(p,e,s,10000)&&last==finisher(cid),"stage3 bypasses BOTH decision and fresh defense reaction cid"+cid);
            int casts=traces.get(p).requests;run(p,e,s,10001);
            verify(traces.get(p).requests==casts&&p.isCastingNow(),"never duplicate/interrupt the real finisher cast");
            p.setCastingNow(false);
        }
        for(boolean ranked:new boolean[]{false,true})
        {
            Player e=actor(88,ranked,true),p=knight(90,ranked,e);apply(p,406,p.getKnownSkill(406).getLevel());apply(p,5562,3);
            verify(run(p,e,centered(),10000)&&last==785,"Angelic Icon no longer bans a ready Flame Icon");p.setCastingNow(false);
            e=actor(94,ranked,true);p=knight(91,ranked,e);apply(p,5561,3);Object s=arena(p,e);
            at(e,p.getX()+180,p.getY());verify(run(p,e,s,10000)&&last==0,"Crusher never fires merely inside native200 AURA");
            at(e,p.getX()+30,p.getY());verify(run(p,e,s,10100)&&last==762,"Crusher casts immediately on contact");p.setCastingNow(false);
            e=actor(94,ranked,true);p=knight(106,ranked,e);apply(p,5564,3);e.setInvul(true);
            verify(run(p,e,centered(),10000)&&last==789,"support finisher precedes immunity movement without attacking protected target");p.setCastingNow(false);
        }
    }
    static void guards95()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{90,91,106})
        {
            Player e=actor(88,ranked,true),p=knight(cid,ranked,e);register(p,e,ranked,true);
            try
            {
                BotTactics.beginCombat(p,e);int id=finisher(cid),tr=trigger(cid);Skill skill=p.getKnownSkill(id);
                for(int stage:new int[]{0,1,2,3})
                {
                    clear(p,tr);if(stage>0)apply(p,tr,stage);
                    verify(BotSpawner.tacticalSkillAllowed(p,e,skill,cid!=91)==(stage==3),"selection guard requires own actual3");
                    verify((BotRecoveryRules.guard(castEvent(p,cid==91?e:p,id))==null)==(stage==3),"legacy/auto-use synchronous guard requires own actual3 cid="+cid+" ranked="+ranked+" stage="+stage);
                }
                var charge=p.getEffectList().getBuffInfoBySkillId(tr);charge.setInUse(false);
                verify(!TankClassMechanics.castAllowed(p,e,skill),"inactive third stage cannot unlock finisher");charge.setInUse(true);
                disable(p,id);run(p,e,centered(),10000);verify(last!=id,"native reuse not bypassed");disabled(p).clear();
                flags(p,EffectFlag.STUNNED.getMask());run(p,e,centered(),11000);verify(last!=id,"stun not bypassed");flags(p,0);
                flags(p,EffectFlag.PSYCHICAL_MUTED.getMask());run(p,e,centered(),12000);verify(last!=id,"physical mute not bypassed");flags(p,0);
                if(cid==91)
                {
                    mp(p,0);run(p,e,centered(),13000);verify(last!=id,"Crusher MP cost not bypassed");mp(p,10000);
                    at(e,p.getX()+180,p.getY());verify(BotRecoveryRules.guard(castEvent(p,e,id))!=null,"legacy Crusher outside contact vetoed");at(e,p.getX()+30,p.getY());
                    e.setInvul(true);verify(BotRecoveryRules.guard(castEvent(p,e,id))!=null,"Crusher cannot attack invulnerability");e.setInvul(false);
                }
                else
                {
                    hp(p,1253);run(p,e,centered(),14000);verify(last!=id,"icon HP cost not bypassed");hp(p,10000);
                }
                p.removeSkill(id,false);verify(TankClassMechanics.readyFinisher(p,e)==0,"absent finisher never learned by AI");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void mandatory95()throws Exception
    {
        boolean enabled=(boolean)f(TacticalSettings.class,"enabled").get(null);f(TacticalSettings.class,"enabled").setBoolean(null,false);
        try{for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{90,91,106})
        {
            Player e=actor(88,ranked,true),p=knight(cid,ranked,e);register(p,e,ranked,true);
            try
            {
                int[] c=ArenaCombatRules.center(p);at(p,c[0]+30,c[1]);at(e,c[0],c[1]);apply(p,trigger(cid),3);
                last=0;targetEnemy=e;verify(BotTactics.tickTankFinisher(p,e,BotPresets.getPresetByClassId(cid),ranked)&&last==finisher(cid),"mandatory hook independent of optional AI");
                int casts=traces.get(p).requests;
                verify(BotTactics.tickTankFinisher(p,e,BotPresets.getPresetByClassId(cid),ranked)&&casts==traces.get(p).requests,"public hook serializes pending finisher");
                clear(p,trigger(cid));finish(p,e,publicState(p),Long.MAX_VALUE/2);
                verify(!BotTactics.tickTankFinisher(p,e,BotPresets.getPresetByClassId(cid),ranked)&&!BotTactics.isOwned(p.getObjectId()),"transient hook releases without enduring ownership");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }}finally{f(TacticalSettings.class,"enabled").setBoolean(null,enabled);}
    }
    static void targets95()throws Exception
    {
        Player e=actor(88,false,true),p=knight(90,false,e);PetAI[] out=new PetAI[1];var summon=pet(p,out);
        verify(TankClassMechanics.iconTargets(p,List.of(p,summon)),"own summon is a valid PARTY icon recipient");
        verify(!TankClassMechanics.iconTargets(p,List.of(p,e)),"opponent never receives icon");
        verify(!TankClassMechanics.iconTargets(p,List.of(summon)),"caster must be among targets");
        verify(!TankClassMechanics.iconTargets(p,List.of()),"empty target list rejected");
        f(Player.class,"_summon").set(p,null);
        verify(!TankClassMechanics.iconTargets(p,List.of(p,summon)),"foreign/old summon rejected");
    }
    public static void main(String[]args){try
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
        handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("tank95-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        org.l2jmobius.gameserver.handler.TargetHandler.getInstance().registerHandler(new handlers.skill.targets.Party());
        immediate95();guards95();mandatory95();targets95();
        System.out.println("PASS: "+checks+" rule95 stage3 immediate/contact/guard/mandatory/own-summon checks; no live server");System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
