package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class NoblessMasterConfig {
   private static final String NOBLESS_MASTER_CONFIG_FILE = "./config/Custom/NoblessMaster.ini";
   public static boolean NOBLESS_MASTER_ENABLED;
   public static int NOBLESS_MASTER_NPCID;
   public static int NOBLESS_MASTER_LEVEL_REQUIREMENT;
   public static int NOBLESS_MASTER_ITEM_ID;
   public static long NOBLESS_MASTER_ITEM_COUNT;
   public static boolean NOBLESS_MASTER_REWARD_TIARA;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/NoblessMaster.ini");
      NOBLESS_MASTER_ENABLED = config.getBoolean("Enabled", false);
      NOBLESS_MASTER_NPCID = config.getInt("NpcId", 1003000);
      NOBLESS_MASTER_LEVEL_REQUIREMENT = config.getInt("LevelRequirement", 80);
      NOBLESS_MASTER_ITEM_ID = config.getInt("ItemId", 57);
      NOBLESS_MASTER_ITEM_COUNT = config.getLong("ItemCount", 0L);
      NOBLESS_MASTER_REWARD_TIARA = config.getBoolean("RewardTiara", false);
   }
}
