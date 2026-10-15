package org.l2jmobius.gameserver.network;

import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class Disconnection {
   private static final Logger LOGGER = Logger.getLogger(org.l2jmobius.gameserver.network.Disconnection.class.getName());
   private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
   private final org.l2jmobius.gameserver.network.GameClient _client;
   private final Player _player;

   private Disconnection(org.l2jmobius.gameserver.network.GameClient client, Player player) {
      this._client = client != null ? client : (player != null ? player.getClient() : null);
      this._player = player != null ? player : (client != null ? client.getPlayer() : null);
      if (this._player != null) {
         this._player.stopAllTasks();
      }

      AntiFeedManager.getInstance().onDisconnect(this._client);
      if (this._client != null) {
         this._client.setPlayer(null);
      }

      if (this._player != null) {
         this._player.setClient(null);
      }
   }

   public static org.l2jmobius.gameserver.network.Disconnection of(org.l2jmobius.gameserver.network.GameClient client) {
      return new org.l2jmobius.gameserver.network.Disconnection(client, null);
   }

   public static org.l2jmobius.gameserver.network.Disconnection of(Player player) {
      return new org.l2jmobius.gameserver.network.Disconnection(null, player);
   }

   public static org.l2jmobius.gameserver.network.Disconnection of(org.l2jmobius.gameserver.network.GameClient client, Player player) {
      return new org.l2jmobius.gameserver.network.Disconnection(client, player);
   }

   public void storeAndDelete() {
      try {
         if (this._player != null) {
            this._player.storeMe();
            if (this._player.isOnline()) {
               this._player.deleteMe();
               LOGGER_ACCOUNTING.info("Logged out, " + this._player);
            }
         } else if (this._client != null) {
            LOGGER_ACCOUNTING.info("Logged out, " + this._client);
         }
      } catch (Exception var2) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Problem with storeAndDelete: " + var2.getMessage());
      }
   }

   public void storeAndDeleteWith(ServerPacket packet) {
      this.storeAndDelete();
      if (this._client != null) {
         this._client.close(packet);
      }
   }

   public void onDisconnection() {
      if (this._player != null) {
         if (this._player.canLogout()) {
            this.storeAndDelete();
         } else {
            ThreadPool.schedule(() -> {
               if (this._player.isOnline()) {
                  this.storeAndDelete();
               }
            }, 15000L);
         }
      }
   }
}
