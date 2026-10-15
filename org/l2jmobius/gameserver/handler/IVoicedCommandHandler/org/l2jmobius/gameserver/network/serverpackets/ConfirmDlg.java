package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ConfirmDlg extends ServerPacket {
   private int _time;
   private int _requesterId;
   private final SystemMessage _systemMessage;

   public ConfirmDlg(SystemMessageId smId) {
      this._systemMessage = new SystemMessage(smId);
   }

   public ConfirmDlg(int id) {
      this._systemMessage = new SystemMessage(id);
   }

   public ConfirmDlg(String text) {
      this._systemMessage = new SystemMessage(SystemMessageId.S1_3);
      this._systemMessage.addString(text);
   }

   public ConfirmDlg addTime(int time) {
      this._time = time;
      return this;
   }

   public ConfirmDlg addRequesterId(int id) {
      this._requesterId = id;
      return this;
   }

   public SystemMessage getSystemMessage() {
      return this._systemMessage;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CONFIRM_DLG.writeId(this, buffer);
      SystemMessage.SMParam[] params = this._systemMessage.getParams();
      buffer.writeInt(this._systemMessage.getId());
      buffer.writeInt(params.length);

      for (SystemMessage.SMParam param : params) {
         buffer.writeInt(param.getType());
         switch (param.getType()) {
            case 0:
            case 12:
               buffer.writeString(param.getStringValue());
               break;
            case 1:
            case 2:
            case 3:
            case 5:
            case 9:
            case 10:
            case 11:
            case 13:
               buffer.writeInt(param.getIntValue());
               break;
            case 4: {
               int[] array = param.getIntArrayValue();
               buffer.writeInt(array[0]);
               buffer.writeInt(array[1]);
               break;
            }
            case 6:
               buffer.writeLong(param.getLongValue());
               break;
            case 7: {
               int[] array = param.getIntArrayValue();
               buffer.writeInt(array[0]);
               buffer.writeInt(array[1]);
               buffer.writeInt(array[2]);
            }
            case 8:
         }
      }

      buffer.writeInt(this._time);
      buffer.writeInt(this._requesterId);
   }
}
