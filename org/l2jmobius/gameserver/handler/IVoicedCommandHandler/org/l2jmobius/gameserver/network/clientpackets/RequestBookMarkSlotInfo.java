package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;

public class RequestBookMarkSlotInfo extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      player.sendPacket(new ExGetBookMarkInfoPacket(player));
   }
}
