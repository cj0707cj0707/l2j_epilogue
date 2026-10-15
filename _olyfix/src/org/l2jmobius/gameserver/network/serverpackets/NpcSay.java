package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class NpcSay extends ServerPacket {
   private final int _objectId;
   private final ChatType _textType;
   private final int _npcId;
   private final String _text;

   public NpcSay(int objectId, ChatType messageType, int npcId, String text) {
      this._objectId = objectId;
      this._textType = messageType;
      this._npcId = 1000000 + npcId;
      this._text = text;
   }

   public NpcSay(Npc npc, ChatType messageType, String text) {
      this._objectId = npc.getObjectId();
      this._textType = messageType;
      this._npcId = 1000000 + npc.getTemplate().getDisplayId();
      this._text = text;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.NPC_SAY.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._textType.getClientId());
      buffer.writeInt(this._npcId);
      buffer.writeString(this._text);
   }
}
