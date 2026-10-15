package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionLogicOr extends Condition {
   private static Condition[] _emptyConditions = new Condition[0];
   public Condition[] conditions = _emptyConditions;

   public void add(Condition condition) {
      if (condition != null) {
         if (this.getListener() != null) {
            condition.setListener(this);
         }

         int len = this.conditions.length;
         Condition[] tmp = new Condition[len + 1];
         System.arraycopy(this.conditions, 0, tmp, 0, len);
         tmp[len] = condition;
         this.conditions = tmp;
      }
   }

   @Override
   void setListener(ConditionListener listener) {
      if (listener != null) {
         for (Condition c : this.conditions) {
            c.setListener(this);
         }
      } else {
         for (Condition c : this.conditions) {
            c.setListener(null);
         }
      }

      super.setListener(listener);
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      for (Condition c : this.conditions) {
         if (c.test(effector, effected, skill, item)) {
            return true;
         }
      }

      return false;
   }
}
