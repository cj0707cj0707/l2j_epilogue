package org.l2jmobius.gameserver.mechanics.events.holders.item;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnItemBypassEvent implements IBaseEvent {
   private final Item _item;
   private final Player _player;
   private final String _event;

   public OnItemBypassEvent(Item item, Player player, String event) {
      this._item = item;
      this._player = player;
      this._event = event;
   }

   public Item getItem() {
      return this._item;
   }

   public Player getPlayer() {
      return this._player;
   }

   public String getEvent() {
      return this._event;
   }

   @Override
   public EventType getType() {
      return EventType.ON_ITEM_BYPASS_EVENT;
   }
}
