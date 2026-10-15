package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;

public class ConditionPlayerBaseStats extends Condition {
   private final BaseStat _stat;
   private final int _value;

   public ConditionPlayerBaseStats(Creature creature, BaseStat stat, int value) {
      this._stat = stat;
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      if (player == null) {
         return false;
      } else {
         switch (this._stat) {
            case INT:
               return player.getINT() >= this._value;
            case STR:
               return player.getSTR() >= this._value;
            case CON:
               return player.getCON() >= this._value;
            case DEX:
               return player.getDEX() >= this._value;
            case MEN:
               return player.getMEN() >= this._value;
            case WIT:
               return player.getWIT() >= this._value;
            default:
               return false;
         }
      }
   }
}
