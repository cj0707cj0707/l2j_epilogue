package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;

public class ExShowCastleInfo extends ServerPacket {
   private final Collection<Castle> _castles = CastleManager.getInstance().getCastles();

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_CASTLE_INFO.writeId(this, buffer);
      buffer.writeInt(this._castles.size());

      for (Castle castle : this._castles) {
         buffer.writeInt(castle.getResidenceId());
         if (castle.getOwnerId() > 0) {
            if (ClanTable.getInstance().getClan(castle.getOwnerId()) != null) {
               buffer.writeString(ClanTable.getInstance().getClan(castle.getOwnerId()).getName());
            } else {
               org.l2jmobius.gameserver.network.PacketLogger.warning(
                  "Castle owner with no name! Castle: " + castle.getName() + " has an OwnerId = " + castle.getOwnerId() + " who does not have a  name!"
               );
               buffer.writeString("");
            }
         } else {
            buffer.writeString("");
         }

         buffer.writeInt(castle.getTaxPercent());
         buffer.writeInt((int)(castle.getSiege().getSiegeDate().getTimeInMillis() / 1000L));
      }
   }
}
