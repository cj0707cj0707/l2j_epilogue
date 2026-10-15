package org.l2jmobius.commons.time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.StringUtil;

public class SchedulingPattern {
   private static final int MINUTE_MIN = 0;
   private static final int MINUTE_MAX = 59;
   private static final int HOUR_MIN = 0;
   private static final int HOUR_MAX = 23;
   private static final int DAY_MIN = 1;
   private static final int DAY_MAX = 31;
   private static final int MONTH_MIN = 1;
   private static final int MONTH_MAX = 12;
   private static final int DAY_OF_WEEK_MIN = 0;
   private static final int DAY_OF_WEEK_MAX = 6;
   private static final int LAST_DAY_MARKER = 32;
   private static final int CALENDAR_MONTH_OFFSET = 1;
   private static final int CALENDAR_DAY_OF_WEEK_OFFSET = 1;
   private static final int SEARCH_LIMIT_YEARS = 4;
   private static final int MINIMUM_CRON_FIELDS = 5;
   private static final int MAXIMUM_CRON_FIELDS = 6;
   private static final int CRON_PARTS_EXPECTED = 2;
   private static final String PIPE_SEPARATOR = "\\|";
   private static final String WHITESPACE_PATTERN = "\\s+";
   private static final String FIELD_VALIDATION_REGEX = "^[0-9a-zA-Z*,\\-/:~+L]+$";
   private static final String NO_FUTURE_MATCH_MESSAGE = "No future match.";
   private static final Map<String, Integer> MONTH_ALIASES = new HashMap<>();
   private static final Map<String, Integer> DAY_ALIASES = new HashMap<>();
   private final String _originalPattern;
   private final List<SchedulingPattern.CronExpression> _cronExpressions;

   public SchedulingPattern(String pattern) throws RuntimeException {
      this._originalPattern = Objects.requireNonNull(pattern, "Pattern cannot be null.");

      try {
         this._cronExpressions = this.parsePattern(pattern);
      } catch (Exception var3) {
         throw new RuntimeException("Invalid scheduling pattern: " + pattern, var3);
      }
   }

   public static boolean validate(String schedulingPattern) {
      if (schedulingPattern == null) {
         return false;
      } else {
         try {
            String[] orPatterns = schedulingPattern.split("\\|");
            String[] var2 = orPatterns;
            int var3 = orPatterns.length;
            int var4 = 0;

            while (var4 < var3) {
               String orPattern = var2[var4];
               String[] fields = orPattern.trim().split("\\s+");
               if (fields.length >= 5 && fields.length <= 6) {
                  if (isValidField(fields[0]) && isValidField(fields[1]) && isValidField(fields[2]) && isValidField(fields[3]) && isValidField(fields[4])) {
                     if (fields.length == 6) {
                        String weekField = fields[5].trim();
                        if (!weekField.startsWith("+") || !StringUtil.isNumeric(weekField.substring(1))) {
                           return false;
                        }
                     }

                     var4++;
                     continue;
                  }

                  return false;
               }

               return false;
            }

            return true;
         } catch (Exception var8) {
            return false;
         }
      }
   }

   private static boolean isValidField(String field) {
      return field != null && !field.trim().isEmpty() ? field.matches("^[0-9a-zA-Z*,\\-/:~+L]+$") : false;
   }

   public boolean match(TimeZone timezone, long millis) {
      Calendar calendar = Calendar.getInstance(timezone);
      calendar.setTimeInMillis(millis);
      calendar.set(13, 0);
      calendar.set(14, 0);

      for (SchedulingPattern.CronExpression cronExpression : this._cronExpressions) {
         if (cronExpression.matches(calendar)) {
            return true;
         }
      }

      return false;
   }

   public boolean match(long millis) {
      return this.match(TimeZone.getDefault(), millis);
   }

   public long next(TimeZone timezone, long millis) {
      long earliestMatch = -1L;

      for (SchedulingPattern.CronExpression cronExpression : this._cronExpressions) {
         long nextMatch = cronExpression.getNextMatch(millis, timezone);
         if (nextMatch > millis && (earliestMatch == -1L || nextMatch < earliestMatch)) {
            earliestMatch = nextMatch;
         }
      }

      return earliestMatch;
   }

   public long next(long millis) {
      return this.next(TimeZone.getDefault(), millis);
   }

   public long nextFrom(long millis) {
      long nextMatch = this.next(millis);
      return nextMatch > millis ? nextMatch - millis : -1L;
   }

   public long nextFromNow() {
      return this.nextFrom(System.currentTimeMillis());
   }

   public long getDelayToNextFromNow() {
      return this.nextFromNow();
   }

   public long getOffsettedDelayToNextFromNow(int offsetInMinutes) {
      long delay = this.getDelayToNextFromNow();
      long offsetMillis = TimeUnit.MINUTES.toMillis(offsetInMinutes);
      return Math.max(0L, delay - offsetMillis);
   }

   public String getNextAsFormattedDateString() {
      long nextMatch = this.next(System.currentTimeMillis());
      return nextMatch > 0L ? new Date(nextMatch).toString() : "No future match.";
   }

   @Override
   public String toString() {
      return this._originalPattern;
   }

   private List<SchedulingPattern.CronExpression> parsePattern(String pattern) {
      List<SchedulingPattern.CronExpression> result = new ArrayList<>();
      String[] orPatterns = pattern.split("\\|");

      for (String orPattern : orPatterns) {
         String[] fields = orPattern.trim().split("\\s+");
         if (fields.length < 5 || fields.length > 6) {
            throw new IllegalArgumentException("Pattern must have 5 or 6 fields: " + orPattern);
         }

         try {
            SchedulingPattern.ExtendedFieldResult minuteResult = this.parseExtendedField(fields[0]);
            SchedulingPattern.ExtendedFieldResult hourResult = this.parseExtendedField(fields[1]);
            SchedulingPattern.ExtendedFieldResult dayResult = this.parseExtendedField(fields[2]);
            SchedulingPattern.FieldMatcher minuteMatcher = this.parseField(minuteResult.pattern, 0, 59, null);
            SchedulingPattern.FieldMatcher hourMatcher = this.parseField(hourResult.pattern, 0, 23, null);
            SchedulingPattern.FieldMatcher dayMatcher = this.parseField(dayResult.pattern, 1, 31, null);
            SchedulingPattern.FieldMatcher monthMatcher = this.parseField(fields[3], 1, 12, MONTH_ALIASES);
            SchedulingPattern.FieldMatcher dayOfWeekMatcher = this.parseField(fields[4], 0, 6, DAY_ALIASES);
            int weekOffset = 0;
            if (fields.length == 6) {
               String weekField = fields[5].trim();
               if (!weekField.startsWith("+")) {
                  throw new IllegalArgumentException("Week offset must start with '+': " + weekField);
               }

               weekOffset = Integer.parseInt(weekField.substring(1));
            }

            result.add(
               new SchedulingPattern.CronExpression(
                  minuteMatcher,
                  hourMatcher,
                  dayMatcher,
                  monthMatcher,
                  dayOfWeekMatcher,
                  minuteResult.randomModifier,
                  hourResult.randomModifier,
                  hourResult.addModifier,
                  dayResult.addModifier,
                  weekOffset
               )
            );
         } catch (Exception var19) {
            throw new IllegalArgumentException("Invalid pattern format: " + orPattern, var19);
         }
      }

      return result;
   }

   private SchedulingPattern.ExtendedFieldResult parseExtendedField(String field) {
      if (!field.contains(":")) {
         return new SchedulingPattern.ExtendedFieldResult(field, 0, 0);
      } else {
         String[] parts = field.split(":");
         if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid extended field format: " + field);
         } else {
            String modifier = parts[0];
            String pattern = parts[1];
            int randomModifier = 0;
            int addModifier = 0;
            if (modifier.startsWith("~")) {
               randomModifier = Integer.parseInt(modifier.substring(1));
            } else if (modifier.startsWith("+")) {
               addModifier = Integer.parseInt(modifier.substring(1));
            } else if (!modifier.isEmpty()) {
               throw new IllegalArgumentException("Unknown modifier: " + modifier);
            }

            return new SchedulingPattern.ExtendedFieldResult(pattern, randomModifier, addModifier);
         }
      }
   }

   private SchedulingPattern.FieldMatcher parseField(String field, int min, int max, Map<String, Integer> aliases) {
      if ("*".equals(field)) {
         return new SchedulingPattern.WildcardMatcher();
      } else {
         Set<Integer> values = new HashSet<>();
         String[] parts = field.split(",");

         for (String part : parts) {
            values.addAll(this.parseFieldPart(part.trim(), min, max, aliases));
         }

         return new SchedulingPattern.ValueSetMatcher(values);
      }
   }

   private Set<Integer> parseFieldPart(String part, int min, int max, Map<String, Integer> aliases) {
      Set<Integer> values = new HashSet<>();
      String[] stepParts = part.split("/");
      int step = stepParts.length > 1 ? Integer.parseInt(stepParts[1]) : 1;
      String rangePart = stepParts[0];
      if ("*".equals(rangePart)) {
         for (int i = min; i <= max; i += step) {
            values.add(i);
         }
      } else if (rangePart.contains("-")) {
         String[] range = rangePart.split("-", 2);
         int start = this.parseValue(range[0], aliases);
         int end = this.parseValue(range[1], aliases);
         if (start <= end) {
            for (int i = start; i <= end; i += step) {
               values.add(i);
            }
         } else {
            for (int i = start; i <= max; i += step) {
               values.add(i);
            }

            for (int i = min; i <= end; i += step) {
               values.add(i);
            }
         }
      } else {
         values.add(this.parseValue(rangePart, aliases));
      }

      return values;
   }

   private int parseValue(String value, Map<String, Integer> aliases) {
      if ("L".equalsIgnoreCase(value)) {
         return 32;
      } else if (aliases != null && aliases.containsKey(value.toLowerCase())) {
         return aliases.get(value.toLowerCase());
      } else {
         try {
            return Integer.parseInt(value);
         } catch (NumberFormatException var4) {
            throw new IllegalArgumentException("Invalid value: " + value, var4);
         }
      }
   }

   static {
      MONTH_ALIASES.put("jan", 1);
      MONTH_ALIASES.put("feb", 2);
      MONTH_ALIASES.put("mar", 3);
      MONTH_ALIASES.put("apr", 4);
      MONTH_ALIASES.put("may", 5);
      MONTH_ALIASES.put("jun", 6);
      MONTH_ALIASES.put("jul", 7);
      MONTH_ALIASES.put("aug", 8);
      MONTH_ALIASES.put("sep", 9);
      MONTH_ALIASES.put("oct", 10);
      MONTH_ALIASES.put("nov", 11);
      MONTH_ALIASES.put("dec", 12);
      DAY_ALIASES.put("sun", 0);
      DAY_ALIASES.put("mon", 1);
      DAY_ALIASES.put("tue", 2);
      DAY_ALIASES.put("wed", 3);
      DAY_ALIASES.put("thu", 4);
      DAY_ALIASES.put("fri", 5);
      DAY_ALIASES.put("sat", 6);
   }

   private static class CronExpression {
      private final SchedulingPattern.FieldMatcher _minuteMatcher;
      private final SchedulingPattern.FieldMatcher _hourMatcher;
      private final SchedulingPattern.FieldMatcher _dayMatcher;
      private final SchedulingPattern.FieldMatcher _monthMatcher;
      private final SchedulingPattern.FieldMatcher _dayOfWeekMatcher;
      private final int _minuteRandomModifier;
      private final int _hourRandomModifier;
      private final int _hourAddModifier;
      private final int _dayAddModifier;
      private final int _weekOffset;

      CronExpression(
         SchedulingPattern.FieldMatcher minuteMatcher,
         SchedulingPattern.FieldMatcher hourMatcher,
         SchedulingPattern.FieldMatcher dayMatcher,
         SchedulingPattern.FieldMatcher monthMatcher,
         SchedulingPattern.FieldMatcher dayOfWeekMatcher,
         int minuteRandomModifier,
         int hourRandomModifier,
         int hourAddModifier,
         int dayAddModifier,
         int weekOffset
      ) {
         this._minuteMatcher = minuteMatcher;
         this._hourMatcher = hourMatcher;
         this._dayMatcher = dayMatcher;
         this._monthMatcher = monthMatcher;
         this._dayOfWeekMatcher = dayOfWeekMatcher;
         this._minuteRandomModifier = minuteRandomModifier;
         this._hourRandomModifier = hourRandomModifier;
         this._hourAddModifier = hourAddModifier;
         this._dayAddModifier = dayAddModifier;
         this._weekOffset = weekOffset;
      }

      boolean matches(Calendar calendar) {
         Calendar testCalendar = Calendar.getInstance(calendar.getTimeZone());
         testCalendar.setTimeInMillis(calendar.getTimeInMillis());
         if (this._weekOffset != 0) {
            testCalendar.add(3, -this._weekOffset);
         }

         if (this._dayAddModifier != 0) {
            testCalendar.add(6, -this._dayAddModifier);
         }

         if (this._hourAddModifier != 0) {
            testCalendar.add(11, -this._hourAddModifier);
         }

         int minute = testCalendar.get(12);
         int hour = testCalendar.get(11);
         int day = testCalendar.get(5);
         int month = testCalendar.get(2) + 1;
         int dayOfWeek = testCalendar.get(7) - 1;
         return this._minuteMatcher.matches(minute, testCalendar)
            && this._hourMatcher.matches(hour, testCalendar)
            && this._dayMatcher.matches(day, testCalendar)
            && this._monthMatcher.matches(month, testCalendar)
            && this._dayOfWeekMatcher.matches(dayOfWeek, testCalendar);
      }

      long getNextMatch(long afterMillis, TimeZone timeZone) {
         Calendar calendar = Calendar.getInstance(timeZone);
         calendar.setTimeInMillis(afterMillis);
         calendar.add(12, 1);
         calendar.set(13, 0);
         calendar.set(14, 0);
         Calendar endCalendar = Calendar.getInstance(timeZone);
         endCalendar.setTimeInMillis(afterMillis);
         endCalendar.add(1, 4);
         Calendar resultCalendar = Calendar.getInstance(timeZone);

         while (calendar.before(endCalendar)) {
            if (this.matches(calendar)) {
               resultCalendar.setTimeInMillis(calendar.getTimeInMillis());
               if (this._weekOffset != 0) {
                  resultCalendar.add(3, this._weekOffset);
               }

               if (this._dayAddModifier != 0) {
                  resultCalendar.add(6, this._dayAddModifier);
               }

               if (this._hourAddModifier != 0) {
                  resultCalendar.add(11, this._hourAddModifier);
               }

               if (this._hourRandomModifier > 0) {
                  resultCalendar.add(11, Rnd.get(this._hourRandomModifier + 1));
               }

               if (this._minuteRandomModifier > 0) {
                  resultCalendar.add(12, Rnd.get(this._minuteRandomModifier + 1));
               }

               return resultCalendar.getTimeInMillis();
            }

            calendar.add(12, 1);
         }

         return -1L;
      }
   }

   private static class ExtendedFieldResult {
      final String pattern;
      final int randomModifier;
      final int addModifier;

      ExtendedFieldResult(String pattern, int randomModifier, int addModifier) {
         this.pattern = pattern;
         this.randomModifier = randomModifier;
         this.addModifier = addModifier;
      }
   }

   private interface FieldMatcher {
      boolean matches(int var1, Calendar var2);
   }

   private static class ValueSetMatcher implements SchedulingPattern.FieldMatcher {
      private final Set<Integer> _values;

      ValueSetMatcher(Set<Integer> values) {
         this._values = new HashSet<>(values);
      }

      @Override
      public boolean matches(int value, Calendar calendar) {
         return this._values.contains(value) ? true : this._values.contains(32) && this.isLastDayOfMonth(calendar);
      }

      private boolean isLastDayOfMonth(Calendar calendar) {
         int currentDay = calendar.get(5);
         int lastDay = calendar.getActualMaximum(5);
         return currentDay == lastDay;
      }
   }

   private static class WildcardMatcher implements SchedulingPattern.FieldMatcher {
      @Override
      public boolean matches(int value, Calendar calendar) {
         return true;
      }
   }
}
