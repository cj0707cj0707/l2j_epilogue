package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.mechanics.buylist.Product;

public class BuyList extends ServerPacket {
   private final int _listId;
   private final Collection<Product> _list;
   private final long _money;
   private double _taxRate = 0.0;

   public BuyList(BuyListHolder list, long currentMoney, double taxRate) {
      this._listId = list.getListId();
      this._list = list.getProducts();
      this._money = currentMoney;
      this._taxRate = taxRate;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BUY_SELL_LIST.writeId(this, buffer);
      buffer.writeLong(this._money);
      buffer.writeInt(this._listId);
      buffer.writeShort(this._list.size());

      for (Product product : this._list) {
         if (product.getCount() > 0L || !product.hasLimitedStock()) {
            buffer.writeShort(product.getItem().getType1());
            buffer.writeInt(0);
            buffer.writeInt(product.getItemId());
            buffer.writeLong(product.getCount() < 0L ? 0L : product.getCount());
            buffer.writeShort(product.getItem().getType2());
            buffer.writeShort(0);
            if (product.getItem().getType1() != 4) {
               buffer.writeInt(product.getItem().getBodyPart().getMask());
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
            } else {
               buffer.writeInt(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
               buffer.writeShort(0);
            }

            if (product.getItemId() >= 3960 && product.getItemId() <= 4026) {
               buffer.writeLong((long)((float)product.getPrice() * RatesConfig.RATE_SIEGE_GUARDS_PRICE * (1.0 + this._taxRate)));
            } else {
               buffer.writeLong((long)(product.getPrice() * (1.0 + this._taxRate)));
            }

            for (byte i = 0; i < 8; i++) {
               buffer.writeShort(0);
            }

            buffer.writeShort(0);
            buffer.writeShort(0);
            buffer.writeShort(0);
         }
      }
   }
}
