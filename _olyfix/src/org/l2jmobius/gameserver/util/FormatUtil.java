package org.l2jmobius.gameserver.util;

import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtil {
   private static final NumberFormat ADENA_FORMATTER = NumberFormat.getIntegerInstance(Locale.ENGLISH);

   public static String formatAdena(long amount) {
      synchronized (ADENA_FORMATTER) {
         return ADENA_FORMATTER.format(amount);
      }
   }
}
