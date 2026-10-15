package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Fort;

public class ExShowFortressInfo extends ServerPacket {
   public static final ExShowFortressInfo STATIC_PACKET = new ExShowFortressInfo();

   private ExShowFortressInfo() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_SHOW_FORTRESS_INFO.writeId(this, buffer);
      List<Fort> forts = FortManager.getInstance().getForts();
      buffer.writeInt(forts.size());

      for (Fort fort : forts) {
         Clan clan = fort.getOwnerClan();
         buffer.writeInt(fort.getResidenceId());
         buffer.writeString(clan != null ? clan.getName() : "");
         buffer.writeInt(fort.getSiege().isInProgress());
         buffer.writeInt(fort.getOwnedTime());
      }
   }
}
