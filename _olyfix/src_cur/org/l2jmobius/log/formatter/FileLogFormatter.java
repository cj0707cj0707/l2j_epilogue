package org.l2jmobius.log.formatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.l2jmobius.commons.util.StringUtil;

public class FileLogFormatter extends Formatter {
   private static final String TAB = "\t";
   private static final DateTimeFormatter _dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss,SSS");

   @Override
   public String format(LogRecord record) {
      return StringUtil.concat(
         _dateFormat.format(Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault())),
         "\t",
         record.getLevel().getName(),
         "\t",
         String.valueOf(record.getLongThreadID()),
         "\t",
         record.getLoggerName(),
         "\t",
         record.getMessage(),
         System.lineSeparator()
      );
   }
}
