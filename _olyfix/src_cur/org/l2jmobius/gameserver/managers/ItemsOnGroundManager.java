package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;

public class ItemsOnGroundManager implements Runnable {
   private static final Logger LOGGER = Logger.getLogger(ItemsOnGroundManager.class.getName());
   private final Collection<Item> _items = ConcurrentHashMap.newKeySet();

   protected ItemsOnGroundManager() {
      if (GeneralConfig.SAVE_DROPPED_ITEM_INTERVAL > 0) {
         ThreadPool.scheduleAtFixedRate(this, GeneralConfig.SAVE_DROPPED_ITEM_INTERVAL, GeneralConfig.SAVE_DROPPED_ITEM_INTERVAL);
      }

      this.load();
   }

   private void load() {
      if (!GeneralConfig.SAVE_DROPPED_ITEM && GeneralConfig.CLEAR_DROPPED_ITEM_TABLE) {
         this.emptyTable();
      }

      if (GeneralConfig.SAVE_DROPPED_ITEM) {
         if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM) {
            String str = null;
            if (!GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM) {
               str = "UPDATE itemsonground SET drop_time = ? WHERE drop_time = -1 AND equipable = 0";
            } else if (GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM) {
               str = "UPDATE itemsonground SET drop_time = ? WHERE drop_time = -1";
            }

            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement ps = con.prepareStatement(str);
            ) {
               ps.setLong(1, System.currentTimeMillis());
               ps.execute();
            } catch (Exception var19) {
               LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error while updating table ItemsOnGround " + var19.getMessage(), (Throwable)var19);
            }
         }

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable FROM itemsonground");
         ) {
            int count = 0;

            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  Item item = new Item(rs.getInt(1), rs.getInt(2));
                  World.addObject(item);
                  if (item.isStackable() && rs.getInt(3) > 1) {
                     item.setCount(rs.getInt(3));
                  }

                  if (rs.getInt(4) > 0) {
                     item.setEnchantLevel(rs.getInt(4));
                  }

                  item.setXYZ(rs.getInt(5), rs.getInt(6), rs.getInt(7));
                  item.setWorldRegion(World.getRegion(item));
                  item.getWorldRegion().addVisibleObject(item);
                  long dropTime = rs.getLong(8);
                  item.setDropTime(dropTime);
                  item.setProtected(dropTime == -1L);
                  item.setSpawned(true);
                  World.addVisibleObject(item, item.getWorldRegion());
                  this._items.add(item);
                  count++;
                  if (!GeneralConfig.LIST_PROTECTED_ITEMS.contains(item.getId())
                     && dropTime > -1L
                     && (
                        GeneralConfig.AUTODESTROY_ITEM_AFTER > 0 && !item.getTemplate().hasExImmediateEffect()
                           || GeneralConfig.HERB_AUTO_DESTROY_TIME > 0 && item.getTemplate().hasExImmediateEffect()
                     )) {
                     ItemsAutoDestroyTaskManager.getInstance().addItem(item);
                  }
               }
            }

            LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + count + " items.");
         } catch (Exception var16) {
            LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error while loading ItemsOnGround " + var16.getMessage(), (Throwable)var16);
         }

         if (GeneralConfig.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD) {
            this.emptyTable();
         }
      }
   }

   public void save(Item item) {
      if (GeneralConfig.SAVE_DROPPED_ITEM) {
         this._items.add(item);
      }
   }

   public void removeObject(Item item) {
      if (GeneralConfig.SAVE_DROPPED_ITEM) {
         this._items.remove(item);
      }
   }

   public void saveInDb() {
      this.run();
   }

   public void cleanUp() {
      this._items.clear();
   }

   public void emptyTable() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
      ) {
         s.executeUpdate("DELETE FROM itemsonground");
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error while cleaning table ItemsOnGround " + var9.getMessage(), (Throwable)var9);
      }
   }

   @Override
   public synchronized void run() {
      if (GeneralConfig.SAVE_DROPPED_ITEM) {
         this.emptyTable();
         if (!this._items.isEmpty()) {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement ps = con.prepareStatement(
                  "INSERT INTO itemsonground(object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable) VALUES(?,?,?,?,?,?,?,?,?)"
               );
            ) {
               for (Item item : this._items) {
                  if (item != null && !CursedWeaponsManager.getInstance().isCursed(item.getId())) {
                     try {
                        ps.setInt(1, item.getObjectId());
                        ps.setInt(2, item.getId());
                        ps.setLong(3, item.getCount());
                        ps.setInt(4, item.getEnchantLevel());
                        ps.setInt(5, item.getX());
                        ps.setInt(6, item.getY());
                        ps.setInt(7, item.getZ());
                        ps.setLong(8, item.isProtected() ? -1L : item.getDropTime());
                        ps.setLong(9, item.isEquipable() ? 1L : 0L);
                        ps.execute();
                        ps.clearParameters();
                     } catch (Exception var8) {
                        LOGGER.log(
                           Level.SEVERE,
                           this.getClass().getSimpleName() + ": Error while inserting into table ItemsOnGround: " + var8.getMessage(),
                           (Throwable)var8
                        );
                     }
                  }
               }
            } catch (SQLException var11) {
               LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": SQL error while storing items on ground: " + var11.getMessage(), (Throwable)var11);
            }
         }
      }
   }

   public static ItemsOnGroundManager getInstance() {
      return ItemsOnGroundManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ItemsOnGroundManager INSTANCE = new ItemsOnGroundManager();
   }
}
