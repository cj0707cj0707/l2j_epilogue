package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ExFishingEnd extends ServerPacket {
   private final boolean _win;
   private final Creature _creature;

   public ExFishingEnd(boolean win, Player character) {
      this._win = win;
      this._creature = character;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_FISHING_END.writeId(this, buffer);
      buffer.writeInt(this._creature.getObjectId());
      buffer.writeByte(this._win);
   }
}
