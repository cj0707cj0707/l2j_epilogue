package custom.fakeplayersai;

import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/** Role-specific selection only. Native cast/effects/resources remain authoritative. */
final class RoleCombatPolicy
{
    private RoleCombatPolicy() { }
    static boolean archer(Player p)
    {
        PlayerClass c=p.getPlayerClass();
        if(c.equalsOrChildOf(PlayerClass.HAWKEYE)||c.equalsOrChildOf(PlayerClass.SILVER_RANGER)
            ||c.equalsOrChildOf(PlayerClass.PHANTOM_RANGER)||c.equalsOrChildOf(PlayerClass.ARBALESTER))return true;
        BotClassPreset s=BotPresets.getPresetByClassId(c.getId());
        return s!=null&&(s.getWeaponKind()==BotClassPreset.WeaponKind.BOW||s.getWeaponKind()==BotClassPreset.WeaponKind.CROSSBOW);
    }
    static boolean rangedDominator(Player p,Player e)
    {
        if(p==null||e==null||p.getClient()!=null||p.getPlayerClass().getId()!=115)return false;
        BotClassPreset s=BotPresets.getPresetByClassId(e.getPlayerClass().getId());
        return archer(e)||e.isMageClass()||s!=null&&s.isCaster();
    }
    static BuffInfo ownSeal(Player p,Player e)
    {
        if(!rangedDominator(p,e))return null;
        for(BuffInfo info:e.getEffectList().getEffects())
            if(info!=null&&!info.isRemoved()&&info.isInUse()&&info.getEffector()==p&&info.getEffected()==e
                &&info.getSkill()!=null&&info.getSkill().getId()==1246
                &&e.getEffectList().getBuffInfoByAbnormalType(info.getSkill().getAbnormalType())==info)return info;
        return null;
    }
    static boolean candidate(Player p,Player e,Skill skill,boolean self)
    {
        if(!rangedDominator(p,e))return true;
        int id=skill.getId();
        if(archer(e))return !self&&id==1245&&!e.isSleeping();
        // Once this Dominator's Seal has landed, spend its whole live window on Steal.
        if(ownSeal(p,e)!=null)return !self&&id==1245;
        if(e.isSleeping())return SleepRetreat.ownSleep(p,e)!=null&&(id==1246||self&&TacticsPolicy.dominatorCpSkill(id)&&p.getCurrentCp()<p.getMaxCp());
        return !self&&(id==1245||id==1097);
    }
    static boolean rage(int id) { return id==345||id==346; }
    static boolean builder(int id) { return rage(id)||id==8||id==50; }
    static boolean needsCharge(Player p) { return p.getCharges()<7; }
    static boolean kited(Player p,Player e)
    {
        if(!e.isMoving()||e.isMovementDisabled()||e.isOutOfControl())return false;
        double dx=(double)e.getX()-p.getX(),dy=(double)e.getY()-p.getY();
        double reach=p.getPhysicalAttackRange()+p.getTemplate().getCollisionRadius()+e.getTemplate().getCollisionRadius();
        return Math.hypot(dx,dy)>reach+20
            &&dx*((double)e.getXdestination()-e.getX())+dy*((double)e.getYdestination()-e.getY())>20*Math.max(1,Math.hypot(dx,dy));
    }
    static void buildersLast(List<TacticsPolicy.Option> legal)
    {
        if(legal.stream().anyMatch(o->!builder(o.id())))legal.removeIf(o->builder(o.id()));
        else if(legal.stream().anyMatch(o->rage(o.id())))legal.removeIf(o->!rage(o.id()));
    }
    static final class DominatorCycle
    {
        private int drains;
        private boolean asleep;
        void reset() { drains=0;asleep=false; }
        void observe(boolean sleeping) { if(asleep&&!sleeping)drains=0;asleep=sleeping; }
        boolean sleepDue() { return drains>=4; }
        void completed(int id,boolean completed)
        { if(completed){if(id==1245)drains=Math.min(4,drains+1);else if(id==1097)drains=0;} }
        int choose(List<TacticsPolicy.Option> legal,boolean bow,boolean sleeping,boolean sealed)
        {
            int[] order=sealed ? new int[]{1245} : sleeping ? new int[]{1305,1306,1246} : !bow&&sleepDue()?new int[]{1097,1245}:new int[]{1245};
            for(int id:order)for(var o:legal)if(o.id()==id)return id;
            return -1;
        }
    }
}
