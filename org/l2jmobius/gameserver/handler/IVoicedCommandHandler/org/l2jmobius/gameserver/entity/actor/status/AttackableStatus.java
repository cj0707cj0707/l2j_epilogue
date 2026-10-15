package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;

public class AttackableStatus extends NpcStatus {
   public AttackableStatus(Attackable activeChar) {
      super(activeChar);
   }

   @Override
   public void reduceHp(double value, Creature attacker) {
      this.reduceHp(value, attacker, true, false, false);
   }

   @Override
   public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption) {
      Attackable attackable = this.getActiveChar();
      if (!attackable.isDead()) {
         if (value > 0.0) {
            if (attackable.isOverhit()) {
               attackable.setOverhitValues(attacker, value);
            } else {
               attackable.overhitEnabled(false);
            }
         } else {
            attackable.overhitEnabled(false);
         }

         super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
         if (!attackable.isDead()) {
            attackable.overhitEnabled(false);
         }
      }
   }

   public Attackable getActiveChar() {
      return super.getActiveChar().asAttackable();
   }
}
