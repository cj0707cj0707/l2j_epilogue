package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerPKChanged implements IBaseEvent {
   private final Player _player;
   private final int _oldPoints;
   private final int _newPoints;

   public OnPlayerPKChanged(Player player, int oldPoints, int newPoints) {
      this._player = player;
      this._oldPoints = oldPoints;
      this._newPoints = newPoints;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getOldPoints() {
      return this._oldPoints;
   }

   public int getNewPoints() {
      return this._newPoints;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_PK_CHANGED;
   }
}
