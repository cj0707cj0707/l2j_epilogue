package org.l2jmobius.commons.time;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
   public static Duration parseDuration(String durationString) {
      int index = 0;

      while (index < durationString.length() && Character.isDigit(durationString.charAt(index))) {
         index++;
      }

      if (index != 0 && index != durationString.length()) {
         int durationValue;
         String durationUnit;
         try {
            durationValue = Integer.parseInt(durationString.substring(0, index));
            durationUnit = durationString.substring(index).toLowerCase();
         } catch (NumberFormatException var6) {
            throw new IllegalArgumentException("Invalid duration format: " + durationString);
         }

         switch (durationUnit) {
            case "sec":
            case "secs":
               return Duration.ofSeconds(durationValue);
            case "min":
            case "mins":
               return Duration.ofMinutes(durationValue);
            case "hour":
            case "hours":
               return Duration.ofHours(durationValue);
            case "day":
            case "days":
               return Duration.ofDays(durationValue);
            case "week":
            case "weeks":
               return Duration.ofDays(durationValue * 7L);
            case "month":
            case "months":
               return Duration.ofDays(durationValue * 30L);
            case "year":
            case "years":
               return Duration.ofDays(durationValue * 365L);
            default:
               throw new IllegalArgumentException("Unrecognized time unit: " + durationUnit);
         }
      } else {
         throw new IllegalArgumentException("Invalid duration format: " + durationString);
      }
   }

   public static String formatDuration(long millis) {
      if (millis < 1L) {
         return "0 milliseconds";
      } else {
         long days = millis / 86400000L;
         millis %= 86400000L;
         long hours = millis / 3600000L;
         millis %= 3600000L;
         long minutes = millis / 60000L;
         millis %= 60000L;
         long seconds = millis / 1000L;
         millis %= 1000L;
         StringBuilder sb = new StringBuilder();
         if (days > 0L) {
            sb.append(days).append(" day").append(days > 1L ? "s" : "").append(", ");
         }

         if (hours > 0L) {
            sb.append(hours).append(" hour").append(hours > 1L ? "s" : "").append(", ");
         }

         if (minutes > 0L) {
            sb.append(minutes).append(" minute").append(minutes > 1L ? "s" : "").append(", ");
         }

         if (seconds > 0L) {
            sb.append(seconds).append(" second").append(seconds > 1L ? "s" : "").append(", ");
         }

         if (millis > 0L) {
            sb.append(millis).append(" millisecond").append(millis > 1L ? "s" : "");
         }

         if (sb.length() > 2 && sb.charAt(sb.length() - 2) == ',') {
            sb.setLength(sb.length() - 2);
         }

         return sb.toString();
      }
   }

   public static String formatDate(Date date, String format) {
      return date == null ? null : DateTimeFormatter.ofPattern(format).format(date.toInstant().atZone(ZoneId.systemDefault()));
   }

   public static String getDateString(Date date) {
      return formatDate(date, "dd/MM/yyyy");
   }

   public static String getDateTimeString(Date date) {
      return formatDate(date, "dd/MM/yyyy HH:mm:ss");
   }

   public static String getDateString(long millis) {
      return getDateString(new Date(millis));
   }

   public static String getDateTimeString(long millis) {
      return getDateTimeString(new Date(millis));
   }

   public static Calendar getNextDayTime(int dayOfWeek, int hour, int minute) {
      Calendar calendar = Calendar.getInstance();
      int today = calendar.get(7);
      int daysUntilNext = (dayOfWeek - today + 7) % 7;
      if (daysUntilNext == 0 && (calendar.get(11) > hour || calendar.get(11) == hour && calendar.get(12) >= minute)) {
         daysUntilNext = 7;
      }

      calendar.add(5, daysUntilNext);
      calendar.set(11, hour);
      calendar.set(12, minute);
      calendar.set(13, 0);
      calendar.set(14, 0);
      return calendar;
   }

   public static Calendar getNextTime(int hour, int minute) {
      Calendar calendar = Calendar.getInstance();
      calendar.set(11, hour);
      calendar.set(12, minute);
      calendar.set(13, 0);
      calendar.set(14, 0);
      if (calendar.before(Calendar.getInstance())) {
         calendar.add(6, 1);
      }

      return calendar;
   }
}
