package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ManagePledgePower extends ServerPacket {
   private final int _action;
   private final Clan _clan;
   private final int _rank;

   public ManagePledgePower(Clan clan, int action, int rank) {
      this._clan = clan;
      this._action = action;
      this._rank = rank;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      if (this._action == 1) {
         ServerPackets.MANAGE_PLEDGE_POWER.writeId(this, buffer);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(this._clan.getRankPrivs(this._rank).getMask());
      }
   }
}
