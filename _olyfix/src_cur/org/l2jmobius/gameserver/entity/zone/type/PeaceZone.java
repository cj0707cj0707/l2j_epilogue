package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;

public class PeaceZone extends ZoneType {
   public PeaceZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (player.isCombatFlagEquipped() && TerritoryWarManager.getInstance().isTWInProgress()) {
            TerritoryWarManager.getInstance().dropCombatFlag(player, false, true);
         }

         if (player.getSiegeState() != 0 && GeneralConfig.PEACE_ZONE_MODE == 1) {
            return;
         }
      }

      if (GeneralConfig.PEACE_ZONE_MODE != 2) {
         creature.setInsideZone(ZoneId.PEACE, true);
      }

      if (!this.getAllowStore()) {
         creature.setInsideZone(ZoneId.NO_STORE, true);
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (GeneralConfig.PEACE_ZONE_MODE != 2) {
         creature.setInsideZone(ZoneId.PEACE, false);
      }

      if (!this.getAllowStore()) {
         creature.setInsideZone(ZoneId.NO_STORE, false);
      }
   }
}
