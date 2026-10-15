package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerPledgeClass extends Condition {
   private final int _pledgeClass;

   public ConditionPlayerPledgeClass(int pledgeClass) {
      this._pledgeClass = pledgeClass;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      if (player != null && player.getClan() != null) {
         boolean isClanLeader = player.isClanLeader();
         return this._pledgeClass == -1 && !isClanLeader ? false : isClanLeader || player.getPledgeClass() >= this._pledgeClass;
      } else {
         return false;
      }
   }
}
