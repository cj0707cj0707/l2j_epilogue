package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestPledgeSetAcademyMaster extends ClientPacket {
   private String _currPlayerName;
   private int _set;
   private String _targetPlayerName;

   @Override
   protected void readImpl() {
      this._set = this.readInt();
      this._currPlayerName = this.readString();
      this._targetPlayerName = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      Clan clan = player.getClan();
      if (clan != null) {
         if (!player.hasAccess(ClanAccess.DISMISS_MENTEE)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_DISMISS_AN_APPRENTICE);
         } else {
            ClanMember currentMember = clan.getClanMember(this._currPlayerName);
            ClanMember targetMember = clan.getClanMember(this._targetPlayerName);
            if (currentMember != null && targetMember != null) {
               ClanMember apprenticeMember;
               ClanMember sponsorMember;
               if (currentMember.getPledgeType() == -1) {
                  apprenticeMember = currentMember;
                  sponsorMember = targetMember;
               } else {
                  apprenticeMember = targetMember;
                  sponsorMember = currentMember;
               }

               Player apprentice = apprenticeMember.getPlayer();
               Player sponsor = sponsorMember.getPlayer();
               SystemMessage sm = null;
               if (this._set == 0) {
                  if (apprentice != null) {
                     apprentice.setSponsor(0);
                  } else {
                     apprenticeMember.setApprenticeAndSponsor(0, 0);
                  }

                  if (sponsor != null) {
                     sponsor.setApprentice(0);
                  } else {
                     sponsorMember.setApprenticeAndSponsor(0, 0);
                  }

                  apprenticeMember.saveApprenticeAndSponsor(0, 0);
                  sponsorMember.saveApprenticeAndSponsor(0, 0);
                  sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S2_CLAN_MEMBER_C1_S_APPRENTICE_HAS_BEEN_REMOVED);
               } else {
                  if (apprenticeMember.getSponsor() != 0
                     || sponsorMember.getApprentice() != 0
                     || apprenticeMember.getApprentice() != 0
                     || sponsorMember.getSponsor() != 0) {
                     player.sendMessage("Remove previous connections first.");
                     return;
                  }

                  if (apprentice != null) {
                     apprentice.setSponsor(sponsorMember.getObjectId());
                  } else {
                     apprenticeMember.setApprenticeAndSponsor(0, sponsorMember.getObjectId());
                  }

                  if (sponsor != null) {
                     sponsor.setApprentice(apprenticeMember.getObjectId());
                  } else {
                     sponsorMember.setApprenticeAndSponsor(apprenticeMember.getObjectId(), 0);
                  }

                  apprenticeMember.saveApprenticeAndSponsor(0, sponsorMember.getObjectId());
                  sponsorMember.saveApprenticeAndSponsor(apprenticeMember.getObjectId(), 0);
                  sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S2_HAS_BEEN_DESIGNATED_AS_THE_APPRENTICE_OF_CLAN_MEMBER_S1);
               }

               sm.addString(sponsorMember.getName());
               sm.addString(apprenticeMember.getName());
               if (sponsor != player && sponsor != apprentice) {
                  player.sendPacket(sm);
               }

               if (sponsor != null) {
                  sponsor.sendPacket(sm);
               }

               if (apprentice != null) {
                  apprentice.sendPacket(sm);
               }
            }
         }
      }
   }
}
