package org.l2jmobius.gameserver.network.loginserverpackets.send;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

public class PlayerInGame extends SimpleWritablePacket {
   private static final int OPCODE = 2;
   private final List<String> _players;

   public PlayerInGame(String player) {
      this(Arrays.asList(player != null ? player : ""));
   }

   public PlayerInGame(List<String> players) {
      this._players = players != null ? players : Collections.emptyList();
   }

   public void write() {
      this.writeByte(2);
      this.writeShort(this._players.size());

      for (String p : this._players) {
         this.writeString(p != null ? p : "");
      }
   }
}
