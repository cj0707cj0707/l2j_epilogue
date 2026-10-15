package org.l2jmobius.gameserver.mechanics.siege;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;

public class FortUpdater implements Runnable {
   private static final Logger LOGGER = Logger.getLogger(FortUpdater.class.getName());
   private final Clan _clan;
   private final Fort _fort;
   private int _runCount;
   private final FortUpdaterType _updaterType;

   public FortUpdater(Fort fort, Clan clan, int runCount, FortUpdaterType ut) {
      this._fort = fort;
      this._clan = clan;
      this._runCount = runCount;
      this._updaterType = ut;
   }

   @Override
   public void run() {
      try {
         switch (this._updaterType) {
            case PERIODIC_UPDATE:
               this._runCount++;
               if (this._fort.getOwnerClan() != null && this._fort.getOwnerClan() == this._clan) {
                  this._fort.getOwnerClan().increaseBloodOathCount();
                  if (this._fort.getFortState() == 2) {
                     if (this._clan.getWarehouse().getAdena() >= FeatureConfig.FS_FEE_FOR_CASTLE) {
                        this._clan.getWarehouse().destroyItemByItemId(ItemProcessType.FEE, 57, FeatureConfig.FS_FEE_FOR_CASTLE, null, null);
                        this._fort.getContractedCastle().addToTreasuryNoTax(FeatureConfig.FS_FEE_FOR_CASTLE);
                        this._fort.raiseSupplyLeveL();
                     } else {
                        this._fort.setFortState(1, 0);
                     }
                  }

                  this._fort.saveFortVariables();
                  break;
               }

               return;
            case MAX_OWN_TIME:
               if (this._fort.getOwnerClan() == null || this._fort.getOwnerClan() != this._clan) {
                  return;
               }

               if (this._fort.getOwnedTime() > FeatureConfig.FS_MAX_OWN_TIME * 3600) {
                  this._fort.removeOwner(true);
                  this._fort.setFortState(0, 0);
               }
         }
      } catch (Exception var2) {
         LOGGER.log(Level.WARNING, "run: ", (Throwable)var2);
      }
   }

   public int getRunCount() {
      return this._runCount;
   }
}
