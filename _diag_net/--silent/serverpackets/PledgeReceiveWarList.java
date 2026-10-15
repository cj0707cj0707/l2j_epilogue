package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.Clan;

public class PledgeReceiveWarList extends ServerPacket {
   private final Clan _clan;
   private final int _tab;

   public PledgeReceiveWarList(Clan clan, int tab) {
      this._clan = clan;
      this._tab = tab;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PLEDGE_RECEIVE_WAR_LIST.writeId(this, buffer);
      buffer.writeInt(this._tab);
      buffer.writeInt(0);
      buffer.writeInt(this._tab == 0 ? this._clan.getWarList().size() : this._clan.getAttackerList().size());

      for (Integer i : this._tab == 0 ? this._clan.getWarList() : this._clan.getAttackerList()) {
         Clan clan = ClanTable.getInstance().getClan(i);
         if (clan != null) {
            buffer.writeString(clan.getName());
            buffer.writeInt(this._tab);
            buffer.writeInt(this._tab);
         }
      }
   }
}
