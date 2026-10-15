package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerProfessionCancel implements IBaseEvent {
   private final Player _player;
   private final int _classId;

   public OnPlayerProfessionCancel(Player player, int classId) {
      this._player = player;
      this._classId = classId;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getClassId() {
      return this._classId;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_PROFESSION_CANCEL;
   }
}
