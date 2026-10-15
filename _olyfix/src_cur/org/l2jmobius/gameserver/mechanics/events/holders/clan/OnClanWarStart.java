package org.l2jmobius.gameserver.mechanics.events.holders.clan;

import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnClanWarStart implements IBaseEvent {
   private final Clan _clan1;
   private final Clan _clan2;

   public OnClanWarStart(Clan clan1, Clan clan2) {
      this._clan1 = clan1;
      this._clan2 = clan2;
   }

   public Clan getClan1() {
      return this._clan1;
   }

   public Clan getClan2() {
      return this._clan2;
   }

   @Override
   public EventType getType() {
      return EventType.ON_CLAN_WAR_START;
   }
}
