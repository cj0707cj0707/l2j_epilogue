package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncMaxMpMul extends AbstractFunction {
   private static final FuncMaxMpMul _fmmm_instance = new FuncMaxMpMul();

   public static AbstractFunction getInstance() {
      return _fmmm_instance;
   }

   private FuncMaxMpMul() {
      super(Stat.MAX_MP, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      return initVal * BaseStat.MEN.calcBonus(effector);
   }
}
