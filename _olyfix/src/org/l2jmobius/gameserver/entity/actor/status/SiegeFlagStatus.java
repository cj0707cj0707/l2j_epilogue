package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;

public class SiegeFlagStatus extends NpcStatus {
   public SiegeFlagStatus(SiegeFlag activeChar) {
      super(activeChar);
   }

   @Override
   public void reduceHp(double value, Creature attacker) {
      this.reduceHp(value, attacker, true, false, false);
   }

   @Override
   public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption) {
      if (this.getActiveChar().isAdvancedHeadquarter()) {
         super.reduceHp(value / 2.0, attacker, awake, isDOT, isHpConsumption);
      }

      super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
   }

   public SiegeFlag getActiveChar() {
      return (SiegeFlag)super.getActiveChar();
   }
}
