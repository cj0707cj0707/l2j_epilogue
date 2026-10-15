package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.network.serverpackets.FriendAddRequest;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestFriendInvite extends ClientPacket {
   private String _name;

   @Override
   protected void readImpl() {
      this._name = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player friend = World.getPlayer(this._name);
         if (friend == null || !friend.isOnline() || friend.isInvisible()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_USER_WHO_REQUESTED_TO_BECOME_FRIENDS_IS_NOT_FOUND_IN_THE_GAME);
         } else if (friend == player) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_YOUR_OWN_FRIEND_LIST);
         } else if (player.isInOlympiadMode() || friend.isInOlympiadMode()) {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS
            );
         } else if (BlockList.isBlocked(friend, player)) {
            player.sendMessage("You are in target's block list.");
         } else if (BlockList.isBlocked(player, friend)) {
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_BLOCKED_C1);
            sm.addString(friend.getName());
            player.sendPacket(sm);
         } else if (player.getFriendList().contains(friend.getObjectId())) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_PLAYER_IS_ALREADY_REGISTERED_IN_YOUR_FRIENDS_LIST);
         } else if (friend.isProcessingRequest()) {
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
            sm.addString(this._name);
            player.sendPacket(sm);
         } else {
            player.onTransactionRequest(friend);
            friend.sendPacket(new FriendAddRequest(player.getName()));
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_VE_REQUESTED_C1_TO_BE_ON_YOUR_FRIENDS_LIST);
            sm.addString(this._name);
            player.sendPacket(sm);
         }
      }
   }
}
