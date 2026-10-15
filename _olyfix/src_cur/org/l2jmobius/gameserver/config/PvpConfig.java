package org.l2jmobius.gameserver.config;

import java.util.Arrays;
import org.l2jmobius.commons.util.ConfigReader;

public class PvpConfig {
   private static final String PVP_CONFIG_FILE = "./config/PVP.ini";
   public static boolean KARMA_DROP_GM;
   public static boolean KARMA_AWARD_PK_KILL;
   public static int KARMA_PK_LIMIT;
   public static String KARMA_NONDROPPABLE_PET_ITEMS;
   public static String KARMA_NONDROPPABLE_ITEMS;
   public static int[] KARMA_LIST_NONDROPPABLE_PET_ITEMS;
   public static int[] KARMA_LIST_NONDROPPABLE_ITEMS;
   public static boolean ANTIFEED_ENABLE;
   public static boolean ANTIFEED_DUALBOX;
   public static boolean ANTIFEED_DISCONNECTED_AS_DUALBOX;
   public static int ANTIFEED_INTERVAL;
   public static int PVP_NORMAL_TIME;
   public static int PVP_PVP_TIME;
   public static boolean FLAG_PLAYER_ON_RAID_ATTACK;
   public static boolean FLAG_PLAYER_ON_CHAMPION_ATTACK;
   public static boolean FLAG_PLAYER_ON_SUMMON_PET_RAID_CHAMPION_ATTACK;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/PVP.ini");
      KARMA_DROP_GM = config.getBoolean("CanGMDropEquipment", false);
      KARMA_AWARD_PK_KILL = config.getBoolean("AwardPKKillPVPPoint", false);
      KARMA_PK_LIMIT = config.getInt("MinimumPKRequiredToDrop", 5);
      KARMA_NONDROPPABLE_PET_ITEMS = config.getString("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650,9882");
      KARMA_NONDROPPABLE_ITEMS = config.getString(
         "ListOfNonDroppableItems",
         "57,1147,425,1146,461,10,2368,7,6,2370,2369,6842,6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621,7694,8181,5575,7694,9388,9389,9390"
      );
      String[] karma = KARMA_NONDROPPABLE_PET_ITEMS.split(",");
      KARMA_LIST_NONDROPPABLE_PET_ITEMS = new int[karma.length];

      for (int i = 0; i < karma.length; i++) {
         KARMA_LIST_NONDROPPABLE_PET_ITEMS[i] = Integer.parseInt(karma[i]);
      }

      Arrays.sort(KARMA_LIST_NONDROPPABLE_PET_ITEMS);
      karma = KARMA_NONDROPPABLE_ITEMS.split(",");
      KARMA_LIST_NONDROPPABLE_ITEMS = new int[karma.length];

      for (int i = 0; i < karma.length; i++) {
         KARMA_LIST_NONDROPPABLE_ITEMS[i] = Integer.parseInt(karma[i]);
      }

      Arrays.sort(KARMA_LIST_NONDROPPABLE_ITEMS);
      ANTIFEED_ENABLE = config.getBoolean("AntiFeedEnable", false);
      ANTIFEED_DUALBOX = config.getBoolean("AntiFeedDualbox", true);
      ANTIFEED_DISCONNECTED_AS_DUALBOX = config.getBoolean("AntiFeedDisconnectedAsDualbox", true);
      ANTIFEED_INTERVAL = config.getInt("AntiFeedInterval", 120) * 1000;
      PVP_NORMAL_TIME = config.getInt("PvPVsNormalTime", 120000);
      PVP_PVP_TIME = config.getInt("PvPVsPvPTime", 60000);
      FLAG_PLAYER_ON_RAID_ATTACK = config.getBoolean("FlagPlayerOnRaidAttack", true);
      FLAG_PLAYER_ON_CHAMPION_ATTACK = config.getBoolean("FlagPlayerOnChampionAttack", true);
      FLAG_PLAYER_ON_SUMMON_PET_RAID_CHAMPION_ATTACK = config.getBoolean("FlagPlayerOnSummonPetRaidChampionAttack", true);
   }
}
