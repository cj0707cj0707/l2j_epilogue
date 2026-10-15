package org.l2jmobius.gameserver.entity.zone.type;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

public class NpcSpawnTerritory {
   private final String _name;
   private final ZoneForm _territory;
   private List<ZoneForm> _bannedTerritories;

   public NpcSpawnTerritory(String name, ZoneForm territory) {
      this._name = name;
      this._territory = territory;
   }

   public void addBannedTerritory(ZoneForm territory) {
      if (this._bannedTerritories == null) {
         this._bannedTerritories = new ArrayList<>(1);
      }

      this._bannedTerritories.add(territory);
   }

   public String getName() {
      return this._name;
   }

   public Location getRandomPoint() {
      int count = 0;
      if (this._bannedTerritories != null) {
         label55:
         while (count++ < 100) {
            Location location = this._territory.getRandomPoint();
            int randomX = location.getX();
            int randomY = location.getY();
            int randomZ = location.getZ();

            for (ZoneForm territory : this._bannedTerritories) {
               if (territory.isInsideZone(randomX, randomY, randomZ)) {
                  continue label55;
               }
            }

            if (GeoEngine.getInstance().getHeight(randomX, randomY, randomZ) <= this._territory.getHighZ()) {
               return location;
            }
         }

         count = 0;

         label43:
         while (count++ < 100) {
            Location location = this._territory.getRandomPoint();
            int randomX = location.getX();
            int randomY = location.getY();
            int randomZ = location.getZ();

            for (ZoneForm territoryx : this._bannedTerritories) {
               if (territoryx.isInsideZone(randomX, randomY, randomZ)) {
                  continue label43;
               }
            }

            return location;
         }
      }

      count = 0;

      while (count++ < 100) {
         Location location = this._territory.getRandomPoint();
         int randomX = location.getX();
         int randomY = location.getY();
         int randomZ = location.getZ();
         if (GeoEngine.getInstance().getHeight(randomX, randomY, randomZ) <= this._territory.getHighZ()) {
            return location;
         }
      }

      return this._territory.getRandomPoint();
   }

   public boolean isInsideZone(int x, int y, int z) {
      return this._territory.isInsideZone(x, y, z);
   }

   public boolean isInsideZone(int x, int y) {
      int z = this._territory.getLowZ();
      if (!this._territory.isInsideZone(x, y, z)) {
         return false;
      } else {
         if (this._bannedTerritories != null) {
            for (ZoneForm territory : this._bannedTerritories) {
               if (territory.isInsideZone(x, y, z)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public int getHighZ() {
      return this._territory.getHighZ();
   }

   public void visualizeZone(int z) {
      this._territory.visualizeZone(z);
   }
}
