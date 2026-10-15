package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class ChangeAccessLevel extends SimpleWritablePacket {
   private static final int OPCODE = 4;
   private final String _player;
   private final int _access;

   public ChangeAccessLevel(String player, int access) {
      this._player = player != null ? player : "";
      this._access = access;
   }

   @Override
   public void write() {
      this.writeByte(4);
      this.writeInt(this._access);
      this.writeString(this._player);
   }
}
