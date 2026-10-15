package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncMoveSpeed extends AbstractFunction {
   private static final FuncMoveSpeed _fms_instance = new FuncMoveSpeed();

   public static AbstractFunction getInstance() {
      return _fms_instance;
   }

   private FuncMoveSpeed() {
      super(Stat.MOVE_SPEED, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      return initVal * BaseStat.DEX.calcBonus(effector);
   }
}
