package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;

public class AuthResponse extends SimpleReadablePacket {
   private final int _serverId;
   private final String _serverName;

   public AuthResponse(byte[] decrypt) {
      super(decrypt);
      this.readByte();
      this._serverId = this.readByte();
      this._serverName = this.readString();
   }

   public int getServerId() {
      return this._serverId;
   }

   public String getServerName() {
      return this._serverName;
   }
}
