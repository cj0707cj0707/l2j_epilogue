package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestDuelAnswerStart extends ClientPacket {
   private int _partyDuel;
   private int _unk1;
   private int _response;

   @Override
   protected void readImpl() {
      this._partyDuel = this.readInt();
      this._unk1 = this.readInt();
      this._response = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Player requestor = player.getActiveRequester();
         if (requestor != null) {
            if (this._response == 1) {
               SystemMessage msg1 = null;
               SystemMessage msg2 = null;
               if (requestor.isInDuel()) {
                  msg1 = new SystemMessage(SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL);
                  msg1.addString(requestor.getName());
                  player.sendPacket(msg1);
                  return;
               }

               if (player.isInDuel()) {
                  player.sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
                  return;
               }

               if (this._partyDuel == 1) {
                  msg1 = new SystemMessage(SystemMessageId.YOU_HAVE_ACCEPTED_C1_S_CHALLENGE_TO_A_PARTY_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
                  msg1.addString(requestor.getName());
                  msg2 = new SystemMessage(SystemMessageId.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_DUEL_AGAINST_THEIR_PARTY_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
                  msg2.addString(player.getName());
               } else {
                  msg1 = new SystemMessage(SystemMessageId.YOU_HAVE_ACCEPTED_C1_S_CHALLENGE_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
                  msg1.addString(requestor.getName());
                  msg2 = new SystemMessage(SystemMessageId.C1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
                  msg2.addString(player.getName());
               }

               player.sendPacket(msg1);
               requestor.sendPacket(msg2);
               DuelManager.getInstance().addDuel(requestor, player, this._partyDuel);
            } else if (this._response == -1) {
               requestor.sendMessage(player.getName() + " is set to refuse duel requests and cannot receive a duel request.");
            } else {
               SystemMessage msg = null;
               if (this._partyDuel == 1) {
                  msg = new SystemMessage(SystemMessageId.THE_OPPOSING_PARTY_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
               } else {
                  msg = new SystemMessage(SystemMessageId.C1_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
                  msg.addPcName(player);
               }

               requestor.sendPacket(msg);
            }

            player.setActiveRequester(null);
            requestor.onTransactionResponse();
         }
      }
   }
}
