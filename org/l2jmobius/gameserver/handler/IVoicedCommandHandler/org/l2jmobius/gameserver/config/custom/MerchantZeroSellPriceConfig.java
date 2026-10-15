package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class MerchantZeroSellPriceConfig {
   private static final String MERCHANT_ZERO_SELL_PRICE_CONFIG_FILE = "./config/Custom/MerchantZeroSellPrice.ini";
   public static boolean MERCHANT_ZERO_SELL_PRICE;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/MerchantZeroSellPrice.ini");
      MERCHANT_ZERO_SELL_PRICE = config.getBoolean("MerchantZeroSellPrice", false);
   }
}
