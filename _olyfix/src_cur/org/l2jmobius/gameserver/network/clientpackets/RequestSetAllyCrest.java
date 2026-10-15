package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.CrestTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.Crest;
import org.l2jmobius.gameserver.entity.clan.enums.CrestType;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class RequestSetAllyCrest extends ClientPacket {
   private int _length;
   private byte[] _data = null;

   @Override
   protected void readImpl() {
      this._length = this.readInt();
      if (this._length <= 192) {
         this._data = this.readBytes(this._length);
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (this._length < 0) {
            player.sendMessage("File transfer error.");
         } else if (this._length > 192) {
            player.sendPacket(SystemMessageId.PLEASE_ADJUST_THE_IMAGE_SIZE_TO_8X12);
         } else if (player.getAllyId() == 0) {
            player.sendPacket(SystemMessageId.THIS_FEATURE_IS_ONLY_AVAILABLE_ALLIANCE_LEADERS);
         } else {
            Clan leaderClan = ClanTable.getInstance().getClan(player.getAllyId());
            if (player.getClanId() == leaderClan.getId() && player.isClanLeader()) {
               if (this._length == 0) {
                  if (leaderClan.getAllyCrestId() != 0) {
                     leaderClan.changeAllyCrest(0, false);
                  }
               } else {
                  Crest crest = CrestTable.getInstance().createCrest(this._data, CrestType.ALLY);
                  if (crest != null) {
                     leaderClan.changeAllyCrest(crest.getId(), false);
                     player.sendMessage("The crest was successfully registered.");
                  }
               }
            } else {
               player.sendPacket(SystemMessageId.THIS_FEATURE_IS_ONLY_AVAILABLE_ALLIANCE_LEADERS);
            }
         }
      }
   }
}
