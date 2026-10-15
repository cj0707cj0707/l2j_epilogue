package org.l2jmobius.gameserver.data.holders;

import java.time.DayOfWeek;

public class InstanceReenterTimeHolder {
   private final DayOfWeek _day;
   private final int _hour;
   private final int _minute;
   private final long _time;

   public InstanceReenterTimeHolder(long time) {
      this._time = time;
      this._day = null;
      this._hour = -1;
      this._minute = -1;
   }

   public InstanceReenterTimeHolder(DayOfWeek day, int hour, int minute) {
      this._time = -1L;
      this._day = day;
      this._hour = hour;
      this._minute = minute;
   }

   public long getTime() {
      return this._time;
   }

   public DayOfWeek getDay() {
      return this._day;
   }

   public int getHour() {
      return this._hour;
   }

   public int getMinute() {
      return this._minute;
   }
}
