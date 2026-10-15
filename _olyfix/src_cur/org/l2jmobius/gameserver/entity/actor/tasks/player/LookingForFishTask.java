package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;

public class LookingForFishTask implements Runnable {
   private final Player _player;
   private final boolean _isNoob;
   private final boolean _isUpperGrade;
   private final int _fishGroup;
   private final double _fishGutsCheck;
   private final long _endTaskTime;

   public LookingForFishTask(Player player, int startCombatTime, double fishGutsCheck, int fishGroup, boolean isNoob, boolean isUpperGrade) {
      this._player = player;
      this._fishGutsCheck = fishGutsCheck;
      this._endTaskTime = System.currentTimeMillis() + startCombatTime * 1000 + 10000L;
      this._fishGroup = fishGroup;
      this._isNoob = isNoob;
      this._isUpperGrade = isUpperGrade;
   }

   @Override
   public void run() {
      if (this._player != null) {
         if (System.currentTimeMillis() >= this._endTaskTime) {
            this._player.endFishing(false);
            return;
         }

         if (this._fishGroup == -1) {
            return;
         }

         int check = Rnd.get(100);
         if (this._fishGutsCheck > check) {
            this._player.stopLookingForFishTask();
            this._player.startFishCombat(this._isNoob, this._isUpperGrade);
         }
      }
   }
}
