package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class PetitionVotePacket extends ServerPacket {
   public static final PetitionVotePacket STATIC_PACKET = new PetitionVotePacket();

   private PetitionVotePacket() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PETITION_VOTE.writeId(this, buffer);
   }
}
