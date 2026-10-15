package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanInfo;
import org.l2jmobius.gameserver.network.serverpackets.AllianceInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestAllyInfo extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         int allianceId = player.getAllyId();
         if (allianceId > 0) {
            AllianceInfo ai = new AllianceInfo(allianceId);
            player.sendPacket(ai);
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.ALLIANCE_INFORMATION);
            player.sendPacket(sm);
            sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.ALLIANCE_NAME_S1);
            sm.addString(ai.getName());
            player.sendPacket(sm);
            sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.ALLIANCE_LEADER_S2_OF_S1);
            sm.addString(ai.getLeaderC());
            sm.addString(ai.getLeaderP());
            player.sendPacket(sm);
            sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.CONNECTION_S1_TOTAL_S2);
            sm.addInt(ai.getOnline());
            sm.addInt(ai.getTotal());
            player.sendPacket(sm);
            sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.AFFILIATED_CLANS_TOTAL_S1_CLAN_S);
            sm.addInt(ai.getAllies().length);
            player.sendPacket(sm);
            sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.CLAN_INFORMATION);

            for (ClanInfo aci : ai.getAllies()) {
               player.sendPacket(sm);
               sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.CLAN_NAME_S1);
               sm.addString(aci.getClan().getName());
               player.sendPacket(sm);
               sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.CLAN_LEADER_S1);
               sm.addString(aci.getClan().getLeaderName());
               player.sendPacket(sm);
               sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.CLAN_LEVEL_S1);
               sm.addInt(aci.getClan().getLevel());
               player.sendPacket(sm);
               sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.CONNECTION_S1_TOTAL_S2);
               sm.addInt(aci.getOnline());
               sm.addInt(aci.getTotal());
               player.sendPacket(sm);
               sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.EMPTY_4);
            }

            sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.EMPTY_5);
            player.sendPacket(sm);
         } else {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS);
         }
      }
   }
}
