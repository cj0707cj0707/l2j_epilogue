package org.l2jmobius.gameserver.entity.actor.enums.player;

public enum PlayerAction {
   ADMIN_COMMAND,
   ADMIN_SAVE_ZONE,
   ADMIN_SHOW_TERRITORY,
   USER_ENGAGE,
   OFFLINE_TRADE,
   OFFLINE_PLAY;

   private final int _mask = 1 << this.ordinal();

   public int getMask() {
      return this._mask;
   }
}
