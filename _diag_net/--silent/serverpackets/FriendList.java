package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;

public class FriendList extends ServerPacket {
   private final List<FriendList.FriendInfo> _info;

   public FriendList(Player player) {
      this._info = new ArrayList<>(player.getFriendList().size());

      for (int objId : player.getFriendList()) {
         String name = CharInfoTable.getInstance().getNameById(objId);
         Player player1 = World.getPlayer(objId);
         boolean online = false;
         if (player1 != null && player1.isOnline()) {
            online = true;
         }

         this._info.add(new FriendList.FriendInfo(objId, name, online));
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.L2_FRIEND_LIST.writeId(this, buffer);
      buffer.writeInt(this._info.size());

      for (FriendList.FriendInfo info : this._info) {
         buffer.writeInt(info._objId);
         buffer.writeString(info._name);
         buffer.writeInt(info._online);
         buffer.writeInt(info._online ? info._objId : 0);
      }
   }

   private static class FriendInfo {
      int _objId;
      String _name;
      boolean _online;

      public FriendInfo(int objId, String name, boolean online) {
         this._objId = objId;
         this._name = name;
         this._online = online;
      }
   }
}
