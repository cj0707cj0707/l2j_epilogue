package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class PledgeShowMemberListDeleteAll extends ServerPacket {
   public static final PledgeShowMemberListDeleteAll STATIC_PACKET = new PledgeShowMemberListDeleteAll();

   private PledgeShowMemberListDeleteAll() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PLEDGE_SHOW_MEMBER_LIST_DELETE_ALL.writeId(this, buffer);
   }
}
