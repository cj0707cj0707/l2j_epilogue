package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerIsPvpFlagged extends Condition {
   private final boolean _value;

   public ConditionPlayerIsPvpFlagged(boolean value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      return player != null && player.getPvpFlag() > 0 == this._value;
   }
}
