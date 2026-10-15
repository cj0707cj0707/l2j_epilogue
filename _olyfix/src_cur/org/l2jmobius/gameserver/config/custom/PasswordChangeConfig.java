package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class PasswordChangeConfig {
   private static final String PASSWORD_CHANGE_CONFIG_FILE = "./config/Custom/PasswordChange.ini";
   public static boolean ALLOW_CHANGE_PASSWORD;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/PasswordChange.ini");
      ALLOW_CHANGE_PASSWORD = config.getBoolean("AllowChangePassword", false);
   }
}
