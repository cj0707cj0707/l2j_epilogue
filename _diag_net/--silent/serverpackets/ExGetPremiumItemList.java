package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map.Entry;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.PremiumItem;

public class ExGetPremiumItemList extends ServerPacket {
   private final Player _player;

   public ExGetPremiumItemList(Player player) {
      this._player = player;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_GET_PREMIUM_ITEM_LIST.writeId(this, buffer);
      buffer.writeInt(this._player.getPremiumItemList().size());

      for (Entry<Integer, PremiumItem> entry : this._player.getPremiumItemList().entrySet()) {
         PremiumItem item = entry.getValue();
         buffer.writeInt(entry.getKey());
         buffer.writeInt(this._player.getObjectId());
         buffer.writeInt(item.getItemId());
         buffer.writeLong(item.getCount());
         buffer.writeInt(0);
         buffer.writeString(item.getSender());
      }
   }
}
