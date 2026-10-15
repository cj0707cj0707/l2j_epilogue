package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncMaxCpMul extends AbstractFunction {
   private static final FuncMaxCpMul _fmcm_instance = new FuncMaxCpMul();

   public static AbstractFunction getInstance() {
      return _fmcm_instance;
   }

   private FuncMaxCpMul() {
      super(Stat.MAX_CP, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      return initVal * BaseStat.CON.calcBonus(effector);
   }
}
