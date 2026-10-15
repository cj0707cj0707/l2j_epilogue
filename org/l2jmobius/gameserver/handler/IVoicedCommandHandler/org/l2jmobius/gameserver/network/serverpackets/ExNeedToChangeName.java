package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExNeedToChangeName extends ServerPacket {
   private final int _type;
   private final int _subType;
   private final String _name;

   public ExNeedToChangeName(int type, int subType, String name) {
      this._type = type;
      this._subType = subType;
      this._name = name;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_NEED_TO_CHANGE_NAME.writeId(this, buffer);
      buffer.writeInt(this._type);
      buffer.writeInt(this._subType);
      buffer.writeString(this._name);
   }
}
