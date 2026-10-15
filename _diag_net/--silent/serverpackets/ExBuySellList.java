package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.MerchantZeroSellPriceConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.mechanics.buylist.Product;

public class ExBuySellList extends ServerPacket {
   private final int _buyListId;
   private final List<Product> _buyList = new ArrayList<>();
   private final long _money;
   private double _taxRate = 0.0;
   private Collection<Item> _sellList = null;
   private Collection<Item> _refundList = null;
   private final boolean _done;

   public ExBuySellList(Player player, BuyListHolder list, boolean done) {
      this._money = player.getAdena();
      this._buyListId = list.getListId();

      for (Product item : list.getProducts()) {
         if (!item.hasLimitedStock() || item.getCount() > 0L) {
            this._buyList.add(item);
         }
      }

      this._sellList = player.getInventory().getAvailableItems(false, false, false);
      if (player.hasRefund()) {
         this._refundList = player.getRefund().getItems();
      }

      this._done = done;
   }

   public ExBuySellList(Player player, BuyListHolder list, double taxRate, boolean done) {
      this._money = player.getAdena();
      this._taxRate = taxRate;
      this._buyListId = list.getListId();

      for (Product item : list.getProducts()) {
         if (!item.hasLimitedStock() || item.getCount() > 0L) {
            this._buyList.add(item);
         }
      }

      this._sellList = player.getInventory().getAvailableItems(false, false, false);
      if (player.hasRefund()) {
         this._refundList = player.getRefund().getItems();
      }

      this._done = done;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BUY_SELL_LIST.writeId(this, buffer);
      buffer.writeLong(this._money);
      buffer.writeInt(this._buyListId);
      buffer.writeShort(this._buyList.size());

      for (Product item : this._buyList) {
         buffer.writeShort(item.getItem().getType1());
         buffer.writeInt(0);
         buffer.writeInt(item.getItemId());
         buffer.writeLong(item.getCount() < 0L ? 0L : item.getCount());
         buffer.writeShort(item.getItem().getType2());
         buffer.writeShort(0);
         if (item.getItem().getType1() != 4) {
            buffer.writeInt(item.getItem().getBodyPart().getMask());
            buffer.writeShort(0);
            buffer.writeShort(0);
            buffer.writeShort(0);
         } else {
            buffer.writeInt(0);
            buffer.writeShort(0);
            buffer.writeShort(0);
            buffer.writeShort(0);
         }

         if (item.getItemId() >= 3960 && item.getItemId() <= 4026) {
            buffer.writeLong((long)((float)item.getPrice() * RatesConfig.RATE_SIEGE_GUARDS_PRICE));
         } else {
            buffer.writeLong((long)(item.getPrice() * (1.0 + this._taxRate / 2.0)));
         }

         for (byte i = 0; i < 8; i++) {
            buffer.writeShort(0);
         }

         buffer.writeShort(0);
         buffer.writeShort(0);
         buffer.writeShort(0);
      }

      if (this._sellList != null && !this._sellList.isEmpty()) {
         buffer.writeShort(this._sellList.size());

         for (Item item : this._sellList) {
            buffer.writeShort(item.getTemplate().getType1());
            buffer.writeInt(item.getObjectId());
            buffer.writeInt(item.getId());
            buffer.writeLong(item.getCount());
            buffer.writeShort(item.getTemplate().getType2());
            buffer.writeShort(0);
            buffer.writeInt(item.getTemplate().getBodyPart().getMask());
            buffer.writeShort(item.getEnchantLevel());
            buffer.writeShort(0);
            buffer.writeShort(0);
            buffer.writeLong(MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE ? 0L : item.getTemplate().getReferencePrice() / 2);
            buffer.writeShort(item.getAttackElementType());
            buffer.writeShort(item.getAttackElementPower());

            for (byte i = 0; i < 6; i++) {
               buffer.writeShort(item.getElementDefAttr(i));
            }

            buffer.writeShort(0);
            buffer.writeShort(0);
            buffer.writeShort(0);
         }
      } else {
         buffer.writeShort(0);
      }

      if (this._refundList != null && !this._refundList.isEmpty()) {
         buffer.writeShort(this._refundList.size());
         int idx = 0;

         for (Item item : this._refundList) {
            buffer.writeInt(idx++);
            buffer.writeInt(item.getId());
            buffer.writeLong(item.getCount());
            buffer.writeShort(item.getTemplate().getType2());
            buffer.writeShort(0);
            buffer.writeShort(item.getEnchantLevel());
            buffer.writeShort(0);
            buffer.writeLong(MerchantZeroSellPriceConfig.MERCHANT_ZERO_SELL_PRICE ? 0L : item.getTemplate().getReferencePrice() / 2 * item.getCount());
            buffer.writeShort(item.getAttackElementType());
            buffer.writeShort(item.getAttackElementPower());

            for (byte i = 0; i < 6; i++) {
               buffer.writeShort(item.getElementDefAttr(i));
            }

            buffer.writeShort(0);
            buffer.writeShort(0);
            buffer.writeShort(0);
         }
      } else {
         buffer.writeShort(0);
      }

      buffer.writeByte(this._done);
   }
}
