package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerLogout implements IBaseEvent {
   private final Player _player;

   public OnPlayerLogout(Player player) {
      this._player = player;
   }

   public Player getPlayer() {
      return this._player;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_LOGOUT;
   }
}
