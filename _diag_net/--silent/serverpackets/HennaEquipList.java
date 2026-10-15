package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Henna;

public class HennaEquipList extends ServerPacket {
   private final Player _player;
   private final List<Henna> _hennaEquipList;

   public HennaEquipList(Player player) {
      this._player = player;
      this._hennaEquipList = HennaData.getInstance().getHennaList(player.getPlayerClass());
   }

   public HennaEquipList(Player player, List<Henna> list) {
      this._player = player;
      this._hennaEquipList = list;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.HENNA_EQUIP_LIST.writeId(this, buffer);
      buffer.writeLong(this._player.getAdena());
      buffer.writeInt(3);
      buffer.writeInt(this._hennaEquipList.size());

      for (Henna henna : this._hennaEquipList) {
         if (this._player.getInventory().getItemByItemId(henna.getDyeItemId()) != null) {
            buffer.writeInt(henna.getDyeId());
            buffer.writeInt(henna.getDyeItemId());
            buffer.writeLong(henna.getWearCount());
            buffer.writeLong(henna.getWearFee());
            buffer.writeInt(henna.isAllowedClass(this._player.getPlayerClass()));
         }
      }
   }
}
