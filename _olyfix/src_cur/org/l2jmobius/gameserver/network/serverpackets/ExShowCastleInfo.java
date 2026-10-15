package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowCastleInfo extends ServerPacket {
   private final Collection<Castle> _castles = CastleManager.getInstance().getCastles();

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_CASTLE_INFO.writeId(this, buffer);
      buffer.writeInt(this._castles.size());

      for (Castle castle : this._castles) {
         buffer.writeInt(castle.getResidenceId());
         if (castle.getOwnerId() > 0) {
            if (ClanTable.getInstance().getClan(castle.getOwnerId()) != null) {
               buffer.writeString(ClanTable.getInstance().getClan(castle.getOwnerId()).getName());
            } else {
               PacketLogger.warning(
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
