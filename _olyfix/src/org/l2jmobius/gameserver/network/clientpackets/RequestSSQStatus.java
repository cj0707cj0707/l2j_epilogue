package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.network.serverpackets.SSQStatus;

public class RequestSSQStatus extends ClientPacket {
   private int _page;

   @Override
   protected void readImpl() {
      this._page = this.readByte();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (!SevenSigns.getInstance().isSealValidationPeriod() && !SevenSigns.getInstance().isCompResultsPeriod() || this._page != 4) {
            player.sendPacket(new SSQStatus(player.getObjectId(), this._page));
         }
      }
   }
}
