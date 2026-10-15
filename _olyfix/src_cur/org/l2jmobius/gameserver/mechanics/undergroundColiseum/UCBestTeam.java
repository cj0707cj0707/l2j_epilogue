package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

public class UCBestTeam {
   private final int _arenaId;
   private String _leaderName;
   private int _wins;

   public UCBestTeam(int arenaId, String leaderName, int wins) {
      this._arenaId = arenaId;
      this._leaderName = leaderName;
      this._wins = wins;
   }

   public int getArenaId() {
      return this._arenaId;
   }

   public String getLeaderName() {
      return this._leaderName;
   }

   public void setLeader(String leader) {
      this._leaderName = leader;
   }

   public int getWins() {
      return this._wins;
   }

   public void setWins(int wins) {
      this._wins = wins;
   }
}
