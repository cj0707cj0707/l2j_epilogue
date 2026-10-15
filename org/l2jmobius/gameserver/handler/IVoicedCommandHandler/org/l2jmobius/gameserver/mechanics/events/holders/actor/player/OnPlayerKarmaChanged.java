package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerKarmaChanged implements IBaseEvent {
   private final Player _player;
   private final int _oldKarma;
   private final int _newKarma;

   public OnPlayerKarmaChanged(Player player, int oldKarma, int newKarma) {
      this._player = player;
      this._oldKarma = oldKarma;
      this._newKarma = newKarma;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getOldKarma() {
      return this._oldKarma;
   }

   public int getNewKarma() {
      return this._newKarma;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_KARMA_CHANGED;
   }
}
