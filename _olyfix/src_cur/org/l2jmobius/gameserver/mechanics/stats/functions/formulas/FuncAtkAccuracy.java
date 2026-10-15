package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncAtkAccuracy extends AbstractFunction {
   private static final FuncAtkAccuracy _faa_instance = new FuncAtkAccuracy();

   public static AbstractFunction getInstance() {
      return _faa_instance;
   }

   private FuncAtkAccuracy() {
      super(Stat.ACCURACY_COMBAT, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      int level = effector.getLevel();
      double value = initVal + Math.sqrt(effector.getDEX()) * 6.0 + level;
      if (level > 77) {
         value += level - 76;
      }

      if (level > 69) {
         value += level - 69;
      }

      return value;
   }
}
