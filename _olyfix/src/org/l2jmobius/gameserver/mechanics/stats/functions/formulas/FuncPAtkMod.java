package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncPAtkMod extends AbstractFunction {
   private static final FuncPAtkMod _fpa_instance = new FuncPAtkMod();

   public static AbstractFunction getInstance() {
      return _fpa_instance;
   }

   private FuncPAtkMod() {
      super(Stat.POWER_ATTACK, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      return initVal * BaseStat.STR.calcBonus(effector) * effector.getLevelMod();
   }
}
