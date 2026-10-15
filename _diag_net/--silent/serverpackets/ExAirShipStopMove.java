package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;

public class ExAirShipStopMove extends ServerPacket {
   private final int _playerId;
   private final int _airShipId;
   private final int _x;
   private final int _y;
   private final int _z;

   public ExAirShipStopMove(Player player, AirShip ship, int x, int y, int z) {
      this._playerId = player.getObjectId();
      this._airShipId = ship.getObjectId();
      this._x = x;
      this._y = y;
      this._z = z;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_STOP_MOVE_AIRSHIP.writeId(this, buffer);
      buffer.writeInt(this._airShipId);
      buffer.writeInt(this._playerId);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
   }
}
