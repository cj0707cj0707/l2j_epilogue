package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class RequestGiveNickName extends ClientPacket {
   private String _target;
   private String _title;

   @Override
   protected void readImpl() {
      this._target = this.readString();
      this._title = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isNoble() && this._target.equalsIgnoreCase(player.getName())) {
            player.setTitle(this._title);
            player.sendPacket(SystemMessageId.YOUR_TITLE_HAS_BEEN_CHANGED);
            player.broadcastTitleInfo();
         } else {
            if (!player.hasAccess(ClanAccess.ASSIGN_TITLE)) {
               player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
               return;
            }

            Clan clan = player.getClan();
            if (clan.getLevel() < 3) {
               player.sendPacket(SystemMessageId.A_PLAYER_CAN_ONLY_BE_GRANTED_A_TITLE_IF_THE_CLAN_IS_LEVEL_3_OR_ABOVE);
               return;
            }

            ClanMember member1 = clan.getClanMember(this._target);
            if (member1 != null) {
               Player member = member1.getPlayer();
               if (member != null) {
                  member.setTitle(this._title);
                  member.sendPacket(SystemMessageId.YOUR_TITLE_HAS_BEEN_CHANGED);
                  member.broadcastTitleInfo();
               } else {
                  player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
               }
            } else {
               player.sendPacket(SystemMessageId.THE_TARGET_MUST_BE_A_CLAN_MEMBER);
            }
         }
      }
   }
}
