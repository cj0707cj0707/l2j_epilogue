package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncGatesPDefMod extends AbstractFunction {
   private static final FuncGatesPDefMod _fmm_instance = new FuncGatesPDefMod();

   public static AbstractFunction getInstance() {
      return _fmm_instance;
   }

   private FuncGatesPDefMod() {
      super(Stat.POWER_DEFENCE, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      if (SevenSigns.getInstance().getSealOwner(3) == 2) {
         return initVal * FeatureConfig.ALT_SIEGE_DAWN_GATES_PDEF_MULT;
      } else {
         return SevenSigns.getInstance().getSealOwner(3) == 1 ? initVal * FeatureConfig.ALT_SIEGE_DUSK_GATES_PDEF_MULT : initVal;
      }
   }
}
