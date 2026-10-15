package org.l2jmobius.gameserver.entity.zone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.type.PeaceZone;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ZoneRegion {
   private final int _regionX;
   private final int _regionY;
   private final Map<Integer, ZoneType> _zones = new ConcurrentHashMap<>();

   public ZoneRegion(int regionX, int regionY) {
      this._regionX = regionX;
      this._regionY = regionY;
   }

   public Map<Integer, ZoneType> getZones() {
      return this._zones;
   }

   public int getRegionX() {
      return this._regionX;
   }

   public int getRegionY() {
      return this._regionY;
   }

   public void revalidateZones(Creature creature) {
      if (!creature.isTeleporting()) {
         for (ZoneType z : this._zones.values()) {
            z.revalidateInZone(creature);
         }
      }
   }

   public void removeFromZones(Creature creature) {
      for (ZoneType z : this._zones.values()) {
         z.removeCharacter(creature);
      }
   }

   public boolean checkEffectRangeInsidePeaceZone(Skill skill, int x, int y, int z) {
      int range = skill.getEffectRange();
      int up = y + range;
      int down = y - range;
      int left = x + range;
      int right = x - range;

      for (ZoneType e : this._zones.values()) {
         if (e instanceof PeaceZone) {
            if (e.isInsideZone(x, up, z)) {
               return false;
            }

            if (e.isInsideZone(x, down, z)) {
               return false;
            }

            if (e.isInsideZone(left, y, z)) {
               return false;
            }

            if (e.isInsideZone(right, y, z)) {
               return false;
            }

            if (e.isInsideZone(x, y, z)) {
               return false;
            }
         }
      }

      return true;
   }

   public void onDeath(Creature creature) {
      for (ZoneType z : this._zones.values()) {
         if (z.isInsideZone(creature)) {
            z.onDieInside(creature);
         }
      }
   }

   public void onRevive(Creature creature) {
      for (ZoneType z : this._zones.values()) {
         if (z.isInsideZone(creature)) {
            z.onReviveInside(creature);
         }
      }
   }
}
