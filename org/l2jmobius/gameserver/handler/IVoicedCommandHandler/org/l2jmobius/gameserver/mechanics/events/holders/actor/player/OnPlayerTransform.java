package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerTransform implements IBaseEvent {
   private final Player _player;
   private final int _transformId;

   public OnPlayerTransform(Player player, int transformId) {
      this._player = player;
      this._transformId = transformId;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getTransformId() {
      return this._transformId;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_TRANSFORM;
   }
}
