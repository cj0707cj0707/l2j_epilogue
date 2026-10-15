package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinParty;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestJoinParty extends ClientPacket {
   private String _name;
   private int _partyDistributionTypeId;

   @Override
   protected void readImpl() {
      this._name = this.readString();
      this._partyDistributionTypeId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player requestor = this.getPlayer();
      if (requestor != null) {
         Player target = World.getPlayer(this._name);
         if (target == null) {
            requestor.sendPacket(SystemMessageId.YOU_MUST_FIRST_SELECT_A_USER_TO_INVITE_TO_YOUR_PARTY);
         } else if (target.getClient() == null || target.getClient().isDetached()) {
            requestor.sendMessage("Player is in offline mode.");
         } else if (requestor.isPartyBanned()) {
            requestor.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_PARTICIPATING_IN_A_PARTY_IS_NOT_ALLOWED);
            requestor.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (target.isPartyBanned()) {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_CANNOT_JOIN_A_PARTY);
            sm.addString(target.getName());
            requestor.sendPacket(sm);
         } else if (!target.isVisibleFor(requestor)) {
            requestor.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
         } else {
            if (requestor.isRegisteredOnEvent() || target.isRegisteredOnEvent()) {
               if (!GeneralConfig.ALLOW_PARTY_IN_SAME_EVENT) {
                  requestor.sendMessage("Event paticipants cannot be invited to parties.");
                  return;
               }

               if (requestor.getInstanceId() != target.getInstanceId() || !requestor.isRegisteredOnEvent() || !target.isRegisteredOnEvent()) {
                  requestor.sendMessage("Event paticipants cannot be invited to parties.");
                  return;
               }

               if (!requestor.getTeam().equals(target.getTeam())) {
                  requestor.sendMessage("You cannot be invited to a party of another team.");
                  return;
               }
            }

            if (target.isInParty()) {
               SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED);
               sm.addString(target.getName());
               requestor.sendPacket(sm);
            } else if (BlockList.isBlocked(target, requestor)) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_PLACED_YOU_ON_HIS_HER_IGNORE_LIST);
               sm.addString(target.getName());
               requestor.sendPacket(sm);
            } else if (target == requestor) {
               requestor.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
            } else if (target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped()) {
               requestor.sendPacket(SystemMessageId.INVALID_TARGET);
            } else if (target.isJailed() || requestor.isJailed()) {
               requestor.sendMessage("You cannot invite a player while is in Jail.");
            } else if ((target.isInOlympiadMode() || requestor.isInOlympiadMode())
               && (
                  target.isInOlympiadMode() != requestor.isInOlympiadMode()
                     || target.getOlympiadGameId() != requestor.getOlympiadGameId()
                     || target.getOlympiadSide() != requestor.getOlympiadSide()
               )) {
               requestor.sendPacket(SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
            } else if (requestor.isProcessingRequest()) {
               requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
            } else if (target.isProcessingRequest()) {
               SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
               sm.addString(target.getName());
               requestor.sendPacket(sm);
            } else {
               Party party = requestor.getParty();
               if (party != null && !party.isLeader(requestor)) {
                  requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
               } else {
                  SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_INVITED_TO_THE_PARTY);
                  sm.addString(target.getName());
                  requestor.sendPacket(sm);
                  if (!requestor.isInParty()) {
                     this.createNewParty(target, requestor);
                  } else if (requestor.getParty().isInDimensionalRift()) {
                     requestor.sendMessage("You cannot invite a player when you are in the Dimensional Rift.");
                  } else {
                     this.addTargetToParty(target, requestor);
                  }
               }
            }
         }
      }
   }

   private void addTargetToParty(Player target, Player requestor) {
      Party party = requestor.getParty();
      if (!party.isLeader(requestor)) {
         requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
      } else if (party.getMemberCount() >= 9) {
         requestor.sendPacket(SystemMessageId.THE_PARTY_IS_FULL);
      } else if (party.getPendingInvitation() && !party.isInvitationRequestExpired()) {
         requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
      } else {
         if (!target.isProcessingRequest()) {
            requestor.onTransactionRequest(target);
            target.sendPacket(new AskJoinParty(requestor.getName(), party.getDistributionType()));
            party.setPendingInvitation(true);
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
            sm.addString(target.getName());
            requestor.sendPacket(sm);
         }
      }
   }

   private void createNewParty(Player target, Player requestor) {
      PartyDistributionType partyDistributionType = PartyDistributionType.findById(this._partyDistributionTypeId);
      if (partyDistributionType != null) {
         if (!target.isProcessingRequest()) {
            target.sendPacket(new AskJoinParty(requestor.getName(), partyDistributionType));
            target.setActiveRequester(requestor);
            requestor.onTransactionRequest(target);
            requestor.setPartyDistributionType(partyDistributionType);
         } else {
            requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
         }
      }
   }
}
