package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.holders.PrimeShopProductHolder;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExBrProductList extends ServerPacket {
   private final Collection<PrimeShopProductHolder> _itemList = PrimeShopData.getInstance().getAllItems();

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BR_PRODUCT_LIST.writeId(this, buffer);
      buffer.writeInt(this._itemList.size());

      for (PrimeShopProductHolder product : this._itemList) {
         int category = product.getCategory();
         buffer.writeInt(product.getProductId());
         buffer.writeShort(category);
         buffer.writeInt(product.getPrice());
         switch (category) {
            case 6:
               buffer.writeInt(1);
               break;
            case 7:
               buffer.writeInt(2);
               break;
            case 8:
               buffer.writeInt(3);
               break;
            default:
               buffer.writeInt(0);
         }

         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeByte(0);
         buffer.writeByte(0);
         buffer.writeByte(0);
         buffer.writeByte(0);
         buffer.writeByte(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
      }
   }
}
