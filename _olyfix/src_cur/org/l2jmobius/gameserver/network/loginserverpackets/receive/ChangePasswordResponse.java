package org.l2jmobius.gameserver.network.loginserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;

public class ChangePasswordResponse extends SimpleReadablePacket {
   public ChangePasswordResponse(byte[] decrypt) {
      super(decrypt);
      this.readByte();
      String character = this.readString();
      String msgToSend = this.readString();
      Player player = World.getPlayer(character);
      if (player != null) {
         player.sendMessage(msgToSend);
      }
   }
}
