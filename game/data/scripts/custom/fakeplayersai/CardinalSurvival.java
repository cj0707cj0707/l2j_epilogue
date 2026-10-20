package custom.fakeplayersai;

import java.util.List;
import java.lang.ref.WeakReference;
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
    static boolean allowed(int id) { return id==1394||id==1418||id==1398||id==1218; }
    /** Per-pair live-effect history, not accepted casts or a guessed sleep timer. */
    static final class Window
    {
        private WeakReference<BuffInfo> clarity = new WeakReference<>(null);
        private boolean observedTrance, endedTrance;
        void reset() { clarity.clear(); observedTrance = false; endedTrance = false; }
        void observe(Player bot, Player enemy)
        {
            if (!active(bot, enemy)) { reset(); return; }
            BuffInfo live = enemy.getEffectList().getBuffInfoBySkillId(3282);
            if (clarity.get() != live) { reset(); clarity = new WeakReference<>(live); }
            BuffInfo sleep = SleepRetreat.ownSleep(bot, enemy);
            if (sleep != null && sleep.getSkill().getId() == 1394)
            { observedTrance = true; endedTrance = false; }
            else if (observedTrance && !enemy.isSleeping())
            { observedTrance = false; endedTrance = true; }
        }
        boolean manaBurn(Player enemy) { return endedTrance && !enemy.isSleeping(); }
    }
    static int choose(List<TacticsPolicy.Option> options,double hp)
    {
        // Native ready/legal options only. Shield buys time, heal precedes all damage.
        for(int id:new int[]{hp<.65?1418:-1,hp<.95?1218:-1,1394,1398})
            if(id>0&&options.stream().anyMatch(o->o.id()==id))return id;
        return -1;
    }
}
