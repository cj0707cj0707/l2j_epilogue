package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExCursedWeaponList extends ServerPacket {
   private final List<Integer> _cursedWeaponIds;

   public ExCursedWeaponList(List<Integer> cursedWeaponIds) {
      this._cursedWeaponIds = cursedWeaponIds;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_CURSED_WEAPON_LIST.writeId(this, buffer);
      buffer.writeInt(this._cursedWeaponIds.size());

      for (int i : this._cursedWeaponIds) {
         buffer.writeInt(i);
      }
   }
}
