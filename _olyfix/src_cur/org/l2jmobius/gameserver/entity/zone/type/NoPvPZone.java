package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class NoPvPZone extends ZoneType {
   public NoPvPZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (player.getSiegeState() != 0 && GeneralConfig.PEACE_ZONE_MODE == 1) {
            return;
         }
      }

      if (GeneralConfig.PEACE_ZONE_MODE != 2) {
         creature.setInsideZone(ZoneId.NO_PVP, true);
      }

      if (creature.isPlayer()) {
         creature.broadcastInfo();
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (GeneralConfig.PEACE_ZONE_MODE != 2) {
         creature.setInsideZone(ZoneId.NO_PVP, false);
      }

      if (creature.isPlayer() && !creature.isTeleporting()) {
         creature.broadcastInfo();
      }
   }

   @Override
   public void setEnabled(boolean value) {
      super.setEnabled(value);
      if (value) {
         for (Player player : World.getPlayers()) {
            if (player != null && this.isInsideZone(player)) {
               this.revalidateInZone(player);
               if (player.getSummon() != null) {
                  this.revalidateInZone(player.getSummon());
               }
            }
         }
      } else {
         for (Creature creature : this.getCharactersInside()) {
            if (creature != null) {
               this.removeCharacter(creature);
            }
         }
      }
   }
}
