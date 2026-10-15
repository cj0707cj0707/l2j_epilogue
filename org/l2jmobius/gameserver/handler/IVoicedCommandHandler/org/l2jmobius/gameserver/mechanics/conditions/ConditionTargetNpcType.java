package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetNpcType extends Condition {
   private final InstanceType[] _npcType;

   public ConditionTargetNpcType(InstanceType[] type) {
      this._npcType = type;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effected == null ? false : effected.getInstanceType().isTypes(this._npcType);
   }
}
