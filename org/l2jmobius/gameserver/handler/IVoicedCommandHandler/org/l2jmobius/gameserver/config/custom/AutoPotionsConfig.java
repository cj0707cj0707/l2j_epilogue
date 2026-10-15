package org.l2jmobius.gameserver.config.custom;

import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.commons.util.ConfigReader;

public class AutoPotionsConfig {
   private static final String AUTO_POTIONS_CONFIG_FILE = "./config/Custom/AutoPotions.ini";
   public static boolean AUTO_POTIONS_ENABLED;
   public static boolean AUTO_POTIONS_IN_OLYMPIAD;
   public static int AUTO_POTION_MIN_LEVEL;
   public static boolean AUTO_CP_ENABLED;
   public static boolean AUTO_HP_ENABLED;
   public static boolean AUTO_MP_ENABLED;
   public static int AUTO_CP_PERCENTAGE;
   public static int AUTO_HP_PERCENTAGE;
   public static int AUTO_MP_PERCENTAGE;
   public static Set<Integer> AUTO_CP_ITEM_IDS = new HashSet<>();
   public static Set<Integer> AUTO_HP_ITEM_IDS = new HashSet<>();
   public static Set<Integer> AUTO_MP_ITEM_IDS = new HashSet<>();

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/AutoPotions.ini");
      AUTO_POTIONS_ENABLED = config.getBoolean("AutoPotionsEnabled", false);
      AUTO_POTIONS_IN_OLYMPIAD = config.getBoolean("AutoPotionsInOlympiad", false);
      AUTO_POTION_MIN_LEVEL = config.getInt("AutoPotionMinimumLevel", 1);
      AUTO_CP_ENABLED = config.getBoolean("AutoCpEnabled", true);
      AUTO_HP_ENABLED = config.getBoolean("AutoHpEnabled", true);
      AUTO_MP_ENABLED = config.getBoolean("AutoMpEnabled", true);
      AUTO_CP_PERCENTAGE = config.getInt("AutoCpPercentage", 70);
      AUTO_HP_PERCENTAGE = config.getInt("AutoHpPercentage", 70);
      AUTO_MP_PERCENTAGE = config.getInt("AutoMpPercentage", 70);
      AUTO_CP_ITEM_IDS.clear();

      for (String s : config.getString("AutoCpItemIds", "0").split(",")) {
         AUTO_CP_ITEM_IDS.add(Integer.parseInt(s));
      }

      AUTO_HP_ITEM_IDS.clear();

      for (String s : config.getString("AutoHpItemIds", "0").split(",")) {
         AUTO_HP_ITEM_IDS.add(Integer.parseInt(s));
      }

      AUTO_MP_ITEM_IDS.clear();

      for (String s : config.getString("AutoMpItemIds", "0").split(",")) {
         AUTO_MP_ITEM_IDS.add(Integer.parseInt(s));
      }
   }
}
