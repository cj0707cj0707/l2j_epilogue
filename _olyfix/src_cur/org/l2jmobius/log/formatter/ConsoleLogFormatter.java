package org.l2jmobius.log.formatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.commons.util.TraceUtil;

public class ConsoleLogFormatter extends Formatter {
   private static final DateTimeFormatter _dateFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

   @Override
   public String format(LogRecord record) {
      StringBuilder output = new StringBuilder(128);
      StringUtil.append(
         output,
         "[",
         _dateFormat.format(Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault())),
         "] " + record.getMessage(),
         System.lineSeparator()
      );
      if (record.getThrown() != null) {
         try {
            StringUtil.append(output, TraceUtil.getStackTrace(record.getThrown()), System.lineSeparator());
         } catch (Exception var4) {
         }
      }

      return output.toString();
   }
}
