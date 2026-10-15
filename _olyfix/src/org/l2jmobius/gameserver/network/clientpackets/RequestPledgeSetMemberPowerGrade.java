package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.ClanMember;

public class RequestPledgeSetMemberPowerGrade extends ClientPacket {
   private String _member;
   private int _powerGrade;

   @Override
   protected void readImpl() {
      this._member = this.readString();
      this._powerGrade = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Clan clan = player.getClan();
         if (clan != null) {
            if (player.hasAccess(ClanAccess.MODIFY_RANKS)) {
               ClanMember member = clan.getClanMember(this._member);
               if (member != null) {
                  if (member.getObjectId() != clan.getLeaderId()) {
                     if (member.getPledgeType() == -1) {
                        player.sendMessage("You cannot change academy member grade");
                     } else {
                        member.setPowerGrade(this._powerGrade);
                        clan.broadcastClanStatus();
                     }
                  }
               }
            }
         }
      }
   }
}
