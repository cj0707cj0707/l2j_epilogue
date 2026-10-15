package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExBrProductInfo;

public class RequestBrProductInfo extends ClientPacket {
   private int _productId;

   @Override
   protected void readImpl() {
      this._productId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.sendPacket(new ExBrProductInfo(this._productId));
      }
   }
}
