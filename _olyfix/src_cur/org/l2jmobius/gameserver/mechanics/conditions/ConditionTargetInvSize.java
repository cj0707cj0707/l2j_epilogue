package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetInvSize extends Condition {
   private final int _size;

   public ConditionTargetInvSize(int size) {
      this._size = size;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effected != null && effected.isPlayer()) {
         Player target = effected.asPlayer();
         return target.getInventory().getNonQuestSize() <= target.getInventoryLimit() - this._size;
      } else {
         return false;
      }
   }
}
