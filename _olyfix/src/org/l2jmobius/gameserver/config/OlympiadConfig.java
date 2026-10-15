package org.l2jmobius.gameserver.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.l2jmobius.commons.util.ConfigReader;

public class OlympiadConfig {
   private static final String OLYMPIAD_CONFIG_FILE = "./config/Olympiad.ini";
   public static boolean OLYMPIAD_ENABLED;
   public static int OLYMPIAD_START_TIME;
   public static int OLYMPIAD_MIN;
   public static int OLYMPIAD_MAX_BUFFS;
   public static long OLYMPIAD_CPERIOD;
   public static long OLYMPIAD_BATTLE;
   public static long OLYMPIAD_WPERIOD;
   public static long OLYMPIAD_VPERIOD;
   public static int OLYMPIAD_START_POINTS;
   public static int OLYMPIAD_WEEKLY_POINTS;
   public static int OLYMPIAD_CLASSED;
   public static int OLYMPIAD_NONCLASSED;
   public static int OLYMPIAD_REG_DISPLAY;
   public static int OLYMPIAD_BATTLE_REWARD_ITEM;
   public static int OLYMPIAD_CLASSED_RITEM_C;
   public static int OLYMPIAD_NONCLASSED_RITEM_C;
   public static int OLYMPIAD_COMP_RITEM;
   public static int OLYMPIAD_GP_PER_POINT;
   public static int OLYMPIAD_HERO_POINTS;
   public static int OLYMPIAD_RANK1_POINTS;
   public static int OLYMPIAD_RANK2_POINTS;
   public static int OLYMPIAD_RANK3_POINTS;
   public static int OLYMPIAD_RANK4_POINTS;
   public static int OLYMPIAD_RANK5_POINTS;
   public static int OLYMPIAD_MAX_POINTS;
   public static boolean OLYMPIAD_LOG_FIGHTS;
   public static boolean OLYMPIAD_SHOW_MONTHLY_WINNERS;
   public static boolean OLYMPIAD_ANNOUNCE_GAMES;
   public static Set<Integer> LIST_OLY_RESTRICTED_ITEMS = new HashSet<>();
   public static boolean OLYMPIAD_DISABLE_BLESSED_SPIRITSHOTS;
   public static int OLYMPIAD_ENCHANT_LIMIT;
   public static int OLYMPIAD_WAIT_TIME;
   public static boolean OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS;
   public static String OLYMPIAD_PERIOD;
   public static int OLYMPIAD_PERIOD_MULTIPLIER;
   public static List<Integer> OLYMPIAD_COMPETITION_DAYS;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Olympiad.ini");
      OLYMPIAD_ENABLED = config.getBoolean("OlympiadEnabled", true);
      OLYMPIAD_START_TIME = config.getInt("OlympiadStartTime", 18);
      OLYMPIAD_MIN = config.getInt("OlympiadMin", 0);
      OLYMPIAD_MAX_BUFFS = config.getInt("OlympiadMaxBuffs", 5);
      OLYMPIAD_CPERIOD = config.getLong("OlympiadCPeriod", 21600000L);
      OLYMPIAD_BATTLE = config.getLong("OlympiadBattle", 360000L);
      OLYMPIAD_WPERIOD = config.getLong("OlympiadWPeriod", 604800000L);
      OLYMPIAD_VPERIOD = config.getLong("OlympiadVPeriod", 86400000L);
      OLYMPIAD_START_POINTS = config.getInt("OlympiadStartPoints", 10);
      OLYMPIAD_WEEKLY_POINTS = config.getInt("OlympiadWeeklyPoints", 10);
      OLYMPIAD_CLASSED = config.getInt("OlympiadClassedParticipants", 5);
      OLYMPIAD_NONCLASSED = config.getInt("OlympiadNonClassedParticipants", 9);
      OLYMPIAD_REG_DISPLAY = config.getInt("OlympiadRegistrationDisplayNumber", 100);
      OLYMPIAD_BATTLE_REWARD_ITEM = config.getInt("OlympiadBattleRewItem", 6651);
      OLYMPIAD_CLASSED_RITEM_C = config.getInt("OlympiadClassedRewItemCount", 50);
      OLYMPIAD_NONCLASSED_RITEM_C = config.getInt("OlympiadNonClassedRewItemCount", 40);
      OLYMPIAD_COMP_RITEM = config.getInt("OlympiadCompRewItem", 13722);
      OLYMPIAD_GP_PER_POINT = config.getInt("OlympiadGPPerPoint", 1);
      OLYMPIAD_HERO_POINTS = config.getInt("OlympiadHeroPoints", 180);
      OLYMPIAD_RANK1_POINTS = config.getInt("OlympiadRank1Points", 120);
      OLYMPIAD_RANK2_POINTS = config.getInt("OlympiadRank2Points", 80);
      OLYMPIAD_RANK3_POINTS = config.getInt("OlympiadRank3Points", 55);
      OLYMPIAD_RANK4_POINTS = config.getInt("OlympiadRank4Points", 35);
      OLYMPIAD_RANK5_POINTS = config.getInt("OlympiadRank5Points", 20);
      OLYMPIAD_MAX_POINTS = config.getInt("OlympiadMaxPoints", 10);
      OLYMPIAD_LOG_FIGHTS = config.getBoolean("OlympiadLogFights", false);
      OLYMPIAD_SHOW_MONTHLY_WINNERS = config.getBoolean("OlympiadShowMonthlyWinners", true);
      OLYMPIAD_ANNOUNCE_GAMES = config.getBoolean("OlympiadAnnounceGames", true);
      String olyRestrictedItems = config.getString("OlympiadRestrictedItems", "").trim();
      if (!olyRestrictedItems.isEmpty()) {
         String[] olyRestrictedItemsSplit = olyRestrictedItems.split(",");
         LIST_OLY_RESTRICTED_ITEMS = new HashSet<>(olyRestrictedItemsSplit.length);

         for (String id : olyRestrictedItemsSplit) {
            LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(id));
         }
      } else {
         LIST_OLY_RESTRICTED_ITEMS.clear();
      }

      OLYMPIAD_DISABLE_BLESSED_SPIRITSHOTS = config.getBoolean("OlympiadDisableBlessedSpiritShots", false);
      OLYMPIAD_ENCHANT_LIMIT = config.getInt("OlympiadEnchantLimit", -1);
      OLYMPIAD_WAIT_TIME = config.getInt("OlympiadWaitTime", 120);
      if (OLYMPIAD_WAIT_TIME != 120 && OLYMPIAD_WAIT_TIME != 60 && OLYMPIAD_WAIT_TIME != 30 && OLYMPIAD_WAIT_TIME != 15 && OLYMPIAD_WAIT_TIME != 10 && OLYMPIAD_WAIT_TIME != 5) {
         OLYMPIAD_WAIT_TIME = 120;
      }

      OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS = config.getBoolean("OlympiadUseCustomPeriodSettings", false);
      OLYMPIAD_PERIOD = config.getString("OlympiadPeriod", "MONTH");
      OLYMPIAD_PERIOD_MULTIPLIER = config.getInt("OlympiadPeriodMultiplier", 1);
      OLYMPIAD_COMPETITION_DAYS = new ArrayList<>();

      for (String s : config.getString("OlympiadCompetitionDays", "1,2,3,4,5,6,7").split(",")) {
         OLYMPIAD_COMPETITION_DAYS.add(Integer.parseInt(s));
      }
   }
}
