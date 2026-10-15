package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.network.packet.WritablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;

public abstract class ServerPacket extends WritablePacket<GameClient> {
   private static final int[] PAPERDOLL_ORDER = new int[]{0, 8, 9, 4, 13, 14, 1, 5, 7, 10, 6, 11, 12, 23, 5, 2, 3, 16, 15, 17, 18, 19, 20, 21, 22, 24};

   protected int[] getPaperdollOrder() {
      return PAPERDOLL_ORDER;
   }

   protected boolean write(GameClient client, WriteBuffer buffer) {
      GameClient c = client;
      if (client != null && !client.isDetached() && client.getConnectionState() != ConnectionState.DISCONNECTED) {
         try {
            this.writeImpl(c, buffer);
            return true;
         } catch (Exception var5) {
            PacketLogger.warning("Error writing packet " + this + " to client (" + var5.getMessage() + ") " + client + "]]");
            PacketLogger.warning(TraceUtil.getStackTrace(var5));
            return false;
         }
      } else {
         return true;
      }
   }

   public void runImpl(Player player) {
   }

   protected abstract void writeImpl(GameClient var1, WriteBuffer var2) throws Exception;
}
