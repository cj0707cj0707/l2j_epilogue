package custom.fakeplayersai;

import java.util.List;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/** Native tank mechanics only: no HP manipulation, fake proc/charges, or SkillData edits. */
final class TankClassMechanics
{
    private TankClassMechanics() { }
    static boolean skill(int id)
    { return id == 406 || id == 784 || id == 785 || id == 761 || id == 762 || id == 788 || id == 789; }
    static int[] skills(int classId)
    {
        return switch (classId)
        {
            case 90 -> new int[]{406,784,785};
            case 91 -> new int[]{761,762};
            case 106 -> new int[]{788,789};
            default -> new int[0];
        };
    }
    static boolean support(int id) { return skill(id) && id != 762; }
    static boolean finisher(int id) { return id==785||id==762||id==789; }
    /** Exact contact envelope, additionally bounded by the native AURA radius. */
    static boolean crusherContact(Player bot,Creature enemy,Skill skill)
    {
        if(bot==null||enemy==null||skill==null)return false;
        double reach=bot.getPhysicalAttackRange()+bot.getTemplate().getCollisionRadius()+enemy.getTemplate().getCollisionRadius();
        return Math.abs(bot.getZ()-enemy.getZ())<150
            &&bot.calculateDistance2D(enemy)<=Math.min(reach,skill.getAffectRange())
            &&GeoEngine.getInstance().canSeeTarget(bot,enemy);
    }
    /** PARTY icons may buff our own summon, never an arena opponent/foreign party. */
    static boolean iconTargets(Player bot,Skill skill)
    {
        return iconTargets(bot,skill.getTargetList(bot,false,bot));
    }
    static boolean iconTargets(Player bot,List<?> targets)
    {
        if(targets==null||!targets.contains(bot))return false;
        for(var target:targets)if(target==null||target!=bot&&target!=bot.getSummon())return false;
        return true;
    }
    /** Shared tactical + synchronous legacy/auto-use guard. Native costs/reuse still apply. */
    static boolean castAllowed(Player bot,Creature enemy,Skill skill)
    {
        if(!finisher(skill.getId()))return true;
        return allowed(bot,skill.getId())&&(skill.getId()==762
            ?enemy!=null&&!enemy.isInvul()&&!enemy.isSleeping()&&crusherContact(bot,enemy,skill)
            :iconTargets(bot,skill));
    }
    /** A real stage3 whose learned finisher is off native reuse; no charge simulation. */
    static int readyFinisher(Player bot,Player enemy)
    {
        if(bot==null||bot.getClient()!=null)return 0;
        int id=switch(bot.getPlayerClass().getId()){case 90->785;case 91->762;case 106->789;default->0;};
        if(id==0)return 0;
        var skill=bot.getKnownSkill(id);
        return skill!=null&&!bot.isSkillDisabled(skill)&&allowed(bot,id)&&skill.checkCondition(bot,id==762?enemy:bot,false)?id:0;
    }
    private static BuffInfo live(Player p,int id)
    {
        BuffInfo b=p.getEffectList().getBuffInfoBySkillId(id);
        return b!=null&&!b.isRemoved()&&b.isInUse()?b:null;
    }
    static boolean tank(Player enemy)
    {
        if(enemy==null)return false;
        PlayerClass c=enemy.getPlayerClass();
        // Include human second/third-class tanks, including Eva (no bot preset here).
        return c.equalsOrChildOf(PlayerClass.PALADIN)||c.equalsOrChildOf(PlayerClass.DARK_AVENGER)
            ||c.equalsOrChildOf(PlayerClass.TEMPLE_KNIGHT)||c.equalsOrChildOf(PlayerClass.SHILLIEN_KNIGHT);
    }
    static boolean defendedTank(Player enemy)
    {
        if(!tank(enemy))return false;
        return live(enemy,110)!=null||live(enemy,368)!=null||live(enemy,760)!=null;
    }
    static boolean sacrificeBlocked(Player bot)
    { return bot!=null&&bot.getClient()==null&&bot.getPlayerClass().getId()==90&&(live(bot,406)!=null||live(bot,785)!=null); }
    static boolean allowed(Player bot,int id)
    {
        if(!skill(id))return true;
        if(bot==null||bot.getClient()!=null||bot.getKnownSkill(id)==null)return false;
        int seed,trigger,finish;
        switch(bot.getPlayerClass().getId())
        {
            case 90 -> { seed=784;trigger=5562;finish=785; }
            case 91 -> { seed=761;trigger=5561;finish=762; }
            case 106 -> { seed=788;trigger=5564;finish=789; }
            default -> { return false; }
        }
        boolean possession=live(bot,406)!=null||live(bot,785)!=null||live(bot,789)!=null;
        if(id==406)return bot.getPlayerClass().getId()==90&&!possession&&bot.getCurrentHp()<=bot.getMaxHp()*0.30;
        if(id!=seed&&id!=finish)return false;
        if(id==seed)
        {
            var finisher=bot.getKnownSkill(finish);
            return !possession&&live(bot,seed)==null&&live(bot,trigger)==null&&finisher!=null&&!bot.isSkillDisabled(finisher);
        }
        BuffInfo charge=live(bot,trigger);
        // Angelic Icon must not veto a newly ready Flame Icon. Do not refresh a live finisher.
        return live(bot,785)==null&&live(bot,789)==null&&charge!=null&&charge.getSkill().getLevel()==3;
    }
    /** Only legal, ready options from the common engine; no skill/condition bypass. */
    static int choose(List<TacticsPolicy.Option> options)
    {
        for(int id:new int[]{785,406,789,762,784,761,788})
            for(var o:options)if(o.id()==id)return id;
        return -1;
    }
}
