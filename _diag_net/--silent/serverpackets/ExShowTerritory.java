package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.interfaces.ILocational;

public class ExShowTerritory extends ServerPacket {
   private final int _minZ;
   private final int _maxZ;
   private final List<ILocational> _vertices = new ArrayList<>();

   public ExShowTerritory(int minZ, int maxZ) {
      this._minZ = minZ;
      this._maxZ = maxZ;
   }

   public void addVertice(ILocational loc) {
      this._vertices.add(loc);
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_TERRITORY.writeId(this, buffer);
      buffer.writeInt(this._vertices.size());
      buffer.writeInt(this._minZ);
      buffer.writeInt(this._maxZ);

      for (ILocational loc : this._vertices) {
         buffer.writeInt(loc.getX());
         buffer.writeInt(loc.getY());
      }
   }
}
