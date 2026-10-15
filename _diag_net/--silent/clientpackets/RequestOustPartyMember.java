package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;

public class RequestOustPartyMember extends ClientPacket {
   private String _name;

   @Override
   protected void readImpl() {
      this._name = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Party party = player.getParty();
         if (party != null && party.isLeader(player)) {
            if (player.getUCState() != 0) {
               player.sendMessage("You can't dismiss party member when you are in Underground Coliseum.");
            } else if (player.getParty().isInDimensionalRift() && !player.getParty().getDimensionalRift().getRevivedAtWaitingRoom().contains(player)) {
               player.sendMessage("You can't dismiss party member when you are in Dimensional Rift.");
            } else {
               party.removePartyMember(this._name, PartyMessageType.EXPELLED);
            }
         }
      }
   }
}
