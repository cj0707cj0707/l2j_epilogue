package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class WarehouseSortingConfig {
   private static final String WAREHOUSE_SORTING_CONFIG_FILE = "./config/Custom/WarehouseSorting.ini";
   public static boolean ENABLE_WAREHOUSESORTING_CLAN;
   public static boolean ENABLE_WAREHOUSESORTING_PRIVATE;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/WarehouseSorting.ini");
      ENABLE_WAREHOUSESORTING_CLAN = config.getBoolean("EnableWarehouseSortingClan", false);
      ENABLE_WAREHOUSESORTING_PRIVATE = config.getBoolean("EnableWarehouseSortingPrivate", false);
   }
}
