package org.l2jmobius.gameserver.entity.itemcontainer;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clanwh.OnPlayerClanWHItemAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clanwh.OnPlayerClanWHItemDestroy;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.clanwh.OnPlayerClanWHItemTransfer;

public class ClanWarehouse extends Warehouse {
   private final Clan _clan;

   public ClanWarehouse(Clan clan) {
      this._clan = clan;
   }

   @Override
   public String getName() {
      return "ClanWarehouse";
   }

   @Override
   public int getOwnerId() {
      return this._clan.getId();
   }

   public Player getOwner() {
      return this._clan.getLeader().getPlayer();
   }

   @Override
   public ItemLocation getBaseLocation() {
      return ItemLocation.CLANWH;
   }

   public String getLocationId() {
      return "0";
   }

   public int getLocationId(boolean dummy) {
      return 0;
   }

   public void setLocationId(Player dummy) {
   }

   @Override
   public boolean validateCapacity(long slots) {
      return this._items.size() + slots <= PlayerConfig.WAREHOUSE_SLOTS_CLAN;
   }

   @Override
   public Item addItem(ItemProcessType process, int itemId, long count, Player actor, Object reference) {
      Item item = super.addItem(process, itemId, count, actor, reference);
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_WH_ITEM_ADD, item.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemAdd(actor, item, this), item.getTemplate());
      }

      return item;
   }

   @Override
   public Item addItem(ItemProcessType process, Item item, Player actor, Object reference) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_WH_ITEM_ADD, item.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemAdd(actor, item, this), item.getTemplate());
      }

      return super.addItem(process, item, actor, reference);
   }

   @Override
   public Item destroyItem(ItemProcessType process, Item item, long count, Player actor, Object reference) {
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_WH_ITEM_DESTROY, item.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemDestroy(actor, item, count, this), item.getTemplate());
      }

      return super.destroyItem(process, item, count, actor, reference);
   }

   @Override
   public Item transferItem(ItemProcessType process, int objectId, long count, ItemContainer target, Player actor, Object reference) {
      Item item = this.getItemByObjectId(objectId);
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_WH_ITEM_TRANSFER, item.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemTransfer(actor, item, count, target), item.getTemplate());
      }

      return super.transferItem(process, objectId, count, target, actor, reference);
   }
}
