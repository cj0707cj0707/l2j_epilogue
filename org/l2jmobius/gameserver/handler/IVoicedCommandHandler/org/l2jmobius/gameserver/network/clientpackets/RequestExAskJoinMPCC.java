package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAskJoinMPCC;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestExAskJoinMPCC extends ClientPacket {
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
            if (!player.isInParty() || !target.isInParty() || !player.getParty().equals(target.getParty())) {
               if (player.isInParty()) {
                  Party activeParty = player.getParty();
                  if (activeParty.getLeader().equals(player)) {
                     if (activeParty.isInCommandChannel() && activeParty.getCommandChannel().getLeader().equals(player)) {
                        if (target.isInParty()) {
                           if (target.getParty().isInCommandChannel()) {
                              SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_PARTY_IS_ALREADY_A_MEMBER_OF_THE_COMMAND_CHANNEL);
                              sm.addString(target.getName());
                              player.sendPacket(sm);
                           } else {
                              this.askJoinMPCC(player, target);
                           }
                        } else {
                           player.sendMessage(target.getName() + " doesn't have party and cannot be invited to Command Channel.");
                        }
                     } else if (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getLeader().equals(player)) {
                        SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_INVITE_SOMEONE_TO_THE_COMMAND_CHANNEL);
                        player.sendPacket(sm);
                     } else if (target.isInParty()) {
                        if (target.getParty().isInCommandChannel()) {
                           SystemMessage sm = new SystemMessage(SystemMessageId.C1_S_PARTY_IS_ALREADY_A_MEMBER_OF_THE_COMMAND_CHANNEL);
                           sm.addString(target.getName());
                           player.sendPacket(sm);
                        } else {
                           this.askJoinMPCC(player, target);
                        }
                     } else {
                        player.sendMessage(target.getName() + " doesn't have party and cannot be invited to Command Channel.");
                     }
                  } else {
                     player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_INVITE_SOMEONE_TO_THE_COMMAND_CHANNEL);
                  }
               }
            }
         }
      }
   }

   private void askJoinMPCC(Player requestor, Player target) {
      boolean hasRight = false;
      if (requestor.isClanLeader() && requestor.getClan().getLevel() >= 5) {
         hasRight = true;
      } else if (requestor.getInventory().getItemByItemId(8871) != null) {
         hasRight = true;
      } else if (requestor.getPledgeClass() >= 5 && requestor.getKnownSkill(391) != null) {
         hasRight = true;
      }

      if (!hasRight) {
         requestor.sendPacket(SystemMessageId.COMMAND_CHANNELS_CAN_ONLY_BE_FORMED_BY_A_PARTY_LEADER_WHO_IS_ALSO_THE_LEADER_OF_A_LEVEL_5_CLAN);
      } else {
         Player targetLeader = target.getParty().getLeader();
         if (!targetLeader.isProcessingRequest()) {
            requestor.onTransactionRequest(targetLeader);
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_INVITING_YOU_TO_THE_COMMAND_CHANNEL_DO_YOU_ACCEPT);
            sm.addString(requestor.getName());
            targetLeader.sendPacket(sm);
            targetLeader.sendPacket(new ExAskJoinMPCC(requestor.getName()));
            requestor.sendMessage("You invited " + targetLeader.getName() + " to your Command Channel.");
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
            sm.addString(targetLeader.getName());
            requestor.sendPacket(sm);
         }
      }
   }
}
