package org.l2jmobius.log.formatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.l2jmobius.commons.util.StringUtil;

public class ChatLogFormatter extends Formatter {
   private static final DateTimeFormatter _dateFormat = DateTimeFormatter.ofPattern("dd MMM H:mm:ss");

   @Override
   public String format(LogRecord record) {
      Object[] params = record.getParameters();
      StringBuilder output = new StringBuilder(30 + record.getMessage().length() + (params != null ? 10 * params.length : 0));
      StringUtil.append(output, "[", _dateFormat.format(Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault())), "] ");
      if (params != null) {
         for (Object p : params) {
            StringUtil.append(output, p, " ");
         }
      }

      StringUtil.append(output, record.getMessage(), System.lineSeparator());
      return output.toString();
   }
}
