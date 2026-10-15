package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class DayNightSpawnManager {
   private static final Logger LOGGER = Logger.getLogger(DayNightSpawnManager.class.getName());
   private final List<Spawn> _dayCreatures = new ArrayList<>();
   private final List<Spawn> _nightCreatures = new ArrayList<>();
   private final Map<Spawn, RaidBoss> _bosses = new ConcurrentHashMap<>();

   protected DayNightSpawnManager() {
   }

   public void addDayCreature(Spawn spawnDat) {
      this._dayCreatures.add(spawnDat);
   }

   public void addNightCreature(Spawn spawnDat) {
      this._nightCreatures.add(spawnDat);
   }

   public void spawnDayCreatures() {
      this.spawnCreatures(this._nightCreatures, this._dayCreatures, "night", "day");
   }

   public void spawnNightCreatures() {
      this.spawnCreatures(this._dayCreatures, this._nightCreatures, "day", "night");
   }

   private void spawnCreatures(List<Spawn> unSpawnCreatures, List<Spawn> spawnCreatures, String unspawnLogInfo, String spawnLogInfo) {
      try {
         if (!unSpawnCreatures.isEmpty()) {
            int i = 0;

            for (Spawn spawn : unSpawnCreatures) {
               if (spawn != null) {
                  spawn.stopRespawn();
                  Npc last = spawn.getLastSpawn();
                  if (last != null) {
                     last.deleteMe();
                     i++;
                  }
               }
            }

            LOGGER.info("DayNightSpawnManager: Removed " + i + " " + unspawnLogInfo + " creatures");
         }

         int i = 0;

         for (Spawn spawnDat : spawnCreatures) {
            if (spawnDat != null) {
               spawnDat.startRespawn();
               spawnDat.doSpawn();
               i++;
            }
         }

         LOGGER.info("DayNightSpawnManager: Spawned " + i + " " + spawnLogInfo + " creatures");
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Error while spawning creatures: " + var9.getMessage(), (Throwable)var9);
      }
   }

   private void changeMode(int mode) {
      if (!this._nightCreatures.isEmpty() || !this._dayCreatures.isEmpty() || !this._bosses.isEmpty()) {
         switch (mode) {
            case 0:
               this.spawnDayCreatures();
               break;
            case 1:
               this.spawnNightCreatures();
               break;
            default:
               LOGGER.warning("DayNightSpawnManager: Wrong mode sent");
         }
      }
   }

   public DayNightSpawnManager trim() {
      ((ArrayList)this._nightCreatures).trimToSize();
      ((ArrayList)this._dayCreatures).trimToSize();
      return this;
   }

   public void notifyChangeMode() {
      try {
         this.changeMode(GameTimeTaskManager.getInstance().isNight() ? 1 : 0);
      } catch (Exception var2) {
         LOGGER.log(Level.WARNING, "Error while notifyChangeMode(): " + var2.getMessage(), (Throwable)var2);
      }
   }

   public void cleanUp() {
      this._nightCreatures.clear();
      this._dayCreatures.clear();
      this._bosses.clear();
   }

   public static DayNightSpawnManager getInstance() {
      return DayNightSpawnManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final DayNightSpawnManager INSTANCE = new DayNightSpawnManager();
   }
}
