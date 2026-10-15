package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerItemUnequip implements IBaseEvent {
   private final Player _player;
   private final Item _item;

   public OnPlayerItemUnequip(Player player, Item item) {
      this._player = player;
      this._item = item;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Item getItem() {
      return this._item;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_ITEM_UNEQUIP;
   }
}
