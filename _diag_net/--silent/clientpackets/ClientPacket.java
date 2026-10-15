package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.network.packet.ReadablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.entity.actor.Player;

public abstract class ClientPacket extends ReadablePacket<org.l2jmobius.gameserver.network.GameClient> {
   public boolean read() {
      try {
         this.readImpl();
         return true;
      } catch (Exception var2) {
         org.l2jmobius.gameserver.network.PacketLogger.warning(
            "Client: " + this.getClient() + " - Failed reading: " + this.getClass().getSimpleName() + " ; " + var2.getMessage()
         );
         org.l2jmobius.gameserver.network.PacketLogger.warning(TraceUtil.getStackTrace(var2));
         return false;
      }
   }

   protected abstract void readImpl();

   public void run() {
      try {
         this.runImpl();
      } catch (Exception var2) {
         org.l2jmobius.gameserver.network.PacketLogger.warning(
            "Client: " + this.getClient() + " - Failed running: " + this.getClass().getSimpleName() + " ; " + var2.getMessage()
         );
         org.l2jmobius.gameserver.network.PacketLogger.warning(TraceUtil.getStackTrace(var2));
         if (this instanceof EnterWorld) {
            ((org.l2jmobius.gameserver.network.GameClient)this.getClient()).closeNow();
         }
      }
   }

   protected abstract void runImpl();

   protected Player getPlayer() {
      return ((org.l2jmobius.gameserver.network.GameClient)this.getClient()).getPlayer();
   }
}
