package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.GameClient;

public class AntiFeedManager {
   protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
   public static final int GAME_ID = 0;
   public static final int OLYMPIAD_ID = 1;
   public static final int TVT_ID = 2;
   public static final int L2EVENT_ID = 3;
   public static final int OFFLINE_PLAY = 4;
   private final Map<Integer, Long> _lastDeathTimes = new ConcurrentHashMap<>();
   private final Map<Integer, Map<Integer, AtomicInteger>> _eventIPs = new ConcurrentHashMap<>();

   protected AntiFeedManager() {
   }

   public void setLastDeathTime(int objectId) {
      this._lastDeathTimes.put(objectId, System.currentTimeMillis());
   }

   public boolean check(Creature attacker, Creature target) {
      if (!PvpConfig.ANTIFEED_ENABLE) {
         return true;
      } else if (target == null) {
         return false;
      } else {
         Player targetPlayer = target.asPlayer();
         if (targetPlayer == null) {
            return false;
         } else if (targetPlayer.getClient().isDetached()) {
            return false;
         } else if (PvpConfig.ANTIFEED_INTERVAL > 0
            && this._lastDeathTimes.containsKey(targetPlayer.getObjectId())
            && System.currentTimeMillis() - this._lastDeathTimes.get(targetPlayer.getObjectId()) < PvpConfig.ANTIFEED_INTERVAL) {
            return false;
         } else if (PvpConfig.ANTIFEED_DUALBOX && attacker != null) {
            Player attackerPlayer = attacker.asPlayer();
            if (attackerPlayer == null) {
               return false;
            } else {
               GameClient targetClient = targetPlayer.getClient();
               GameClient attackerClient = attackerPlayer.getClient();
               return targetClient != null && attackerClient != null && !targetClient.isDetached() && !attackerClient.isDetached()
                  ? !targetClient.getIp().equals(attackerClient.getIp())
                  : !PvpConfig.ANTIFEED_DISCONNECTED_AS_DUALBOX;
            }
         } else {
            return true;
         }
      }
   }

   public void clear() {
      this._lastDeathTimes.clear();
   }

   public void registerEvent(int eventId) {
      this._eventIPs.putIfAbsent(eventId, new ConcurrentHashMap<>());
   }

   public boolean tryAddPlayer(int eventId, Player player, int max) {
      return this.tryAddClient(eventId, player.getClient(), max);
   }

   public boolean tryAddClient(int eventId, GameClient client, int max) {
      if (client == null) {
         return false;
      } else {
         Map<Integer, AtomicInteger> event = this._eventIPs.get(eventId);
         if (event == null) {
            return false;
         } else {
            Integer addrHash = client.getIp().hashCode();
            AtomicInteger connectionCount = event.computeIfAbsent(addrHash, var0 -> new AtomicInteger());
            if (connectionCount.get() + 1 <= max + DualboxCheckConfig.DUALBOX_CHECK_WHITELIST.getOrDefault(addrHash, 0)) {
               connectionCount.incrementAndGet();
               return true;
            } else {
               return false;
            }
         }
      }
   }

   public boolean removePlayer(int eventId, Player player) {
      return this.removeClient(eventId, player.getClient());
   }

   public boolean removeClient(int eventId, GameClient client) {
      if (client == null) {
         return false;
      } else {
         Map<Integer, AtomicInteger> event = this._eventIPs.get(eventId);
         if (event == null) {
            return false;
         } else {
            Integer addrHash = client.getIp().hashCode();
            return event.computeIfPresent(addrHash, (var0, v) -> (AtomicInteger)(v != null && v.decrementAndGet() != 0 ? v : null)) != null;
         }
      }
   }

   public void onDisconnect(GameClient client) {
      if (client != null) {
         Player player = client.getPlayer();
         if (player != null) {
            if (!player.isInOfflineMode()) {
               String clientIp = client.getIp();
               if (clientIp != null) {
                  for (Entry<Integer, Map<Integer, AtomicInteger>> entry : this._eventIPs.entrySet()) {
                     int eventId = entry.getKey();
                     if (eventId == 1) {
                        AtomicInteger count = entry.getValue().get(clientIp.hashCode());
                        if (count != null && (Olympiad.getInstance().isRegistered(player) || player.getOlympiadGameId() != -1)) {
                           count.decrementAndGet();
                        }
                     } else {
                        this.removeClient(eventId, client);
                     }
                  }
               }
            }
         }
      }
   }

   public void clear(int eventId) {
      Map<Integer, AtomicInteger> event = this._eventIPs.get(eventId);
      if (event != null) {
         event.clear();
      }
   }

   public int getLimit(Player player, int max) {
      return this.getLimit(player.getClient(), max);
   }

   public int getLimit(GameClient client, int max) {
      if (client == null) {
         return max;
      } else {
         Integer addrHash = client.getIp().hashCode();
         int limit = max;
         Integer whiteListLimit = DualboxCheckConfig.DUALBOX_CHECK_WHITELIST.get(addrHash);
         if (whiteListLimit != null) {
            if (whiteListLimit < 1) {
               return Integer.MAX_VALUE;
            }

            limit = max + whiteListLimit;
         }

         return limit;
      }
   }

   public static AntiFeedManager getInstance() {
      return AntiFeedManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final AntiFeedManager INSTANCE = new AntiFeedManager();
   }
}
