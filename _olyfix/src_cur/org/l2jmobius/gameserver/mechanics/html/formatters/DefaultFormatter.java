package org.l2jmobius.gameserver.mechanics.html.formatters;

import org.l2jmobius.gameserver.mechanics.html.IBypassFormatter;

public class DefaultFormatter implements IBypassFormatter {
   public static final DefaultFormatter INSTANCE = new DefaultFormatter();

   @Override
   public String formatBypass(String bypass, int page) {
      return bypass + " " + page;
   }
}
