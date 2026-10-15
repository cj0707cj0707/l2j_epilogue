package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class Snoop extends ServerPacket {
   private final int _convoId;
   private final String _name;
   private final ChatType _type;
   private final String _speaker;
   private final String _msg;

   public Snoop(int id, String name, ChatType type, String speaker, String msg) {
      this._convoId = id;
      this._name = name;
      this._type = type;
      this._speaker = speaker;
      this._msg = msg;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.SNOOP.writeId(this, buffer);
      buffer.writeInt(this._convoId);
      buffer.writeString(this._name);
      buffer.writeInt(0);
      buffer.writeInt(this._type.getClientId());
      buffer.writeString(this._speaker);
      buffer.writeString(this._msg);
   }
}
