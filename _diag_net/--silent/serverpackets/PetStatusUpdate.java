package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;

public class PetStatusUpdate extends ServerPacket {
   private final Summon _summon;
   private int _maxFed;
   private int _curFed;

   public PetStatusUpdate(Summon summon) {
      this._summon = summon;
      if (this._summon.isPet()) {
         Pet pet = this._summon.asPet();
         this._curFed = pet.getCurrentFed();
         this._maxFed = pet.getMaxFed();
      } else if (this._summon.isServitor()) {
         Servitor sum = this._summon.asServitor();
         this._curFed = sum.getLifeTimeRemaining();
         this._maxFed = sum.getLifeTime();
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PET_STATUS_UPDATE.writeId(this, buffer);
      buffer.writeInt(this._summon.getSummonType());
      buffer.writeInt(this._summon.getObjectId());
      buffer.writeInt(this._summon.getX());
      buffer.writeInt(this._summon.getY());
      buffer.writeInt(this._summon.getZ());
      buffer.writeString(this._summon.getTitle());
      buffer.writeInt(this._curFed);
      buffer.writeInt(this._maxFed);
      buffer.writeInt((int)this._summon.getCurrentHp());
      buffer.writeInt(this._summon.getMaxHp());
      buffer.writeInt((int)this._summon.getCurrentMp());
      buffer.writeInt(this._summon.getMaxMp());
      buffer.writeInt(this._summon.getLevel());
      buffer.writeLong(this._summon.getStat().getExp());
      buffer.writeLong(this._summon.getExpForThisLevel());
      buffer.writeLong(this._summon.getExpForNextLevel());
   }
}
