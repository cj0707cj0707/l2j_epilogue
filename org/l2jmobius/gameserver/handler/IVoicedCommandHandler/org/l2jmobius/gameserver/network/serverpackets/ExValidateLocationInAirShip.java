package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExValidateLocationInAirShip extends ServerPacket {
   private final Player _player;
   private final int shipId;
   private final int x;
   private final int y;
   private final int z;
   private final int h;

   public ExValidateLocationInAirShip(Player player) {
      this._player = player;
      this.shipId = this._player.getAirShip().getObjectId();
      this.x = player.getInVehiclePosition().getX();
      this.y = player.getInVehiclePosition().getY();
      this.z = player.getInVehiclePosition().getZ();
      this.h = player.getHeading();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_VALIDATE_LOCATION_IN_AIRSHIP.writeId(this, buffer);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeInt(this.shipId);
      buffer.writeInt(this.x);
      buffer.writeInt(this.y);
      buffer.writeInt(this.z);
      buffer.writeInt(this.h);
   }
}
