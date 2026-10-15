package org.l2jmobius.gameserver.mechanics.events.holders.actor.player.inventory;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerItemDrop implements IBaseEvent {
   private final Player _player;
   private final Item _item;
   private final Location _loc;

   public OnPlayerItemDrop(Player player, Item item, Location loc) {
      this._player = player;
      this._item = item;
      this._loc = loc;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Item getItem() {
      return this._item;
   }

   public Location getLocation() {
      return this._loc;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_ITEM_DROP;
   }
}
