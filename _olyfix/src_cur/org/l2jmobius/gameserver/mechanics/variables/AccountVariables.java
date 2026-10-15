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

public class AccountVariables extends AbstractVariables {
   private static final Logger LOGGER = Logger.getLogger(AccountVariables.class.getName());
   private static final String SELECT_QUERY = "SELECT * FROM account_gsdata WHERE account_name = ?";
   private static final String DELETE_QUERY = "DELETE FROM account_gsdata WHERE account_name = ? AND var = ?";
   private static final String DELETE_ALL_QUERY = "DELETE FROM account_gsdata WHERE account_name = ?";
   private static final String UPSERT_QUERY = "INSERT INTO account_gsdata (account_name, var, value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value = VALUES(value)";
   private static final String DELETE_QUERY_VAR = "DELETE FROM account_gsdata WHERE var = ?";
   private static final long SAVE_INTERVAL = 60000L;
   private static final boolean ASYNC_SAVE_ENABLED = true;
   public static final String HWID = "HWID";
   public static final String HWIDSLIT_VAR = "\t";
   private final AtomicBoolean _scheduledSave = new AtomicBoolean(false);
   private final String _accountName;

   public AccountVariables(String accountName) {
      this._accountName = accountName;
      this.restoreMe();
   }

   public boolean restoreMe() {
      this.clearChangeTracking();

      label137: {
         boolean st;
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement stx = con.prepareStatement("SELECT * FROM account_gsdata WHERE account_name = ?");
         ) {
            stx.setString(1, this._accountName);

            try (ResultSet rset = stx.executeQuery()) {
               while (rset.next()) {
                  this.set(rset.getString("var"), rset.getString("value"), false);
               }
               break label137;
            }
         } catch (SQLException var21) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not restore variables for: " + this._accountName, (Throwable)var21);
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
            try (PreparedStatement st = con.prepareStatement("DELETE FROM account_gsdata WHERE account_name = ? AND var = ?")) {
               List<String> sortedDeleted = new ArrayList<>(this._deleted);
               sortedDeleted.sort(null);

               for (String name : sortedDeleted) {
                  st.setString(1, this._accountName);
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
                  "INSERT INTO account_gsdata (account_name, var, value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value = VALUES(value)"
               )) {
               for (String name : allChangesx) {
                  Object value = this.getSet().get(name);
                  if (value != null) {
                     st.setString(1, this._accountName);
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
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not update variables for: " + this._accountName, (Throwable)var22);
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
         PreparedStatement st = con.prepareStatement("DELETE FROM account_gsdata WHERE account_name = ?");
      ) {
         st.setString(1, this._accountName);
         st.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not delete variables for: " + this._accountName, (Throwable)var9);
         this._saveLock.unlock();
         return false;
      }

      this.getSet().clear();
      this.clearChangeTracking();
      this._saveLock.unlock();
      return true;
   }

   public static boolean deleteVipPurchases(String var) {
      try {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement st = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ?");
         ) {
            st.setString(1, var);
            st.execute();
         }

         return true;
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "AccountVariables: Could not delete vip variables!", (Throwable)var9);
         return false;
      }
   }
}
