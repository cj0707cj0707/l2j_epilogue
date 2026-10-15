package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class TempBan extends SimpleWritablePacket {
   private static final int OPCODE = 10;
   private static final long MINUTE_MILLIS = 60000L;
   private final String _accountName;
   private final String _ip;
   private final long _minutes;

   public TempBan(String accountName, String ip, long time) {
      this._accountName = accountName != null ? accountName : "";
      this._ip = ip != null ? ip : "";
      this._minutes = time > 0L ? time : 0L;
   }

   @Override
   public void write() {
      this.writeByte(10);
      this.writeString(this._accountName);
      this.writeString(this._ip);
      this.writeLong(System.currentTimeMillis() + this._minutes * 60000L);
      this.writeByte(0);
   }
}
