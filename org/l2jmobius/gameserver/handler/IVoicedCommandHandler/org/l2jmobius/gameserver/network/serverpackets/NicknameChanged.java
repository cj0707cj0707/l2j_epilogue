package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class NicknameChanged extends ServerPacket {
   private final String _title;
   private final int _objectId;

   public NicknameChanged(Creature creature) {
      this._objectId = creature.getObjectId();
      this._title = creature.getTitle();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.NICK_NAME_CHANGED.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeString(this._title);
   }
}
