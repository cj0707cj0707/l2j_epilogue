package org.l2jmobius.gameserver.config.custom;

import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.commons.util.ConfigReader;

public class AutoPlayConfig {
   private static final String AUTO_PLAY_CONFIG_FILE = "./config/Custom/AutoPlay.ini";
   public static boolean ENABLE_AUTO_PLAY;
   public static boolean ENABLE_AUTO_POTION;
   public static boolean ENABLE_AUTO_SKILL;
   public static boolean ENABLE_AUTO_ITEM;
   public static boolean RESUME_AUTO_PLAY;
   public static boolean ENABLE_AUTO_ASSIST;
   public static int AUTO_PLAY_SHORT_RANGE;
   public static int AUTO_PLAY_LONG_RANGE;
   public static boolean AUTO_PLAY_PREMIUM;
   public static Set<Integer> DISABLED_AUTO_SKILLS = new HashSet<>();
   public static Set<Integer> DISABLED_AUTO_ITEMS = new HashSet<>();
   public static Set<Integer> IGNORED_AUTO_PICK_ITEMS = new HashSet<>();
   public static String AUTO_PLAY_LOGIN_MESSAGE;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/AutoPlay.ini");
      ENABLE_AUTO_PLAY = config.getBoolean("EnableAutoPlay", false);
      ENABLE_AUTO_POTION = config.getBoolean("EnableAutoPotion", true);
      ENABLE_AUTO_SKILL = config.getBoolean("EnableAutoSkill", true);
      ENABLE_AUTO_ITEM = config.getBoolean("EnableAutoItem", true);
      RESUME_AUTO_PLAY = config.getBoolean("ResumeAutoPlay", false);
      ENABLE_AUTO_ASSIST = config.getBoolean("AssistLeader", false);
      AUTO_PLAY_SHORT_RANGE = config.getInt("ShortRange", 600);
      AUTO_PLAY_LONG_RANGE = config.getInt("LongRange", 1400);
      AUTO_PLAY_PREMIUM = config.getBoolean("AutoPlayPremium", false);
      DISABLED_AUTO_SKILLS.clear();
      String disabledSkills = config.getString("DisabledSkillIds", "");
      if (!disabledSkills.isEmpty()) {
         for (String s : disabledSkills.split(",")) {
            DISABLED_AUTO_SKILLS.add(Integer.parseInt(s.trim()));
         }
      }

      DISABLED_AUTO_ITEMS.clear();
      String disabledItems = config.getString("DisabledItemIds", "");
      if (!disabledItems.isEmpty()) {
         for (String s : disabledItems.split(",")) {
            DISABLED_AUTO_ITEMS.add(Integer.parseInt(s.trim()));
         }
      }

      IGNORED_AUTO_PICK_ITEMS.clear();
      String ignoredAutoPickItems = config.getString("IgnoredAutoPickItems", "").trim();
      if (!ignoredAutoPickItems.isEmpty()) {
         for (String itemIdString : ignoredAutoPickItems.split(",")) {
            IGNORED_AUTO_PICK_ITEMS.add(Integer.parseInt(itemIdString.trim()));
         }
      }

      AUTO_PLAY_LOGIN_MESSAGE = config.getString("AutoPlayLoginMessage", "");
   }
}
