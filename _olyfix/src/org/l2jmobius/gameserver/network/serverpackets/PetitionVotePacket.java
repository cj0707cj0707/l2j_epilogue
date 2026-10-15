package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PetitionVotePacket extends ServerPacket {
   public static final PetitionVotePacket STATIC_PACKET = new PetitionVotePacket();

   private PetitionVotePacket() {
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PETITION_VOTE.writeId(this, buffer);
   }
}
