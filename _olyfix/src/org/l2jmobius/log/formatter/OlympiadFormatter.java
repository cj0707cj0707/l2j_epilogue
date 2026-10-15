package org.l2jmobius.log.formatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.l2jmobius.commons.util.StringUtil;

public class OlympiadFormatter extends Formatter {
   private static final DateTimeFormatter _dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm:ss");

   @Override
   public String format(LogRecord record) {
      Object[] params = record.getParameters();
      StringBuilder output = new StringBuilder(30 + record.getMessage().length() + (params == null ? 0 : params.length * 10));
      StringUtil.append(output, _dateFormat.format(Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault())), ",", record.getMessage());
      if (params != null) {
         for (Object p : params) {
            if (p != null) {
               StringUtil.append(output, ",", p.toString());
            }
         }
      }

      output.append(System.lineSeparator());
      return output.toString();
   }
}
