package org.l2jmobius.gameserver.config;

import org.l2jmobius.commons.util.ConfigReader;

public class GraciaSeedsConfig {
   private static final String GRACIA_SEEDS_CONFIG_FILE = "./config/GraciaSeeds.ini";
   public static int SOD_TIAT_KILL_COUNT;
   public static long SOD_STAGE_2_LENGTH;
   public static int MIN_TIAT_PLAYERS;
   public static int MAX_TIAT_PLAYERS;
   public static int MIN_TIAT_LEVEL;
   public static int SOI_EKIMUS_KILL_COUNT;
   public static int EROSION_ATTACK_MIN_PLAYERS;
   public static int EROSION_ATTACK_MAX_PLAYERS;
   public static int EROSION_DEFENCE_MIN_PLAYERS;
   public static int EROSION_DEFENCE_MAX_PLAYERS;
   public static int HEART_ATTACK_MIN_PLAYERS;
   public static int HEART_ATTACK_MAX_PLAYERS;
   public static int HEART_DEFENCE_MIN_PLAYERS;
   public static int HEART_DEFENCE_MAX_PLAYERS;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/GraciaSeeds.ini");
      SOD_TIAT_KILL_COUNT = config.getInt("TiatKillCountForNextState", 10);
      SOD_STAGE_2_LENGTH = config.getLong("Stage2Length", 720L) * 60000L;
      MIN_TIAT_PLAYERS = config.getInt("MinPlayers", 36);
      MAX_TIAT_PLAYERS = config.getInt("MaxPlayers", 45);
      MIN_TIAT_LEVEL = config.getInt("MinLevel", 75);
      SOI_EKIMUS_KILL_COUNT = config.getInt("EkimusKillCount", 5);
      EROSION_ATTACK_MIN_PLAYERS = config.getInt("MinEroAttPlayers", 18);
      EROSION_ATTACK_MAX_PLAYERS = config.getInt("MaxEroAttPlayers", 27);
      EROSION_DEFENCE_MIN_PLAYERS = config.getInt("MinEroDefPlayers", 18);
      EROSION_DEFENCE_MAX_PLAYERS = config.getInt("MaxEroDefPlayers", 27);
      HEART_ATTACK_MIN_PLAYERS = config.getInt("MinHeaAttPlayers", 18);
      HEART_ATTACK_MAX_PLAYERS = config.getInt("MaxHeaAttPlayers", 27);
      HEART_DEFENCE_MIN_PLAYERS = config.getInt("MinHeaDefPlayers", 18);
      HEART_DEFENCE_MAX_PLAYERS = config.getInt("MaxHeaDefPlayers", 27);
   }
}
