package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class ConditionTargetMyPartyExceptMe extends Condition {
   private final boolean _value;

   public ConditionTargetMyPartyExceptMe(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      boolean isPartyMember = true;
      Player player = effector.asPlayer();
      if (player == null || effected == null || !effected.isPlayer()) {
         isPartyMember = false;
      } else if (player == effected) {
         player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
         isPartyMember = false;
      } else if (!player.isInParty() || !player.getParty().equals(effected.getParty())) {
         SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
         sm.addSkillName(skill);
         player.sendPacket(sm);
         isPartyMember = false;
      }

      return this._value == isPartyMember;
   }
}
