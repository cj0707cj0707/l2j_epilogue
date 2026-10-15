package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExDominionWarStart extends ServerPacket {
   private final int _objId;
   private final int _terId;
   private final boolean _isDisguised;

   public ExDominionWarStart(Player player) {
      this._objId = player.getObjectId();
      this._terId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(player);
      this._isDisguised = TerritoryWarManager.getInstance().isDisguised(this._objId);
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_DOMINION_WAR_START.writeId(this, buffer);
      buffer.writeInt(this._objId);
      buffer.writeInt(1);
      buffer.writeInt(this._terId);
      buffer.writeInt(this._isDisguised);
      buffer.writeInt(this._isDisguised ? this._terId : 0);
   }
}
