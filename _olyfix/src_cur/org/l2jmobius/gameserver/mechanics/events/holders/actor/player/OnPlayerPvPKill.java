package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerPvPKill implements IBaseEvent {
   private final Player _player;
   private final Player _target;

   public OnPlayerPvPKill(Player player, Player target) {
      this._player = player;
      this._target = target;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Player getTarget() {
      return this._target;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_PVP_KILL;
   }
}
