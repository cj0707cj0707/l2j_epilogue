package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class HellboundStatusConfig {
   private static final String HELLBOUND_STATUS_CONFIG_FILE = "./config/Custom/HellboundStatus.ini";
   public static boolean HELLBOUND_STATUS;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/HellboundStatus.ini");
      HELLBOUND_STATUS = config.getBoolean("HellboundStatus", false);
   }
}
