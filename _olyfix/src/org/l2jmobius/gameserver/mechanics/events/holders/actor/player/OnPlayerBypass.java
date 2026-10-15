package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerBypass implements IBaseEvent {
   private final Player _player;
   private final String _command;

   public OnPlayerBypass(Player player, String command) {
      this._player = player;
      this._command = command;
   }

   public Player getPlayer() {
      return this._player;
   }

   public String getCommand() {
      return this._command;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_BYPASS;
   }
}
