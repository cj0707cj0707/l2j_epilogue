package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncAtkEvasion extends AbstractFunction {
   private static final FuncAtkEvasion _fae_instance = new FuncAtkEvasion();

   public static AbstractFunction getInstance() {
      return _fae_instance;
   }

   private FuncAtkEvasion() {
      super(Stat.EVASION_RATE, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      int level = effector.getLevel();
      double value;
      if (effector.isPlayer()) {
         value = initVal + (Math.sqrt(effector.getDEX()) * 6.0 + level);
         double diff = level - 69;
         if (level >= 78) {
            diff *= 1.2;
         }

         if (level >= 70) {
            value += diff;
         }
      } else {
         value = initVal + (Math.sqrt(effector.getDEX()) * 6.0 + level);
         if (level > 69) {
            value += level - 69 + 2;
         }
      }

      return value;
   }
}
