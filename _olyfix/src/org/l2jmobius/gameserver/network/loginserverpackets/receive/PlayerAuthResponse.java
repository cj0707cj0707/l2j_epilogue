package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;

public class PlayerAuthResponse extends SimpleReadablePacket {
   private final String _account;
   private final boolean _authed;

   public PlayerAuthResponse(byte[] decrypt) {
      super(decrypt);
      this.readByte();
      this._account = this.readString();
      this._authed = this.readByte() != 0;
   }

   public String getAccount() {
      return this._account;
   }

   public boolean isAuthed() {
      return this._authed;
   }
}
