package org.l2jmobius.gameserver.network.serverpackets;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.mechanics.siege.manor.CropProcure;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class SellListProcure extends ServerPacket {
   private final long _money;
   private final Map<Item, Long> _sellList = new HashMap<>();

   public SellListProcure(Player player, int castleId) {
      this._money = player.getAdena();

      for (CropProcure c : CastleManorManager.getInstance().getCropProcure(castleId, false)) {
         Item item = player.getInventory().getItemByItemId(c.getId());
         if (item != null && c.getAmount() > 0L) {
            this._sellList.put(item, c.getAmount());
         }
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SELL_LIST_PROCURE.writeId(this, buffer);
      buffer.writeLong(this._money);
      buffer.writeInt(0);
      buffer.writeShort(this._sellList.size());

      for (Entry<Item, Long> entry : this._sellList.entrySet()) {
         Item item = entry.getKey();
         buffer.writeShort(item.getTemplate().getType1());
         buffer.writeInt(item.getObjectId());
         buffer.writeInt(item.getDisplayId());
         buffer.writeLong(entry.getValue());
         buffer.writeShort(item.getTemplate().getType2());
         buffer.writeShort(0);
         buffer.writeLong(0L);
      }
   }
}
