package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;
import java.util.Map.Entry;
import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class PackageToList extends ServerPacket {
   private final Map<Integer, String> _players;

   public PackageToList(Map<Integer, String> chars) {
      this._players = chars;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PACKAGE_TO_LIST.writeId(this, buffer);
      buffer.writeInt(this._players.size());

      for (Entry<Integer, String> entry : this._players.entrySet()) {
         buffer.writeInt(entry.getKey());
         buffer.writeString(entry.getValue());
      }
   }
}
