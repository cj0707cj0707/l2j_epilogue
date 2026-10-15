package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ObservationReturn extends ServerPacket {
   private final Location _loc;

   public ObservationReturn(Location loc) {
      this._loc = loc;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.OBSERVER_END.writeId(this, buffer);
      buffer.writeInt(this._loc.getX());
      buffer.writeInt(this._loc.getY());
      buffer.writeInt(this._loc.getZ());
   }
}
