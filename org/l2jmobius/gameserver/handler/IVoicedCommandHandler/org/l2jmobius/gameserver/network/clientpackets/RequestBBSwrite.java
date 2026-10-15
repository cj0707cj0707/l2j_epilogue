package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;

public class RequestBBSwrite extends ClientPacket {
   private String _url;
   private String _arg1;
   private String _arg2;
   private String _arg3;
   private String _arg4;
   private String _arg5;

   @Override
   protected void readImpl() {
      this._url = this.readString();
      this._arg1 = this.readString();
      this._arg2 = this.readString();
      this._arg3 = this.readString();
      this._arg4 = this.readString();
      this._arg5 = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         CommunityBoardHandler.getInstance().handleWriteCommand(player, this._url, this._arg1, this._arg2, this._arg3, this._arg4, this._arg5);
      }
   }
}
