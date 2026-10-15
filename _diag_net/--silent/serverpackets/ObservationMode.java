package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;

public class ObservationMode extends ServerPacket {
   private final Location _loc;

   public ObservationMode(Location loc) {
      this._loc = loc;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.OBSERVER_START.writeId(this, buffer);
      buffer.writeInt(this._loc.getX());
      buffer.writeInt(this._loc.getY());
      buffer.writeInt(this._loc.getZ());
      buffer.writeByte(0);
      buffer.writeByte(192);
      buffer.writeByte(0);
   }
}
