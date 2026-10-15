package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class ServerTimeConfig {
   private static final String SERVER_TIME_CONFIG_FILE = "./config/Custom/ServerTime.ini";
   public static boolean DISPLAY_SERVER_TIME;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/ServerTime.ini");
      DISPLAY_SERVER_TIME = config.getBoolean("DisplayServerTime", false);
   }
}
