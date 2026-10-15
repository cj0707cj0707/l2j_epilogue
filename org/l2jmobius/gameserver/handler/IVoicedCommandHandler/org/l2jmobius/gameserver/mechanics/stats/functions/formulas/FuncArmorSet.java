package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import java.util.EnumMap;
import java.util.Map;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncArmorSet extends AbstractFunction {
   private static final Map<Stat, FuncArmorSet> _fh_instance = new EnumMap<>(Stat.class);

   public static AbstractFunction getInstance(Stat st) {
      _fh_instance.computeIfAbsent(st, k -> new FuncArmorSet(st));
      return _fh_instance.get(st);
   }

   private FuncArmorSet(Stat stat) {
      super(stat, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      double value = initVal;
      if (effector.isPlayer()) {
         Player player = effector.asPlayer();
         Item chest = player.getChestArmorInstance();
         if (chest != null) {
            ArmorSet set = ArmorSetData.getInstance().getSet(chest.getId());
            if (set != null && set.containAll(player)) {
               switch (this.getStat()) {
                  case STAT_STR:
                     value = initVal + set.getSTR();
                     break;
                  case STAT_DEX:
                     value = initVal + set.getDEX();
                     break;
                  case STAT_INT:
                     value = initVal + set.getINT();
                     break;
                  case STAT_MEN:
                     value = initVal + set.getMEN();
                     break;
                  case STAT_CON:
                     value = initVal + set.getCON();
                     break;
                  case STAT_WIT:
                     value = initVal + set.getWIT();
               }
            }
         }
      }

      return value;
   }
}
