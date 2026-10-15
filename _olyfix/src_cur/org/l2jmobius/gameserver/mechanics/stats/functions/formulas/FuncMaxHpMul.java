package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncMaxHpMul extends AbstractFunction {
   private static final FuncMaxHpMul _fmhm_instance = new FuncMaxHpMul();

   public static AbstractFunction getInstance() {
      return _fmhm_instance;
   }

   private FuncMaxHpMul() {
      super(Stat.MAX_HP, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      return initVal * BaseStat.CON.calcBonus(effector);
   }
}
