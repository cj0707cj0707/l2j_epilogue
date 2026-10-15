package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.EventMonster;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemCreate;
import org.l2jmobius.gameserver.util.GMAudit;

public class ItemManager {
   private static final Logger LOGGER = Logger.getLogger(ItemManager.class.getName());
   private static final Logger LOGGER_ITEMS = Logger.getLogger("item");

   private ItemManager() {
   }

   public static Item createItem(ItemProcessType process, int itemId, long count, Player actor) {
      return createItem(process, itemId, count, actor, null);
   }

   public static Item createItem(ItemProcessType process, int itemId, long count, Creature actor, Object reference) {
      Item item = new Item(IdManager.getInstance().getNextId(), itemId);
      if (process == ItemProcessType.LOOT && !PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(itemId)) {
         if (reference instanceof Attackable && ((Attackable)reference).isRaid()) {
            Attackable raid = (Attackable)reference;
            if (raid.getFirstCommandChannelAttacked() != null && !PlayerConfig.AUTO_LOOT_RAIDS) {
               item.setOwnerId(raid.getFirstCommandChannelAttacked().getLeaderObjectId());
               ScheduledFuture<?> itemLootShedule = ThreadPool.schedule(new ItemManager.ResetOwner(item), PlayerConfig.LOOT_RAIDS_PRIVILEGE_INTERVAL);
               item.setItemLootShedule(itemLootShedule);
            }
         } else if (!PlayerConfig.AUTO_LOOT || reference instanceof EventMonster && ((EventMonster)reference).eventDropOnGround()) {
            item.setOwnerId(actor.getObjectId());
            ScheduledFuture<?> itemLootShedule = ThreadPool.schedule(new ItemManager.ResetOwner(item), 15000L);
            item.setItemLootShedule(itemLootShedule);
         }
      }

      World.addObject(item);
      if (item.isStackable() && count > 1L) {
         item.setCount(count);
      }

      if (GeneralConfig.LOG_ITEMS && !GeneralConfig.LOG_ITEMS_SMALL_LOG && !GeneralConfig.LOG_ITEMS_IDS_ONLY
         || GeneralConfig.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || item.getId() == 57)
         || GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(item.getId())) {
         if (item.isEnchanted()) {
            LOGGER_ITEMS.info(
               StringUtil.concat(
                  "CREATE:",
                  String.valueOf(process),
                  ", item ",
                  String.valueOf(item.getObjectId()),
                  ":+",
                  String.valueOf(item.getEnchantLevel()),
                  " ",
                  item.getTemplate().getName(),
                  "(",
                  String.valueOf(item.getCount()),
                  "), ",
                  String.valueOf(actor),
                  ", ",
                  String.valueOf(reference)
               )
            );
         } else {
            LOGGER_ITEMS.info(
               StringUtil.concat(
                  "CREATE:",
                  String.valueOf(process),
                  ", item ",
                  String.valueOf(item.getObjectId()),
                  ":",
                  item.getTemplate().getName(),
                  "(",
                  String.valueOf(item.getCount()),
                  "), ",
                  String.valueOf(actor),
                  ", ",
                  String.valueOf(reference)
               )
            );
         }
      }

      if (actor != null && actor.isGM() && GeneralConfig.GMAUDIT) {
         String referenceName = "no-reference";
         if (reference instanceof WorldObject) {
            referenceName = ((WorldObject)reference).getName() != null ? ((WorldObject)reference).getName() : "no-name";
         } else if (reference instanceof String) {
            referenceName = (String)reference;
         }

         String targetName = actor.getTarget() != null ? actor.getTarget().getName() : "no-target";
         GMAudit.logAction(
            String.valueOf(actor),
            StringUtil.concat(
               String.valueOf(process),
               "(id: ",
               String.valueOf(itemId),
               " count: ",
               String.valueOf(count),
               " name: ",
               item.getItemName(),
               " objId: ",
               String.valueOf(item.getObjectId()),
               ")"
            ),
            targetName,
            StringUtil.concat("Object referencing this action is: ", referenceName)
         );
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_CREATE, item.getTemplate())) {
         EventDispatcher.getInstance().notifyEventAsync(new OnItemCreate(item, actor, reference), item.getTemplate());
      }

      return item;
   }

   public static void destroyItem(ItemProcessType process, Item item, Player actor, Object reference) {
      synchronized (item) {
         long old = item.getCount();
         item.setCount(0L);
         item.setOwnerId(0);
         item.setItemLocation(ItemLocation.VOID);
         item.setLastChange(3);
         World.removeObject(item);
         IdManager.getInstance().releaseId(item.getObjectId());
         if (process != null && process != ItemProcessType.NONE) {
            if (GeneralConfig.LOG_ITEMS && !GeneralConfig.LOG_ITEMS_SMALL_LOG && !GeneralConfig.LOG_ITEMS_IDS_ONLY
               || GeneralConfig.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || item.getId() == 57)
               || GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(item.getId())) {
               if (item.isEnchanted()) {
                  LOGGER_ITEMS.info(
                     StringUtil.concat(
                        "DELETE:",
                        String.valueOf(process),
                        ", item ",
                        String.valueOf(item.getObjectId()),
                        ":+",
                        String.valueOf(item.getEnchantLevel()),
                        " ",
                        item.getTemplate().getName(),
                        "(",
                        String.valueOf(item.getCount()),
                        "), PrevCount(",
                        String.valueOf(old),
                        "), ",
                        String.valueOf(actor),
                        ", ",
                        String.valueOf(reference)
                     )
                  );
               } else {
                  LOGGER_ITEMS.info(
                     StringUtil.concat(
                        "DELETE:",
                        String.valueOf(process),
                        ", item ",
                        String.valueOf(item.getObjectId()),
                        ":",
                        item.getTemplate().getName(),
                        "(",
                        String.valueOf(item.getCount()),
                        "), PrevCount(",
                        String.valueOf(old),
                        "), ",
                        String.valueOf(actor),
                        ", ",
                        String.valueOf(reference)
                     )
                  );
               }
            }

            if (actor != null && actor.isGM() && GeneralConfig.GMAUDIT) {
               String referenceName = "no-reference";
               if (reference instanceof WorldObject) {
                  referenceName = ((WorldObject)reference).getName() != null ? ((WorldObject)reference).getName() : "no-name";
               } else if (reference instanceof String) {
                  referenceName = (String)reference;
               }

               String targetName = actor.getTarget() != null ? actor.getTarget().getName() : "no-target";
               GMAudit.logAction(
                  String.valueOf(actor),
                  StringUtil.concat(
                     String.valueOf(process),
                     "(id: ",
                     String.valueOf(item.getId()),
                     " count: ",
                     String.valueOf(item.getCount()),
                     " itemObjId: ",
                     String.valueOf(item.getObjectId()),
                     ")"
                  ),
                  targetName,
                  StringUtil.concat("Object referencing this action is: ", referenceName)
               );
            }
         }

         if (item.getTemplate().isPetItem()) {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
            ) {
               statement.setInt(1, item.getObjectId());
               statement.execute();
            } catch (Exception var16) {
               LOGGER.log(Level.WARNING, "ItemManager: Could not delete pet objectid:", (Throwable)var16);
            }
         }
      }
   }

   protected static class ResetOwner implements Runnable {
      Item _item;

      public ResetOwner(Item item) {
         this._item = item;
      }

      @Override
      public void run() {
         if (this._item.getItemLocation() == ItemLocation.VOID) {
            this._item.setOwnerId(0);
         }

         this._item.setItemLootShedule(null);
      }
   }
}
