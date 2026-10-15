package org.l2jmobius.gameserver.mechanics.stats.functions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class FuncAdd extends AbstractFunction {
   public FuncAdd(Stat stat, int order, Object owner, double value, Condition applyCond) {
      super(stat, order, owner, value, applyCond);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      return this.getApplyCond() != null && !this.getApplyCond().test(effector, effected, skill) ? initVal : initVal + this.getValue();
   }
}
