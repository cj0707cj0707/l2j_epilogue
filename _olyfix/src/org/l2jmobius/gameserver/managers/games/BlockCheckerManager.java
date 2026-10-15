package org.l2jmobius.gameserver.managers.games;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Block;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameChangePoints;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameCloseUI;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameExtendedChangePoints;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class BlockCheckerManager {
   protected static final Logger LOGGER = Logger.getLogger(BlockCheckerManager.class.getName());
   protected ArenaParticipantsHolder _holder;
   protected Map<Player, Integer> _redTeamPoints = new ConcurrentHashMap<>();
   protected Map<Player, Integer> _blueTeamPoints = new ConcurrentHashMap<>();
   protected int _redPoints = 15;
   protected int _bluePoints = 15;
   protected int _arena = -1;
   protected Collection<Spawn> _spawns = ConcurrentHashMap.newKeySet();
   protected boolean _isRedWinner;
   protected long _startedTime;
   protected static final int[][] _arenaCoordinates = new int[][]{
      {-58368, -62745, -57751, -62131, -58053, -62417},
      {-58350, -63853, -57756, -63266, -58053, -63551},
      {-57194, -63861, -56580, -63249, -56886, -63551},
      {-57200, -62727, -56584, -62115, -56850, -62391}
   };
   private static final int Z_COORD = -2405;
   protected Collection<Item> _drops = ConcurrentHashMap.newKeySet();
   private static final byte DEFAULT_ARENA = -1;
   protected boolean _isStarted = false;
   protected ScheduledFuture<?> _task;
   protected boolean _abnormalEnd = false;

   public BlockCheckerManager(ArenaParticipantsHolder holder, int arena) {
      this._holder = holder;
      if (arena > -1 && arena < 4) {
         this._arena = arena;
      }

      for (Player player : holder.getRedPlayers()) {
         this._redTeamPoints.put(player, 0);
      }

      for (Player player : holder.getBluePlayers()) {
         this._blueTeamPoints.put(player, 0);
      }
   }

   public void updatePlayersOnStart(ArenaParticipantsHolder holder) {
      this._holder = holder;
   }

   public ArenaParticipantsHolder getHolder() {
      return this._holder;
   }

   public int getArena() {
      return this._arena;
   }

   public long getStarterTime() {
      return this._startedTime;
   }

   public int getRedPoints() {
      synchronized (this) {
         return this._redPoints;
      }
   }

   public int getBluePoints() {
      synchronized (this) {
         return this._bluePoints;
      }
   }

   public int getPlayerPoints(Player player, boolean isRed) {
      if (!this._redTeamPoints.containsKey(player) && !this._blueTeamPoints.containsKey(player)) {
         return 0;
      } else {
         return isRed ? this._redTeamPoints.get(player) : this._blueTeamPoints.get(player);
      }
   }

   public synchronized void increasePlayerPoints(Player player, int team) {
      if (player != null) {
         if (team == 0) {
            int points = this._redTeamPoints.get(player) + 1;
            this._redTeamPoints.put(player, points);
            this._redPoints++;
            this._bluePoints--;
         } else {
            int points = this._blueTeamPoints.get(player) + 1;
            this._blueTeamPoints.put(player, points);
            this._bluePoints++;
            this._redPoints--;
         }
      }
   }

   public void addNewDrop(Item item) {
      if (item != null) {
         this._drops.add(item);
      }
   }

   public boolean isStarted() {
      return this._isStarted;
   }

   protected void broadcastRelationChanged(Player plr) {
      for (Player p : this._holder.getAllPlayers()) {
         p.sendPacket(new RelationChanged(plr, plr.getRelation(p), plr.isAutoAttackable(p)));
      }
   }

   public void endEventAbnormally() {
      try {
         synchronized (this) {
            this._isStarted = false;
            if (this._task != null) {
               this._task.cancel(true);
            }

            this._abnormalEnd = true;
            ThreadPool.execute(new BlockCheckerManager.EndEvent());
         }
      } catch (Exception var4) {
         LOGGER.log(Level.SEVERE, "Could not end Block Checker event at " + this._arena, (Throwable)var4);
      }
   }

   private class CarryingGirlUnspawn implements Runnable {
      private final Spawn _spawn;

      protected CarryingGirlUnspawn(Spawn spawn) {
         Objects.requireNonNull(BlockCheckerManager.this);
         super();
         this._spawn = spawn;
      }

      @Override
      public void run() {
         if (this._spawn == null) {
            BlockCheckerManager.LOGGER.warning("HBCE: Block Carrying Girl is null");
         } else {
            SpawnTable.getInstance().removeSpawn(this._spawn);
            this._spawn.stopRespawn();
            this._spawn.getLastSpawn().deleteMe();
         }
      }
   }

   protected class EndEvent implements Runnable {
      protected EndEvent() {
         Objects.requireNonNull(BlockCheckerManager.this);
         super();
      }

      private void clearMe() {
         HandysBlockCheckerManager.getInstance().clearPaticipantQueueByArenaId(BlockCheckerManager.this._arena);
         BlockCheckerManager.this._holder.clearPlayers();
         BlockCheckerManager.this._blueTeamPoints.clear();
         BlockCheckerManager.this._redTeamPoints.clear();
         HandysBlockCheckerManager.getInstance().setArenaFree(BlockCheckerManager.this._arena);

         for (Spawn spawn : BlockCheckerManager.this._spawns) {
            spawn.stopRespawn();
            spawn.getLastSpawn().deleteMe();
            SpawnTable.getInstance().removeSpawn(spawn);
         }

         BlockCheckerManager.this._spawns.clear();

         for (Item item : BlockCheckerManager.this._drops) {
            if (item != null && item.isSpawned() && item.getOwnerId() == 0) {
               item.decayMe();
            }
         }

         BlockCheckerManager.this._drops.clear();
      }

      private void rewardPlayers() {
         if (BlockCheckerManager.this._redPoints != BlockCheckerManager.this._bluePoints) {
            BlockCheckerManager.this._isRedWinner = BlockCheckerManager.this._redPoints > BlockCheckerManager.this._bluePoints;
            if (BlockCheckerManager.this._isRedWinner) {
               this.rewardAsWinner(true);
               this.rewardAsLooser(false);
               SystemMessage msg = new SystemMessage(SystemMessageId.THE_C1_TEAM_HAS_WON);
               msg.addString("Red Team");
               BlockCheckerManager.this._holder.broadCastPacketToTeam(msg);
            } else if (BlockCheckerManager.this._bluePoints > BlockCheckerManager.this._redPoints) {
               this.rewardAsWinner(false);
               this.rewardAsLooser(true);
               SystemMessage msg = new SystemMessage(SystemMessageId.THE_C1_TEAM_HAS_WON);
               msg.addString("Blue Team");
               BlockCheckerManager.this._holder.broadCastPacketToTeam(msg);
            } else {
               this.rewardAsLooser(true);
               this.rewardAsLooser(false);
            }
         }
      }

      private void rewardAsWinner(boolean isRed) {
         Map<Player, Integer> tempPoints = isRed ? BlockCheckerManager.this._redTeamPoints : BlockCheckerManager.this._blueTeamPoints;

         for (Entry<Player, Integer> points : tempPoints.entrySet()) {
            if (points.getKey() != null) {
               if (points.getValue() >= 10) {
                  points.getKey().addItem(ItemProcessType.QUEST, 13067, 2L, points.getKey(), true);
               } else {
                  tempPoints.remove(points.getKey());
               }
            }
         }

         int first = 0;
         int second = 0;
         Player winner1 = null;
         Player winner2 = null;

         for (Entry<Player, Integer> entry : tempPoints.entrySet()) {
            Player pc = entry.getKey();
            int pcPoints = entry.getValue();
            if (pcPoints > first) {
               second = first;
               winner2 = winner1;
               first = pcPoints;
               winner1 = pc;
            } else if (pcPoints > second) {
               second = pcPoints;
               winner2 = pc;
            }
         }

         if (winner1 != null) {
            winner1.addItem(ItemProcessType.QUEST, 13067, 8L, winner1, true);
         }

         if (winner2 != null) {
            winner2.addItem(ItemProcessType.QUEST, 13067, 5L, winner2, true);
         }
      }

      private void rewardAsLooser(boolean isRed) {
         for (Entry<Player, Integer> entry : (isRed ? BlockCheckerManager.this._redTeamPoints : BlockCheckerManager.this._blueTeamPoints).entrySet()) {
            Player player = entry.getKey();
            if (player != null && entry.getValue() >= 10) {
               player.addItem(ItemProcessType.QUEST, 13067, 2L, player, true);
            }
         }
      }

      private void setPlayersBack() {
         ExCubeGameEnd end = new ExCubeGameEnd(BlockCheckerManager.this._isRedWinner);

         for (Player player : BlockCheckerManager.this._holder.getAllPlayers()) {
            if (player != null) {
               player.stopAllEffects();
               player.setTeam(Team.NONE);
               player.setBlockCheckerArena((byte)-1);
               player.untransform();
               PlayerInventory inv = player.getInventory();
               if (inv.getItemByItemId(13787) != null) {
                  inv.destroyItemByItemId(ItemProcessType.DESTROY, 13787, inv.getInventoryItemCount(13787, 0), player, player);
               }

               if (inv.getItemByItemId(13788) != null) {
                  inv.destroyItemByItemId(ItemProcessType.DESTROY, 13788, inv.getInventoryItemCount(13788, 0), player, player);
               }

               BlockCheckerManager.this.broadcastRelationChanged(player);
               player.teleToLocation(-57478, -60367, -2370);
               player.setInsideZone(ZoneId.PVP, false);
               player.sendPacket(end);
               player.broadcastUserInfo();
            }
         }
      }

      @Override
      public void run() {
         if (!BlockCheckerManager.this._abnormalEnd) {
            this.rewardPlayers();
         }

         this.setPlayersBack();
         this.clearMe();
         BlockCheckerManager.this._isStarted = false;
         BlockCheckerManager.this._abnormalEnd = false;
      }
   }

   private class SpawnRound implements Runnable {
      int _numOfBoxes;
      int _round;

      SpawnRound(int numberOfBoxes, int round) {
         Objects.requireNonNull(BlockCheckerManager.this);
         super();
         this._numOfBoxes = numberOfBoxes;
         this._round = round;
      }

      @Override
      public void run() {
         if (BlockCheckerManager.this._isStarted) {
            switch (this._round) {
               case 1:
                  BlockCheckerManager.this._task = ThreadPool.schedule(BlockCheckerManager.this.new SpawnRound(20, 2), 60000L);
                  break;
               case 2:
                  BlockCheckerManager.this._task = ThreadPool.schedule(BlockCheckerManager.this.new SpawnRound(14, 3), 60000L);
                  break;
               case 3:
                  BlockCheckerManager.this._task = ThreadPool.schedule(BlockCheckerManager.this.new EndEvent(), 180000L);
            }

            byte random = 2;

            try {
               for (int i = 0; i < this._numOfBoxes; i++) {
                  Spawn spawn = new Spawn(18672);
                  spawn.setXYZ(
                     BlockCheckerManager._arenaCoordinates[BlockCheckerManager.this._arena][4] + Rnd.get(-400, 400),
                     BlockCheckerManager._arenaCoordinates[BlockCheckerManager.this._arena][5] + Rnd.get(-400, 400),
                     -2405
                  );
                  spawn.setAmount(1);
                  spawn.setHeading(1);
                  spawn.setRespawnDelay(1);
                  SpawnTable.getInstance().addSpawn(spawn);
                  spawn.init();
                  Block block = (Block)spawn.getLastSpawn();
                  block.setRed(random % 2 == 0);
                  block.disableCoreAI(true);
                  BlockCheckerManager.this._spawns.add(spawn);
                  random++;
               }
            } catch (Exception var6) {
               BlockCheckerManager.LOGGER.warning(this.getClass().getSimpleName() + ": " + var6.getMessage());
            }

            if (this._round == 1 || this._round == 2) {
               try {
                  Spawn girlSpawn = new Spawn(18676);
                  girlSpawn.setXYZ(
                     BlockCheckerManager._arenaCoordinates[BlockCheckerManager.this._arena][4] + Rnd.get(-400, 400),
                     BlockCheckerManager._arenaCoordinates[BlockCheckerManager.this._arena][5] + Rnd.get(-400, 400),
                     -2405
                  );
                  girlSpawn.setAmount(1);
                  girlSpawn.setHeading(1);
                  girlSpawn.setRespawnDelay(1);
                  SpawnTable.getInstance().addSpawn(girlSpawn);
                  girlSpawn.init();
                  ThreadPool.schedule(BlockCheckerManager.this.new CarryingGirlUnspawn(girlSpawn), 9000L);
               } catch (Exception var5) {
                  BlockCheckerManager.LOGGER.warning("Could not Spawn Block Checker NPCs! Wrong instance type at npc table?");
                  BlockCheckerManager.LOGGER.warning(this.getClass().getSimpleName() + ": " + var5.getMessage());
               }
            }

            BlockCheckerManager.this._redPoints = BlockCheckerManager.this._redPoints + this._numOfBoxes / 2;
            BlockCheckerManager.this._bluePoints = BlockCheckerManager.this._bluePoints + this._numOfBoxes / 2;
            BlockCheckerManager.this._holder
               .broadCastPacketToTeam(
                  new ExCubeGameChangePoints(
                     (int)((BlockCheckerManager.this._startedTime - System.currentTimeMillis()) / 1000L),
                     BlockCheckerManager.this.getBluePoints(),
                     BlockCheckerManager.this.getRedPoints()
                  )
               );
         }
      }
   }

   public class StartEvent implements Runnable {
      private final Skill _freeze;
      private final Skill _transformationRed;
      private final Skill _transformationBlue;
      private final ExCubeGameCloseUI _closeUserInterface;

      public StartEvent() {
         Objects.requireNonNull(BlockCheckerManager.this);
         super();
         this._closeUserInterface = new ExCubeGameCloseUI();
         this._freeze = SkillData.getInstance().getSkill(6034, 1);
         this._transformationRed = SkillData.getInstance().getSkill(6035, 1);
         this._transformationBlue = SkillData.getInstance().getSkill(6036, 1);
      }

      private void setUpPlayers() {
         HandysBlockCheckerManager.getInstance().setArenaBeingUsed(BlockCheckerManager.this._arena);
         BlockCheckerManager.this._redPoints = BlockCheckerManager.this._spawns.size() / 2;
         BlockCheckerManager.this._bluePoints = BlockCheckerManager.this._spawns.size() / 2;
         ExCubeGameChangePoints initialPoints = new ExCubeGameChangePoints(300, BlockCheckerManager.this._bluePoints, BlockCheckerManager.this._redPoints);

         for (Player player : BlockCheckerManager.this._holder.getAllPlayers()) {
            if (player != null) {
               boolean isRed = BlockCheckerManager.this._holder.getRedPlayers().contains(player);
               ExCubeGameExtendedChangePoints clientSetUp = new ExCubeGameExtendedChangePoints(
                  300, BlockCheckerManager.this._bluePoints, BlockCheckerManager.this._redPoints, isRed, player, 0
               );
               player.sendPacket(clientSetUp);
               player.sendPacket(ActionFailed.STATIC_PACKET);
               int tc = BlockCheckerManager.this._holder.getPlayerTeam(player) * 2;
               int x = BlockCheckerManager._arenaCoordinates[BlockCheckerManager.this._arena][tc];
               int y = BlockCheckerManager._arenaCoordinates[BlockCheckerManager.this._arena][tc + 1];
               player.teleToLocation(x, y, -2405);
               if (isRed) {
                  BlockCheckerManager.this._redTeamPoints.put(player, 0);
                  player.setTeam(Team.RED);
               } else {
                  BlockCheckerManager.this._blueTeamPoints.put(player, 0);
                  player.setTeam(Team.BLUE);
               }

               player.stopAllEffects();
               if (player.hasSummon()) {
                  player.getSummon().unSummon(player);
               }

               this._freeze.applyEffects(player, player);
               if (BlockCheckerManager.this._holder.getPlayerTeam(player) == 0) {
                  this._transformationRed.applyEffects(player, player);
               } else {
                  this._transformationBlue.applyEffects(player, player);
               }

               player.setBlockCheckerArena((byte)BlockCheckerManager.this._arena);
               player.setInsideZone(ZoneId.PVP, true);
               player.sendPacket(initialPoints);
               player.sendPacket(this._closeUserInterface);
               player.sendPacket(ExBasicActionList.STATIC_PACKET);
               BlockCheckerManager.this.broadcastRelationChanged(player);
            }
         }
      }

      @Override
      public void run() {
         if (BlockCheckerManager.this._arena == -1) {
            BlockCheckerManager.LOGGER.severe("Could not set up the arena Id for the Block Checker event, cancelling event...");
         } else {
            BlockCheckerManager.this._isStarted = true;
            ThreadPool.execute(BlockCheckerManager.this.new SpawnRound(16, 1));
            this.setUpPlayers();
            BlockCheckerManager.this._startedTime = System.currentTimeMillis() + 300000L;
         }
      }
   }
}
