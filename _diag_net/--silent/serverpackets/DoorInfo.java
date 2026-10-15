package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.Door;

public class DoorInfo extends ServerPacket {
   private final Door _door;

   public DoorInfo(Door door) {
      this._door = door;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.DOOR_INFO.writeId(this, buffer);
      buffer.writeInt(this._door.getObjectId());
      buffer.writeInt(this._door.getId());
   }
}
