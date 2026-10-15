package org.l2jmobius.gameserver.managers;

import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GraciaSeedsConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

public class SeedOfInfinityManager {
   protected static final Logger LOGGER = Logger.getLogger(SeedOfInfinityManager.class.getName());
   private static final long SOI_OPEN_TIME = 86400000L;
   private static final Location[] openSeedTeleportLocs = new Location[]{
      new Location(-179537, 209551, -15504),
      new Location(-179779, 212540, -15520),
      new Location(-177028, 211135, -15520),
      new Location(-176355, 208043, -15520),
      new Location(-179284, 205990, -15520),
      new Location(-182268, 208218, -15520),
      new Location(-182069, 211140, -15520),
      new Location(-176036, 210002, -11948),
      new Location(-176039, 208203, -11949),
      new Location(-183288, 208205, -11939),
      new Location(-183290, 210004, -11939),
      new Location(-187776, 205696, -9536),
      new Location(-186327, 208286, -9536),
      new Location(-184429, 211155, -9536),
      new Location(-182811, 213871, -9504),
      new Location(-180921, 216789, -9536),
      new Location(-177264, 217760, -9536),
      new Location(-173727, 218169, -9536)
   };

   protected SeedOfInfinityManager() {
      checkStageAndSpawn();
      if (isSeedOpen()) {
         openSeed(getOpenedTime());
      }

      LOGGER.info("Seed of Infinity Manager: Loaded. Current stage is: " + getCurrentStage());
   }

   public static int getCurrentStage() {
      return GlobalVariablesManager.getInstance().getInt("SoI_stage", 1);
   }

   public static long getOpenedTime() {
      return getCurrentStage() != 3 ? 0L : GlobalVariablesManager.getInstance().getLong("SoI_opened", 0L) * 1000L - System.currentTimeMillis();
   }

   public static void setCurrentStage(int stage) {
      if (getCurrentStage() != stage) {
         if (stage == 3) {
            openSeed(86400000L);
         } else if (isSeedOpen()) {
            closeSeed();
         }

         GlobalVariablesManager.getInstance().set("SoI_stage", stage);
         setCohemenesCount(0);
         setEkimusCount(0);
         setHoEDefCount(0);
         checkStageAndSpawn();
         LOGGER.info("Seed of Infinity Manager: Set to stage " + stage);
      }
   }

   public static boolean isSeedOpen() {
      return getOpenedTime() > 0L;
   }

   public static void openSeed(long time) {
      if (time > 0L) {
         long closeTime = System.currentTimeMillis() + time;
         GlobalVariablesManager.getInstance().set("SoI_opened", closeTime / 1000L);
         LOGGER.info("Seed of Infinity Manager: Opening the seed until " + TimeUtil.getDateTimeString(time));
         spawnOpenedSeed();
         DoorData.getInstance().getDoor(14240102).openMe();
         ThreadPool.schedule(() -> {
            closeSeed();
            setCurrentStage(4);
         }, time);
      }
   }

   public static void closeSeed() {
      LOGGER.info("Seed of Infinity Manager: Closing the seed.");
      GlobalVariablesManager.getInstance().remove("SoI_opened");
      DoorData.getInstance().getDoor(14240102).closeMe();

      for (Player ch : ZoneManager.getInstance().getZoneById(60010).getPlayersInside()) {
         if (ch != null) {
            ch.teleToLocation(-183285, 205996, -12896);
         }
      }
   }

   public static void checkStageAndSpawn() {
      switch (getCurrentStage()) {
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
      }
   }

   public static void notifyCohemenesKill() {
      if (getCurrentStage() == 1) {
         if (getCohemenesCount() < 9) {
            setCohemenesCount(getCohemenesCount() + 1);
         } else {
            setCurrentStage(2);
         }
      }
   }

   public static void notifyEkimusKill() {
      if (getCurrentStage() == 2) {
         if (getEkimusCount() < GraciaSeedsConfig.SOI_EKIMUS_KILL_COUNT) {
            setEkimusCount(getEkimusCount() + 1);
         } else {
            setCurrentStage(3);
         }
      }
   }

   public static void notifyHoEDefSuccess() {
      if (getCurrentStage() == 4) {
         if (getHoEDefCount() < 9) {
            setHoEDefCount(getHoEDefCount() + 1);
         } else {
            setCurrentStage(5);
         }
      }
   }

   public static void setCohemenesCount(int i) {
      GlobalVariablesManager.getInstance().set("SoI_CohemenesCount", i);
   }

   public static void setEkimusCount(int i) {
      GlobalVariablesManager.getInstance().set("SoI_EkimusCount", i);
   }

   public static void setHoEDefCount(int i) {
      GlobalVariablesManager.getInstance().set("SoI_hoedefkillcount", i);
   }

   public static int getCohemenesCount() {
      return GlobalVariablesManager.getInstance().getInt("SoI_CohemenesCount", 0);
   }

   public static int getEkimusCount() {
      return GlobalVariablesManager.getInstance().getInt("SoI_EkimusCount", 0);
   }

   public static int getHoEDefCount() {
      return GlobalVariablesManager.getInstance().getInt("SoI_hoedefkillcount", 0);
   }

   private static void spawnOpenedSeed() {
   }

   public static void teleportInSeed(Player player) {
      player.teleToLocation(openSeedTeleportLocs[Rnd.get(openSeedTeleportLocs.length)], false);
   }

   public static SeedOfInfinityManager getInstance() {
      return SeedOfInfinityManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SeedOfInfinityManager INSTANCE = new SeedOfInfinityManager();
   }
}
