package org.l2jmobius.gameserver.config;

import org.l2jmobius.commons.util.ConfigReader;

public class ConquerableHallSiegeConfig {
   private static final String CONQUERABLE_HALL_SIEGE_CONFIG_FILE = "./config/ConquerableHallSiege.ini";
   public static int CHS_MAX_ATTACKERS;
   public static int CHS_CLAN_MINLEVEL;
   public static int CHS_MAX_FLAGS_PER_CLAN;
   public static boolean CHS_ENABLE_FAME;
   public static int CHS_FAME_AMOUNT;
   public static int CHS_FAME_FREQUENCY;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/ConquerableHallSiege.ini");
      CHS_MAX_ATTACKERS = config.getInt("MaxAttackers", 500);
      CHS_CLAN_MINLEVEL = config.getInt("MinClanLevel", 4);
      CHS_MAX_FLAGS_PER_CLAN = config.getInt("MaxFlagsPerClan", 1);
      CHS_ENABLE_FAME = config.getBoolean("EnableFame", false);
      CHS_FAME_AMOUNT = config.getInt("FameAmount", 0);
      CHS_FAME_FREQUENCY = config.getInt("FameFrequency", 0);
   }
}
