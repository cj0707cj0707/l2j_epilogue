package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetClassIdRestriction extends Condition {
   private final Set<Integer> _classIds;

   public ConditionTargetClassIdRestriction(Set<Integer> classId) {
      this._classIds = classId;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effected.isPlayer() && this._classIds.contains(effected.asPlayer().getPlayerClass().getId());
   }
}
