package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class BankingConfig {
   private static final String BANKING_CONFIG_FILE = "./config/Custom/Banking.ini";
   public static boolean BANKING_SYSTEM_ENABLED;
   public static int BANKING_SYSTEM_GOLDBAR_COUNT;
   public static int BANKING_SYSTEM_ADENA_COUNT;
   public static boolean BANKING_SYSTEM_AUTO_CONVERT_ENABLED;
   public static long BANKING_SYSTEM_AUTO_CONVERT_ADENA_LIMIT;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/Banking.ini");
      BANKING_SYSTEM_ENABLED = config.getBoolean("BankingEnabled", false);
      BANKING_SYSTEM_GOLDBAR_COUNT = config.getInt("BankingGoldbarCount", 1);
      BANKING_SYSTEM_ADENA_COUNT = config.getInt("BankingAdenaCount", 1000000000);
      BANKING_SYSTEM_AUTO_CONVERT_ENABLED = config.getBoolean("BankingAutoConvert", false);
      BANKING_SYSTEM_AUTO_CONVERT_ADENA_LIMIT = config.getLong("BankingAutoConvertAdenaLimit", 51000000000L);
   }
}
