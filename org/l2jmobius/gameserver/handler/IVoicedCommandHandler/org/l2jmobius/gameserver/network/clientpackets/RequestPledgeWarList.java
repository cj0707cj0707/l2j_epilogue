package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.serverpackets.PledgeReceiveWarList;

public class RequestPledgeWarList extends ClientPacket {
   private int _unk1;
   private int _tab;

   @Override
   protected void readImpl() {
      this._unk1 = this.readInt();
      this._tab = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Clan clan = player.getClan();
         if (clan != null) {
            player.sendPacket(new PledgeReceiveWarList(clan, this._tab));
         }
      }
   }
}
