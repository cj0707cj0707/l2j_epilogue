package org.l2jmobius.gameserver.network.clientpackets;

import java.util.concurrent.TimeUnit;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestOustPledgeMember extends ClientPacket {
   private String _target;

   @Override
   protected void readImpl() {
      this._target = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.getClan() == null) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
         } else if (!player.hasAccess(ClanAccess.REMOVE_MEMBER)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
         } else if (player.getName().equalsIgnoreCase(this._target)) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_DISMISS_YOURSELF);
         } else {
            Clan clan = player.getClan();
            ClanMember member = clan.getClanMember(this._target);
            if (member == null) {
               org.l2jmobius.gameserver.network.PacketLogger.warning("Target (" + this._target + ") is not member of the clan");
            } else if (member.isOnline() && member.getPlayer().isInCombat()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.A_CLAN_MEMBER_MAY_NOT_BE_DISMISSED_DURING_COMBAT);
            } else {
               clan.removeClanMember(member.getObjectId(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(PlayerConfig.ALT_CLAN_JOIN_DAYS));
               clan.setCharPenaltyExpiryTime(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(PlayerConfig.ALT_CLAN_JOIN_DAYS));
               clan.updateClanInDB();
               SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.CLAN_MEMBER_S1_HAS_BEEN_EXPELLED);
               sm.addString(member.getName());
               clan.broadcastToOnlineMembers(sm);
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_SUCCEEDED_IN_EXPELLING_THE_CLAN_MEMBER);
               player.sendPacket(
                  org.l2jmobius.gameserver.network.SystemMessageId.AFTER_A_CLAN_MEMBER_IS_DISMISSED_FROM_A_CLAN_THE_CLAN_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_ACCEPTING_A_NEW_MEMBER
               );
               clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(this._target));
               if (member.isOnline()) {
                  Player target = member.getPlayer();
                  target.sendPacket(
                     org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS
                  );
               }
            }
         }
      }
   }
}
