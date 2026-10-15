package org.l2jmobius.gameserver.entity.actor.enums.creature;

public enum FenceState {
   HIDDEN(0),
   OPENED(1),
   CLOSED(2),
   CLOSED_HIDDEN(0);

   private final int _clientId;

   private FenceState(int clientId) {
      this._clientId = clientId;
   }

   public int getClientId() {
      return this._clientId;
   }

   public boolean isGeodataEnabled() {
      return this == CLOSED_HIDDEN || this == CLOSED;
   }
}
