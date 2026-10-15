package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExShowDominionRegistry extends ServerPacket {
   private static final int MINID = 80;
   private final int _castleId;
   private int _clanReq = 0;
   private int _mercReq = 0;
   private boolean _isMercRegistered = false;
   private boolean _isClanRegistered = false;
   private int _warTime = (int)(System.currentTimeMillis() / 1000L);
   private final int _currentTime = (int)(System.currentTimeMillis() / 1000L);

   public ExShowDominionRegistry(int castleId, Player player) {
      this._castleId = castleId;
      if (TerritoryWarManager.getInstance().getRegisteredClans(castleId) != null) {
         this._clanReq = TerritoryWarManager.getInstance().getRegisteredClans(castleId).size();
         Clan clan = player.getClan();
         if (clan != null) {
            this._isClanRegistered = TerritoryWarManager.getInstance().getRegisteredClans(castleId).contains(clan);
         }
      }

      if (TerritoryWarManager.getInstance().getRegisteredMercenaries(castleId) != null) {
         this._mercReq = TerritoryWarManager.getInstance().getRegisteredMercenaries(castleId).size();
         this._isMercRegistered = TerritoryWarManager.getInstance().getRegisteredMercenaries(castleId).contains(player.getObjectId());
      }

      this._warTime = (int)(TerritoryWarManager.getInstance().getTWStartTimeInMillis() / 1000L);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SHOW_DOMINION_REGISTRY.writeId(this, buffer);
      buffer.writeInt(80 + this._castleId);
      if (TerritoryWarManager.getInstance().getTerritory(this._castleId) == null) {
         buffer.writeString("No Owner");
         buffer.writeString("No Owner");
         buffer.writeString("No Ally");
      } else {
         Clan clan = TerritoryWarManager.getInstance().getTerritory(this._castleId).getOwnerClan();
         if (clan == null) {
            buffer.writeString("No Owner");
            buffer.writeString("No Owner");
            buffer.writeString("No Ally");
         } else {
            buffer.writeString(clan.getName());
            buffer.writeString(clan.getLeaderName());
            buffer.writeString(clan.getAllyName());
         }
      }

      buffer.writeInt(this._clanReq);
      buffer.writeInt(this._mercReq);
      buffer.writeInt(this._warTime);
      buffer.writeInt(this._currentTime);
      buffer.writeInt(this._isClanRegistered);
      buffer.writeInt(this._isMercRegistered);
      buffer.writeInt(1);
      List<TerritoryWarManager.Territory> territoryList = TerritoryWarManager.getInstance().getAllTerritories();
      buffer.writeInt(territoryList.size());

      for (TerritoryWarManager.Territory t : territoryList) {
         buffer.writeInt(t.getTerritoryId());
         buffer.writeInt(t.getOwnedWardIds().size());

         for (int i : t.getOwnedWardIds()) {
            buffer.writeInt(i);
         }
      }
   }
}
