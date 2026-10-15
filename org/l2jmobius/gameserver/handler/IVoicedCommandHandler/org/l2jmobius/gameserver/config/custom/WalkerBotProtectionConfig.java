package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class WalkerBotProtectionConfig {
   private static final String WALKER_BOT_PROTECTION_CONFIG_FILE = "./config/Custom/WalkerBotProtection.ini";
   public static boolean L2WALKER_PROTECTION;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/WalkerBotProtection.ini");
      L2WALKER_PROTECTION = config.getBoolean("L2WalkerProtection", false);
   }
}
