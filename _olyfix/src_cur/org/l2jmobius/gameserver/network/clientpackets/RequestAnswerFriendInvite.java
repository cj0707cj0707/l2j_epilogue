package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.FriendPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestAnswerFriendInvite extends ClientPacket {
   private int _response;

   @Override
   protected void readImpl() {
      this._response = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player requestor = player.getActiveRequester();
         if (requestor != null) {
            if (!player.getFriendList().contains(requestor.getObjectId()) && !requestor.getFriendList().contains(player.getObjectId())) {
               if (this._response == 1) {
                  try (
                     Connection con = DatabaseFactory.getConnection();
                     PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (charId, friendId) VALUES (?, ?), (?, ?)");
                  ) {
                     statement.setInt(1, requestor.getObjectId());
                     statement.setInt(2, player.getObjectId());
                     statement.setInt(3, player.getObjectId());
                     statement.setInt(4, requestor.getObjectId());
                     statement.execute();
                     SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_INVITING_A_FRIEND_TO_YOUR_FRIENDS_LIST);
                     requestor.sendPacket(msg);
                     msg = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED_TO_YOUR_FRIENDS_LIST);
                     msg.addString(player.getName());
                     requestor.sendPacket(msg);
                     requestor.getFriendList().add(player.getObjectId());
                     msg = new SystemMessage(SystemMessageId.S1_HAS_JOINED_AS_A_FRIEND);
                     msg.addString(requestor.getName());
                     player.sendPacket(msg);
                     player.getFriendList().add(requestor.getObjectId());
                     player.sendPacket(new FriendPacket(true, requestor.getObjectId()));
                     requestor.sendPacket(new FriendPacket(true, player.getObjectId()));
                  } catch (Exception var11) {
                     PacketLogger.warning("Could not add friend objectid: " + var11.getMessage());
                  }
               } else {
                  requestor.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_ADD_A_FRIEND_TO_YOUR_FRIENDS_LIST));
               }

               player.setActiveRequester(null);
               requestor.onTransactionResponse();
            } else {
               requestor.sendPacket(SystemMessageId.THIS_PLAYER_IS_ALREADY_REGISTERED_IN_YOUR_FRIENDS_LIST);
            }
         }
      }
   }
}
