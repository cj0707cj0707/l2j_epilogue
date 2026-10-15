package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.holders.PrimeShopProductHolder;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;

public class ExBrProductInfo extends ServerPacket {
   private final PrimeShopProductHolder _product;

   public ExBrProductInfo(int id) {
      this._product = PrimeShopData.getInstance().getProduct(id);
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      if (this._product != null) {
         org.l2jmobius.gameserver.network.ServerPackets.EX_BR_PRODUCT_INFO.writeId(this, buffer);
         buffer.writeInt(this._product.getProductId());
         buffer.writeInt(this._product.getPrice());
         buffer.writeInt(1);
         buffer.writeInt(this._product.getItemId());
         buffer.writeInt(this._product.getItemCount());
         buffer.writeInt(this._product.getItemWeight());
         buffer.writeInt(this._product.isTradable());
      }
   }
}
