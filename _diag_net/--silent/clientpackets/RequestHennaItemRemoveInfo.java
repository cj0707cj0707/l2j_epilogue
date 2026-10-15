package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.HennaItemRemoveInfo;

public class RequestHennaItemRemoveInfo extends ClientPacket {
   private int _symbolId;

   @Override
   protected void readImpl() {
      this._symbolId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && this._symbolId != 0) {
         Henna henna = HennaData.getInstance().getHenna(this._symbolId);
         if (henna == null) {
            org.l2jmobius.gameserver.network.PacketLogger.warning(this.getClass().getName() + ": Invalid Henna Id: " + this._symbolId + " from " + player);
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else {
            player.sendPacket(new HennaItemRemoveInfo(henna, player));
         }
      }
   }
}
