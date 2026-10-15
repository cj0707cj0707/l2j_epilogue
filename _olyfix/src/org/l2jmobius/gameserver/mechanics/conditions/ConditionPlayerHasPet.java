package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerHasPet extends Condition {
   private final List<Integer> _controlItemIds;

   public ConditionPlayerHasPet(List<Integer> itemIds) {
      if (itemIds.size() == 1 && itemIds.get(0) == 0) {
         this._controlItemIds = null;
      } else {
         this._controlItemIds = itemIds;
      }
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (this._controlItemIds == null) {
         return true;
      } else {
         Player player = effector.asPlayer();
         if (player == null) {
            return false;
         } else {
            Summon pet = player.getSummon();
            if (!(pet instanceof Pet)) {
               return false;
            } else {
               Item controlItem = pet.asPet().getControlItem();
               return controlItem != null && this._controlItemIds.contains(controlItem.getId());
            }
         }
      }
   }
}
