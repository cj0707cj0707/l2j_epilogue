package org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clan;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerClanLvlUp implements IBaseEvent {
   private final Player _player;
   private final Clan _clan;

   public OnPlayerClanLvlUp(Player player, Clan clan) {
      this._player = player;
      this._clan = clan;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Clan getClan() {
      return this._clan;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_CLAN_LEVELUP;
   }
}
