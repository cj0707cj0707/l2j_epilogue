package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeSkillListAdd extends ServerPacket {
   private final int _id;
   private final int _level;

   public PledgeSkillListAdd(int id, int level) {
      this._id = id;
      this._level = level;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PLEDGE_SKILL_LIST_ADD.writeId(this, buffer);
      buffer.writeInt(this._id);
      buffer.writeInt(this._level);
   }
}
