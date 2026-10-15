package org.l2jmobius.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class FriendListExtended extends ServerPacket {
   private final List<FriendListExtended.FriendInfo> _info;

   public FriendListExtended(Player player) {
      this._info = new ArrayList<>(player.getFriendList().size());

      for (int objId : player.getFriendList()) {
         String name = CharInfoTable.getInstance().getNameById(objId);
         Player player1 = World.getPlayer(objId);
         boolean online = false;
         int classid = 0;
         int level = 0;
         if (player1 == null) {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("SELECT char_name, online, classid, level FROM characters WHERE charId = ?");
            ) {
               statement.setInt(1, objId);

               try (ResultSet rset = statement.executeQuery()) {
                  if (rset.next()) {
                     this._info.add(new FriendListExtended.FriendInfo(objId, rset.getString(1), rset.getInt(2) == 1, rset.getInt(3), rset.getInt(4)));
                  }
               }
            } catch (Exception var20) {
            }
         } else {
            if (player1.isOnline()) {
               online = true;
            }

            classid = player1.getPlayerClass().getId();
            level = player1.getLevel();
            this._info.add(new FriendListExtended.FriendInfo(objId, name, online, classid, level));
         }
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.FRIEND_LIST.writeId(this, buffer);
      buffer.writeInt(this._info.size());

      for (FriendListExtended.FriendInfo info : this._info) {
         buffer.writeInt(info._objId);
         buffer.writeString(info._name);
         buffer.writeInt(info._online);
         buffer.writeInt(info._online ? info._objId : 0);
         buffer.writeInt(info._classid);
         buffer.writeInt(info._level);
      }
   }

   private static class FriendInfo {
      int _objId;
      String _name;
      boolean _online;
      int _classid;
      int _level;

      public FriendInfo(int objId, String name, boolean online, int classid, int level) {
         this._objId = objId;
         this._name = name;
         this._online = online;
         this._classid = classid;
         this._level = level;
      }
   }
}
