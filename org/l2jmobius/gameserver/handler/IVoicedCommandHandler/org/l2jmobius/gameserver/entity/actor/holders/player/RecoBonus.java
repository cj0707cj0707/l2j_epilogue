package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.entity.actor.Player;

public class RecoBonus {
   private static final int[][] _recoBonus = new int[][]{
      {25, 50, 50, 50, 50, 50, 50, 50, 50, 50},
      {16, 33, 50, 50, 50, 50, 50, 50, 50, 50},
      {12, 25, 37, 50, 50, 50, 50, 50, 50, 50},
      {10, 20, 30, 40, 50, 50, 50, 50, 50, 50},
      {8, 16, 25, 33, 41, 50, 50, 50, 50, 50},
      {7, 14, 21, 28, 35, 42, 50, 50, 50, 50},
      {6, 12, 18, 25, 31, 37, 43, 50, 50, 50},
      {5, 11, 16, 22, 27, 33, 38, 44, 50, 50},
      {5, 10, 15, 20, 25, 30, 35, 40, 45, 50}
   };

   public static int getRecoBonus(Player player) {
      if (player != null && player.isOnline() && player.getRecomHave() != 0) {
         int level = player.getLevel() / 10;
         int exp = (Math.min(100, player.getRecomHave()) - 1) / 10;
         return _recoBonus[level][exp];
      } else {
         return 0;
      }
   }

   public static double getRecoMultiplier(Player player) {
      double multiplier = 1.0;
      double bonus = getRecoBonus(player);
      if (bonus > 0.0) {
         multiplier += bonus / 100.0;
      }

      return multiplier;
   }
}
