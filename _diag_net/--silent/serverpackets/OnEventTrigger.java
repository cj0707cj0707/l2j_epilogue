package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.Door;

public class OnEventTrigger extends ServerPacket {
   private final int _emitterId;
   private final boolean _enabled;

   public OnEventTrigger(Door door, boolean enabled) {
      this._emitterId = door.getEmitter();
      this._enabled = enabled;
   }

   public OnEventTrigger(int id, boolean enabled) {
      this._emitterId = id;
      this._enabled = enabled;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EVENT_TRIGGER.writeId(this, buffer);
      buffer.writeInt(this._emitterId);
      buffer.writeByte(this._enabled);
   }
}
