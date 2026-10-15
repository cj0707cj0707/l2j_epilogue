package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class JoinPledge extends ServerPacket {
   private final int _pledgeId;

   public JoinPledge(int pledgeId) {
      this._pledgeId = pledgeId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.JOIN_PLEDGE.writeId(this, buffer);
      buffer.writeInt(this._pledgeId);
   }
}
