package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestSurrenderPledgeWar extends ClientPacket {
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
            } else {
               PacketLogger.info("RequestSurrenderPledgeWar by " + player.getClan().getName() + " with " + this._pledgeName);
               if (!playerClan.isAtWarWith(clan.getId())) {
                  player.sendMessage("You aren't at war with this clan.");
                  player.sendPacket(ActionFailed.STATIC_PACKET);
               } else {
                  SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN);
                  msg.addString(this._pledgeName);
                  player.sendPacket(msg);
                  ClanTable.getInstance().deleteClanWars(playerClan.getId(), clan.getId());
               }
            }
         }
      }
   }
}
