package org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clanwh;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayerClanWHItemTransfer implements IBaseEvent {
   private final Player _player;
   private final Item _item;
   private final long _count;
   private final ItemContainer _container;

   public OnPlayerClanWHItemTransfer(Player player, Item item, long count, ItemContainer container) {
      this._player = player;
      this._item = item;
      this._count = count;
      this._container = container;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Item getItem() {
      return this._item;
   }

   public long getCount() {
      return this._count;
   }

   public ItemContainer getContainer() {
      return this._container;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_CLAN_WH_ITEM_TRANSFER;
   }
}
