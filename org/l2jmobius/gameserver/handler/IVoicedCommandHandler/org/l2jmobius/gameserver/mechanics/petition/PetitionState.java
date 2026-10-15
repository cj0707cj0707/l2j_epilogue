package org.l2jmobius.gameserver.mechanics.petition;

public enum PetitionState {
   PENDING,
   RESPONDER_CANCEL,
   RESPONDER_MISSING,
   RESPONDER_REJECT,
   RESPONDER_COMPLETE,
   PETITIONER_CANCEL,
   PETITIONER_MISSING,
   IN_PROCESS,
   COMPLETED;
}
