package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class OnPlayerChat implements IBaseEvent {
   private final Player _player;
   private final Player _target;
   private final String _text;
   private final ChatType _type;

   public OnPlayerChat(Player player, Player target, String text, ChatType type) {
      this._player = player;
      this._target = target;
      this._text = text;
      this._type = type;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Player getTarget() {
      return this._target;
   }

   public String getText() {
      return this._text;
   }

   public ChatType getChatType() {
      return this._type;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_CHAT;
   }
}
