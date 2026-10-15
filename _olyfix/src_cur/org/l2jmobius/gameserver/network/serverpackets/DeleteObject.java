package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class DeleteObject extends ServerPacket {
   private final int _objectId;

   public DeleteObject(WorldObject obj) {
      this._objectId = obj.getObjectId();
   }

   public DeleteObject(int objectId) {
      this._objectId = objectId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.DELETE_OBJECT.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(1);
   }
}
