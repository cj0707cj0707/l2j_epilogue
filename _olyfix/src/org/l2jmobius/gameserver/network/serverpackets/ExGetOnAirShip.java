package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExGetOnAirShip extends ServerPacket {
   private final int _playerId;
   private final int _airShipId;
   private final Location _pos;

   public ExGetOnAirShip(Player player, Creature ship) {
      this._playerId = player.getObjectId();
      this._airShipId = ship.getObjectId();
      this._pos = player.getInVehiclePosition();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_GET_ON_AIRSHIP.writeId(this, buffer);
      buffer.writeInt(this._playerId);
      buffer.writeInt(this._airShipId);
      buffer.writeInt(this._pos.getX());
      buffer.writeInt(this._pos.getY());
      buffer.writeInt(this._pos.getZ());
   }
}
