package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerInvSize extends Condition {
   private final int _size;

   public ConditionPlayerInvSize(int size) {
      this._size = size;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      return player != null ? player.getInventory().getNonQuestSize() <= player.getInventoryLimit() - this._size : true;
   }
}
