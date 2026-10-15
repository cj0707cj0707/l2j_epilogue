package org.l2jmobius.gameserver.mechanics.petition;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.PetitionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.PetitionVotePacket;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Petition {
   private final long _submitTime = System.currentTimeMillis();
   private final int _id;
   private final PetitionType _type;
   private PetitionState _state = PetitionState.PENDING;
   private final String _content;
   private final Collection<CreatureSay> _messageLog = ConcurrentHashMap.newKeySet();
   private final Player _petitioner;
   private Player _responder;

   public Petition(Player petitioner, String petitionText, int petitionType) {
      this._id = IdManager.getInstance().getNextId();
      this._type = PetitionType.values()[petitionType - 1];
      this._content = petitionText;
      this._petitioner = petitioner;
   }

   public boolean addLogMessage(CreatureSay cs) {
      return this._messageLog.add(cs);
   }

   public Collection<CreatureSay> getLogMessages() {
      return this._messageLog;
   }

   public boolean endPetitionConsultation(PetitionState endState) {
      this.setState(endState);
      if (this._responder != null && this._responder.isOnline()) {
         if (endState == PetitionState.RESPONDER_REJECT) {
            this._petitioner.sendMessage("Your petition was rejected. Please try again later.");
         } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.ENDING_PETITION_CONSULTATION_WITH_C1);
            sm.addString(this._petitioner.getName());
            this._responder.sendPacket(sm);
            if (endState == PetitionState.PETITIONER_CANCEL) {
               sm = new SystemMessage(SystemMessageId.RECEIPT_NO_S1_PETITION_CANCELLED);
               sm.addInt(this._id);
               this._responder.sendPacket(sm);
            }
         }
      }

      if (this._petitioner != null && this._petitioner.isOnline()) {
         this._petitioner.sendPacket(SystemMessageId.THIS_ENDS_THE_GM_PETITION_CONSULTATION_PLEASE_GIVE_US_FEEDBACK_ON_THE_PETITION_SERVICE);
         this._petitioner.sendPacket(PetitionVotePacket.STATIC_PACKET);
      }

      PetitionManager.getInstance().getCompletedPetitions().put(this.getId(), this);
      return PetitionManager.getInstance().getPendingPetitions().remove(this.getId()) != null;
   }

   public String getContent() {
      return this._content;
   }

   public int getId() {
      return this._id;
   }

   public Player getPetitioner() {
      return this._petitioner;
   }

   public Player getResponder() {
      return this._responder;
   }

   public long getSubmitTime() {
      return this._submitTime;
   }

   public PetitionState getState() {
      return this._state;
   }

   public String getTypeAsString() {
      return this._type.toString().replace('_', ' ');
   }

   public void sendPetitionerPacket(ServerPacket responsePacket) {
      if (this._petitioner != null && this._petitioner.isOnline()) {
         this._petitioner.sendPacket(responsePacket);
      }
   }

   public void sendResponderPacket(ServerPacket responsePacket) {
      if (this._responder != null && this._responder.isOnline()) {
         this._responder.sendPacket(responsePacket);
      } else {
         this.endPetitionConsultation(PetitionState.RESPONDER_MISSING);
      }
   }

   public void setState(PetitionState state) {
      this._state = state;
   }

   public void setResponder(Player respondingAdmin) {
      if (this._responder == null) {
         this._responder = respondingAdmin;
      }
   }
}
