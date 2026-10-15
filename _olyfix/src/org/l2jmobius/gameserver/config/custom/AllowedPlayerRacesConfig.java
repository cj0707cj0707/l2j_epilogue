package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class AllowedPlayerRacesConfig {
   private static final String ALLOWED_PLAYER_RACES_CONFIG_FILE = "./config/Custom/AllowedPlayerRaces.ini";
   public static boolean ALLOW_HUMAN;
   public static boolean ALLOW_ELF;
   public static boolean ALLOW_DARKELF;
   public static boolean ALLOW_ORC;
   public static boolean ALLOW_DWARF;
   public static boolean ALLOW_KAMAEL;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/AllowedPlayerRaces.ini");
      ALLOW_HUMAN = config.getBoolean("AllowHuman", true);
      ALLOW_ELF = config.getBoolean("AllowElf", true);
      ALLOW_DARKELF = config.getBoolean("AllowDarkElf", true);
      ALLOW_ORC = config.getBoolean("AllowOrc", true);
      ALLOW_DWARF = config.getBoolean("AllowDwarf", true);
      ALLOW_KAMAEL = config.getBoolean("AllowKamael", true);
   }
}
