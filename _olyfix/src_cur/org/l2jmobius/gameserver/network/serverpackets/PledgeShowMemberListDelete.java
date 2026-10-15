package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeShowMemberListDelete extends ServerPacket {
   private final String _player;

   public PledgeShowMemberListDelete(String playerName) {
      this._player = playerName;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PLEDGE_SHOW_MEMBER_LIST_DELETE.writeId(this, buffer);
      buffer.writeString(this._player);
   }
}
