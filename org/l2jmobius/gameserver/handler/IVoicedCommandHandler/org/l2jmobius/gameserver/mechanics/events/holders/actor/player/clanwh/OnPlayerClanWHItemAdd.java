package org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clanwh;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerClanWHItemAdd implements IBaseEvent {
   private final Player _player;
   private final Item _item;
   private final ItemContainer _container;

   public OnPlayerClanWHItemAdd(Player player, Item item, ItemContainer container) {
      this._player = player;
      this._item = item;
      this._container = container;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Item getItem() {
      return this._item;
   }

   public ItemContainer getContainer() {
      return this._container;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_CLAN_WH_ITEM_ADD;
   }
}
