package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetNpcId extends Condition {
   private final Set<Integer> _npcIds;

   public ConditionTargetNpcId(Set<Integer> npcIds) {
      this._npcIds = npcIds;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      WorldObject target = (WorldObject)(effector != null && effector.isPlayer() ? effector.getTarget() : effected);
      return target != null && (target.isNpc() || target.isDoor()) && this._npcIds.contains(target.getId());
   }
}
