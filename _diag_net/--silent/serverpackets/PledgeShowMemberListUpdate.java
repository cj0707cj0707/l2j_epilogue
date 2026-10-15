package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanMember;

public class PledgeShowMemberListUpdate extends ServerPacket {
   private final int _pledgeType;
   private boolean _hasSponsor;
   private final String _name;
   private final int _level;
   private final int _classId;
   private final int _objectId;
   private final boolean _isOnline;
   private final int _race;
   private final boolean _female;

   public PledgeShowMemberListUpdate(Player player) {
      this._pledgeType = player.getPledgeType();
      if (this._pledgeType == -1) {
         this._hasSponsor = player.getSponsor() != 0;
      } else {
         this._hasSponsor = false;
      }

      this._name = player.getName();
      this._level = player.getLevel();
      this._classId = player.getPlayerClass().getId();
      this._race = player.getRace().ordinal();
      this._female = player.getAppearance().isFemale();
      this._objectId = player.getObjectId();
      this._isOnline = player.isOnline();
   }

   public PledgeShowMemberListUpdate(ClanMember member) {
      this._name = member.getName();
      this._level = member.getLevel();
      this._classId = member.getClassId();
      this._objectId = member.getObjectId();
      this._isOnline = member.isOnline();
      this._pledgeType = member.getPledgeType();
      this._race = member.getRaceOrdinal();
      this._female = member.getSex();
      if (this._pledgeType == -1) {
         this._hasSponsor = member.getSponsor() != 0;
      } else {
         this._hasSponsor = false;
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PLEDGE_SHOW_MEMBER_LIST_UPDATE.writeId(this, buffer);
      buffer.writeString(this._name);
      buffer.writeInt(this._level);
      buffer.writeInt(this._classId);
      buffer.writeInt(this._female);
      buffer.writeInt(this._race);
      if (this._isOnline) {
         buffer.writeInt(this._objectId);
         buffer.writeInt(this._pledgeType);
      } else {
         buffer.writeInt(0);
         buffer.writeInt(0);
      }

      buffer.writeInt(this._hasSponsor);
   }
}
