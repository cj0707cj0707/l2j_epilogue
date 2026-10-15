package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExSetCompassZoneCode extends ServerPacket {
   public static final int ALTEREDZONE = 8;
   public static final int SIEGEWARZONE1 = 10;
   public static final int SIEGEWARZONE2 = 11;
   public static final int PEACEZONE = 12;
   public static final int SEVENSIGNSZONE = 13;
   public static final int PVPZONE = 14;
   public static final int GENERALZONE = 15;
   private final int _zoneType;

   public ExSetCompassZoneCode(int value) {
      this._zoneType = value;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SET_COMPASS_ZONE_CODE.writeId(this, buffer);
      buffer.writeInt(this._zoneType);
   }
}
