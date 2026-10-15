package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.siege.TerritoryWard;

public class ExShowOwnthingPos extends ServerPacket {
   public static final ExShowOwnthingPos STATIC_PACKET = new ExShowOwnthingPos();

   private ExShowOwnthingPos() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_OWNTHING_POS.writeId(this, buffer);
      if (TerritoryWarManager.getInstance().isTWInProgress()) {
         Collection<TerritoryWard> territoryWardList = TerritoryWarManager.getInstance().getAllTerritoryWards();
         buffer.writeInt(territoryWardList.size());

         for (TerritoryWard ward : territoryWardList) {
            buffer.writeInt(ward.getTerritoryId());
            if (ward.getNpc() != null) {
               buffer.writeInt(ward.getNpc().getX());
               buffer.writeInt(ward.getNpc().getY());
               buffer.writeInt(ward.getNpc().getZ());
            } else if (ward.getPlayer() != null) {
               buffer.writeInt(ward.getPlayer().getX());
               buffer.writeInt(ward.getPlayer().getY());
               buffer.writeInt(ward.getPlayer().getZ());
            } else {
               buffer.writeInt(0);
               buffer.writeInt(0);
               buffer.writeInt(0);
            }
         }
      } else {
         buffer.writeInt(0);
         buffer.writeInt(0);
      }
   }
}
