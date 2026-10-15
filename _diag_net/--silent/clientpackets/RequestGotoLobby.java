package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;

public class RequestGotoLobby extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      org.l2jmobius.gameserver.network.GameClient client = (org.l2jmobius.gameserver.network.GameClient)this.getClient();
      client.sendPacket(new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1));
   }
}
