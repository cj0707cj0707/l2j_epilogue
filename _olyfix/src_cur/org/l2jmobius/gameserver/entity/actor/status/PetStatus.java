package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PetStatus extends SummonStatus {
   private int _currentFed = 0;

   public PetStatus(Pet activeChar) {
      super(activeChar);
   }

   @Override
   public void reduceHp(double value, Creature attacker) {
      this.reduceHp(value, attacker, true, false, false);
   }

   @Override
   public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHpConsumption) {
      Pet pet = this.getActiveChar();
      if (!pet.isDead()) {
         super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
         if (attacker != null) {
            if (!isDOT && pet.getOwner() != null) {
               SystemMessage sm = new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_C1);
               sm.addString(attacker.getName());
               sm.addInt((int)value);
               pet.sendPacket(sm);
            }

            pet.getAI().notifyActionAttacked(attacker);
         }
      }
   }

   public int getCurrentFed() {
      return this._currentFed;
   }

   public void setCurrentFed(int value) {
      this._currentFed = value;
   }

   public Pet getActiveChar() {
      return super.getActiveChar().asPet();
   }
}
