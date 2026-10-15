package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestFriendList extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         player.sendPacket(SystemMessageId.FRIENDS_LIST);
         Player friend = null;

         for (int id : player.getFriendList()) {
            String friendName = CharInfoTable.getInstance().getNameById(id);
            if (friendName != null) {
               friend = World.getPlayer(friendName);
               SystemMessage sm;
               if (friend != null && friend.isOnline()) {
                  sm = new SystemMessage(SystemMessageId.S1_CURRENTLY_ONLINE);
                  sm.addString(friendName);
               } else {
                  sm = new SystemMessage(SystemMessageId.S1_CURRENTLY_OFFLINE);
                  sm.addString(friendName);
               }

               player.sendPacket(sm);
            }
         }

         player.sendPacket(SystemMessageId.EMPTY_3);
      }
   }
}
