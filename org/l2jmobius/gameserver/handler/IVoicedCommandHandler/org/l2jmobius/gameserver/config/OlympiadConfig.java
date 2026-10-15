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
      ConfigReader var0 = new ConfigReader("./config/Olympiad.ini");
      OLYMPIAD_ENABLED = var0.getBoolean("OlympiadEnabled", true);
      OLYMPIAD_START_TIME = var0.getInt("OlympiadStartTime", 18);
      OLYMPIAD_MIN = var0.getInt("OlympiadMin", 0);
      OLYMPIAD_MAX_BUFFS = var0.getInt("OlympiadMaxBuffs", 5);
      OLYMPIAD_CPERIOD = var0.getLong("OlympiadCPeriod", 21600000L);
      OLYMPIAD_BATTLE = var0.getLong("OlympiadBattle", 360000L);
      OLYMPIAD_WPERIOD = var0.getLong("OlympiadWPeriod", 604800000L);
      OLYMPIAD_VPERIOD = var0.getLong("OlympiadVPeriod", 86400000L);
      OLYMPIAD_START_POINTS = var0.getInt("OlympiadStartPoints", 10);
      OLYMPIAD_WEEKLY_POINTS = var0.getInt("OlympiadWeeklyPoints", 10);
      OLYMPIAD_CLASSED = var0.getInt("OlympiadClassedParticipants", 5);
      OLYMPIAD_NONCLASSED = var0.getInt("OlympiadNonClassedParticipants", 9);
      OLYMPIAD_REG_DISPLAY = var0.getInt("OlympiadRegistrationDisplayNumber", 100);
      OLYMPIAD_BATTLE_REWARD_ITEM = var0.getInt("OlympiadBattleRewItem", 6651);
      OLYMPIAD_CLASSED_RITEM_C = var0.getInt("OlympiadClassedRewItemCount", 50);
      OLYMPIAD_NONCLASSED_RITEM_C = var0.getInt("OlympiadNonClassedRewItemCount", 40);
      OLYMPIAD_COMP_RITEM = var0.getInt("OlympiadCompRewItem", 13722);
      OLYMPIAD_GP_PER_POINT = var0.getInt("OlympiadGPPerPoint", 1);
      OLYMPIAD_HERO_POINTS = var0.getInt("OlympiadHeroPoints", 180);
      OLYMPIAD_RANK1_POINTS = var0.getInt("OlympiadRank1Points", 120);
      OLYMPIAD_RANK2_POINTS = var0.getInt("OlympiadRank2Points", 80);
      OLYMPIAD_RANK3_POINTS = var0.getInt("OlympiadRank3Points", 55);
      OLYMPIAD_RANK4_POINTS = var0.getInt("OlympiadRank4Points", 35);
      OLYMPIAD_RANK5_POINTS = var0.getInt("OlympiadRank5Points", 20);
      OLYMPIAD_MAX_POINTS = var0.getInt("OlympiadMaxPoints", 10);
      OLYMPIAD_LOG_FIGHTS = var0.getBoolean("OlympiadLogFights", false);
      OLYMPIAD_SHOW_MONTHLY_WINNERS = var0.getBoolean("OlympiadShowMonthlyWinners", true);
      OLYMPIAD_ANNOUNCE_GAMES = var0.getBoolean("OlympiadAnnounceGames", true);
      String var1 = var0.getString("OlympiadRestrictedItems", "").trim();
      if (!var1.isEmpty()) {
         String[] var2 = var1.split(",");
         LIST_OLY_RESTRICTED_ITEMS = new HashSet<>(var2.length);

         for (String var6 : var2) {
            LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(var6));
         }
      } else {
         LIST_OLY_RESTRICTED_ITEMS.clear();
      }

      OLYMPIAD_DISABLE_BLESSED_SPIRITSHOTS = var0.getBoolean("OlympiadDisableBlessedSpiritShots", false);
      OLYMPIAD_ENCHANT_LIMIT = var0.getInt("OlympiadEnchantLimit", -1);
      OLYMPIAD_WAIT_TIME = var0.getInt("OlympiadWaitTime", 120);
      if (OLYMPIAD_WAIT_TIME != 120
         && OLYMPIAD_WAIT_TIME != 60
         && OLYMPIAD_WAIT_TIME != 30
         && OLYMPIAD_WAIT_TIME != 15
         && OLYMPIAD_WAIT_TIME != 10
         && OLYMPIAD_WAIT_TIME != 5) {
         OLYMPIAD_WAIT_TIME = 120;
      }

      OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS = var0.getBoolean("OlympiadUseCustomPeriodSettings", false);
      OLYMPIAD_PERIOD = var0.getString("OlympiadPeriod", "MONTH");
      OLYMPIAD_PERIOD_MULTIPLIER = var0.getInt("OlympiadPeriodMultiplier", 1);
      OLYMPIAD_COMPETITION_DAYS = new ArrayList<>();

      for (String var10 : var0.getString("OlympiadCompetitionDays", "1,2,3,4,5,6,7").split(",")) {
         OLYMPIAD_COMPETITION_DAYS.add(Integer.parseInt(var10));
      }
   }
}
