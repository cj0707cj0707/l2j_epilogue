package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerDlgAnswer implements IBaseEvent {
   private final Player _player;
   private final int _messageId;
   private final int _answer;
   private final int _requesterId;

   public OnPlayerDlgAnswer(Player player, int messageId, int answer, int requesterId) {
      this._player = player;
      this._messageId = messageId;
      this._answer = answer;
      this._requesterId = requesterId;
   }

   public Player getPlayer() {
      return this._player;
   }

   public int getMessageId() {
      return this._messageId;
   }

   public int getAnswer() {
      return this._answer;
   }

   public int getRequesterId() {
      return this._requesterId;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_DLG_ANSWER;
   }
}
