package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ShowBoard extends ServerPacket {
   private final String _content;
   private int _showBoard = 1;

   public ShowBoard(String htmlCode, String id) {
      this._content = id + "\b" + htmlCode;
   }

   public ShowBoard(List<String> arg) {
      StringBuilder builder = new StringBuilder(256).append("1002\b");

      for (String str : arg) {
         builder.append(str).append("\b");
      }

      this._content = builder.toString();
   }

   public ShowBoard() {
      this._showBoard = 0;
      this._content = "";
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.SHOW_BOARD.writeId(this, buffer);
      buffer.writeByte(this._showBoard);
      buffer.writeString("bypass _bbshome");
      buffer.writeString("bypass _bbsgetfav");
      buffer.writeString("bypass _bbsloc");
      buffer.writeString("bypass _bbsclan");
      buffer.writeString("bypass _bbsmemo");
      buffer.writeString("bypass _bbsmail");
      buffer.writeString("bypass _bbsfriends");
      buffer.writeString("bypass bbs_add_fav");
      buffer.writeString(this._content);
   }
}
