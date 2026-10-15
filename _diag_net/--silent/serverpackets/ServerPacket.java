package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.network.packet.WritablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.entity.actor.Player;

public abstract class ServerPacket extends WritablePacket<org.l2jmobius.gameserver.network.GameClient> {
   private static final int[] PAPERDOLL_ORDER = new int[]{0, 8, 9, 4, 13, 14, 1, 5, 7, 10, 6, 11, 12, 23, 5, 2, 3, 16, 15, 17, 18, 19, 20, 21, 22, 24};

   protected int[] getPaperdollOrder() {
      return PAPERDOLL_ORDER;
   }

   protected boolean write(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.GameClient c = client;
      if (client != null && !client.isDetached() && client.getConnectionState() != org.l2jmobius.gameserver.network.ConnectionState.DISCONNECTED) {
         try {
            this.writeImpl(c, buffer);
            return true;
         } catch (Exception var5) {
            org.l2jmobius.gameserver.network.PacketLogger.warning("Error writing packet " + this + " to client (" + var5.getMessage() + ") " + client + "]]");
            org.l2jmobius.gameserver.network.PacketLogger.warning(TraceUtil.getStackTrace(var5));
            return false;
         }
      } else {
         return true;
      }
   }

   public void runImpl(Player player) {
   }

   protected abstract void writeImpl(org.l2jmobius.gameserver.network.GameClient var1, WriteBuffer var2) throws Exception;
}
