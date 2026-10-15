package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;

public class ExAirShipTeleportList extends ServerPacket {
   private final int _dockId;
   private final VehiclePathPoint[][] _teleports;
   private final int[] _fuelConsumption;

   public ExAirShipTeleportList(int dockId, VehiclePathPoint[][] teleports, int[] fuelConsumption) {
      this._dockId = dockId;
      this._teleports = teleports;
      this._fuelConsumption = fuelConsumption;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_AIRSHIP_TELEPORT_LIST.writeId(this, buffer);
      buffer.writeInt(this._dockId);
      if (this._teleports != null) {
         buffer.writeInt(this._teleports.length);

         for (int i = 0; i < this._teleports.length; i++) {
            buffer.writeInt(i - 1);
            buffer.writeInt(this._fuelConsumption[i]);
            VehiclePathPoint[] path = this._teleports[i];
            VehiclePathPoint dst = path[path.length - 1];
            buffer.writeInt(dst.getX());
            buffer.writeInt(dst.getY());
            buffer.writeInt(dst.getZ());
         }
      } else {
         buffer.writeInt(0);
      }
   }
}
