package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class PvpRewardItemConfig {
   private static final String PVP_REWARD_ITEM_CONFIG_FILE = "./config/Custom/PvpRewardItem.ini";
   public static boolean REWARD_PVP_ITEM;
   public static int REWARD_PVP_ITEM_ID;
   public static int REWARD_PVP_ITEM_AMOUNT;
   public static boolean REWARD_PVP_ITEM_MESSAGE;
   public static boolean REWARD_PK_ITEM;
   public static int REWARD_PK_ITEM_ID;
   public static int REWARD_PK_ITEM_AMOUNT;
   public static boolean REWARD_PK_ITEM_MESSAGE;
   public static boolean DISABLE_REWARDS_IN_INSTANCES;
   public static boolean DISABLE_REWARDS_IN_PVP_ZONES;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/PvpRewardItem.ini");
      REWARD_PVP_ITEM = config.getBoolean("RewardPvpItem", false);
      REWARD_PVP_ITEM_ID = config.getInt("RewardPvpItemId", 57);
      REWARD_PVP_ITEM_AMOUNT = config.getInt("RewardPvpItemAmount", 1000);
      REWARD_PVP_ITEM_MESSAGE = config.getBoolean("RewardPvpItemMessage", true);
      REWARD_PK_ITEM = config.getBoolean("RewardPkItem", false);
      REWARD_PK_ITEM_ID = config.getInt("RewardPkItemId", 57);
      REWARD_PK_ITEM_AMOUNT = config.getInt("RewardPkItemAmount", 500);
      REWARD_PK_ITEM_MESSAGE = config.getBoolean("RewardPkItemMessage", true);
      DISABLE_REWARDS_IN_INSTANCES = config.getBoolean("DisableRewardsInInstances", true);
      DISABLE_REWARDS_IN_PVP_ZONES = config.getBoolean("DisableRewardsInPvpZones", true);
   }
}
