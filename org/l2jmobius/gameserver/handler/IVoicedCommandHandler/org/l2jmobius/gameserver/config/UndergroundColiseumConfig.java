package org.l2jmobius.gameserver.config;

import org.l2jmobius.commons.util.ConfigReader;

public class UndergroundColiseumConfig {
   private static final String UNDERGROUND_COLISEUM_CONFIG_FILE = "./config/UndergroundColiseum.ini";
   public static String UC_START_TIME;
   public static int UC_TIME_PERIOD;
   public static boolean UC_ALLOW_ANNOUNCE;
   public static int UC_PARTY_SIZE;
   public static int UC_RESS_TIME;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/UndergroundColiseum.ini");
      UC_START_TIME = config.getString("BattleStartTime", "0 17 * * *");
      UC_TIME_PERIOD = config.getInt("BattlePeriod", 2);
      UC_ALLOW_ANNOUNCE = config.getBoolean("AllowAnnouncements", false);
      UC_PARTY_SIZE = config.getInt("PartySize", 7);
      UC_RESS_TIME = config.getInt("ResurrectionTime", 10);
   }
}
