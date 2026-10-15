package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExPutItemResultForVariationCancel extends ServerPacket {
   private final int _itemObjId;
   private final int _itemId;
   private final int _itemAug1;
   private final int _itemAug2;
   private final int _price;

   public ExPutItemResultForVariationCancel(Item item, int price) {
      this._itemObjId = item.getObjectId();
      this._itemId = item.getDisplayId();
      this._price = price;
      this._itemAug1 = (short)item.getAugmentation().getAugmentationId();
      this._itemAug2 = item.getAugmentation().getAugmentationId() >> 16;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_PUT_ITEM_RESULT_FOR_VARIATION_CANCEL.writeId(this, buffer);
      buffer.writeInt(this._itemObjId);
      buffer.writeInt(this._itemId);
      buffer.writeInt(this._itemAug1);
      buffer.writeInt(this._itemAug2);
      buffer.writeLong(this._price);
      buffer.writeInt(1);
   }
}
