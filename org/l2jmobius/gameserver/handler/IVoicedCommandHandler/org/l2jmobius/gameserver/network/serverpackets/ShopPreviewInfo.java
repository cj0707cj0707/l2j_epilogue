package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ShopPreviewInfo extends ServerPacket {
   private final Map<Integer, Integer> _itemlist;

   public ShopPreviewInfo(Map<Integer, Integer> itemlist) {
      this._itemlist = itemlist;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SHOP_PREVIEW_INFO.writeId(this, buffer);
      buffer.writeInt(25);
      buffer.writeInt(this.getFromList(0));
      buffer.writeInt(this.getFromList(8));
      buffer.writeInt(this.getFromList(9));
      buffer.writeInt(this.getFromList(4));
      buffer.writeInt(this.getFromList(13));
      buffer.writeInt(this.getFromList(14));
      buffer.writeInt(this.getFromList(1));
      buffer.writeInt(this.getFromList(5));
      buffer.writeInt(this.getFromList(7));
      buffer.writeInt(this.getFromList(10));
      buffer.writeInt(this.getFromList(6));
      buffer.writeInt(this.getFromList(11));
      buffer.writeInt(this.getFromList(12));
      buffer.writeInt(this.getFromList(23));
      buffer.writeInt(this.getFromList(5));
      buffer.writeInt(this.getFromList(2));
      buffer.writeInt(this.getFromList(3));
      buffer.writeInt(this.getFromList(16));
      buffer.writeInt(this.getFromList(15));
   }

   private int getFromList(int key) {
      return this._itemlist.getOrDefault(key, 0);
   }
}
