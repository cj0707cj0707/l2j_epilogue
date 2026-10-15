package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncMDefMod extends AbstractFunction {
   private static final FuncMDefMod _fmm_instance = new FuncMDefMod();

   public static AbstractFunction getInstance() {
      return _fmm_instance;
   }

   private FuncMDefMod() {
      super(Stat.MAGIC_DEFENCE, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      double value = initVal;
      if (effector.isPlayer()) {
         Player p = effector.asPlayer();
         if (!p.getInventory().isPaperdollSlotEmpty(14)) {
            value = initVal - p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 14) : 14);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(13)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 13) : 13);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(9)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 9) : 9);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(8)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 8) : 8);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(4)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 4) : 4);
         }
      } else if (effector.isPet() && effector.getInventory().getPaperdollObjectId(4) != 0) {
         value = initVal - 13.0;
      }

      return value * BaseStat.MEN.calcBonus(effector) * effector.getLevelMod();
   }
}
