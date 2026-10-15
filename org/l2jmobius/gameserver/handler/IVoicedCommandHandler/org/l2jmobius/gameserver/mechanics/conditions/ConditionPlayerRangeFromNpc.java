package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerRangeFromNpc extends Condition {
   private final Set<Integer> _npcIds;
   private final int _radius;
   private final boolean _value;

   public ConditionPlayerRangeFromNpc(Set<Integer> npcIds, int radius, boolean value) {
      this._npcIds = npcIds;
      this._radius = radius;
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      boolean existNpc = false;
      if (!this._npcIds.isEmpty() && this._radius > 0) {
         existNpc = World.getFirstVisibleObjectInRange(effector, Npc.class, this._radius, target -> this._npcIds.contains(target.getId())) != null;
      }

      return existNpc == this._value;
   }
}
