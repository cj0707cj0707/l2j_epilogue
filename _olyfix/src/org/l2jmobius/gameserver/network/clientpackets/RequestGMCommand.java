package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.GMHennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.GMViewCharacterInfo;
import org.l2jmobius.gameserver.network.serverpackets.GMViewItemList;
import org.l2jmobius.gameserver.network.serverpackets.GMViewPledgeInfo;
import org.l2jmobius.gameserver.network.serverpackets.GMViewSkillInfo;
import org.l2jmobius.gameserver.network.serverpackets.GMViewWarehouseWithdrawList;
import org.l2jmobius.gameserver.network.serverpackets.GmViewQuestInfo;

public class RequestGMCommand extends ClientPacket {
   private String _targetName;
   private int _command;

   @Override
   protected void readImpl() {
      this._targetName = this.readString();
      this._command = this.readInt();
   }

   @Override
   protected void runImpl() {
      GameClient client = this.getClient();
      if (client.getPlayer().isGM() && client.getPlayer().getAccessLevel().allowAltG()) {
         Player player = World.getPlayer(this._targetName);
         Clan clan = ClanTable.getInstance().getClanByName(this._targetName);
         if (player != null || clan != null && this._command == 6) {
            switch (this._command) {
               case 1:
                  client.sendPacket(new GMViewCharacterInfo(player));
                  client.sendPacket(new GMHennaInfo(player));
                  break;
               case 2:
                  if (player != null && player.getClan() != null) {
                     client.sendPacket(new GMViewPledgeInfo(player.getClan(), player));
                  }
                  break;
               case 3:
                  client.sendPacket(new GMViewSkillInfo(player));
                  break;
               case 4:
                  client.sendPacket(new GmViewQuestInfo(player));
                  break;
               case 5:
                  client.sendPacket(new GMViewItemList(player));
                  client.sendPacket(new GMHennaInfo(player));
                  break;
               case 6:
                  if (player != null) {
                     client.sendPacket(new GMViewWarehouseWithdrawList(player));
                  } else {
                     client.sendPacket(new GMViewWarehouseWithdrawList(clan));
                  }
            }
         }
      }
   }
}
