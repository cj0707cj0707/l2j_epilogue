package org.l2jmobius.gameserver.network;

import java.util.logging.Logger;
import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.packet.PacketHandler;
import org.l2jmobius.commons.network.packet.ReadablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.network.clientpackets.RequestBookMarkSlotInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestChangeBookMarkSlot;
import org.l2jmobius.gameserver.network.clientpackets.RequestDeleteBookMarkSlot;
import org.l2jmobius.gameserver.network.clientpackets.RequestModifyBookMarkSlot;
import org.l2jmobius.gameserver.network.clientpackets.RequestSaveBookMarkSlot;
import org.l2jmobius.gameserver.network.clientpackets.RequestTeleportBookMark;

public class GamePacketHandler implements PacketHandler<org.l2jmobius.gameserver.network.GameClient> {
   private static final Logger LOGGER = Logger.getLogger(org.l2jmobius.gameserver.network.GamePacketHandler.class.getName());

   public ReadablePacket<org.l2jmobius.gameserver.network.GameClient> handle(ReadBuffer buffer, org.l2jmobius.gameserver.network.GameClient client) {
      int packetId;
      try {
         packetId = Byte.toUnsignedInt(buffer.readByte());
      } catch (Exception var7) {
         LOGGER.warning("PacketHandler: Problem receiving packet id from " + client);
         LOGGER.warning(TraceUtil.getStackTrace(var7));
         client.closeNow();
         return null;
      }

      if (packetId == 208) {
         int exPacketId = Short.toUnsignedInt(buffer.readShort());
         if (exPacketId >= 0 && exPacketId < org.l2jmobius.gameserver.network.ExClientPackets.PACKET_ARRAY.length) {
            org.l2jmobius.gameserver.network.ExClientPackets packetEnum = org.l2jmobius.gameserver.network.ExClientPackets.PACKET_ARRAY[exPacketId];
            if (packetEnum == null) {
               return null;
            } else if (!packetEnum.getConnectionStates().contains(client.getConnectionState())) {
               return null;
            } else if (exPacketId == 81) {
               int subId = buffer.readInt();
               switch (subId) {
                  case 0:
                     return new RequestBookMarkSlotInfo();
                  case 1:
                     return new RequestSaveBookMarkSlot();
                  case 2:
                     return new RequestModifyBookMarkSlot();
                  case 3:
                     return new RequestDeleteBookMarkSlot();
                  case 4:
                     return new RequestTeleportBookMark();
                  case 5:
                     return new RequestChangeBookMarkSlot();
                  default:
                     return null;
               }
            } else {
               return packetEnum.newPacket();
            }
         } else {
            return null;
         }
      } else if (packetId >= 0 && packetId < org.l2jmobius.gameserver.network.ClientPackets.PACKET_ARRAY.length) {
         org.l2jmobius.gameserver.network.ClientPackets packetEnum = org.l2jmobius.gameserver.network.ClientPackets.PACKET_ARRAY[packetId];
         if (packetEnum == null) {
            return null;
         } else {
            return !packetEnum.getConnectionStates().contains(client.getConnectionState()) ? null : packetEnum.newPacket();
         }
      } else {
         return null;
      }
   }
}
