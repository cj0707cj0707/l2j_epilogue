package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;

public class KickPlayer extends SimpleReadablePacket {
   private final String _account;

   public KickPlayer(byte[] decrypt) {
      super(decrypt);
      this.readByte();
      this._account = this.readString();
   }

   public String getAccount() {
      return this._account;
   }
}
