package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExChangePostState extends ServerPacket {
   private final boolean _receivedBoard;
   private final int[] _changedMsgIds;
   private final int _changeId;

   public ExChangePostState(boolean receivedBoard, int[] changedMsgIds, int changeId) {
      this._receivedBoard = receivedBoard;
      this._changedMsgIds = changedMsgIds;
      this._changeId = changeId;
   }

   public ExChangePostState(boolean receivedBoard, int changedMsgId, int changeId) {
      this._receivedBoard = receivedBoard;
      this._changedMsgIds = new int[]{changedMsgId};
      this._changeId = changeId;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_CHANGE_POST_STATE.writeId(this, buffer);
      buffer.writeInt(this._receivedBoard);
      buffer.writeInt(this._changedMsgIds.length);

      for (int postId : this._changedMsgIds) {
         buffer.writeInt(postId);
         buffer.writeInt(this._changeId);
      }
   }
}
