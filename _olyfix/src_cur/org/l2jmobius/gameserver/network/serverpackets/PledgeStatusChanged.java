package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeStatusChanged extends ServerPacket {
   private final Clan _clan;

   public PledgeStatusChanged(Clan clan) {
      this._clan = clan;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PLEDGE_STATUS_CHANGED.writeId(this, buffer);
      buffer.writeInt(this._clan.getLeaderId());
      buffer.writeInt(this._clan.getId());
      buffer.writeInt(this._clan.getCrestId());
      buffer.writeInt(this._clan.getAllyId());
      buffer.writeInt(this._clan.getAllyCrestId());
      buffer.writeInt(0);
      buffer.writeInt(0);
   }
}
