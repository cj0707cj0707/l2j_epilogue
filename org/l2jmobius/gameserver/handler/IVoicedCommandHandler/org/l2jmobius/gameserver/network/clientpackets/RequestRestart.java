package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;
import org.l2jmobius.gameserver.network.serverpackets.RestartResponse;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

public class RequestRestart extends ClientPacket {
   protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");

   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.getActiveEnchantItemId() != -1 || player.getActiveEnchantAttrItemId() != -1) {
            player.sendPacket(RestartResponse.valueOf(false));
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (player.isChangingClass()) {
            PacketLogger.warning(player + " tried to restart during class change.");
            player.sendPacket(RestartResponse.valueOf(false));
            player.sendPacket(ActionFailed.STATIC_PACKET);
         } else if (player.isInStoreMode()) {
            player.sendMessage("Cannot restart while trading.");
            player.sendPacket(RestartResponse.valueOf(false));
         } else if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || player.isGM() && GeneralConfig.GM_RESTART_FIGHTING) {
            if (player.isFestivalParticipant()) {
               if (SevenSignsFestival.getInstance().isFestivalInitialized()) {
                  player.sendMessage("You cannot restart while you are a participant in a festival.");
                  player.sendPacket(RestartResponse.valueOf(false));
                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  return;
               }

               Party playerParty = player.getParty();
               if (playerParty != null) {
                  player.getParty().broadcastString(player.getName() + " has been removed from the upcoming festival.");
               }
            }

            if (!player.canLogout()) {
               player.sendPacket(RestartResponse.valueOf(false));
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

               GameClient client = this.getClient();
               if (OfflineTraderTable.getInstance().enteredOfflineMode(player)) {
                  LOGGER_ACCOUNTING.info("Entered offline mode, " + client);
               } else {
                  Disconnection.of(client, player).storeAndDelete();
               }

               client.setConnectionState(ConnectionState.AUTHENTICATED);
               client.sendPacket(RestartResponse.valueOf(true));
               CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
               client.sendPacket(new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1));
               client.setCharSelection(cl.getCharInfo());
            }
         } else {
            player.sendPacket(SystemMessageId.YOU_CANNOT_RESTART_WHILE_IN_COMBAT);
            player.sendPacket(RestartResponse.valueOf(false));
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }
}
