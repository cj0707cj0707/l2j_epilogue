package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExGetOffAirShip extends ServerPacket {
   private final int _playerId;
   private final int _airShipId;
   private final int _x;
   private final int _y;
   private final int _z;

   public ExGetOffAirShip(Creature creature, Creature ship, int x, int y, int z) {
      this._playerId = creature.getObjectId();
      this._airShipId = ship.getObjectId();
      this._x = x;
      this._y = y;
      this._z = z;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_GET_OFF_AIRSHIP.writeId(this, buffer);
      buffer.writeInt(this._playerId);
      buffer.writeInt(this._airShipId);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
   }
}
