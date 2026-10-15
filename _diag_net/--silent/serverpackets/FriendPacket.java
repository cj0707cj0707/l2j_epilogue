package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.entity.World;

public class FriendPacket extends ServerPacket {
   private final boolean _action;
   private final boolean _online;
   private final int _objid;
   private final String _name;

   public FriendPacket(boolean action, int objId) {
      this._action = action;
      this._objid = objId;
      this._name = CharInfoTable.getInstance().getNameById(objId);
      this._online = World.getPlayer(objId) != null;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.L2_FRIEND.writeId(this, buffer);
      buffer.writeInt(this._action ? 1 : 3);
      buffer.writeInt(this._objid);
      buffer.writeString(this._name);
      buffer.writeInt(this._online);
      buffer.writeInt(this._online ? this._objid : 0);
   }
}
