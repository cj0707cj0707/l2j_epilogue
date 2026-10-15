package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.SellBuffsManager;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public class PrivateStoreListSell extends ServerPacket {
   private final Player _player;
   private final Player _seller;

   public PrivateStoreListSell(Player player, Player seller) {
      this._player = player;
      this._seller = seller;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      if (this._seller.isSellingBuffs()) {
         SellBuffsManager.getInstance().sendBuffMenu(this._player, this._seller, 0);
      } else {
         org.l2jmobius.gameserver.network.ServerPackets.PRIVATE_STORE_SELL_LIST.writeId(this, buffer);
         buffer.writeInt(this._seller.getObjectId());
         buffer.writeInt(this._seller.getSellList().isPackaged());
         buffer.writeLong(this._player.getAdena());
         buffer.writeInt(this._seller.getSellList().getItems().size());

         for (TradeItem item : this._seller.getSellList().getItems()) {
            buffer.writeInt(item.getItem().getType2());
            buffer.writeInt(item.getObjectId());
            buffer.writeInt(item.getItem().getId());
            buffer.writeLong(item.getCount());
            buffer.writeShort(0);
            buffer.writeShort(item.getEnchant());
            buffer.writeShort(item.getCustomType2());
            buffer.writeInt(item.getItem().getBodyPart().getMask());
            buffer.writeLong(item.getPrice());
            buffer.writeLong(item.getItem().getReferencePrice());
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
