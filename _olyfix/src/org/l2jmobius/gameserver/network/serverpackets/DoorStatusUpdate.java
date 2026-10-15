package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class DoorStatusUpdate extends ServerPacket {
   private final Door _door;

   public DoorStatusUpdate(Door door) {
      this._door = door;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.DOOR_STATUS_UPDATE.writeId(this, buffer);
      buffer.writeInt(this._door.getObjectId());
      buffer.writeInt(!this._door.isOpen());
      buffer.writeInt(this._door.getDamage());
      buffer.writeInt(this._door.isEnemy());
      buffer.writeInt(this._door.getId());
      buffer.writeInt((int)this._door.getCurrentHp());
      buffer.writeInt(this._door.getMaxHp());
   }
}
