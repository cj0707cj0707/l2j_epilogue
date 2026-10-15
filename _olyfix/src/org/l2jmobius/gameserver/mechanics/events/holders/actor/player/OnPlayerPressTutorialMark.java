package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerPressTutorialMark implements IBaseEvent {
   private final Player _player;
   private final int _markId;

   public OnPlayerPressTutorialMark(Player player, int markId) {
      this._player = player;
      this._markId = markId;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getMarkId() {
      return this._markId;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_PRESS_TUTORIAL_MARK;
   }
}
