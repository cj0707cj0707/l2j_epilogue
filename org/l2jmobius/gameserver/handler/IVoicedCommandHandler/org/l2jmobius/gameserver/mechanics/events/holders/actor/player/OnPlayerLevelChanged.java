package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerLevelChanged implements IBaseEvent {
   private final Player _player;
   private final int _oldLevel;
   private final int _newLevel;

   public OnPlayerLevelChanged(Player player, int oldLevel, int newLevel) {
      this._player = player;
      this._oldLevel = oldLevel;
      this._newLevel = newLevel;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getOldLevel() {
      return this._oldLevel;
   }

   public int getNewLevel() {
      return this._newLevel;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_LEVEL_CHANGED;
   }
}
