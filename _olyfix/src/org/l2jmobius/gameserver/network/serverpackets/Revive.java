package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class Revive extends ServerPacket {
   private final int _objectId;

   public Revive(WorldObject obj) {
      this._objectId = obj.getObjectId();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.REVIVE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
   }
}
