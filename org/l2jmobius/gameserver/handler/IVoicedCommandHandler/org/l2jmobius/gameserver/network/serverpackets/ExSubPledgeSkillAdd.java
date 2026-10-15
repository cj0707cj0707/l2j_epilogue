package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExSubPledgeSkillAdd extends ServerPacket {
   private final int _type;
   private final int _skillId;
   private final int _skillLevel;

   public ExSubPledgeSkillAdd(int type, int skillId, int skillLevel) {
      this._type = type;
      this._skillId = skillId;
      this._skillLevel = skillLevel;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_SUB_PLEDGE_SKILL_ADD.writeId(this, buffer);
      buffer.writeInt(this._type);
      buffer.writeInt(this._skillId);
      buffer.writeInt(this._skillLevel);
   }
}
