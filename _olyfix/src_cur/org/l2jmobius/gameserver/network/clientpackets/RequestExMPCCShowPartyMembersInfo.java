package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExMPCCShowPartyMemberInfo;

public class RequestExMPCCShowPartyMembersInfo extends ClientPacket {
   private int _partyLeaderId;

   @Override
   protected void readImpl() {
      this._partyLeaderId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player target = World.getPlayer(this._partyLeaderId);
         if (target != null && target.getParty() != null) {
            player.sendPacket(new ExMPCCShowPartyMemberInfo(target.getParty()));
         }
      }
   }
}
