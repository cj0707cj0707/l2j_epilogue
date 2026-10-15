package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class PlayerLogout extends SimpleWritablePacket {
   private static final int OPCODE = 3;
   private final String _player;

   public PlayerLogout(String player) {
      this._player = player != null ? player : "";
   }

   public void write() {
      this.writeByte(3);
      this.writeString(this._player);
   }
}
