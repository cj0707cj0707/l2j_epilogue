package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class AskJoinPledge extends ServerPacket {
   private final int _requestorObjId;
   private final String _subPledgeName;
   private final int _pledgeType;
   private final String _pledgeName;

   public AskJoinPledge(int requestorObjId, String subPledgeName, int pledgeType, String pledgeName) {
      this._requestorObjId = requestorObjId;
      this._subPledgeName = subPledgeName;
      this._pledgeType = pledgeType;
      this._pledgeName = pledgeName;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.ASK_JOIN_PLEDGE.writeId(this, buffer);
      buffer.writeInt(this._requestorObjId);
      if (this._subPledgeName != null) {
         buffer.writeString(this._pledgeType > 0 ? this._subPledgeName : this._pledgeName);
      }

      if (this._pledgeType != 0) {
         buffer.writeInt(this._pledgeType);
      }

      buffer.writeString(this._pledgeName);
   }
}
