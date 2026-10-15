package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncPDefMod extends AbstractFunction {
   private static final FuncPDefMod _fmm_instance = new FuncPDefMod();

   public static AbstractFunction getInstance() {
      return _fmm_instance;
   }

   private FuncPDefMod() {
      super(Stat.POWER_DEFENCE, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      double value = initVal;
      if (effector.isPlayer()) {
         Player p = effector.asPlayer();
         if (!p.getInventory().isPaperdollSlotEmpty(6)) {
            value = initVal - (p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 6) : p.getTemplate().getBaseDefBySlot(6));
         }

         if (!p.getInventory().isPaperdollSlotEmpty(11)
            || !p.getInventory().isPaperdollSlotEmpty(6) && p.getInventory().getPaperdollItem(6).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 11) : 11);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(1)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 1) : 1);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(12)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 12) : 12);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(10)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 10) : 10);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(0)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 0) : 0);
         }

         if (!p.getInventory().isPaperdollSlotEmpty(23)) {
            value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, 23) : 23);
         }
      }

      return value * effector.getLevelMod();
   }
}
