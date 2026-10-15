package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeEmblem;

public class RequestExPledgeCrestLarge extends ClientPacket {
   private int _crestId;

   @Override
   protected void readImpl() {
      this._crestId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.sendPacket(new ExPledgeEmblem(this._crestId));
      }
   }
}
