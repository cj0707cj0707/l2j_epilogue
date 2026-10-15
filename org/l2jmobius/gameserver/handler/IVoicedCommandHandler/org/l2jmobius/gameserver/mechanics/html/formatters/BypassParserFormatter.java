package org.l2jmobius.gameserver.mechanics.html.formatters;

import org.l2jmobius.gameserver.mechanics.html.IBypassFormatter;

public class BypassParserFormatter implements IBypassFormatter {
   public static final BypassParserFormatter INSTANCE = new BypassParserFormatter();

   @Override
   public String formatBypass(String bypass, int page) {
      return bypass + " page=" + page;
   }
}
