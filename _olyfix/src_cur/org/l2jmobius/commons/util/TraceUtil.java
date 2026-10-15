package org.l2jmobius.commons.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringJoiner;

public class TraceUtil {
   public static String getStackTrace(Throwable throwable) {
      StringWriter writer = new StringWriter();
      throwable.printStackTrace(new PrintWriter(writer));
      return writer.toString();
   }

   public static String getTraceString(StackTraceElement[] stackTraceElements) {
      StringJoiner joiner = new StringJoiner(System.lineSeparator());

      for (StackTraceElement stackTraceElement : stackTraceElements) {
         joiner.add(stackTraceElement.toString());
      }

      return joiner.toString();
   }
}
