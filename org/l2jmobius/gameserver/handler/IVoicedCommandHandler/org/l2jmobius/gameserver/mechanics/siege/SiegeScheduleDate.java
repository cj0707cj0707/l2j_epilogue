package org.l2jmobius.gameserver.mechanics.siege;

import org.l2jmobius.gameserver.util.StatSet;

public class SiegeScheduleDate {
   private final int _day;
   private final int _hour;
   private final int _maxConcurrent;
   private final boolean _siegeEnabled;

   public SiegeScheduleDate(StatSet set) {
      this._day = set.getInt("day", 1);
      this._hour = set.getInt("hour", 16);
      this._maxConcurrent = set.getInt("maxConcurrent", 5);
      this._siegeEnabled = set.getBoolean("siegeEnabled", false);
   }

   public int getDay() {
      return this._day;
   }

   public int getHour() {
      return this._hour;
   }

   public int getMaxConcurrent() {
      return this._maxConcurrent;
   }

   public boolean siegeEnabled() {
      return this._siegeEnabled;
   }
}
