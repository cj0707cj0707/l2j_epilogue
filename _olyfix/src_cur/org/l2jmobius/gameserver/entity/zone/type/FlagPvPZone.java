package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

public class FlagPvPZone extends ZoneType {
   public FlagPvPZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         player.updatePvPFlag(1);
         player.sendMessage("You entered a PvP Flag zone.");
         player.broadcastUserInfo();
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (player.getPvpFlag() > 0) {
            player.updatePvPFlag(0);
         }

         player.sendMessage("You left the PvP Flag zone.");
         player.broadcastUserInfo();
      }
   }

   @Override
   public void onDieInside(Creature creature) {
      this.onExit(creature);
   }

   @Override
   public void onReviveInside(Creature creature) {
      this.onEnter(creature);
   }
}
