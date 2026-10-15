package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;

public class ExMultiPartyCommandChannelInfo extends ServerPacket {
   private final CommandChannel _channel;

   public ExMultiPartyCommandChannelInfo(CommandChannel channel) {
      this._channel = channel;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      if (this._channel != null) {
         org.l2jmobius.gameserver.network.ServerPackets.EX_MULTI_PARTY_COMMAND_CHANNEL_INFO.writeId(this, buffer);
         buffer.writeString(this._channel.getLeader().getName());
         buffer.writeInt(0);
         buffer.writeInt(this._channel.getMemberCount());
         buffer.writeInt(this._channel.getParties().size());

         for (Party p : this._channel.getParties()) {
            buffer.writeString(p.getLeader().getName());
            buffer.writeInt(p.getLeaderObjectId());
            buffer.writeInt(p.getMemberCount());
         }
      }
   }
}
