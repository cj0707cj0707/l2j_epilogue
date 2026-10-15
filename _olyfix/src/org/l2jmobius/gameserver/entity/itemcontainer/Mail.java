package org.l2jmobius.gameserver.entity.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;

public class Mail extends ItemContainer {
   private final int _ownerId;
   private int _messageId;

   public Mail(int objectId, int messageId) {
      this._ownerId = objectId;
      this._messageId = messageId;
   }

   @Override
   public String getName() {
      return "Mail";
   }

   public Player getOwner() {
      return null;
   }

   @Override
   public ItemLocation getBaseLocation() {
      return ItemLocation.MAIL;
   }

   public int getMessageId() {
      return this._messageId;
   }

   public void setNewMessageId(int messageId) {
      this._messageId = messageId;

      for (Item item : this._items) {
         if (item != null) {
            item.setItemLocation(this.getBaseLocation(), messageId);
         }
      }

      this.updateDatabase();
   }

   public void returnToWh(ItemContainer wh) {
      for (Item item : this._items) {
         if (item != null) {
            if (wh != null) {
               this.transferItem(ItemProcessType.TRANSFER, item.getObjectId(), item.getCount(), wh, null, null);
            } else {
               item.setItemLocation(ItemLocation.WAREHOUSE);
            }
         }
      }
   }

   @Override
   protected void addItem(Item item) {
      super.addItem(item);
      item.setItemLocation(this.getBaseLocation(), this._messageId);
      item.updateDatabase(true);
   }

   @Override
   public void updateDatabase() {
      this._items.forEach(i -> i.updateDatabase(true));
   }

   @Override
   public void restore() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND loc=? AND loc_data=?"
         );
      ) {
         ps.setInt(1, this._ownerId);
         ps.setString(2, this.getBaseLocation().name());
         ps.setInt(3, this._messageId);

         try (ResultSet inv = ps.executeQuery()) {
            while (inv.next()) {
               Item item = Item.restoreFromDb(this._ownerId, inv);
               if (item != null) {
                  World.addObject(item);
                  if (item.isStackable() && this.getItemByItemId(item.getId()) != null) {
                     this.addItem(ItemProcessType.RESTORE, item, null, null);
                  } else {
                     this.addItem(item);
                  }
               }
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "could not restore container:", (Throwable)var12);
      }
   }

   @Override
   public int getOwnerId() {
      return this._ownerId;
   }

   @Override
   public void deleteMe() {
      this._items.forEach(item -> {
         item.updateDatabase(true);
         item.stopAllTasks();
         World.removeObject(item);
      });
      this._items.clear();
   }
}
