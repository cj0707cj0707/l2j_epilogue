package org.l2jmobius.gameserver.mechanics.stats.functions.formulas;

import java.util.EnumMap;
import java.util.Map;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.AbstractFunction;

public class FuncHenna extends AbstractFunction {
   private static final Map<Stat, FuncHenna> _fh_instance = new EnumMap<>(Stat.class);

   public static AbstractFunction getInstance(Stat st) {
      _fh_instance.computeIfAbsent(st, k -> new FuncHenna(st));
      return _fh_instance.get(st);
   }

   private FuncHenna(Stat stat) {
      super(stat, 1, null, 0.0, null);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      double value = initVal;
      if (effector.isPlayer()) {
         Player pc = effector.asPlayer();
         switch (this.getStat()) {
            case STAT_STR:
               value = initVal + pc.getHennaStatSTR();
               break;
            case STAT_CON:
               value = initVal + pc.getHennaStatCON();
               break;
            case STAT_DEX:
               value = initVal + pc.getHennaStatDEX();
               break;
            case STAT_INT:
               value = initVal + pc.getHennaStatINT();
               break;
            case STAT_WIT:
               value = initVal + pc.getHennaStatWIT();
               break;
            case STAT_MEN:
               value = initVal + pc.getHennaStatMEN();
         }
      }

      return value;
   }
}
