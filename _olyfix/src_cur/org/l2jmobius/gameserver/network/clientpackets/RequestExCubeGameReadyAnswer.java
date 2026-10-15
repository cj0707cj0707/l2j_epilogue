package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.network.PacketLogger;

public class RequestExCubeGameReadyAnswer extends ClientPacket {
   private int _arena;
   private int _answer;

   @Override
   protected void readImpl() {
      this._arena = this.readInt() + 1;
      this._answer = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         switch (this._answer) {
            case 0:
               break;
            case 1:
               HandysBlockCheckerManager.getInstance().increaseArenaVotes(this._arena);
               break;
            default:
               PacketLogger.warning("Unknown Cube Game Answer ID: " + this._answer);
         }
      }
   }
}
