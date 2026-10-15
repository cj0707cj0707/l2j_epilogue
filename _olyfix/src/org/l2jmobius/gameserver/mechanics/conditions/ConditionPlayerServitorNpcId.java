package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerServitorNpcId extends Condition {
   private final List<Integer> _npcIds;

   public ConditionPlayerServitorNpcId(List<Integer> npcIds) {
      if (npcIds.size() == 1 && npcIds.get(0) == 0) {
         this._npcIds = null;
      } else {
         this._npcIds = npcIds;
      }
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      return player != null && player.hasSummon() ? this._npcIds == null || this._npcIds.contains(player.getSummon().getId()) : false;
   }
}
