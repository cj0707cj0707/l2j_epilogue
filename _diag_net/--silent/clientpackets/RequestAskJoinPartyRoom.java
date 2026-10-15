package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExAskJoinPartyRoom;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestAskJoinPartyRoom extends ClientPacket {
   private String _name;

   @Override
   protected void readImpl() {
      this._name = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player target = World.getPlayer(this._name);
         if (target != null) {
            if (!target.isProcessingRequest()) {
               player.onTransactionRequest(target);
               target.sendPacket(new ExAskJoinPartyRoom(player.getName()));
            } else {
               player.sendPacket(
                  new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER).addPcName(target)
               );
            }
         } else {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
         }
      }
   }
}
