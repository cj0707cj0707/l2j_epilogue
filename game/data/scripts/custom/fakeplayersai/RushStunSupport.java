package custom.fakeplayersai;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

/** Current applied STUN, not a template, sleep, timer guess or caster-specific lease. */
final class RushStunSupport
{
    private RushStunSupport() { }
    static BuffInfo stun(Creature enemy)
    {
        if(enemy==null||!enemy.isStunned())return null;
        for(BuffInfo info:enemy.getEffectList().getEffects())
        {
            if(info==null||info.isRemoved()||!info.isInUse()||info.getEffected()!=enemy||info.getSkill()==null
                ||enemy.getEffectList().getBuffInfoByAbnormalType(info.getSkill().getAbnormalType())!=info)continue;
            for(var effect:info.getEffects())if(!effect.isInstant()&&effect.getEffectType()==EffectType.STUN
                &&(effect.getEffectFlags()&EffectFlag.STUNNED.getMask())!=0)return info;
        }
        return null;
    }
    /** Native Rush requires >=200; choose >=220, fully INSIDE the destination boundary.
     * Do not plan an outside point which the core will clip to a too-short retreat. */
    static List<Location> retreat(int x,int y,int z,int ex,int ey,int cx,int cy,double radius)
    {
        List<Location> points=new ArrayList<>();
        double distance=Math.hypot((double)x-ex,(double)y-ey);
        if(!Double.isFinite(radius)||radius<=0||distance>=220)return points;
        double angle=distance<1?Math.atan2((double)cy-y,(double)cx-x):Math.atan2((double)y-ey,(double)x-ex);
        for(int i=0;i<24;i++)
        {
            double a=angle+(i==0?0:((i+1)/2)*(i%2==1?1:-1)*Math.PI/12);
            int px=ex+(int)Math.round(225*Math.cos(a)),py=ey+(int)Math.round(225*Math.sin(a));
            if(Math.hypot((double)px-ex,(double)py-ey)<220||Math.hypot((double)px-cx,(double)py-cy)>radius
                ||Math.hypot((double)px-x,(double)py-y)>240)continue;
            points.add(new Location(px,py,z));
        }
        return points;
    }
}
