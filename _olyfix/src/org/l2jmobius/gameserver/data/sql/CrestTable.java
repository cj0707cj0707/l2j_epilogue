package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.Crest;
import org.l2jmobius.gameserver.entity.clan.enums.CrestType;

public class CrestTable {
   private static final Logger LOGGER = Logger.getLogger(CrestTable.class.getName());
   private final Map<Integer, Crest> _crests = new ConcurrentHashMap<>();
   private final AtomicInteger _nextId = new AtomicInteger(1);

   protected CrestTable() {
      this.load();
   }

   public synchronized void load() {
      this._crests.clear();
      Set<Integer> crestsInUse = new HashSet<>();

      for (Clan clan : ClanTable.getInstance().getClans()) {
         if (clan.getCrestId() != 0) {
            crestsInUse.add(clan.getCrestId());
         }

         if (clan.getCrestLargeId() != 0) {
            crestsInUse.add(clan.getCrestLargeId());
         }

         if (clan.getAllyCrestId() != 0) {
            crestsInUse.add(clan.getAllyCrestId());
         }
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("SELECT `crest_id`, `data`, `type` FROM `crests` ORDER BY `crest_id` DESC");
         ResultSet rs = statement.executeQuery();
      ) {
         while (rs.next()) {
            int id = rs.getInt("crest_id");
            if (this._nextId.get() <= id) {
               this._nextId.set(id + 1);
            }

            if (!crestsInUse.contains(id) && id != this._nextId.get() - 1) {
               this.removeCrest(id);
            } else {
               byte[] data = rs.getBytes("data");
               CrestType crestType = CrestType.getById(rs.getInt("type"));
               if (crestType != null) {
                  this._crests.put(id, new Crest(id, data, crestType));
               } else {
                  LOGGER.warning("Unknown crest type found in database. Type:" + rs.getInt("type"));
               }
            }
         }
      } catch (SQLException var14) {
         LOGGER.log(Level.WARNING, "There was an error while loading crests from database:", (Throwable)var14);
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._crests.size() + " Crests.");

      for (Clan clan : ClanTable.getInstance().getClans()) {
         if (clan.getCrestId() != 0 && this.getCrest(clan.getCrestId()) == null) {
            LOGGER.info("Removing non-existent crest for clan " + clan.getName() + " [" + clan.getId() + "], crestId:" + clan.getCrestId());
            clan.setCrestId(0);
            clan.changeClanCrest(0);
         }

         if (clan.getCrestLargeId() != 0 && this.getCrest(clan.getCrestLargeId()) == null) {
            LOGGER.info("Removing non-existent large crest for clan " + clan.getName() + " [" + clan.getId() + "], crestLargeId:" + clan.getCrestLargeId());
            clan.setCrestLargeId(0);
            clan.changeLargeCrest(0);
         }

         if (clan.getAllyCrestId() != 0 && this.getCrest(clan.getAllyCrestId()) == null) {
            LOGGER.info("Removing non-existent ally crest for clan " + clan.getName() + " [" + clan.getId() + "], allyCrestId:" + clan.getAllyCrestId());
            clan.setAllyCrestId(0);
            clan.changeAllyCrest(0, true);
         }
      }
   }

   public Crest getCrest(int crestId) {
      return this._crests.get(crestId);
   }

   public Crest createCrest(byte[] data, CrestType crestType) {
      try {
         Crest var6;
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement statement = con.prepareStatement("INSERT INTO `crests`(`crest_id`, `data`, `type`) VALUES(?, ?, ?)");
         ) {
            Crest crest = new Crest(this._nextId.getAndIncrement(), data, crestType);
            statement.setInt(1, crest.getId());
            statement.setBytes(2, crest.getData());
            statement.setInt(3, crest.getType().getId());
            statement.executeUpdate();
            this._crests.put(crest.getId(), crest);
            var6 = crest;
         }

         return var6;
      } catch (SQLException var11) {
         LOGGER.log(Level.WARNING, "There was an error while saving crest in database:", (Throwable)var11);
         return null;
      }
   }

   public void removeCrest(int crestId) {
      this._crests.remove(crestId);
      if (crestId != this._nextId.get() - 1) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement statement = con.prepareStatement("DELETE FROM `crests` WHERE `crest_id`=?");
         ) {
            statement.setInt(1, crestId);
            statement.executeUpdate();
         } catch (SQLException var10) {
            LOGGER.log(Level.WARNING, "There was an error while deleting crest from database:", (Throwable)var10);
         }
      }
   }

   public int getNextId() {
      return this._nextId.getAndIncrement();
   }

   public static CrestTable getInstance() {
      return CrestTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CrestTable INSTANCE = new CrestTable();
   }
}
