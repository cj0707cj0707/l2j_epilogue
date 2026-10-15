package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionTargetAggro extends Condition {
   private final boolean _isAggro;

   public ConditionTargetAggro(boolean isAggro) {
      this._isAggro = isAggro;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      if (effected != null) {
         if (effected.isMonster()) {
            return effected.asMonster().isAggressive() == this._isAggro;
         }

         if (effected.isPlayer()) {
            return effected.asPlayer().getKarma() > 0;
         }
      }

      return false;
   }
}
