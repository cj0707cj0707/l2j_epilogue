package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ShowMiniMap extends ServerPacket {
   private final int _mapId;

   public ShowMiniMap(int mapId) {
      this._mapId = mapId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SHOW_MINIMAP.writeId(this, buffer);
      buffer.writeInt(this._mapId);
      buffer.writeByte(SevenSigns.getInstance().getCurrentPeriod());
   }
}
