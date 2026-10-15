package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.tasks.player.TeleportTask;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class JailZone extends ZoneType {
   private static final Location JAIL_IN_LOC = new Location(-114356, -249645, -2984);
   private static final Location JAIL_OUT_LOC = new Location(17836, 170178, -3507);

   public JailZone(int id) {
      super(id);
   }

   @Override
   protected void onEnter(Creature creature) {
      if (creature.isPlayer()) {
         creature.setInsideZone(ZoneId.JAIL, true);
         creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
         if (GeneralConfig.JAIL_IS_PVP) {
            creature.setInsideZone(ZoneId.PVP, true);
            creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
         }

         if (GeneralConfig.JAIL_DISABLE_TRANSACTION) {
            creature.setInsideZone(ZoneId.NO_STORE, true);
         }
      }
   }

   @Override
   protected void onExit(Creature creature) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         player.setInsideZone(ZoneId.JAIL, false);
         player.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
         if (GeneralConfig.JAIL_IS_PVP) {
            creature.setInsideZone(ZoneId.PVP, false);
            creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
         }

         if (player.isJailed()) {
            ThreadPool.schedule(new TeleportTask(player, JAIL_IN_LOC), 2000L);
            creature.sendMessage("You cannot cheat your way out of here. You must wait until your jail time is over.");
         }

         if (GeneralConfig.JAIL_DISABLE_TRANSACTION) {
            creature.setInsideZone(ZoneId.NO_STORE, false);
         }
      }
   }

   public static Location getLocationIn() {
      return JAIL_IN_LOC;
   }

   public static Location getLocationOut() {
      return JAIL_OUT_LOC;
   }
}
