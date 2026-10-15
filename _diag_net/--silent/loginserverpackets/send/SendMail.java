package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class SendMail extends SimpleWritablePacket {
   private static final int OPCODE = 9;
   private final String _accountName;
   private final String _mailId;
   private final String[] _args;

   public SendMail(String accountName, String mailId, String... args) {
      this._accountName = accountName != null ? accountName : "";
      this._mailId = mailId != null ? mailId : "";
      this._args = args != null ? args : new String[0];
   }

   public void write() {
      this.writeByte(9);
      this.writeString(this._accountName);
      this.writeString(this._mailId);
      this.writeByte(this._args.length);

      for (String a : this._args) {
         this.writeString(a != null ? a : "");
      }
   }
}
