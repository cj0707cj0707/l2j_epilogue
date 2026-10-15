package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class SellBuffsConfig {
   private static final String SELL_BUFFS_CONFIG_FILE = "./config/Custom/SellBuffs.ini";
   public static boolean SELLBUFF_ENABLED;
   public static int SELLBUFF_MP_MULTIPLER;
   public static int SELLBUFF_PAYMENT_ID;
   public static long SELLBUFF_MIN_PRICE;
   public static long SELLBUFF_MAX_PRICE;
   public static int SELLBUFF_MAX_BUFFS;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/SellBuffs.ini");
      SELLBUFF_ENABLED = config.getBoolean("SellBuffEnable", false);
      SELLBUFF_MP_MULTIPLER = config.getInt("MpCostMultipler", 1);
      SELLBUFF_PAYMENT_ID = config.getInt("PaymentID", 57);
      SELLBUFF_MIN_PRICE = config.getLong("MinimumPrice", 100000L);
      SELLBUFF_MAX_PRICE = config.getLong("MaximumPrice", 100000000L);
      SELLBUFF_MAX_BUFFS = config.getInt("MaxBuffs", 15);
   }
}
