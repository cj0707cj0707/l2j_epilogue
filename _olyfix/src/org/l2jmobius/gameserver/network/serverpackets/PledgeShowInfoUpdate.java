package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeShowInfoUpdate extends ServerPacket {
   private final Clan _clan;

   public PledgeShowInfoUpdate(Clan clan) {
      this._clan = clan;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PLEDGE_SHOW_INFO_UPDATE.writeId(this, buffer);
      buffer.writeInt(this._clan.getId());
      buffer.writeInt(this._clan.getCrestId());
      buffer.writeInt(this._clan.getLevel());
      buffer.writeInt(this._clan.getCastleId());
      buffer.writeInt(this._clan.getHideoutId());
      buffer.writeInt(this._clan.getFortId());
      buffer.writeInt(this._clan.getRank());
      buffer.writeInt(this._clan.getReputationScore());
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(this._clan.getAllyId());
      buffer.writeString(this._clan.getAllyName());
      buffer.writeInt(this._clan.getAllyCrestId());
      buffer.writeInt(this._clan.isAtWar());
   }
}
