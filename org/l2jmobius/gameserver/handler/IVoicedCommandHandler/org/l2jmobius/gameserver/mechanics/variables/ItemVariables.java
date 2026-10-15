package org.l2jmobius.gameserver.mechanics.variables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;

public class ItemVariables extends AbstractVariables {
   private static final Logger LOGGER = Logger.getLogger(ItemVariables.class.getName());
   private static final String SELECT_QUERY = "SELECT * FROM item_variables WHERE id = ?";
   private static final String SELECT_COUNT = "SELECT COUNT(*) FROM item_variables WHERE id = ?";
   private static final String DELETE_QUERY = "DELETE FROM item_variables WHERE id = ? AND var = ?";
   private static final String DELETE_ALL_QUERY = "DELETE FROM item_variables WHERE id = ?";
   private static final String UPSERT_QUERY = "INSERT INTO item_variables (id, var, val) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE val = VALUES(val)";
   private static final long SAVE_INTERVAL = 60000L;
   private static final boolean ASYNC_SAVE_ENABLED = true;
   public static final String TRANSMOG_ID = "transmogId";
   private final AtomicBoolean _scheduledSave = new AtomicBoolean(false);
   private final int _objectId;

   public ItemVariables(int objectId) {
      this._objectId = objectId;
      this.restoreMe();
   }

   public static boolean hasVariables(int objectId) {
      try {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement st = con.prepareStatement("SELECT COUNT(*) FROM item_variables WHERE id = ?");
         ) {
            st.setInt(1, objectId);

            try (ResultSet rset = st.executeQuery()) {
               if (rset.next()) {
                  return rset.getInt(1) > 0;
               }
            }
         }

         return true;
      } catch (SQLException var12) {
         LOGGER.log(Level.WARNING, ItemVariables.class.getSimpleName() + ": Could not select variables count for: " + objectId, (Throwable)var12);
         return false;
      }
   }

   public boolean restoreMe() {
      this.clearChangeTracking();

      label137: {
         boolean st;
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement stx = con.prepareStatement("SELECT * FROM item_variables WHERE id = ?");
         ) {
            stx.setInt(1, this._objectId);

            try (ResultSet rset = stx.executeQuery()) {
               while (rset.next()) {
                  this.set(rset.getString("var"), rset.getString("val"), false);
               }
               break label137;
            }
         } catch (SQLException var21) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not restore variables for: " + this._objectId, (Throwable)var21);
            st = false;
         } finally {
            this.compareAndSetChanges(true, false);
         }

         return st;
      }

      return true;
   }

   public boolean storeMe() {
      if (!this.hasChanges()) {
         return false;
      } else if (!this._scheduledSave.get()) {
         this._scheduledSave.set(true);
         ThreadPool.schedule(() -> {
            this._scheduledSave.set(false);
            this.saveNow();
         }, 60000L);
         return true;
      } else {
         return this.saveNow();
      }
   }

   public boolean saveNow() {
      return !this.hasChanges() ? false : this.saveNowSync();
   }

   private boolean saveNowSync() {
      this._saveLock.lock();

      boolean allChanges;
      try (Connection con = DatabaseFactory.getConnection()) {
         if (!this._deleted.isEmpty()) {
            try (PreparedStatement st = con.prepareStatement("DELETE FROM item_variables WHERE id = ? AND var = ?")) {
               List<String> sortedDeleted = new ArrayList<>(this._deleted);
               sortedDeleted.sort(null);

               for (String name : sortedDeleted) {
                  st.setInt(1, this._objectId);
                  st.setString(2, name);
                  st.addBatch();
               }

               st.executeBatch();
            }
         }

         if (!this._added.isEmpty() || !this._modified.isEmpty()) {
            TreeSet<String> allChangesx = new TreeSet<>();
            allChangesx.addAll(this._added);
            allChangesx.addAll(this._modified);

            try (PreparedStatement st = con.prepareStatement(
                  "INSERT INTO item_variables (id, var, val) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE val = VALUES(val)"
               )) {
               for (String name : allChangesx) {
                  Object value = this.getSet().get(name);
                  if (value != null) {
                     st.setInt(1, this._objectId);
                     st.setString(2, name);
                     st.setString(3, String.valueOf(value));
                     st.addBatch();
                  }
               }

               st.executeBatch();
            }
         }

         this.clearChangeTracking();
         this.compareAndSetChanges(true, false);
         return true;
      } catch (SQLException var22) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not update variables for: " + this._objectId, (Throwable)var22);
         allChanges = false;
      } finally {
         this._saveLock.unlock();
      }

      return allChanges;
   }

   public boolean deleteMe() {
      this._saveLock.lock();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement st = con.prepareStatement("DELETE FROM item_variables WHERE id = ?");
      ) {
         st.setInt(1, this._objectId);
         st.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not delete variables for: " + this._objectId, (Throwable)var9);
         this._saveLock.unlock();
         return false;
      }

      this.getSet().clear();
      this.clearChangeTracking();
      this._saveLock.unlock();
      return true;
   }
}
