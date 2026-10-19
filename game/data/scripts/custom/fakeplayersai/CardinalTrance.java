package custom.fakeplayersai;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/** Restore the ordinary SkillData template, including native enchant/resistance rules.
 * No private rate clamp. The name is retained for old script references. */
final class CardinalTrance
{
    private CardinalTrance() { }
    static void apply(Player bot)
    {
        if (bot == null || bot.getClient() != null || bot.getPlayerClass().getId() != 97) return;
        Skill learned = bot.getKnownSkill(1394);
        if (learned == null) return;
        Skill nativeSkill = SkillData.getInstance().getSkill(1394, learned.getLevel());
        if (nativeSkill != null && learned != nativeSkill) bot.addSkill(nativeSkill, false);
    }
}
