package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class SurrenderPledgeWar extends ServerPacket {
   private final String _pledgeName;
   private final String _playerName;

   public SurrenderPledgeWar(String pledge, String charName) {
      this._pledgeName = pledge;
      this._playerName = charName;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SURRENDER_PLEDGE_WAR.writeId(this, buffer);
      buffer.writeString(this._pledgeName);
      buffer.writeString(this._playerName);
   }
}
