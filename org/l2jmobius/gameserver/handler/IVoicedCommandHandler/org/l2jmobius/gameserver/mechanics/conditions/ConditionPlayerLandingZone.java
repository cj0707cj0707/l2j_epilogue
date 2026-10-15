package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerLandingZone extends Condition {
   private final boolean _value;

   public ConditionPlayerLandingZone(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effector.isInsideZone(ZoneId.LANDING) == this._value;
   }
}
