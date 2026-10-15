package org.l2jmobius.gameserver.config.custom;

import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.commons.util.ConfigReader;

public class RandomSpawnsConfig {
   private static final String RANDOM_SPAWNS_CONFIG_FILE = "./config/Custom/RandomSpawns.ini";
   public static boolean ENABLE_RANDOM_MONSTER_SPAWNS;
   public static int MOB_MAX_SPAWN_RANGE;
   public static int MOB_MIN_SPAWN_RANGE;
   public static Set<Integer> MOBS_LIST_NOT_RANDOM;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/RandomSpawns.ini");
      ENABLE_RANDOM_MONSTER_SPAWNS = config.getBoolean("EnableRandomMonsterSpawns", false);
      MOB_MAX_SPAWN_RANGE = config.getInt("MaxSpawnMobRange", 150);
      MOB_MIN_SPAWN_RANGE = MOB_MAX_SPAWN_RANGE * -1;
      if (ENABLE_RANDOM_MONSTER_SPAWNS) {
         String[] mobsIds = config.getString("MobsSpawnNotRandom", "18812,18813,18814,22138").split(",");
         MOBS_LIST_NOT_RANDOM = new HashSet<>(mobsIds.length);

         for (String id : mobsIds) {
            MOBS_LIST_NOT_RANDOM.add(Integer.parseInt(id));
         }
      }
   }
}
