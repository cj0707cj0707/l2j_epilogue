package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;

public class RequestExCubeGameChangeTeam extends ClientPacket {
   private int _arena;
   private int _team;

   @Override
   protected void readImpl() {
      this._arena = this.readInt() + 1;
      this._team = this.readInt();
   }

   @Override
   protected void runImpl() {
      if (!HandysBlockCheckerManager.getInstance().arenaIsBeingUsed(this._arena)) {
         Player player = this.getPlayer();
         switch (this._team) {
            case -1:
               int team = HandysBlockCheckerManager.getInstance().getHolder(this._arena).getPlayerTeam(player);
               if (team > -1) {
                  HandysBlockCheckerManager.getInstance().removePlayer(player, this._arena, team);
               }
               break;
            case 0:
            case 1:
               HandysBlockCheckerManager.getInstance().changePlayerToTeam(player, this._arena);
               break;
            default:
               org.l2jmobius.gameserver.network.PacketLogger.warning("Wrong Cube Game Team ID: " + this._team);
         }
      }
   }
}
