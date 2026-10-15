package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExBaseAttributeCancelResult extends ServerPacket {
   private final int _objId;
   private final byte _attribute;

   public ExBaseAttributeCancelResult(int objId, byte attribute) {
      this._objId = objId;
      this._attribute = attribute;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_BASE_ATTRIBUTE_CANCEL_RESULT.writeId(this, buffer);
      buffer.writeInt(1);
      buffer.writeInt(this._objId);
      buffer.writeInt(this._attribute);
   }
}
