package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.FriendPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestFriendDel extends ClientPacket {
   private String _name;

   @Override
   protected void readImpl() {
      this._name = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         int id = CharInfoTable.getInstance().getIdByName(this._name);
         if (id == -1) {
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_IS_NOT_ON_YOUR_FRIEND_LIST);
            sm.addString(this._name);
            player.sendPacket(sm);
         } else if (!player.getFriendList().contains(id)) {
            SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_IS_NOT_ON_YOUR_FRIEND_LIST);
            sm.addString(this._name);
            player.sendPacket(sm);
         } else {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE (charId=? AND friendId=?) OR (charId=? AND friendId=?)");
            ) {
               statement.setInt(1, player.getObjectId());
               statement.setInt(2, id);
               statement.setInt(3, id);
               statement.setInt(4, player.getObjectId());
               statement.execute();
               SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
               sm.addString(this._name);
               player.sendPacket(sm);
               player.getFriendList().remove(id);
               player.sendPacket(new FriendPacket(false, id));
               Player target = World.getPlayer(this._name);
               if (target != null) {
                  target.getFriendList().remove(player.getObjectId());
                  target.sendPacket(new FriendPacket(false, player.getObjectId()));
               }
            } catch (Exception var12) {
               org.l2jmobius.gameserver.network.PacketLogger.warning(
                  this.getClass().getSimpleName() + ": Could not delele friend objectId: " + var12.getMessage()
               );
            }
         }
      }
   }
}
