package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;
import org.l2jmobius.gameserver.LoginServerThread.SessionKey;

public class PlayerAuthRequest extends SimpleWritablePacket {
   private static final int OPCODE = 5;
   private final String _account;
   private final SessionKey _key;

   public PlayerAuthRequest(String account, SessionKey key) {
      this._account = account != null ? account : "";
      this._key = key;
   }

   public void write() {
      this.writeByte(5);
      this.writeString(this._account);
      this.writeInt(this._key != null ? this._key.playOkID1 : 0);
      this.writeInt(this._key != null ? this._key.playOkID2 : 0);
      this.writeInt(this._key != null ? this._key.loginOkID1 : 0);
      this.writeInt(this._key != null ? this._key.loginOkID2 : 0);
   }
}
