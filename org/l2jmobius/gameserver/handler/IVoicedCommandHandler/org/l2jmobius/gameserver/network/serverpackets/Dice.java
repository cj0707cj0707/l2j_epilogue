package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class Dice extends ServerPacket {
   private final int _objectId;
   private final int _itemId;
   private final int _number;
   private final int _x;
   private final int _y;
   private final int _z;

   public Dice(int charObjId, int itemId, int number, int x, int y, int z) {
      this._objectId = charObjId;
      this._itemId = itemId;
      this._number = number;
      this._x = x;
      this._y = y;
      this._z = z;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.DICE.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._itemId);
      buffer.writeInt(this._number);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
   }
}
