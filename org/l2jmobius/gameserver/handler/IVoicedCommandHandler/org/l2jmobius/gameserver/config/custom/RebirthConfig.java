package org.l2jmobius.gameserver.config.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.ConfigReader;

public class RebirthConfig {
   private static final Logger LOGGER = Logger.getLogger(RebirthConfig.class.getName());
   private static final String REBIRTH_CONFIG_FILE = "config/Custom/Rebirth.ini";
   public static boolean REBIRTH_ALLOW_REBIRTH;
   public static int REBIRTH_MIN_LEVEL;
   public static int REBIRTH_MAX_COUNT;
   public static boolean REBIRTH_INHERIT_SKILLS_TO_SUBCLASSES;
   public static boolean REBIRTH_DELETE_ALL_SKILLS;
   public static Set<Integer> REBIRTH_SKILL_DELETE_WHITELIST = new HashSet<>();
   public static Set<String> REBIRTH_DELETE_PROTECTED_SKILL_GROUPS = new HashSet<>();
   public static boolean REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS;
   public static int REBIRTH_ITEM_ID;
   public static int REBIRTH_ITEM_AMOUNT;
   public static int REBIRTH_MAX_SELECTED_SKILLS;
   public static int REBIRTH_REWARD_ITEM_ID;
   public static int REBIRTH_REWARD_ITEM_AMOUNT;
   public static String REBIRTH_SKILL_REFUND_MODE;
   public static List<String> REBIRTH_MAGE_SKILLS = new ArrayList<>();
   public static List<String> REBIRTH_FIGHTER_SKILLS = new ArrayList<>();
   public static int REBIRTH_FINISH_SOCIAL_ID;
   public static int REBIRTH_FINISH_EFFECT_SKILL_ID;
   public static int REBIRTH_FINISH_EFFECT_SKILL_LVL;
   public static int REBIRTH_SELECT_SOCIAL_ID;
   public static int REBIRTH_SELECT_EFFECT_SKILL_ID;
   public static int REBIRTH_SELECT_EFFECT_SKILL_LVL;
   public static boolean REBIRTH_SCREEN_MESSAGE_ENABLED;
   public static String REBIRTH_SCREEN_MESSAGE;
   public static String REBIRTH_SCREEN_MESSAGE_POSITION;
   public static int REBIRTH_SCREEN_MESSAGE_TIME;
   public static boolean REBIRTH_GLOBAL_ANNOUNCEMENT_ENABLED;
   public static String REBIRTH_GLOBAL_ANNOUNCEMENT;
   public static boolean REBIRTH_GLOBAL_ANNOUNCEMENT_CRITICAL;

   public static void load() {
      ConfigReader config = new ConfigReader("config/Custom/Rebirth.ini");
      REBIRTH_ALLOW_REBIRTH = config.getBoolean("RebirthAllow", true);
      REBIRTH_MIN_LEVEL = config.getInt("RebirthMin", 80);
      REBIRTH_MAX_COUNT = config.getInt("RebirthMaxCount", 3);
      REBIRTH_INHERIT_SKILLS_TO_SUBCLASSES = config.getBoolean("RebirthInheritSkillsToSubclasses", true);
      REBIRTH_DELETE_ALL_SKILLS = config.getBoolean("RebirthDeleteAllSkills", true);
      REBIRTH_SKILL_DELETE_WHITELIST = parseIntegerSet(config.getString("RebirthSkillDeleteWhitelist", ""));
      REBIRTH_DELETE_PROTECTED_SKILL_GROUPS = parseProtectedSkillGroups(config.getString("RebirthDeleteProtectedSkillGroups", ""));
      REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS = config.getBoolean("RebirthDeleteRestoreTemporarySkills", true);
      REBIRTH_ITEM_ID = config.getInt("RebirthItemId", 57);
      REBIRTH_ITEM_AMOUNT = config.getInt("RebirthItemAmount", 3000);
      REBIRTH_MAX_SELECTED_SKILLS = config.getInt("RebirthMaxSelectedSkills", 3);
      REBIRTH_REWARD_ITEM_ID = config.getInt("RebirthRewardItemId", 57);
      REBIRTH_REWARD_ITEM_AMOUNT = config.getInt("RebirthRewardItemAmount", 1);
      REBIRTH_SKILL_REFUND_MODE = config.getString("RebirthSkillRefundMode", "MID").trim().toUpperCase();
      REBIRTH_MAGE_SKILLS = parseSkillList(config.getString("RebirthMageSkills", ""));
      REBIRTH_FIGHTER_SKILLS = parseSkillList(config.getString("RebirthFighterSkills", ""));
      REBIRTH_FINISH_SOCIAL_ID = config.getInt("RebirthFinishSocialId", 3);
      REBIRTH_FINISH_EFFECT_SKILL_ID = config.getInt("RebirthFinishEffectSkillId", 2024);
      REBIRTH_FINISH_EFFECT_SKILL_LVL = config.getInt("RebirthFinishEffectSkillLevel", 1);
      REBIRTH_SELECT_SOCIAL_ID = config.getInt("RebirthSelectSocialId", 3);
      REBIRTH_SELECT_EFFECT_SKILL_ID = config.getInt("RebirthSelectEffectSkillId", 2024);
      REBIRTH_SELECT_EFFECT_SKILL_LVL = config.getInt("RebirthSelectEffectSkillLevel", 1);
      REBIRTH_SCREEN_MESSAGE_ENABLED = config.getBoolean("RebirthScreenMessageEnabled", true);
      REBIRTH_SCREEN_MESSAGE = config.getString("RebirthScreenMessage", "Congratulations %player%! Rebirth status acquired. [%rebirth_count%/%max_rebirth%]");
      REBIRTH_SCREEN_MESSAGE_POSITION = config.getString("RebirthScreenMessagePosition", "TOP_CENTER").trim().toUpperCase();
      REBIRTH_SCREEN_MESSAGE_TIME = config.getInt("RebirthScreenMessageTime", 5000);
      REBIRTH_GLOBAL_ANNOUNCEMENT_ENABLED = config.getBoolean("RebirthGlobalAnnouncementEnabled", true);
      REBIRTH_GLOBAL_ANNOUNCEMENT = config.getString("RebirthGlobalAnnouncement", "Player %player% has reached Rebirth %rebirth_count% of %max_rebirth%!");
      REBIRTH_GLOBAL_ANNOUNCEMENT_CRITICAL = config.getBoolean("RebirthGlobalAnnouncementCritical", false);
   }

   private static List<String> parseSkillList(String value) {
      return (List<String>)(value != null && !value.trim().isEmpty() ? Arrays.asList(value.split(";")) : new ArrayList<>());
   }

   private static Set<Integer> parseIntegerSet(String value) {
      Set<Integer> skillIds = new HashSet<>();
      if (value != null && !value.trim().isEmpty()) {
         for (String token : value.split(",")) {
            String trimmedToken = token.trim();
            if (!trimmedToken.isEmpty()) {
               try {
                  skillIds.add(Integer.parseInt(trimmedToken));
               } catch (NumberFormatException var8) {
                  LOGGER.log(Level.WARNING, "Invalid rebirth whitelist skill id: rawValue=" + trimmedToken + " fullValue=" + value, (Throwable)var8);
               }
            }
         }

         return skillIds;
      } else {
         return skillIds;
      }
   }

   private static Set<String> parseProtectedSkillGroups(String value) {
      Set<String> skillGroups = new HashSet<>();
      if (value != null && !value.trim().isEmpty()) {
         for (String token : value.split(",")) {
            String normalizedToken = token.trim().toUpperCase(Locale.ROOT);
            if (!normalizedToken.isEmpty()) {
               switch (normalizedToken) {
                  case "STARTING_CLASS":
                  case "FISHING":
                  case "NOBLE":
                  case "CLAN":
                  case "HERO":
                  case "COLLECT":
                  case "SUBCLASS":
                  case "SUBPLEDGE":
                  case "TRANSFER":
                  case "TRANSFORM":
                     skillGroups.add(normalizedToken);
                     break;
                  default:
                     LOGGER.warning("Ignoring invalid rebirth protected skill group: " + normalizedToken);
               }
            }
         }

         return skillGroups;
      } else {
         return skillGroups;
      }
   }
}
