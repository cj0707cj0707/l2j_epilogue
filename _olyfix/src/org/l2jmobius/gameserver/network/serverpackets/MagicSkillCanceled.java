package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class MagicSkillCanceled extends ServerPacket {
   private final int _objectId;

   public MagicSkillCanceled(int objectId) {
      this._objectId = objectId;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.MAGIC_SKILL_CANCELED.writeId(this, buffer);
      buffer.writeInt(this._objectId);
   }
}
