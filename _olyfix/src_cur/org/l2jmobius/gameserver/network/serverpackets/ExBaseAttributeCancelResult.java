package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExBaseAttributeCancelResult extends ServerPacket {
   private final int _objId;
   private final byte _attribute;

   public ExBaseAttributeCancelResult(int objId, byte attribute) {
      this._objId = objId;
      this._attribute = attribute;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_BASE_ATTRIBUTE_CANCEL_RESULT.writeId(this, buffer);
      buffer.writeInt(1);
      buffer.writeInt(this._objId);
      buffer.writeInt(this._attribute);
   }
}
