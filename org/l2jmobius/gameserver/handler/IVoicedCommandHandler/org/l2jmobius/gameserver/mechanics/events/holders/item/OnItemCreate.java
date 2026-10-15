package org.l2jmobius.gameserver.mechanics.events.holders.item;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnItemCreate implements IBaseEvent {
   private final Item _item;
   private final Creature _creature;
   private final Object _reference;

   public OnItemCreate(Item item, Creature actor, Object reference) {
      this._item = item;
      this._creature = actor;
      this._reference = reference;
   }

   public Item getItem() {
      return this._item;
   }

   public Creature getActiveChar() {
      return this._creature;
   }

   public Object getReference() {
      return this._reference;
   }

   @Override
   public EventType getType() {
      return EventType.ON_ITEM_CREATE;
   }
}
