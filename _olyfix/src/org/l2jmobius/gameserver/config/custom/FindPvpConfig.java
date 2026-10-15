package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class FindPvpConfig {
   private static final String FIND_PVP_CONFIG_FILE = "./config/Custom/FindPvP.ini";
   public static boolean ENABLE_FIND_PVP;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/FindPvP.ini");
      ENABLE_FIND_PVP = config.getBoolean("EnableFindPvP", false);
   }
}
