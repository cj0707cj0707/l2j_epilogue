package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExStopMoveInAirShip extends ServerPacket {
   private final Player _player;
   private final int _shipObjId;
   private final int x;
   private final int y;
   private final int z;
   private final int h;

   public ExStopMoveInAirShip(Player player, int shipObjId) {
      this._player = player;
      this._shipObjId = shipObjId;
      this.x = player.getInVehiclePosition().getX();
      this.y = player.getInVehiclePosition().getY();
      this.z = player.getInVehiclePosition().getZ();
      this.h = player.getHeading();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_STOP_MOVE_IN_AIRSHIP.writeId(this, buffer);
      buffer.writeInt(this._player.getObjectId());
      buffer.writeInt(this._shipObjId);
      buffer.writeInt(this.x);
      buffer.writeInt(this.y);
      buffer.writeInt(this.z);
      buffer.writeInt(this.h);
   }
}
