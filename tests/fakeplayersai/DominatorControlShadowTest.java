package custom.fakeplayersai;

import java.util.*;
import org.l2jmobius.gameserver.entity.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.mechanics.effects.*;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.util.StatSet;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Native effect identity, act/poll, MP/reuse/conditions; recording cast/movement only. */
public class DominatorControlShadowTest extends PostSleepSummonTest
{
    static BuffInfo applied(Player p,Player e,int id)throws Exception {
        clearSleep(e);Skill spell=nativeSkill(id);learn(p,spell);StatSet set=new StatSet();set.set("name",id==260?"Stun":"Root");
        var eff=id==260?new handlers.skill.effects.Stun(null,null,set,new StatSet()):new handlers.skill.effects.Root(null,null,set,new StatSet());
        BuffInfo b=CpMeleeRushRecoveryTest.info(p,e,spell,eff);e.getEffectList().getDebuffs().add(b);
        @SuppressWarnings("unchecked") Map<AbnormalType,BuffInfo> slots=(Map<AbnormalType,BuffInfo>)f(EffectList.class,"_stackedEffects").get(e.getEffectList());slots.put(spell.getAbnormalType(),b);
        flags(e,id==260?EffectFlag.STUNNED.getMask():EffectFlag.ROOTED.getMask());return b;
    }
    static void controlled()throws Exception {
        for(boolean ranked:new boolean[]{false,true})for(int cc:new int[]{1208,260})for(int foe:new int[]{88,93,101,108,113,114,131}) {
            Player p=owner(115,ranked),e=actor(foe,ranked,true);at(p,350,0);at(e,370,0);f(Creature.class,"_zones").set(e,new byte[64]);p.setTarget(e);
            for(int id:new int[]{1245,1367,1097,1306,260})learn(p,nativeSkill(id));
            MoveAI ai=new MoveAI(p);f(Creature.class,"_ai").set(p,ai);BuffInfo applied=applied(p,e,cc);Object s=centered();f(state,"nextDecision").setLong(s,Long.MAX_VALUE);
            verify(DominatorControlRetreat.control(p,e)==applied,"own actual root/Hammer stun, not request");
            verify(run(p,e,s,10000)&&ai.requests==1&&last==0,"retreat before ordinary decision/curses/damage");
            verify(Math.abs(ai.requested.getX()-171)<=1&&ai.requested.getY()==0,"200 from enemy towards center, not away from center");
            for(int i=1;i<=8;i++)verify(run(p,e,s,10000+100*i)&&ai.requests==1&&last==0,"whole run owned");
            at(p,ai.requested.getX(),ai.requested.getY());f(Creature.class,"_move").set(p,null);f(state,"nextDecision").setLong(s,0);
            verify(run(p,e,s,12000)&&last==1245,"restricted mode only Steal Essence; no Hammer/sleep/curse");finish(p,e,s,17000);
            long clock=20000;
            for(double cp:new double[]{.84,.85,.86,1}) {
                f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(p.getStatus(),cp*10000);
                run(p,e,s,clock);verify(last==(cp<=.85?1306:1245),"CP recovery through85%, stop above85%; native self/enemy targeting cp="+cp+" last="+last);finish(p,e,s,clock+6000);clock+=10000;
                if(cp<=.85){run(p,e,s,clock);verify(last==1245,"CP woven with Steal Essence, no endless heal loop");finish(p,e,s,clock+6000);clock+=10000;}
            }
            f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(p.getStatus(),10000);
            mp(p,0);run(p,e,s,200000);verify(last==0&&ai.requests==1,"noMP: hold, never chase Hammer or free cast");mp(p,10000);
            @SuppressWarnings("unchecked") Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);
            disabled.put(p.getKnownSkill(1245).getReuseHashCode(),System.currentTimeMillis()+60000);run(p,e,s,205000);verify(last==0&&ai.requests==1,"drain reuse: no other offense/chase");disabled.clear();
            applied.setInUse(false);verify(DominatorControlRetreat.control(p,e)==null,"inactive effect ends restriction");applied.setInUse(true);
            f(BuffInfo.class,"_effector").set(applied,actor(115,ranked,false));verify(DominatorControlRetreat.control(p,e)==null,"foreign control not owned");
            clearSleep(e);verify(DominatorControlRetreat.control(p,e)==null,"expiry/disspel ends mode immediately");
        }
        // Honor retains its actual PARTY_CLAN target-handler safety in the restricted mode.
        Player honor=owner(115,false),victim=actor(93,false,true);at(honor,0,0);at(victim,199,0);f(Creature.class,"_zones").set(victim,new byte[64]);
        learn(honor,nativeSkill(1305));learn(honor,nativeSkill(1245));applied(honor,victim,1208);
        f(org.l2jmobius.gameserver.entity.actor.status.PlayerStatus.class,"_currentCp").setDouble(honor.getStatus(),8000);
        targetEnemy=honor;last=0;Object hs=centered();act.invoke(null,honor,victim,BotPresets.getPresetByClassId(115),hs,10000L,false);
        verify(last==1305&&honor.getTarget()==honor,"native Honor PARTY_CLAN safely targets self at80% under own root");finish(honor,victim,hs,16000);
        Player ranged=actor(94,false,true),d=owner(115,false);at(d,0,0);at(ranged,30,0);f(Creature.class,"_zones").set(ranged,new byte[64]);applied(d,ranged,1208);learn(d,nativeSkill(1245));learn(d,nativeSkill(1097));
        run(d,ranged,centered(),10000);verify(last==1245,"rule88 ranged target starts with Steal, not melee-only restriction");d.setCastingNow(false);
        BuffInfo otherStun=applied(d,ranged,260);f(BuffInfo.class,"_skill").set(otherStun,nativeSkill(358));
        verify(DominatorControlRetreat.control(d,ranged)==null,"other stun skill cannot impersonate Hammer Crush");
        for(int angle=0;angle<360;angle+=5)for(int radius:new int[]{0,40,199,449,450,600}) {
            int ex=(int)Math.round(radius*Math.cos(Math.toRadians(angle))),ey=(int)Math.round(radius*Math.sin(Math.toRadians(angle)));
            Location p=DominatorControlRetreat.goal(ex+20,ey,7,ex,ey,0,0);
            verify(Math.hypot(p.getX(),p.getY())<450&&p.getZ()==7,"centerward destination stays within450");
            if(radius<=600)verify(Math.abs(Math.hypot(p.getX()-ex,p.getY()-ey)-199)<1,"200 separation with rounding safety");
        }
        for(double cp:new double[]{0,.49,.5,.84,.85,.850001,1,Double.NaN,-1}) {
            verify(TacticsPolicy.dominatorControlCpNeeded(cp)==(Double.isFinite(cp)&&cp>=0&&cp<=.85),"scoped85% CP threshold");
            verify(TacticsPolicy.dominatorCpNeeded(cp)==(Double.isFinite(cp)&&cp>=0&&cp<.5),"normal50% threshold unchanged");
        }
    }
    @SuppressWarnings("unchecked") static void shadows()throws Exception {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{93,101,108})for(int foe:new int[]{94,95,97,103,110,115}) {
            Player p=owner(cid,ranked),e=actor(foe,ranked,true);at(p,0,0);at(e,500,0);f(Creature.class,"_zones").set(e,new byte[64]);p.setTarget(e);learn(p,nativeSkill(821));learn(p,nativeSkill(358));learn(p,nativeSkill(30));Object s=centered();
            verify(run(p,e,s,10000)&&last==821&&p.getTarget()==e,"all3 daggers open native Shadow Step vs caster in both arenas cid="+cid+" foe="+foe+" ranked="+ranked+" last="+last);finish(p,e,s,15000);
            for(int block=0;block<4;block++) {
                s=centered();if(block==0)mp(p,0);if(block==1)flags(p,EffectFlag.ROOTED.getMask());
                Map<Integer,Long> disabled=(Map<Integer,Long>)f(Creature.class,"_disabledSkills").get(p);if(block==2)disabled.put(p.getKnownSkill(821).getReuseHashCode(),System.currentTimeMillis()+60000);if(block==3)at(e,601,0);
                run(p,e,s,20000);verify(last!=821,"no free MP/root/reuse/out-of-range teleport");p.setCastingNow(false);mp(p,10000);flags(p,0);disabled.clear();at(e,500,0);
            }
            at(e,30,0);s=centered();run(p,e,s,30000);verify(last==821,"Shadow Step before ready melee Bluff/blows and rear step");finish(p,e,s,35000);
            traces.get(p).accept=false;s=centered();run(p,e,s,40000);verify(last==821&&f(state,"pendingId").getInt(s)==0,"rejected native request not fake success");run(p,e,s,40100);verify(last!=821,"failure backoff suppresses per-tick teleport retry");traces.get(p).accept=true;
            Player melee=actor(92,ranked,true);f(Creature.class,"_zones").set(melee,new byte[64]);at(melee,500,0);s=centered();run(p,melee,s,50000);verify(last!=821,"no broad teleport permission vs non-caster archer");
        }
    }
    public static void main(String[]args)throws Exception {
        PostSleepSummonTest.main(args);org.l2jmobius.gameserver.handler.TargetHandler.getInstance().registerHandler(new handlers.skill.targets.PartyClan());int prior=checks;BotPresets.loadOverrides();controlled();shadows();
        System.out.println("PASS: "+(checks-prior)+" native Dominator melee-control retreat/85%CP/restricted rotation and dagger caster Shadow Step checks; no live server");
    }
}
