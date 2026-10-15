package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.network.packet.ReadablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;

public abstract class ClientPacket extends ReadablePacket<GameClient> {
   @Override
   public boolean read() {
      try {
         this.readImpl();
         return true;
      } catch (Exception var2) {
         PacketLogger.warning("Client: " + this.getClient() + " - Failed reading: " + this.getClass().getSimpleName() + " ; " + var2.getMessage());
         PacketLogger.warning(TraceUtil.getStackTrace(var2));
         return false;
      }
   }

   protected abstract void readImpl();

   @Override
   public void run() {
      try {
         this.runImpl();
      } catch (Exception var2) {
         PacketLogger.warning("Client: " + this.getClient() + " - Failed running: " + this.getClass().getSimpleName() + " ; " + var2.getMessage());
         PacketLogger.warning(TraceUtil.getStackTrace(var2));
         if (this instanceof EnterWorld) {
            this.getClient().closeNow();
         }
      }
   }

   protected abstract void runImpl();

   protected Player getPlayer() {
      return this.getClient().getPlayer();
   }
}
