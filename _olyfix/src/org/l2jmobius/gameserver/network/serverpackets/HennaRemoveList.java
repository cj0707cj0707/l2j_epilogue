package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class HennaRemoveList extends ServerPacket {
   private final Player _player;

   public HennaRemoveList(Player player) {
      this._player = player;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.HENNA_UNEQUIP_LIST.writeId(this, buffer);
      buffer.writeLong(this._player.getAdena());
      buffer.writeInt(0);
      buffer.writeInt(3 - this._player.getHennaEmptySlots());

      for (Henna henna : this._player.getHennaList()) {
         if (henna != null) {
            buffer.writeInt(henna.getDyeId());
            buffer.writeInt(henna.getDyeItemId());
            buffer.writeInt(henna.getCancelCount());
            buffer.writeInt(0);
            buffer.writeInt(henna.getCancelFee());
            buffer.writeInt(0);
            buffer.writeInt(1);
         }
      }
   }
}
