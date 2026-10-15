package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class PvpAnnounceConfig {
   private static final String PVP_ANNOUNCE_CONFIG_FILE = "./config/Custom/PvpAnnounce.ini";
   public static boolean ANNOUNCE_PK_PVP;
   public static boolean ANNOUNCE_PK_PVP_NORMAL_MESSAGE;
   public static String ANNOUNCE_PK_MSG;
   public static String ANNOUNCE_PVP_MSG;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/PvpAnnounce.ini");
      ANNOUNCE_PK_PVP = config.getBoolean("AnnouncePkPvP", false);
      ANNOUNCE_PK_PVP_NORMAL_MESSAGE = config.getBoolean("AnnouncePkPvPNormalMessage", true);
      ANNOUNCE_PK_MSG = config.getString("AnnouncePkMsg", "$killer has slaughtered $target");
      ANNOUNCE_PVP_MSG = config.getString("AnnouncePvpMsg", "$killer has defeated $target");
   }
}
