package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

public class RequestStopPledgeWar extends ClientPacket {
   private String _pledgeName;

   @Override
   protected void readImpl() {
      this._pledgeName = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Clan playerClan = player.getClan();
         if (playerClan != null) {
            Clan clan = ClanTable.getInstance().getClanByName(this._pledgeName);
            if (clan == null) {
               player.sendMessage("No such clan.");
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (!playerClan.isAtWarWith(clan.getId())) {
               player.sendMessage("You aren't at war with this clan.");
               player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (!player.hasAccess(ClanAccess.WAR_DECLARATION)) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            } else {
               for (ClanMember member : playerClan.getMembers()) {
                  if (member != null && member.getPlayer() != null && AttackStanceTaskManager.getInstance().hasAttackStanceTask(member.getPlayer())) {
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.A_CEASE_FIRE_DURING_A_CLAN_WAR_CAN_NOT_BE_CALLED_WHILE_MEMBERS_OF_YOUR_CLAN_ARE_ENGAGED_IN_BATTLE
                     );
                     return;
                  }
               }

               ClanTable.getInstance().deleteClanWars(playerClan.getId(), clan.getId());

               for (Player memberx : playerClan.getOnlineMembers(0)) {
                  memberx.broadcastUserInfo();
               }

               for (Player memberx : clan.getOnlineMembers(0)) {
                  memberx.broadcastUserInfo();
               }
            }
         }
      }
   }
}
