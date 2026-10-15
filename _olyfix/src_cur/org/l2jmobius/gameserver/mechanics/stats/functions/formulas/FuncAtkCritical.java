package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncAtkCritical extends AbstractFunction {
   private static final FuncAtkCritical _fac_instance = new FuncAtkCritical();

   public static AbstractFunction getInstance() {
      return _fac_instance;
   }

   private FuncAtkCritical() {
      super(Stat.CRITICAL_RATE, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      return initVal * BaseStat.DEX.calcBonus(effector) * 10.0;
   }
}
