package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

import java.util.ArrayList;
import java.util.List;

public class UCRunningTask implements Runnable {
   private final UCArena _arena;

   public UCRunningTask(UCArena arena) {
      this._arena = arena;
   }

   @Override
   public void run() {
      this._arena.generateWinner();
      this._arena.removeTeams();
      UCTeam winnerTeam = null;

      for (UCTeam team : this._arena.getTeams()) {
         if (team.getStatus() == 1) {
            winnerTeam = team;
         } else if (team.getStatus() == 2) {
            team.cleanUp();
         }
      }

      for (UCPoint point : this._arena.getPoints()) {
         point.actionDoors(false);
         point.getPlayers().clear();
      }

      if (winnerTeam != null) {
         if (!this._arena.getWaitingList().isEmpty()) {
            UCTeam other = winnerTeam.getOtherTeam();
            UCWaiting otherWaiting = this._arena.getWaitingList().get(0);
            other.setParty(otherWaiting.getParty());
            other.setRegisterTime(otherWaiting.getRegisterMillis());
            this._arena.getWaitingList().remove(0);
            this._arena.prepareStart();
            return;
         }

         winnerTeam.cleanUp();
      }

      if (this._arena.getWaitingList().size() < 2) {
         this._arena.setIsBattleNow(false);
         this._arena.runNewTask(false);
      } else {
         int i = 0;
         UCWaiting teamWaiting = null;
         List<UCWaiting> removeList = new ArrayList<>();

         for (UCTeam teamx : this._arena.getTeams()) {
            teamWaiting = this._arena.getWaitingList().get(i);
            removeList.add(teamWaiting);
            teamx.setParty(teamWaiting.getParty());
            teamx.setRegisterTime(teamWaiting.getRegisterMillis());
            if (++i == 2) {
               break;
            }
         }

         for (UCWaiting tm : removeList) {
            this._arena.getWaitingList().remove(tm);
         }

         removeList.clear();
         this._arena.prepareStart();
      }
   }
}
