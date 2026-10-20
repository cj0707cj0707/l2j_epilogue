package custom.fakeplayersai;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/** Only bot97/1394 and bot115/1097,1092. Independent templates, never SkillData/DB writes.
 * These three native skills are simple A2/ONE/MEN controls without conditions/stat funcs. */
final class BotControlChance
{
    private static final Map<Skill,Map<Integer,Skill>> COPIES = new HashMap<>();
    private BotControlChance() { }
    static boolean applies(int classId,int skillId)
    { return classId==97&&skillId==1394 || classId==115&&(skillId==1097||skillId==1092); }
    static synchronized void apply(Player bot)
    {
        if(bot==null||bot.getClient()!=null)return;
        int cid=bot.getPlayerClass().getId();
        if(cid==97)apply(bot,1394);else if(cid==115){apply(bot,1097);apply(bot,1092);}
    }
    private static void apply(Player bot,int id)
    {
        Skill known=bot.getKnownSkill(id);if(known==null)return;
        Skill original=SkillData.getInstance().getSkill(id,known.getLevel());
        if(original==null)throw new IllegalStateException("Missing native control "+id+"/"+known.getLevel());
        // Native chance fields are integers. Equal 16/17 rolls give exactly 16.5%
        // in expectation (15% * 1.10), without rounding or changing global formulas.
        int chance=id==1394 ? 16+ThreadLocalRandom.current().nextInt(2) : 15;
        Map<Integer,Skill> variants=COPIES.computeIfAbsent(original,k->new HashMap<>());
        Skill copy=variants.get(chance);
        if(copy==null)
        {
            StatSet s=new StatSet();
            s.set("skill_id",id);s.set("level",original.getLevel());s.set("name",original.getName());s.set("icon",original.getIcon());
            s.set("operateType","A2");s.set("targetType","ONE");s.set("basicProperty",original.getBasicProperty().name());
            s.set("abnormalType",original.getAbnormalType().name());s.set("abnormalVisualEffect",id==1092?"TURN_FLEE":"SLEEP");
            s.set("abnormalLevel",original.getAbnormalLevel());s.set("abnormalTime",original.getAbnormalTime());s.set("trait",original.getTraitType().name());
            // Below100 avoids the native guaranteed-success fast path; min=max pins the roll.
            s.set("activateRate",chance);s.set("minChance",chance);s.set("maxChance",chance);
            s.set("castRange",original.getCastRange());s.set("effectRange",original.getEffectRange());s.set("effectPoint",original.getEffectPoint());
            s.set("hitTime",original.getHitTime());s.set("reuseDelay",original.getReuseDelay());s.set("staticReuse",original.isStaticReuse());
            s.set("isDebuff",true);s.set("isMagic",1);s.set("lvlBonusRate",original.getLvlBonusRate());s.set("magicLevel",original.getMagicLevel());
            s.set("removedOnDamage",original.isRemovedOnDamage());
            s.set("mpConsume",original.getMpConsume());s.set("mpInitialConsume",original.getMpInitialConsume());
            copy=new Skill(s);
            for(EffectScope scope:EffectScope.values())
            {
                var effects=original.getEffects(scope);
                if(effects!=null)for(var effect:effects)copy.addEffect(scope,effect);
            }
            variants.put(chance,copy);
        }
        if(known!=copy)bot.addSkill(copy,false);
    }
}
