package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;

public class RequestGotoLobby extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      GameClient client = this.getClient();
      client.sendPacket(new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1));
   }
}
