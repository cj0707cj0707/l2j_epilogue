package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.network.serverpackets.KeyPacket;

public class ProtocolVersion extends ClientPacket {
   private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
   private int _version;

   @Override
   protected void readImpl() {
      try {
         this._version = this.readInt();
      } catch (Exception var2) {
         this._version = 0;
      }
   }

   @Override
   protected void runImpl() {
      org.l2jmobius.gameserver.network.GameClient client = (org.l2jmobius.gameserver.network.GameClient)this.getClient();
      if (this._version == -2) {
         client.disconnect();
      } else if (!ServerConfig.PROTOCOL_LIST.contains(this._version)) {
         LOGGER_ACCOUNTING.warning("Wrong protocol version " + this._version + ", " + client);
         client.setProtocolOk(false);
         client.close(new KeyPacket(client.enableCrypt(), 0));
      } else {
         client.setProtocolVersion(this._version);
         client.setProtocolOk(true);
         client.sendPacket(new KeyPacket(client.enableCrypt(), 1));
      }
   }
}
