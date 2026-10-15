package org.l2jmobius.gameserver.config;

import org.l2jmobius.commons.util.ConfigReader;

public class GrandBossConfig {
   private static final String GRANDBOSS_CONFIG_FILE = "./config/GrandBoss.ini";
   public static int ANTHARAS_WAIT_TIME;
   public static int ANTHARAS_SPAWN_INTERVAL;
   public static int ANTHARAS_SPAWN_RANDOM;
   public static boolean ANTHARAS_RECOGNIZE_HERO;
   public static int VALAKAS_WAIT_TIME;
   public static int VALAKAS_SPAWN_INTERVAL;
   public static int VALAKAS_SPAWN_RANDOM;
   public static boolean VALAKAS_RECOGNIZE_HERO;
   public static int BAIUM_SPAWN_INTERVAL;
   public static int BAIUM_SPAWN_RANDOM;
   public static boolean BAIUM_RECOGNIZE_HERO;
   public static int CORE_SPAWN_INTERVAL;
   public static int CORE_SPAWN_RANDOM;
   public static int ORFEN_SPAWN_INTERVAL;
   public static int ORFEN_SPAWN_RANDOM;
   public static int QUEEN_ANT_SPAWN_INTERVAL;
   public static int QUEEN_ANT_SPAWN_RANDOM;
   public static int FRINTEZZA_SPAWN_INTERVAL;
   public static int FRINTEZZA_SPAWN_RANDOM;
   public static int BELETH_SPAWN_INTERVAL;
   public static int BELETH_SPAWN_RANDOM;
   public static int BELETH_MIN_PLAYERS;
   public static boolean DISABLE_RAIDBOSS_HEAL_FROM_PLAYERS;
   public static boolean DISABLE_RAIDBOSS_BUFF_FROM_PLAYERS;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/GrandBoss.ini");
      ANTHARAS_WAIT_TIME = config.getInt("AntharasWaitTime", 30);
      ANTHARAS_SPAWN_INTERVAL = config.getInt("IntervalOfAntharasSpawn", 264);
      ANTHARAS_SPAWN_RANDOM = config.getInt("RandomOfAntharasSpawn", 72);
      ANTHARAS_RECOGNIZE_HERO = config.getBoolean("AntharasRecognizeHero", true);
      VALAKAS_WAIT_TIME = config.getInt("ValakasWaitTime", 30);
      VALAKAS_SPAWN_INTERVAL = config.getInt("IntervalOfValakasSpawn", 264);
      VALAKAS_SPAWN_RANDOM = config.getInt("RandomOfValakasSpawn", 72);
      VALAKAS_RECOGNIZE_HERO = config.getBoolean("ValakasRecognizeHero", true);
      BAIUM_SPAWN_INTERVAL = config.getInt("IntervalOfBaiumSpawn", 168);
      BAIUM_SPAWN_RANDOM = config.getInt("RandomOfBaiumSpawn", 48);
      BAIUM_RECOGNIZE_HERO = config.getBoolean("BaiumRecognizeHero", true);
      CORE_SPAWN_INTERVAL = config.getInt("IntervalOfCoreSpawn", 60);
      CORE_SPAWN_RANDOM = config.getInt("RandomOfCoreSpawn", 24);
      ORFEN_SPAWN_INTERVAL = config.getInt("IntervalOfOrfenSpawn", 48);
      ORFEN_SPAWN_RANDOM = config.getInt("RandomOfOrfenSpawn", 20);
      QUEEN_ANT_SPAWN_INTERVAL = config.getInt("IntervalOfQueenAntSpawn", 36);
      QUEEN_ANT_SPAWN_RANDOM = config.getInt("RandomOfQueenAntSpawn", 17);
      FRINTEZZA_SPAWN_INTERVAL = config.getInt("IntervalOfFrintezzaSpawn", 48);
      FRINTEZZA_SPAWN_RANDOM = config.getInt("RandomOfFrintezzaSpawn", 8);
      BELETH_SPAWN_INTERVAL = config.getInt("IntervalOfBelethSpawn", 192);
      BELETH_SPAWN_RANDOM = config.getInt("RandomOfBelethSpawn", 148);
      BELETH_MIN_PLAYERS = config.getInt("BelethMinPlayers", 36);
      DISABLE_RAIDBOSS_HEAL_FROM_PLAYERS = config.getBoolean("DisableRaidBossHealFromPlayers", false);
      DISABLE_RAIDBOSS_BUFF_FROM_PLAYERS = config.getBoolean("DisableRaidBossBuffFromPlayers", false);
   }
}
