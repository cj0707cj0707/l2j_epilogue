package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

import org.l2jmobius.gameserver.entity.groups.Party;

public class UCWaiting {
   private final Party _party;
   private long _registerMillis;
   private final UCArena _baseArena;

   public UCWaiting(Party party, UCArena baseArena) {
      this._party = party;
      this._baseArena = baseArena;
   }

   public void clean() {
      this._registerMillis = 0L;
   }

   public UCArena getBaseArena() {
      return this._baseArena;
   }

   public Party getParty() {
      if (this._party != null && this._party.getLeader() == null) {
         this.setParty(false);
      }

      return this._party;
   }

   public void setParty(boolean isActive) {
      if (isActive) {
         this._party.setUCState(this);
      } else {
         this._party.setUCState(null);
      }
   }

   public void hasRegisterdNow() {
      this._registerMillis = System.currentTimeMillis();
   }

   public long getRegisterMillis() {
      return this._registerMillis;
   }
}
