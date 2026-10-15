package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class VehicleStarted extends ServerPacket {
   private final int _objectId;
   private final int _state;

   public VehicleStarted(Creature boat, int state) {
      this._objectId = boat.getObjectId();
      this._state = state;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.VEHICLE_START.writeId(this, buffer);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._state);
   }
}
