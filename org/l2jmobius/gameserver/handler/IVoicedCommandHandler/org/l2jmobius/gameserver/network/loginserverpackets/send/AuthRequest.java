package org.l2jmobius.gameserver.network.loginserverpackets.send;

import java.util.Collections;
import java.util.List;
import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class AuthRequest extends SimpleWritablePacket {
   private static final int OPCODE = 1;
   private final int _id;
   private final boolean _acceptAlternate;
   private final byte[] _hexId;
   private final int _port;
   private final boolean _reserveHost;
   private final int _maxPlayers;
   private final List<String> _subnets;
   private final List<String> _hosts;

   public AuthRequest(int id, boolean acceptAlternate, byte[] hexid, int port, boolean reserveHost, int maxplayer, List<String> subnets, List<String> hosts) {
      this._id = id;
      this._acceptAlternate = acceptAlternate;
      this._hexId = hexid != null ? hexid : new byte[0];
      this._port = port;
      this._reserveHost = reserveHost;
      this._maxPlayers = maxplayer;
      this._subnets = subnets != null ? subnets : Collections.emptyList();
      this._hosts = hosts != null ? hosts : Collections.emptyList();
   }

   @Override
   public void write() {
      int pairs = Math.min(this._subnets.size(), this._hosts.size());
      this.writeByte(1);
      this.writeByte(this._id);
      this.writeByte(this._acceptAlternate ? 1 : 0);
      this.writeByte(this._reserveHost ? 1 : 0);
      this.writeShort(this._port);
      this.writeInt(this._maxPlayers);
      this.writeInt(this._hexId.length);
      this.writeBytes(this._hexId);
      this.writeInt(pairs);

      for (int i = 0; i < pairs; i++) {
         this.writeString(this._subnets.get(i) != null ? this._subnets.get(i) : "");
         this.writeString(this._hosts.get(i) != null ? this._hosts.get(i) : "");
      }
   }
}
