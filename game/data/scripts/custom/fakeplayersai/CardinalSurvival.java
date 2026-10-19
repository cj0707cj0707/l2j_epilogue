package custom.fakeplayersai;

import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

/** The actual live3282 effect, not a fixed60s stopwatch or our original BuffInfo. */
final class CardinalSurvival
{
    private CardinalSurvival() { }
    static boolean active(Player bot,Player enemy)
    {
        if(bot==null||enemy==null||bot.getClient()!=null||bot.getPlayerClass().getId()!=97)return false;
        BuffInfo b=enemy.getEffectList().getBuffInfoBySkillId(3282);
        return b!=null&&!b.isRemoved()&&b.isInUse();
    }
    static boolean allowed(int id) { return id==1394||id==1418||id==1230||id==1218; }
    static int choose(List<TacticsPolicy.Option> options,double hp)
    {
        // Native ready/legal options only. Shield buys time, heal precedes all damage.
        for(int id:new int[]{hp<.65?1418:-1,hp<.95?1218:-1,1394,1230})
            if(id>0&&options.stream().anyMatch(o->o.id()==id))return id;
        return -1;
    }
}
