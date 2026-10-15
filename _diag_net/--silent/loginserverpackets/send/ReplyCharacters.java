package org.l2jmobius.gameserver.network.loginserverpackets.send;

import java.util.Collections;
import java.util.List;
import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class ReplyCharacters extends SimpleWritablePacket {
   private static final int OPCODE = 8;
   private final String _account;
   private final int _chars;
   private final List<Long> _timeToDelete;

   public ReplyCharacters(String account, int chars, List<Long> timeToDel) {
      this._account = account != null ? account : "";
      this._chars = chars;
      this._timeToDelete = timeToDel != null ? timeToDel : Collections.emptyList();
   }

   public void write() {
      this.writeByte(8);
      this.writeString(this._account);
      this.writeByte(this._chars);
      this.writeByte(this._timeToDelete.size());

      for (Long t : this._timeToDelete) {
         this.writeLong(t != null ? t : 0L);
      }
   }
}
