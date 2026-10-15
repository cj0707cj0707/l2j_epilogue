package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.serverpackets.PledgeReceivePowerInfo;

public class RequestPledgeMemberPowerInfo extends ClientPacket {
   private int _unk1;
   private String _player;

   @Override
   protected void readImpl() {
      this._unk1 = this.readInt();
      this._player = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Clan clan = player.getClan();
         if (clan != null) {
            ClanMember member = clan.getClanMember(this._player);
            if (member != null) {
               player.sendPacket(new PledgeReceivePowerInfo(member));
            }
         }
      }
   }
}
