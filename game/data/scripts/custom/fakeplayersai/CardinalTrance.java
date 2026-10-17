package custom.fakeplayersai;

import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/** Bot-only template: force the normal non-immune effect roll to exactly65%.
 * activateRate MUST stay below100: the core otherwise bypasses the roll entirely. */
final class CardinalTrance
{
    private static final Map<Integer,Skill> COPIES = new HashMap<>();
    private CardinalTrance() { }
    static synchronized void apply(Player bot)
    {
        if(bot==null||bot.getClient()!=null||bot.getPlayerClass().getId()!=97)return;
        Skill original=bot.getKnownSkill(1394);
        if(original==null||COPIES.containsValue(original))return;
        Skill copy=COPIES.get(original.getLevel());
        if(copy==null)
        {
            StatSet s=new StatSet();
            s.set("skill_id",1394);s.set("level",original.getLevel());s.set("name",original.getName());s.set("icon",original.getIcon());
            s.set("operateType","A2");s.set("targetType","ONE");s.set("abnormalType","SLEEP");s.set("abnormalVisualEffect","SLEEP");
            s.set("abnormalLevel",original.getAbnormalLevel());s.set("abnormalTime",original.getAbnormalTime());
            s.set("activateRate",65);s.set("minChance",65);s.set("maxChance",65);s.set("basicProperty","MEN");s.set("trait","SLEEP");
            s.set("castRange",original.getCastRange());s.set("effectRange",original.getEffectRange());s.set("effectPoint",original.getEffectPoint());
            s.set("hitTime",original.getHitTime());s.set("reuseDelay",original.getReuseDelay());s.set("isDebuff",true);s.set("isMagic",1);
            s.set("lvlBonusRate",original.getLvlBonusRate());s.set("magicLevel",original.getMagicLevel());s.set("removedOnDamage",true);
            s.set("mpConsume",original.getMpConsume());s.set("mpInitialConsume",original.getMpInitialConsume());
            copy=new Skill(s);
            for(EffectScope scope:EffectScope.values())
            {
                var effects=original.getEffects(scope);
                if(effects!=null)for(var effect:effects)copy.addEffect(scope,effect);
            }
            COPIES.put(original.getLevel(),copy);
        }
        bot.addSkill(copy,false);
    }
}
