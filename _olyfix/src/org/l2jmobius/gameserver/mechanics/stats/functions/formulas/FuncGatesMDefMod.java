package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncGatesMDefMod extends AbstractFunction {
   private static final FuncGatesMDefMod _fmm_instance = new FuncGatesMDefMod();

   public static AbstractFunction getInstance() {
      return _fmm_instance;
   }

   private FuncGatesMDefMod() {
      super(Stat.MAGIC_DEFENCE, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      if (SevenSigns.getInstance().getSealOwner(3) == 2) {
         return initVal * FeatureConfig.ALT_SIEGE_DAWN_GATES_MDEF_MULT;
      } else {
         return SevenSigns.getInstance().getSealOwner(3) == 1 ? initVal * FeatureConfig.ALT_SIEGE_DUSK_GATES_MDEF_MULT : initVal;
      }
   }
}
