package org.l2jmobius.gameserver.config.custom;

import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.commons.util.ConfigReader;

public class BossAnnouncementsConfig {
   private static final String BOSS_ANNOUNCEMENTS_CONFIG_FILE = "./config/Custom/BossAnnouncements.ini";
   public static boolean RAIDBOSS_SPAWN_ANNOUNCEMENTS;
   public static boolean RAIDBOSS_DEFEAT_ANNOUNCEMENTS;
   public static boolean RAIDBOSS_INSTANCE_ANNOUNCEMENTS;
   public static boolean GRANDBOSS_SPAWN_ANNOUNCEMENTS;
   public static boolean GRANDBOSS_DEFEAT_ANNOUNCEMENTS;
   public static boolean GRANDBOSS_INSTANCE_ANNOUNCEMENTS;
   public static Set<Integer> RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS = new HashSet<>();
   public static Set<Integer> RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS = new HashSet<>();

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/BossAnnouncements.ini");
      RAIDBOSS_SPAWN_ANNOUNCEMENTS = config.getBoolean("RaidBossSpawnAnnouncements", false);
      RAIDBOSS_DEFEAT_ANNOUNCEMENTS = config.getBoolean("RaidBossDefeatAnnouncements", false);
      RAIDBOSS_INSTANCE_ANNOUNCEMENTS = config.getBoolean("RaidBossInstanceAnnouncements", false);
      GRANDBOSS_SPAWN_ANNOUNCEMENTS = config.getBoolean("GrandBossSpawnAnnouncements", false);
      GRANDBOSS_DEFEAT_ANNOUNCEMENTS = config.getBoolean("GrandBossDefeatAnnouncements", false);
      GRANDBOSS_INSTANCE_ANNOUNCEMENTS = config.getBoolean("GrandBossInstanceAnnouncements", false);
      RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.clear();

      for (String raidbossId : config.getString("RaidbossExcludedFromSpawnAnnouncements", "").split(",")) {
         if (!raidbossId.isEmpty()) {
            RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.add(Integer.parseInt(raidbossId.trim()));
         }
      }

      RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.clear();

      for (String raidbossIdx : config.getString("RaidbossExcludedFromDefeatAnnouncements", "").split(",")) {
         if (!raidbossIdx.isEmpty()) {
            RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.add(Integer.parseInt(raidbossIdx.trim()));
         }
      }
   }
}
