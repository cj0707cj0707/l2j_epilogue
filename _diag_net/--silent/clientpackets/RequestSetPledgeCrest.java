package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.CrestTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.Crest;
import org.l2jmobius.gameserver.entity.clan.enums.CrestType;

public class RequestSetPledgeCrest extends ClientPacket {
   private int _length;
   private byte[] _data = null;

   @Override
   protected void readImpl() {
      this._length = this.readInt();
      if (this._length <= 256) {
         this._data = this.readBytes(this._length);
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._length < 0) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_SIZE_OF_THE_IMAGE_FILE_IS_INAPPROPRIATE_PLEASE_ADJUST_TO_16_12);
         } else if (this._length > 256) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_SIZE_OF_THE_IMAGE_FILE_IS_INAPPROPRIATE_PLEASE_ADJUST_TO_16_12);
         } else {
            Clan clan = player.getClan();
            if (clan != null) {
               if (clan.getDissolvingExpiryTime() > System.currentTimeMillis()) {
                  player.sendPacket(
                     org.l2jmobius.gameserver.network.SystemMessageId.DURING_THE_DISSOLUTION_SUSPENSION_PERIOD_YOU_CANNOT_REGISTER_OR_DELETE_THE_CLAN_CREST
                  );
               } else if (!player.hasAccess(ClanAccess.CHANGE_CREST)) {
                  player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
               } else {
                  if (this._length == 0) {
                     if (clan.getCrestId() != 0) {
                        clan.changeClanCrest(0);
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_CLAN_S_CREST_HAS_BEEN_DELETED);
                     }
                  } else {
                     if (clan.getLevel() < 3) {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.A_CLAN_CREST_CAN_ONLY_BE_REGISTERED_WHEN_THE_CLAN_S_SKILL_LEVEL_IS_3_OR_ABOVE
                        );
                        return;
                     }

                     Crest crest = CrestTable.getInstance().createCrest(this._data, CrestType.PLEDGE);
                     if (crest != null) {
                        clan.changeClanCrest(crest.getId());
                        player.sendMessage("The crest was successfully registered.");
                     }
                  }
               }
            }
         }
      }
   }
}
