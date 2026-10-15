package org.l2jmobius.gameserver.config;

import org.l2jmobius.commons.util.ConfigReader;

public class IdManagerConfig {
   private static final String ID_MANAGER_CONFIG_FILE = "./config/IdManager.ini";
   public static boolean DATABASE_CLEAN_UP;
   public static int FIRST_OBJECT_ID;
   public static int LAST_OBJECT_ID;
   public static int INITIAL_CAPACITY;
   public static double RESIZE_THRESHOLD;
   public static double RESIZE_MULTIPLIER;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/IdManager.ini");
      DATABASE_CLEAN_UP = config.getBoolean("DatabaseCleanUp", true);
      FIRST_OBJECT_ID = config.getInt("FirstObjectId", 268435456);
      LAST_OBJECT_ID = config.getInt("LastObjectId", Integer.MAX_VALUE);
      INITIAL_CAPACITY = Math.min(config.getInt("InitialCapacity", 100000), LAST_OBJECT_ID - 1);
      RESIZE_THRESHOLD = config.getDouble("ResizeThreshold", 0.9);
      RESIZE_MULTIPLIER = config.getDouble("ResizeMultiplier", 1.1);
   }
}
