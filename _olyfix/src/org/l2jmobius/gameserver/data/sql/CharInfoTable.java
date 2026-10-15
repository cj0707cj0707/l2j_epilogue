package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;

public class CharInfoTable {
   private static final Logger LOGGER = Logger.getLogger(CharInfoTable.class.getName());
   private final Map<Integer, String> _names = new ConcurrentHashMap<>();
   private final Map<Integer, Integer> _accessLevels = new ConcurrentHashMap<>();

   protected CharInfoTable() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT charId, char_name, accesslevel FROM characters");
      ) {
         while (rs.next()) {
            int id = rs.getInt("charId");
            this._names.put(id, rs.getString("char_name"));
            this._accessLevels.put(id, rs.getInt("accesslevel"));
         }
      } catch (SQLException var12) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't retrieve all char id/name/access: " + var12.getMessage(), (Throwable)var12);
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._names.size() + " char names.");
   }

   public void addName(Player player) {
      if (player != null) {
         this.addName(player.getObjectId(), player.getName());
         this._accessLevels.put(player.getObjectId(), player.getAccessLevel().getLevel());
      }
   }

   private void addName(int objectId, String name) {
      if (name != null && !name.equals(this._names.get(objectId))) {
         this._names.put(objectId, name);
      }
   }

   public void removeName(int objId) {
      this._names.remove(objId);
      this._accessLevels.remove(objId);
   }

   public int getIdByName(String name) {
      if (name != null && !name.isEmpty()) {
         for (Entry<Integer, String> entry : this._names.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
               return entry.getKey();
            }
         }

         int id = -1;
         int accessLevel = 0;

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT charId,accesslevel FROM characters WHERE char_name=?");
         ) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  id = rs.getInt("charId");
                  accessLevel = rs.getInt("accesslevel");
               }
            }
         } catch (SQLException var15) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not check existing char name: " + var15.getMessage(), (Throwable)var15);
         }

         if (id > 0) {
            this._names.put(id, name);
            this._accessLevels.put(id, accessLevel);
            return id;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public String getNameById(int id) {
      if (id <= 0) {
         return null;
      } else {
         String name = this._names.get(id);
         if (name != null) {
            return name;
         } else {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement ps = con.prepareStatement("SELECT char_name,accesslevel FROM characters WHERE charId=?");
            ) {
               ps.setInt(1, id);

               try (ResultSet rset = ps.executeQuery()) {
                  if (rset.next()) {
                     name = rset.getString("char_name");
                     this._names.put(id, name);
                     this._accessLevels.put(id, rset.getInt("accesslevel"));
                     return name;
                  } else {
                     return null;
                  }
               }
            } catch (SQLException var14) {
               LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not check existing char id: " + var14.getMessage(), (Throwable)var14);
               return null;
            }
         }
      }
   }

   public int getAccessLevelById(int objectId) {
      return this.getNameById(objectId) != null ? this._accessLevels.get(objectId) : 0;
   }

   public synchronized boolean doesCharNameExist(String name) {
      boolean result = false;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as count FROM characters WHERE char_name=?");
      ) {
         ps.setString(1, name);

         try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
               result = rs.getInt("count") > 0;
            }
         }
      } catch (SQLException var14) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Could not check existing charname: " + var14.getMessage(), (Throwable)var14);
      }

      return result;
   }

   public int getAccountCharacterCount(String account) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT COUNT(char_name) as count FROM characters WHERE account_name=?");
      ) {
         ps.setString(1, account);

         try (ResultSet rset = ps.executeQuery()) {
            return rset.next() ? rset.getInt("count") : 0;
         }
      } catch (SQLException var13) {
         LOGGER.log(Level.WARNING, "Couldn't retrieve account for id: " + var13.getMessage(), (Throwable)var13);
         return 0;
      }
   }

   public static CharInfoTable getInstance() {
      return CharInfoTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CharInfoTable INSTANCE = new CharInfoTable();
   }
}
