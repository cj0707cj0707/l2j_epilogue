package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class ChangePassword extends SimpleWritablePacket {
   private static final int OPCODE = 11;
   private final String _accountName;
   private final String _characterName;
   private final String _oldPass;
   private final String _newPass;

   public ChangePassword(String accountName, String characterName, String oldPass, String newPass) {
      this._accountName = accountName != null ? accountName : "";
      this._characterName = characterName != null ? characterName : "";
      this._oldPass = oldPass != null ? oldPass : "";
      this._newPass = newPass != null ? newPass : "";
   }

   public void write() {
      this.writeByte(11);
      this.writeString(this._accountName);
      this.writeString(this._characterName);
      this.writeString(this._oldPass);
      this.writeString(this._newPass);
   }
}
