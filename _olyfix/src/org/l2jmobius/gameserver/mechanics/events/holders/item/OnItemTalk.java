package org.l2jmobius.gameserver.mechanics.events.holders.item;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnItemTalk implements IBaseEvent {
   private final Item _item;
   private final Player _player;

   public OnItemTalk(Item item, Player player) {
      this._item = item;
      this._player = player;
   }

   public Item getItem() {
      return this._item;
   }

   public Player getPlayer() {
      return this._player;
   }

   @Override
   public EventType getType() {
      return EventType.ON_ITEM_TALK;
   }
}
