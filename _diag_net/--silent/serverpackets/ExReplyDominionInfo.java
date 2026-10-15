package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager.Territory;

public class ExReplyDominionInfo extends ServerPacket {
   public static final ExReplyDominionInfo STATIC_PACKET = new ExReplyDominionInfo();

   private ExReplyDominionInfo() {
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_REPLY_DOMINION_INFO.writeId(this, buffer);
      List<Territory> territoryList = TerritoryWarManager.getInstance().getAllTerritories();
      buffer.writeInt(territoryList.size());

      for (Territory t : territoryList) {
         buffer.writeInt(t.getTerritoryId());
         buffer.writeString(CastleManager.getInstance().getCastleById(t.getCastleId()).getName().toLowerCase() + "_dominion");
         buffer.writeString(t.getOwnerClan().getName());
         buffer.writeInt(t.getOwnedWardIds().size());

         for (int i : t.getOwnedWardIds()) {
            buffer.writeInt(i);
         }

         buffer.writeInt((int)(TerritoryWarManager.getInstance().getTWStartTimeInMillis() / 1000L));
      }
   }
}
