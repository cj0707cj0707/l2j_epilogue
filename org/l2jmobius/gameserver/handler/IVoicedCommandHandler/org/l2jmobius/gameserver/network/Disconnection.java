package org.l2jmobius.gameserver.network;

import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class Disconnection {
   private static final Logger LOGGER = Logger.getLogger(Disconnection.class.getName());
   private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
   private final GameClient _client;
   private final Player _player;

   private Disconnection(GameClient client, Player player) {
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

   public static Disconnection of(GameClient client) {
      return new Disconnection(client, null);
   }

   public static Disconnection of(Player player) {
      return new Disconnection(null, player);
   }

   public static Disconnection of(GameClient client, Player player) {
      return new Disconnection(client, player);
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
