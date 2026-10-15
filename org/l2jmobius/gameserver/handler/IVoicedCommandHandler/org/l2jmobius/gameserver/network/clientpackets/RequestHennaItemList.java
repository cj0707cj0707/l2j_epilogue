package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.HennaEquipList;

public class RequestHennaItemList extends ClientPacket {
   @Override
   protected void readImpl() {
      this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.sendPacket(new HennaEquipList(player));
      }
   }
}
