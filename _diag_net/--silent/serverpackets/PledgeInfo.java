package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan;

public class PledgeInfo extends ServerPacket {
   private final Clan _clan;

   public PledgeInfo(Clan clan) {
      this._clan = clan;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PLEDGE_INFO.writeId(this, buffer);
      buffer.writeInt(this._clan.getId());
      buffer.writeString(this._clan.getName());
      buffer.writeString(this._clan.getAllyName());
   }
}
