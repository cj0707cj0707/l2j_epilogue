package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowTrace extends ServerPacket {
   private final List<Location> _locations = new ArrayList<>();

   public void addLocation(int x, int y, int z) {
      this._locations.add(new Location(x, y, z));
   }

   public void addLocation(ILocational loc) {
      this.addLocation(loc.getX(), loc.getY(), loc.getZ());
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_TRACE.writeId(this, buffer);
      buffer.writeShort(0);
      buffer.writeInt(0);
      buffer.writeShort(this._locations.size());

      for (Location loc : this._locations) {
         buffer.writeInt(loc.getX());
         buffer.writeInt(loc.getY());
         buffer.writeInt(loc.getZ());
      }
   }
}
