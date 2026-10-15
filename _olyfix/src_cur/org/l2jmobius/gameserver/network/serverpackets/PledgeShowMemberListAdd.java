package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeShowMemberListAdd extends ServerPacket {
   private final String _name;
   private final int _level;
   private final int _classId;
   private final int _isOnline;
   private final int _pledgeType;

   public PledgeShowMemberListAdd(Player player) {
      this._name = player.getName();
      this._level = player.getLevel();
      this._classId = player.getPlayerClass().getId();
      this._isOnline = player.isOnline() ? player.getObjectId() : 0;
      this._pledgeType = player.getPledgeType();
   }

   public PledgeShowMemberListAdd(ClanMember cm) {
      this._name = cm.getName();
      this._level = cm.getLevel();
      this._classId = cm.getClassId();
      this._isOnline = cm.isOnline() ? cm.getObjectId() : 0;
      this._pledgeType = cm.getPledgeType();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.PLEDGE_SHOW_MEMBER_LIST_ADD.writeId(this, buffer);
      buffer.writeString(this._name);
      buffer.writeInt(this._level);
      buffer.writeInt(this._classId);
      buffer.writeInt(0);
      buffer.writeInt(1);
      buffer.writeInt(this._isOnline);
      buffer.writeInt(this._pledgeType);
   }
}
