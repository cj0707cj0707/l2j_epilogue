package org.l2jmobius.gameserver.mechanics.stats.functions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class FuncDiv extends AbstractFunction {
   public FuncDiv(Stat stat, int order, Object owner, double value, Condition applyCond) {
      super(stat, order, owner, value, applyCond);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      if (this.getApplyCond() == null || this.getApplyCond().test(effector, effected, skill)) {
         try {
            return initVal / this.getValue();
         } catch (Exception var7) {
            LOG.warning(FuncDiv.class.getSimpleName() + ": Division by zero: " + this.getValue() + "!");
         }
      }

      return initVal;
   }
}
