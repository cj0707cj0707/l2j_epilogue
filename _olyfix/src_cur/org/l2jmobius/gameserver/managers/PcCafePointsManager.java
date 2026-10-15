package org.l2jmobius.gameserver.managers;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PcCafePointsManager {
   public void run(Player player) {
      if (PremiumSystemConfig.PC_CAFE_ENABLED && PremiumSystemConfig.PC_CAFE_RETAIL_LIKE && player.hasEnteredWorld()) {
         ThreadPool.scheduleAtFixedRate(
            () -> this.giveRetailPcCafePont(player), PremiumSystemConfig.PC_CAFE_REWARD_TIME, PremiumSystemConfig.PC_CAFE_REWARD_TIME
         );
      }
   }

   public void giveRetailPcCafePont(Player player) {
      if (PremiumSystemConfig.PC_CAFE_ENABLED
         && PremiumSystemConfig.PC_CAFE_RETAIL_LIKE
         && player.isOnlineInt() != 0
         && (player.hasPremiumStatus() || !PremiumSystemConfig.PC_CAFE_ONLY_PREMIUM)
         && !player.isInOfflineMode()) {
         int points = PremiumSystemConfig.ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS;
         if (points >= PremiumSystemConfig.PC_CAFE_MAX_POINTS) {
            player.sendPacket(SystemMessageId.THE_MAXIMUM_ACCUMULATION_ALLOWED_OF_PC_CAFE_POINTS_HAS_BEEN_EXCEEDED_YOU_CAN_NO_LONGER_ACQUIRE_PC_CAFE_POINTS);
         } else {
            if (PremiumSystemConfig.PC_CAFE_RANDOM_POINT) {
               points = Rnd.get(points / 2, points);
            }

            SystemMessage message = null;
            if (PremiumSystemConfig.PC_CAFE_ENABLE_DOUBLE_POINTS && Rnd.get(100) < PremiumSystemConfig.PC_CAFE_DOUBLE_POINTS_CHANCE) {
               points *= 2;
               message = new SystemMessage(SystemMessageId.DOUBLE_POINTS_YOU_ACQUIRED_S1_PC_BANG_POINT);
            } else {
               message = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
            }

            if (player.getPcCafePoints() + points > PremiumSystemConfig.PC_CAFE_MAX_POINTS) {
               points = PremiumSystemConfig.PC_CAFE_MAX_POINTS - player.getPcCafePoints();
            }

            message.addLong(points);
            player.sendPacket(message);
            player.setPcCafePoints(player.getPcCafePoints() + points);
            player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), points, 1));
         }
      }
   }

   public void givePcCafePoint(Player player, double exp) {
      if (!PremiumSystemConfig.PC_CAFE_RETAIL_LIKE
         && PremiumSystemConfig.PC_CAFE_ENABLED
         && !player.isInsideZone(ZoneId.PEACE)
         && !player.isInsideZone(ZoneId.PVP)
         && !player.isInsideZone(ZoneId.SIEGE)
         && player.isOnlineInt() != 0
         && !player.isJailed()) {
         if (!PremiumSystemConfig.PC_CAFE_ONLY_PREMIUM || player.hasPremiumStatus()) {
            if (player.getPcCafePoints() >= PremiumSystemConfig.PC_CAFE_MAX_POINTS) {
               SystemMessage message = new SystemMessage(
                  SystemMessageId.THE_MAXIMUM_ACCUMULATION_ALLOWED_OF_PC_CAFE_POINTS_HAS_BEEN_EXCEEDED_YOU_CAN_NO_LONGER_ACQUIRE_PC_CAFE_POINTS
               );
               player.sendPacket(message);
            } else {
               int points = (int)(exp * 1.0E-4 * PremiumSystemConfig.PC_CAFE_POINT_RATE);
               if (PremiumSystemConfig.PC_CAFE_RANDOM_POINT) {
                  points = Rnd.get(points / 2, points);
               }

               if (points == 0
                  && exp > 0.0
                  && PremiumSystemConfig.PC_CAFE_REWARD_LOW_EXP_KILLS
                  && Rnd.get(100) < PremiumSystemConfig.PC_CAFE_LOW_EXP_KILLS_CHANCE) {
                  points = 1;
               }

               if (points > 0) {
                  SystemMessage message = null;
                  if (PremiumSystemConfig.PC_CAFE_ENABLE_DOUBLE_POINTS && Rnd.get(100) < PremiumSystemConfig.PC_CAFE_DOUBLE_POINTS_CHANCE) {
                     points *= 2;
                     message = new SystemMessage(SystemMessageId.DOUBLE_POINTS_YOU_ACQUIRED_S1_PC_BANG_POINT);
                  } else {
                     message = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
                  }

                  if (player.getPcCafePoints() + points > PremiumSystemConfig.PC_CAFE_MAX_POINTS) {
                     points = PremiumSystemConfig.PC_CAFE_MAX_POINTS - player.getPcCafePoints();
                  }

                  message.addLong(points);
                  player.sendPacket(message);
                  player.setPcCafePoints(player.getPcCafePoints() + points);
                  player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), points, 0));
               }
            }
         }
      }
   }

   public static PcCafePointsManager getInstance() {
      return PcCafePointsManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PcCafePointsManager INSTANCE = new PcCafePointsManager();
   }
}
