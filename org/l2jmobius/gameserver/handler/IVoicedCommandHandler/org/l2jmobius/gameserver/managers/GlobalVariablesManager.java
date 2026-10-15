package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.mechanics.variables.AbstractVariables;

public class GlobalVariablesManager extends AbstractVariables {
   private static final Logger LOGGER = Logger.getLogger(GlobalVariablesManager.class.getName());
   private static final String SELECT_QUERY = "SELECT * FROM global_variables";
   private static final String DELETE_QUERY = "DELETE FROM global_variables";
   private static final String INSERT_QUERY = "REPLACE INTO global_variables (var, value) VALUES (?, ?)";
   public static final String DAILY_TASK_RESET = "DAILY_TASK_RESET";

   protected GlobalVariablesManager() {
      this.restoreMe();
   }

   public boolean restoreMe() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement st = con.createStatement();
         ResultSet rset = st.executeQuery("SELECT * FROM global_variables");
      ) {
         while (rset.next()) {
            this.set(rset.getString("var"), rset.getString("value"));
         }
      } catch (SQLException var12) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Couldn't restore global variables.");
         return false;
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this.getSet().size() + " variables.");
      return true;
   }

   public boolean storeMe() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement del = con.createStatement();
         PreparedStatement st = con.prepareStatement("REPLACE INTO global_variables (var, value) VALUES (?, ?)");
      ) {
         del.execute("DELETE FROM global_variables");

         for (Entry<String, Object> entry : this.getSet().entrySet()) {
            st.setString(1, entry.getKey());
            st.setString(2, String.valueOf(entry.getValue()));
            st.addBatch();
         }

         st.executeBatch();
      } catch (SQLException var12) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't save global variables to database.", (Throwable)var12);
         return false;
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Stored " + this.getSet().size() + " variables.");
      return true;
   }

   public boolean deleteMe() {
      try {
         try (
            Connection con = DatabaseFactory.getConnection();
            Statement del = con.createStatement();
         ) {
            del.execute("DELETE FROM global_variables");
         }

         return true;
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't delete global variables to database.", (Throwable)var9);
         return false;
      }
   }

   public static GlobalVariablesManager getInstance() {
      return GlobalVariablesManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final GlobalVariablesManager INSTANCE = new GlobalVariablesManager();
   }
}
