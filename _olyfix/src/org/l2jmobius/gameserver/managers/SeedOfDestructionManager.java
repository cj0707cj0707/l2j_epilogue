package org.l2jmobius.gameserver.managers;

import java.util.Calendar;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GraciaSeedsConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.script.Quest;

public class SeedOfDestructionManager {
   private static final Logger LOGGER = Logger.getLogger(SeedOfDestructionManager.class.getName());
   public static final String ENERGY_SEEDS = "EnergySeeds";
   private static final byte SOITYPE = 2;
   private static final byte SODTYPE = 1;
   private int _SoDTiatKilled = 0;
   private int _SoDState = 1;
   private final Calendar _SoDLastStateChangeDate = Calendar.getInstance();

   protected SeedOfDestructionManager() {
      this.loadData();
      this.handleSodStages();
      LOGGER.info("Seed of Destruction Manager: Loaded. Current stage is: " + this._SoDState);
   }

   public void saveData(byte seedType) {
      switch (seedType) {
         case 1:
            GlobalVariablesManager.getInstance().set("SoDState", this._SoDState);
            GlobalVariablesManager.getInstance().set("SoDTiatKilled", this._SoDTiatKilled);
            GlobalVariablesManager.getInstance().set("SoDLSCDate", this._SoDLastStateChangeDate.getTimeInMillis());
         case 2:
            break;
         default:
            LOGGER.warning(this.getClass().getSimpleName() + ": Unknown SeedType in SaveData: " + seedType);
      }
   }

   public void loadData() {
      if (GlobalVariablesManager.getInstance().hasVariable("SoDState")) {
         this._SoDState = GlobalVariablesManager.getInstance().getInt("SoDState", 0);
         this._SoDTiatKilled = GlobalVariablesManager.getInstance().getInt("SoDTiatKilled", 0);
         this._SoDLastStateChangeDate.setTimeInMillis(GlobalVariablesManager.getInstance().getLong("SoDLSCDate", 0L));
      } else {
         this.saveData((byte)1);
      }
   }

   private void handleSodStages() {
      switch (this._SoDState) {
         case 1:
            break;
         case 2:
            long timePast = System.currentTimeMillis() - this._SoDLastStateChangeDate.getTimeInMillis();
            if (timePast >= GraciaSeedsConfig.SOD_STAGE_2_LENGTH) {
               this.setSoDState(1, true);
            } else {
               ThreadPool.schedule(() -> {
                  this.setSoDState(1, true);
                  this.updateSodState();
               }, GraciaSeedsConfig.SOD_STAGE_2_LENGTH - timePast);
            }
            break;
         case 3:
            this.setSoDState(1, true);
            break;
         default:
            LOGGER.warning(this.getClass().getSimpleName() + ": Unknown Seed of Destruction state(" + this._SoDState + ")! ");
      }

      this.manageEdricSpawn();
   }

   private void manageEdricSpawn() {
      Npc edric = World.getNpc(32527);
      if (this._SoDState == 2) {
         if (edric == null) {
            Quest.addSpawn(32527, -248525, 250048, 4307, 24576);
         }
      } else if (edric != null) {
         edric.deleteMe();
      }
   }

   public void updateSodState() {
      Quest quest = ScriptManager.getInstance().getScript("EnergySeeds");
      if (quest == null) {
         LOGGER.warning(this.getClass().getSimpleName() + ": missing EnergySeeds Quest!");
      } else {
         quest.notifyEvent("StopSoDAi", null, null);
      }
   }

   public void increaseSoDTiatKilled() {
      if (this._SoDState == 1) {
         this._SoDTiatKilled++;
         if (this._SoDTiatKilled >= GraciaSeedsConfig.SOD_TIAT_KILL_COUNT) {
            this.setSoDState(2, false);
         }

         this.saveData((byte)1);
         Quest esQuest = ScriptManager.getInstance().getScript("EnergySeeds");
         if (esQuest == null) {
            LOGGER.warning(this.getClass().getSimpleName() + ": missing EnergySeeds Quest!");
         } else {
            esQuest.notifyEvent("StartSoDAi", null, null);
         }
      }
   }

   public int getSoDTiatKilled() {
      return this._SoDTiatKilled;
   }

   public void setSoDState(int value, boolean doSave) {
      LOGGER.info(this.getClass().getSimpleName() + ": New Seed of Destruction state -> " + value + ".");
      this._SoDLastStateChangeDate.setTimeInMillis(System.currentTimeMillis());
      this._SoDState = value;
      if (this._SoDState == 1) {
         this._SoDTiatKilled = 0;
      }

      this.handleSodStages();
      if (doSave) {
         this.saveData((byte)1);
      }
   }

   public long getSoDTimeForNextStateChange() {
      switch (this._SoDState) {
         case 1:
            return -1L;
         case 2:
            return this._SoDLastStateChangeDate.getTimeInMillis() + GraciaSeedsConfig.SOD_STAGE_2_LENGTH - System.currentTimeMillis();
         case 3:
            return -1L;
         default:
            return -1L;
      }
   }

   public Calendar getSoDLastStateChangeDate() {
      return this._SoDLastStateChangeDate;
   }

   public int getSoDState() {
      return this._SoDState;
   }

   public static SeedOfDestructionManager getInstance() {
      return SeedOfDestructionManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SeedOfDestructionManager INSTANCE = new SeedOfDestructionManager();
   }
}
