package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerProfessionChange implements IBaseEvent {
   private final Player _player;
   private final PlayerTemplate _template;
   private final boolean _isSubClass;

   public OnPlayerProfessionChange(Player player, PlayerTemplate template, boolean isSubClass) {
      this._player = player;
      this._template = template;
      this._isSubClass = isSubClass;
   }

   public Player getPlayer() {
      return this._player;
   }

   public PlayerTemplate getTemplate() {
      return this._template;
   }

   public boolean isSubClass() {
      return this._isSubClass;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_PROFESSION_CHANGE;
   }
}
