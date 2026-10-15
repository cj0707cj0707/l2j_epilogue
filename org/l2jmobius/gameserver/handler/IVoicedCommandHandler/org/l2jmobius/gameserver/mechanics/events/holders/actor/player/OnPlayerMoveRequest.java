package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerMoveRequest implements IBaseEvent {
   private final Player _player;
   private final Location _location;

   public OnPlayerMoveRequest(Player player, Location loc) {
      this._player = player;
      this._location = loc;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Location getLocation() {
      return this._location;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_MOVE_REQUEST;
   }
}
