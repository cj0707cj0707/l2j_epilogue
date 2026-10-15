package org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clan;

import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerClanDestroy implements IBaseEvent {
   private final ClanMember _clanMember;
   private final Clan _clan;

   public OnPlayerClanDestroy(ClanMember clanMember, Clan clan) {
      this._clanMember = clanMember;
      this._clan = clan;
   }

   public ClanMember getClanMember() {
      return this._clanMember;
   }

   public Clan getClan() {
      return this._clan;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_CLAN_DESTROY;
   }
}
