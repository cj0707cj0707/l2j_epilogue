package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.mechanics.buylist.Product;

public class ShopPreviewList extends ServerPacket {
   private final int _listId;
   private final Collection<Product> _list;
   private final long _money;
   private int _expertise;

   public ShopPreviewList(BuyListHolder list, long currentMoney, int expertiseIndex) {
      this._listId = list.getListId();
      this._list = list.getProducts();
      this._money = currentMoney;
      this._expertise = expertiseIndex;
   }

   public ShopPreviewList(Collection<Product> lst, int listId, long currentMoney) {
      this._listId = listId;
      this._list = lst;
      this._money = currentMoney;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SHOP_PREVIEW_LIST.writeId(this, buffer);
      buffer.writeByte(192);
      buffer.writeByte(19);
      buffer.writeByte(0);
      buffer.writeByte(0);
      buffer.writeLong(this._money);
      buffer.writeInt(this._listId);
      int newlength = 0;

      for (Product product : this._list) {
         if (product.getItem().getCrystalType().getLevel() <= this._expertise && product.getItem().isEquipable()) {
            newlength++;
         }
      }

      buffer.writeShort(newlength);

      for (Product productx : this._list) {
         if (productx.getItem().getCrystalType().getLevel() <= this._expertise && productx.getItem().isEquipable()) {
            buffer.writeInt(productx.getItemId());
            buffer.writeShort(productx.getItem().getType2());
            if (productx.getItem().getType1() != 4) {
               buffer.writeShort(productx.getItem().getBodyPart().getMask());
            } else {
               buffer.writeShort(0);
            }

            buffer.writeLong(GeneralConfig.WEAR_PRICE);
         }
      }
   }
}
