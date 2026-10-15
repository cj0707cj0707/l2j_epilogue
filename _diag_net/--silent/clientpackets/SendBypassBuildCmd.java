package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;

public class SendBypassBuildCmd extends ClientPacket {
   public static final int GM_MESSAGE = 9;
   public static final int ANNOUNCEMENT = 10;
   private String _command;

   @Override
   protected void readImpl() {
      this._command = this.readString();
      if (this._command != null) {
         this._command = this._command.trim();
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         AdminCommandHandler.getInstance().onCommand(player, "admin_" + this._command, true);
      }
   }
}
