package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExCursedWeaponLocation extends ServerPacket {
   private final List<ExCursedWeaponLocation.CursedWeaponInfo> _cursedWeaponInfo;

   public ExCursedWeaponLocation(List<ExCursedWeaponLocation.CursedWeaponInfo> cursedWeaponInfo) {
      this._cursedWeaponInfo = cursedWeaponInfo;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_CURSED_WEAPON_LOCATION.writeId(this, buffer);
      if (!this._cursedWeaponInfo.isEmpty()) {
         buffer.writeInt(this._cursedWeaponInfo.size());

         for (ExCursedWeaponLocation.CursedWeaponInfo w : this._cursedWeaponInfo) {
            buffer.writeInt(w.id);
            buffer.writeInt(w.activated);
            buffer.writeInt(w.pos.getX());
            buffer.writeInt(w.pos.getY());
            buffer.writeInt(w.pos.getZ());
         }
      } else {
         buffer.writeInt(0);
         buffer.writeInt(0);
      }
   }

   public static class CursedWeaponInfo {
      public Location pos;
      public int id;
      public int activated;

      public CursedWeaponInfo(Location p, int cwId, int status) {
         this.pos = p;
         this.id = cwId;
         this.activated = status;
      }
   }
}
