package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerHennaAdd implements IBaseEvent {
   private final Player _player;
   private final Henna _henna;

   public OnPlayerHennaAdd(Player player, Henna henna) {
      this._player = player;
      this._henna = henna;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Henna getHenna() {
      return this._henna;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_HENNA_ADD;
   }
}
