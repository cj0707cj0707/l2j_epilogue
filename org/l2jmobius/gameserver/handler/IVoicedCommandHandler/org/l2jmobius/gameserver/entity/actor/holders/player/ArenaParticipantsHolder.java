package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.managers.games.BlockCheckerManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class ArenaParticipantsHolder {
   private final int _arena;
   private final List<Player> _redPlayers;
   private final List<Player> _bluePlayers;
   private final BlockCheckerManager _engine;

   public ArenaParticipantsHolder(int arena) {
      this._arena = arena;
      this._redPlayers = new ArrayList<>(6);
      this._bluePlayers = new ArrayList<>(6);
      this._engine = new BlockCheckerManager(this, this._arena);
   }

   public List<Player> getRedPlayers() {
      return this._redPlayers;
   }

   public List<Player> getBluePlayers() {
      return this._bluePlayers;
   }

   public List<Player> getAllPlayers() {
      List<Player> all = new ArrayList<>(this._redPlayers);
      all.addAll(this._bluePlayers);
      return all;
   }

   public void addPlayer(Player player, int team) {
      if (team == 0) {
         this._redPlayers.add(player);
      } else {
         this._bluePlayers.add(player);
      }
   }

   public void removePlayer(Player player, int team) {
      if (team == 0) {
         this._redPlayers.remove(player);
      } else {
         this._bluePlayers.remove(player);
      }
   }

   public int getPlayerTeam(Player player) {
      if (this._redPlayers.contains(player)) {
         return 0;
      } else {
         return this._bluePlayers.contains(player) ? 1 : -1;
      }
   }

   public int getRedTeamSize() {
      return this._redPlayers.size();
   }

   public int getBlueTeamSize() {
      return this._bluePlayers.size();
   }

   public void broadCastPacketToTeam(ServerPacket packet) {
      for (Player p : this._redPlayers) {
         p.sendPacket(packet);
      }

      for (Player p : this._bluePlayers) {
         p.sendPacket(packet);
      }
   }

   public void clearPlayers() {
      this._redPlayers.clear();
      this._bluePlayers.clear();
   }

   public BlockCheckerManager getEvent() {
      return this._engine;
   }

   public void updateEvent() {
      this._engine.updatePlayersOnStart(this);
   }

   public void checkAndShuffle() {
      int redSize = this._redPlayers.size();
      int blueSize = this._bluePlayers.size();
      if (redSize > blueSize + 1) {
         this.broadCastPacketToTeam(new SystemMessage(SystemMessageId.THE_TEAM_WAS_ADJUSTED_BECAUSE_THE_POPULATION_RATIO_WAS_NOT_CORRECT));

         for (int i = 0; i < redSize - (blueSize + 1) + 1; i++) {
            Player plr = this._redPlayers.get(i);
            if (plr != null) {
               HandysBlockCheckerManager.getInstance().changePlayerToTeam(plr, this._arena);
            }
         }
      } else if (blueSize > redSize + 1) {
         this.broadCastPacketToTeam(new SystemMessage(SystemMessageId.THE_TEAM_WAS_ADJUSTED_BECAUSE_THE_POPULATION_RATIO_WAS_NOT_CORRECT));

         for (int ix = 0; ix < blueSize - (redSize + 1) + 1; ix++) {
            Player plr = this._bluePlayers.get(ix);
            if (plr != null) {
               HandysBlockCheckerManager.getInstance().changePlayerToTeam(plr, this._arena);
            }
         }
      }
   }
}
