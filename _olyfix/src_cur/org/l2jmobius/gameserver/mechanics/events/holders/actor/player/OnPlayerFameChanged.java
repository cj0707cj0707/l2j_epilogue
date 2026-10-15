package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerFameChanged implements IBaseEvent {
   private final Player _player;
   private final int _oldFame;
   private final int _newFame;

   public OnPlayerFameChanged(Player player, int oldFame, int newFame) {
      this._player = player;
      this._oldFame = oldFame;
      this._newFame = newFame;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getOldFame() {
      return this._oldFame;
   }

   public int getNewFame() {
      return this._newFame;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_FAME_CHANGED;
   }
}
