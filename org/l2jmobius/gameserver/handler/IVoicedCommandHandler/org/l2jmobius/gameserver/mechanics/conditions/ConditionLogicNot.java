package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionLogicNot extends Condition {
   private final Condition _condition;

   public ConditionLogicNot(Condition condition) {
      this._condition = condition;
      if (this.getListener() != null) {
         this._condition.setListener(this);
      }
   }

   @Override
   void setListener(ConditionListener listener) {
      if (listener != null) {
         this._condition.setListener(this);
      } else {
         this._condition.setListener(null);
      }

      super.setListener(listener);
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return !this._condition.test(effector, effected, skill, item);
   }
}
