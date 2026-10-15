package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.CrestTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.clan.Crest;
import org.l2jmobius.gameserver.entity.clan.enums.CrestType;

public class RequestExSetPledgeCrestLarge extends ClientPacket {
   private int _length;
   private byte[] _data = null;

   @Override
   protected void readImpl() {
      this._length = this.readInt();
      if (this._length <= 2176) {
         this._data = this.readBytes(this._length);
      }
   }

   @Override
   protected void runImpl() {
      if (this._data != null) {
         Player player = this.getPlayer();
         if (player != null) {
            Clan clan = player.getClan();
            if (clan != null) {
               if (this._length >= 0 && this._length <= 2176) {
                  if (clan.getDissolvingExpiryTime() > System.currentTimeMillis()) {
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.DURING_THE_DISSOLUTION_SUSPENSION_PERIOD_YOU_CANNOT_REGISTER_OR_DELETE_THE_CLAN_CREST
                     );
                  } else if (!player.hasAccess(ClanAccess.CHANGE_CREST)) {
                     player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
                  } else {
                     if (this._length == 0) {
                        if (clan.getCrestLargeId() != 0) {
                           clan.changeLargeCrest(0);
                           player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_CLAN_S_CREST_HAS_BEEN_DELETED);
                        }
                     } else {
                        if (clan.getLevel() < 3) {
                           player.sendPacket(
                              org.l2jmobius.gameserver.network.SystemMessageId.A_CLAN_CREST_CAN_ONLY_BE_REGISTERED_WHEN_THE_CLAN_S_SKILL_LEVEL_IS_3_OR_ABOVE
                           );
                           return;
                        }

                        Crest crest = CrestTable.getInstance().createCrest(this._data, CrestType.PLEDGE_LARGE);
                        if (crest != null) {
                           clan.changeLargeCrest(crest.getId());
                           player.sendPacket(
                              org.l2jmobius.gameserver.network.SystemMessageId.THE_CLAN_CREST_WAS_SUCCESSFULLY_REGISTERED_REMEMBER_ONLY_A_CLAN_THAT_OWNS_A_CLAN_HALL_OR_CASTLE_CAN_HAVE_THEIR_CREST_DISPLAYED
                           );
                        }
                     }
                  }
               } else {
                  player.sendMessage("The size of the uploaded crest or insignia does not meet the standard requirements.");
               }
            }
         }
      }
   }
}
