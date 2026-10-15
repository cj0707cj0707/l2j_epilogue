package org.l2jmobius.gameserver.mechanics.stats.functions;

import org.l2jmobius.gameserver.data.xml.EnchantItemHPBonusData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class FuncEnchantHp extends AbstractFunction {
   public FuncEnchantHp(Stat stat, int order, Object owner, double value, Condition applyCond) {
      super(stat, order, owner, value, applyCond);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      if (this.getApplyCond() != null && !this.getApplyCond().test(effector, effected, skill)) {
         return initVal;
      } else {
         Item item = (Item)this.getFuncOwner();
         return item.isEnchanted() ? initVal + EnchantItemHPBonusData.getInstance().getHPBonus(item) : initVal;
      }
   }
}
