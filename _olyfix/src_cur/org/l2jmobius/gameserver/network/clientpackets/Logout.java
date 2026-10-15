package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

public class Logout extends ClientPacket {
   protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");

   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      GameClient client = this.getClient();
      Player player = client.getPlayer();
      if (player == null) {
         client.disconnect();
      } else if (!player.canLogout()) {
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else {
         player.removeFromBossZone();
         if (Olympiad.getInstance().isRegistered(player)) {
            Olympiad.getInstance().unRegisterNoble(player);
         }

         if (!GeneralConfig.RESTORE_PLAYER_INSTANCE) {
            int instanceId = player.getInstanceId();
            if (instanceId > 0) {
               Instance world = InstanceManager.getInstance().getInstance(instanceId);
               if (world != null) {
                  player.setInstanceId(0);
                  Location location = world.getExitLoc();
                  if (location == null) {
                     location = MapRegionData.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
                  }

                  player.getVariables().set("RESTORE_LOCATION", location.getX() + ";" + location.getY() + ";" + location.getZ());
                  world.removePlayer(player.getObjectId());
               }
            }
         }

         if (OfflineTraderTable.getInstance().enteredOfflineMode(player)) {
            LOGGER_ACCOUNTING.info("Entered offline mode, " + client);
         } else {
            Disconnection.of(client, player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
         }
      }
   }
}
