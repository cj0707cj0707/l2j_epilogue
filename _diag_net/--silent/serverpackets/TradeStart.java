package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class TradeStart extends ServerPacket {
   private final Player _player;
   private final Collection<Item> _itemList;

   public TradeStart(Player player) {
      this._player = player;
      this._itemList = this._player.getInventory().getAvailableItems(true, this._player.isGM() && GeneralConfig.GM_TRADE_RESTRICTED_ITEMS, false);
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      if (this._player.getActiveTradeList() != null && this._player.getActiveTradeList().getPartner() != null) {
         org.l2jmobius.gameserver.network.ServerPackets.TRADE_START.writeId(this, buffer);
         buffer.writeInt(this._player.getActiveTradeList().getPartner().getObjectId());
         buffer.writeShort(this._itemList.size());

         for (Item item : this._itemList) {
            buffer.writeShort(item.getTemplate().getType1());
            buffer.writeInt(item.getObjectId());
            buffer.writeInt(item.getId());
            buffer.writeLong(item.getCount());
            buffer.writeShort(item.getTemplate().getType2());
            buffer.writeShort(0);
            buffer.writeInt(item.getTemplate().getBodyPart().getMask());
            buffer.writeShort(item.getEnchantLevel());
            buffer.writeShort(0);
            buffer.writeShort(item.getCustomType2());
            buffer.writeShort(item.getAttackElementType());
            buffer.writeShort(item.getAttackElementPower());

            for (byte i = 0; i < 6; i++) {
               buffer.writeShort(item.getElementDefAttr(i));
            }

            for (int op : item.getEnchantOptions()) {
               buffer.writeShort(op);
            }
         }
      }
   }
}
