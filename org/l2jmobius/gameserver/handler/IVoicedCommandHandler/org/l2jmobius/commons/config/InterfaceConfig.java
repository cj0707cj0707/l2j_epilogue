package org.l2jmobius.commons.config;

import java.awt.GraphicsEnvironment;
import org.l2jmobius.commons.util.ConfigReader;

public class InterfaceConfig {
   private static final String INTERFACE_CONFIG_FILE = "./config/Interface.ini";
   public static boolean ENABLE_GUI;
   public static boolean DARK_THEME;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Interface.ini");
      ENABLE_GUI = config.getBoolean("EnableGUI", true) && !GraphicsEnvironment.isHeadless();
      if (ENABLE_GUI) {
         DARK_THEME = config.getBoolean("DarkTheme", true);
      }
   }
}
