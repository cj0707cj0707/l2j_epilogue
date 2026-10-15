package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public class TradeOwnAdd extends ServerPacket {
   private final TradeItem _item;

   public TradeOwnAdd(TradeItem item) {
      this._item = item;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.TRADE_OWN_ADD.writeId(this, buffer);
      buffer.writeShort(1);
      buffer.writeShort(this._item.getItem().getType1());
      buffer.writeInt(this._item.getObjectId());
      buffer.writeInt(this._item.getItem().getDisplayId());
      buffer.writeLong(this._item.getCount());
      buffer.writeShort(this._item.getItem().getType2());
      buffer.writeShort(this._item.getCustomType1());
      buffer.writeInt(this._item.getItem().getBodyPart().getMask());
      buffer.writeShort(this._item.getEnchant());
      buffer.writeShort(0);
      buffer.writeShort(this._item.getCustomType2());
      buffer.writeShort((short)this._item.getAttackElementType());
      buffer.writeShort(this._item.getAttackElementPower());

      for (byte i = 0; i < 6; i++) {
         buffer.writeShort(this._item.getElementDefAttr(i));
      }

      for (int op : this._item.getEnchantOptions()) {
         buffer.writeShort(op);
      }
   }
}
