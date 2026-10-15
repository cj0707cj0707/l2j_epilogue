package org.l2jmobius.log.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class GMAuditFilter implements Filter {
   @Override
   public boolean isLoggable(LogRecord record) {
      return "gmaudit".equals(record.getLoggerName());
   }
}
