package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class PledgeShowMemberListDelete extends ServerPacket {
   private final String _player;

   public PledgeShowMemberListDelete(String playerName) {
      this._player = playerName;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PLEDGE_SHOW_MEMBER_LIST_DELETE.writeId(this, buffer);
      buffer.writeString(this._player);
   }
}
