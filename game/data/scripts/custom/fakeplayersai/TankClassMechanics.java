package custom.fakeplayersai;

import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

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
    private static BuffInfo live(Player p,int id)
    {
        BuffInfo b=p.getEffectList().getBuffInfoBySkillId(id);
        return b!=null&&!b.isRemoved()&&b.isInUse()?b:null;
    }
    static boolean defendedTank(Player enemy)
    {
        if(enemy==null)return false;
        PlayerClass c=enemy.getPlayerClass();
        // Include human second/third-class tanks, including Eva (no bot preset here).
        boolean tank=c.equalsOrChildOf(PlayerClass.PALADIN)||c.equalsOrChildOf(PlayerClass.DARK_AVENGER)
            ||c.equalsOrChildOf(PlayerClass.TEMPLE_KNIGHT)||c.equalsOrChildOf(PlayerClass.SHILLIEN_KNIGHT);
        if(!tank)return false;
        return live(enemy,110)!=null||live(enemy,368)!=null||live(enemy,760)!=null;
    }
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
        return !possession&&charge!=null&&charge.getSkill().getLevel()>=3;
    }
    /** Only legal, ready options from the common engine; no skill/condition bypass. */
    static int choose(List<TacticsPolicy.Option> options)
    {
        for(int id:new int[]{406,785,789,762,784,761,788})
            for(var o:options)if(o.id()==id)return id;
        return -1;
    }
}
