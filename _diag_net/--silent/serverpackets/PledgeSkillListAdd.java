package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class PledgeSkillListAdd extends ServerPacket {
   private final int _id;
   private final int _level;

   public PledgeSkillListAdd(int id, int level) {
      this._id = id;
      this._level = level;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PLEDGE_SKILL_LIST_ADD.writeId(this, buffer);
      buffer.writeInt(this._id);
      buffer.writeInt(this._level);
   }
}
