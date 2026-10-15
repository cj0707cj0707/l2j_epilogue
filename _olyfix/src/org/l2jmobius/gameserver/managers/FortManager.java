package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.siege.Fort;

public class FortManager {
   protected static final Logger LOGGER = Logger.getLogger(FortManager.class.getName());
   private static final List<Fort> _forts = new CopyOnWriteArrayList<>();

   public int findNearestFortIndex(WorldObject obj) {
      return this.findNearestFortIndex(obj, Long.MAX_VALUE);
   }

   public int findNearestFortIndex(WorldObject obj, long maxDistanceValue) {
      int index = this.getFortIndex(obj);
      if (index < 0) {
         long maxDistance = maxDistanceValue;

         for (int i = 0; i < _forts.size(); i++) {
            Fort fort = _forts.get(i);
            if (fort != null) {
               double distance = fort.getDistance(obj);
               if (maxDistance > distance) {
                  maxDistance = (long)distance;
                  index = i;
               }
            }
         }
      }

      return index;
   }

   public Fort getFortById(int fortId) {
      for (Fort fort : _forts) {
         if (fort.getResidenceId() == fortId) {
            return fort;
         }
      }

      return null;
   }

   public Fort getFortByOwner(Clan clan) {
      if (clan == null) {
         return null;
      } else {
         for (Fort fort : _forts) {
            if (fort.getOwnerClan() == clan) {
               return fort;
            }
         }

         return null;
      }
   }

   public Fort getFort(String name) {
      for (Fort fort : _forts) {
         if (fort.getName().equalsIgnoreCase(name.trim())) {
            return fort;
         }
      }

      return null;
   }

   public Fort getFort(int x, int y, int z) {
      for (Fort fort : _forts) {
         if (fort.checkIfInZone(x, y, z)) {
            return fort;
         }
      }

      return null;
   }

   public Fort getFort(WorldObject activeObject) {
      return this.getFort(activeObject.getX(), activeObject.getY(), activeObject.getZ());
   }

   public int getFortIndex(int fortId) {
      for (int i = 0; i < _forts.size(); i++) {
         Fort fort = _forts.get(i);
         if (fort != null && fort.getResidenceId() == fortId) {
            return i;
         }
      }

      return -1;
   }

   public int getFortIndex(WorldObject activeObject) {
      return this.getFortIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
   }

   public int getFortIndex(int x, int y, int z) {
      for (int i = 0; i < _forts.size(); i++) {
         Fort fort = _forts.get(i);
         if (fort != null && fort.checkIfInZone(x, y, z)) {
            return i;
         }
      }

      return -1;
   }

   public List<Fort> getForts() {
      return _forts;
   }

   public void loadInstances() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT id FROM fort ORDER BY id");
      ) {
         while (rs.next()) {
            _forts.add(new Fort(rs.getInt("id")));
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + _forts.size() + " fortress");

         for (Fort fort : _forts) {
            fort.getSiege().getSiegeGuardManager().loadSiegeGuard();
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: loadFortData(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void activateInstances() {
      for (Fort fort : _forts) {
         fort.activateInstance();
      }
   }

   public static FortManager getInstance() {
      return FortManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final FortManager INSTANCE = new FortManager();
   }
}
