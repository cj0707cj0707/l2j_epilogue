package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class FreeMountsConfig {
   private static final String FREE_MOUNTS_CONFIG_FILE = "./config/Custom/FreeMounts.ini";
   public static boolean ENABLE_FREE_STRIDER;
   public static boolean ENABLE_FREE_WYVERN;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/FreeMounts.ini");
      ENABLE_FREE_STRIDER = config.getBoolean("EnableFreeStrider", false);
      ENABLE_FREE_WYVERN = config.getBoolean("EnableFreeWyvern", false);
   }
}
