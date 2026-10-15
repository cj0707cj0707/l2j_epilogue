package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class SurrenderPledgeWar extends ServerPacket {
   private final String _pledgeName;
   private final String _playerName;

   public SurrenderPledgeWar(String pledge, String charName) {
      this._pledgeName = pledge;
      this._playerName = charName;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SURRENDER_PLEDGE_WAR.writeId(this, buffer);
      buffer.writeString(this._pledgeName);
      buffer.writeString(this._playerName);
   }
}
