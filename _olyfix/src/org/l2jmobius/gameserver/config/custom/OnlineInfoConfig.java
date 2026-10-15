package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class OnlineInfoConfig {
   private static final String ONLINE_INFO_CONFIG_FILE = "./config/Custom/OnlineInfo.ini";
   public static boolean ENABLE_ONLINE_COMMAND;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/OnlineInfo.ini");
      ENABLE_ONLINE_COMMAND = config.getBoolean("EnableOnlineCommand", false);
   }
}
