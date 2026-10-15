package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class CustomMailManagerConfig {
   private static final String CUSTOM_MAIL_MANAGER_CONFIG_FILE = "./config/Custom/CustomMailManager.ini";
   public static boolean CUSTOM_MAIL_MANAGER_ENABLED;
   public static int CUSTOM_MAIL_MANAGER_DELAY;

   public static void load() {
      ConfigReader custom = new ConfigReader("./config/Custom/CustomMailManager.ini");
      CUSTOM_MAIL_MANAGER_ENABLED = custom.getBoolean("CustomMailManagerEnabled", false);
      CUSTOM_MAIL_MANAGER_DELAY = custom.getInt("DatabaseQueryDelay", 30) * 1000;
   }
}
