package org.l2jmobius.log.formatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import org.l2jmobius.commons.util.StringUtil;

public class AuditFormatter extends Formatter {
   private static final DateTimeFormatter _dateFormat = DateTimeFormatter.ofPattern("dd MMM H:mm:ss");

   @Override
   public String format(LogRecord record) {
      Object[] params = record.getParameters();
      StringBuilder output = new StringBuilder(30 + record.getMessage().length() + (params == null ? 0 : params.length * 10));
      StringUtil.append(output, "[", _dateFormat.format(Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault())), "] ", record.getMessage());
      if (params != null) {
         output.append(Arrays.stream(params).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(", ")));
      }

      output.append(System.lineSeparator());
      return output.toString();
   }
}
