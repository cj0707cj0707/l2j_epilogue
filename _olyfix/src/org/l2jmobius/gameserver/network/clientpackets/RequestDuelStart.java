package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelAskStart;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestDuelStart extends ClientPacket {
   private String _player;
   private int _partyDuel;

   @Override
   protected void readImpl() {
      this._player = this.readString();
      this._partyDuel = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player targetChar = World.getPlayer(this._player);
         if (targetChar == null) {
            player.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
         } else if (player == targetChar) {
            player.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
         } else if (!player.canDuel()) {
            player.sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
         } else if (!targetChar.canDuel()) {
            player.sendPacket(targetChar.getNoDuelReason());
         } else if (!player.isInsideRadius2D(targetChar, 250)) {
            SystemMessage msg = new SystemMessage(SystemMessageId.C1_CANNOT_RECEIVE_A_DUEL_CHALLENGE_BECAUSE_C1_IS_TOO_FAR_AWAY);
            msg.addString(targetChar.getName());
            player.sendPacket(msg);
         } else {
            if (this._partyDuel == 1) {
               Party party = player.getParty();
               if (!player.isInParty() || !party.isLeader(player)) {
                  player.sendMessage("You have to be the leader of a party in order to request a party duel.");
                  return;
               }

               if (!targetChar.isInParty()) {
                  player.sendPacket(SystemMessageId.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY);
                  return;
               }

               if (party.containsPlayer(targetChar)) {
                  player.sendMessage("This player is a member of your own party.");
                  return;
               }

               for (Player temp : party.getMembers()) {
                  if (!temp.canDuel()) {
                     player.sendMessage("Not all the members of your party are ready for a duel.");
                     return;
                  }
               }

               Player partyLeader = null;

               for (Player tempx : targetChar.getParty().getMembers()) {
                  if (partyLeader == null) {
                     partyLeader = tempx;
                  }

                  if (!tempx.canDuel()) {
                     player.sendPacket(SystemMessageId.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
                     return;
                  }
               }

               if (partyLeader != null) {
                  if (!partyLeader.isProcessingRequest()) {
                     player.onTransactionRequest(partyLeader);
                     partyLeader.sendPacket(new ExDuelAskStart(player.getName(), this._partyDuel));
                     SystemMessage msg = new SystemMessage(SystemMessageId.C1_S_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL);
                     msg.addString(partyLeader.getName());
                     player.sendPacket(msg);
                     msg = new SystemMessage(SystemMessageId.C1_S_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL);
                     msg.addString(player.getName());
                     targetChar.sendPacket(msg);
                  } else {
                     SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
                     msg.addString(partyLeader.getName());
                     player.sendPacket(msg);
                  }
               }
            } else if (!targetChar.isProcessingRequest()) {
               player.onTransactionRequest(targetChar);
               targetChar.sendPacket(new ExDuelAskStart(player.getName(), this._partyDuel));
               SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_BEEN_CHALLENGED_TO_A_DUEL);
               msg.addString(targetChar.getName());
               player.sendPacket(msg);
               msg = new SystemMessage(SystemMessageId.C1_HAS_CHALLENGED_YOU_TO_A_DUEL);
               msg.addString(player.getName());
               targetChar.sendPacket(msg);
            } else {
               SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
               msg.addString(targetChar.getName());
               player.sendPacket(msg);
            }
         }
      }
   }
}
