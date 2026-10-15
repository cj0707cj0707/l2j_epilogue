package org.l2jmobius.log.filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class AuditFilter implements Filter {
   @Override
   public boolean isLoggable(LogRecord record) {
      return "audit".equals(record.getLoggerName());
   }
}
