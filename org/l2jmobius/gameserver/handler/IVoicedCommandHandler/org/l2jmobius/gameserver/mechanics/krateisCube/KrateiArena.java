package org.l2jmobius.gameserver.mechanics.krateisCube;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.KrateisMatchManager;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.games.KrateisCubeManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCMyRecord;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRetire;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class KrateiArena {
   private final int _id;
   private final int _minLevel;
   private final int _maxLevel;
   private final int _minPlayers;
   private final int _maxPlayers;
   private KrateisMatchManager _manager = null;
   private final Map<Player, KrateiCubePlayer> _players = new ConcurrentHashMap<>();
   private final List<Skill> _buffs = new ArrayList<>();
   private final List<Door> _doorListA = new ArrayList<>();
   private final List<Door> _doorListB = new ArrayList<>();
   private final List<Location> _waitLoc = new ArrayList<>();
   private final List<Location> _battleLoc = new ArrayList<>();
   private final List<Location> _watcherLoc = new ArrayList<>();
   private final List<KrateisReward> _rewards = new ArrayList<>();
   private final List<Npc> _watchers = new ArrayList<>();
   private StatSet _params;
   private ScheduledFuture<?> _eventTask = null;
   private ScheduledFuture<?> _timeTask = null;
   private ScheduledFuture<?> _watcherTask = null;
   private ScheduledFuture<?> _doorTask = null;
   private boolean _doorsRotation = false;
   private boolean _watcherRotation = false;
   private boolean _isBattleNow = false;
   private boolean _isActiveNow = false;

   public KrateiArena(int id, int manager, int minLevel, int maxLevel, int minPlayers, int maxPlayers) {
      this._id = id;
      this._minLevel = minLevel;
      this._maxLevel = maxLevel;
      this._minPlayers = minPlayers;
      this._maxPlayers = maxPlayers;
      this._rewards.clear();
      Spawn spawn = SpawnTable.getInstance().getAnySpawn(manager);
      if (spawn != null) {
         this._manager = (KrateisMatchManager)spawn.getLastSpawn();
         this._manager.setArenaId(this.getId());
      }
   }

   public int getId() {
      return this._id;
   }

   public int getMinLevel() {
      return this._minLevel;
   }

   public int getMaxLevel() {
      return this._maxLevel;
   }

   public int getMinPlayers() {
      return this._minPlayers;
   }

   public int getMaxPlayers() {
      return this._maxPlayers;
   }

   public KrateisMatchManager getManager() {
      return this._manager;
   }

   public boolean isBattleNow() {
      return this._isBattleNow;
   }

   public void setIsBattleNow(boolean value) {
      this._isBattleNow = value;
   }

   public boolean isActiveNow() {
      return this._isActiveNow;
   }

   public void setReward(KrateisReward ucReward) {
      this._rewards.add(ucReward);
   }

   public List<KrateisReward> getRewards() {
      return this._rewards;
   }

   public void addBuff(Skill skill) {
      this._buffs.add(skill);
   }

   public List<Skill> getBuffList() {
      return this._buffs;
   }

   public void addDoorA(Door door) {
      this._doorListA.add(door);
   }

   public List<Door> getDoorListA() {
      return this._doorListA;
   }

   public void addDoorB(Door door) {
      this._doorListB.add(door);
   }

   public List<Door> getDoorListB() {
      return this._doorListB;
   }

   public List<Location> getWaitLoc() {
      return this._waitLoc;
   }

   public void addWaitLoc(Location loc) {
      this._waitLoc.add(loc);
   }

   public List<Location> getBattleLoc() {
      return this._battleLoc;
   }

   public void addBattleLoc(Location loc) {
      this._battleLoc.add(loc);
   }

   public List<Location> getWatcherLoc() {
      return this._watcherLoc;
   }

   public void addWatcherLoc(Location loc) {
      this._watcherLoc.add(loc);
   }

   public void addParam(StatSet params) {
      this._params = params;
   }

   public StatSet getParams() {
      return this._params;
   }

   public boolean addRegisterPlayer(Player player) {
      if (!this._players.containsKey(player)) {
         KrateiCubePlayer pl = this._players.computeIfAbsent(player, var1x -> new KrateiCubePlayer(player));
         this._players.put(player, pl);
         pl.setIsRegister(true);
         player.setKrateiArena(this);
         player.setRegisteredOnEvent(true);
         return true;
      } else {
         return false;
      }
   }

   public boolean removePlayer(Player player) {
      if (this._players.containsKey(player)) {
         this._players.remove(player);
         player.setKrateiArena(null);
         player.setRegisteredOnEvent(false);
         return true;
      } else {
         return false;
      }
   }

   public boolean isRegisterPlayer(Player player) {
      return this._players.containsKey(player);
   }

   public Map<Player, KrateiCubePlayer> getPlayers() {
      return this._players;
   }

   public void startEvent() {
      if (this._eventTask != null) {
         this._eventTask.cancel(false);
      }

      if (this._timeTask != null) {
         this._timeTask.cancel(false);
      }

      if (this._watcherTask != null) {
         this._watcherTask.cancel(false);
      }

      if (this._doorTask != null) {
         this._doorTask.cancel(false);
      }

      this._doorsRotation = true;
      this.changeDoorStatus(true);
      if (!this.getWatcherLoc().isEmpty()) {
         for (Location loc : this.getWatcherLoc()) {
            if (loc != null) {
               Npc npc = Quest.addSpawn(18601, loc, false, 0L);
               npc.disableCoreAI(true);
               npc.setImmobilized(true);
               this._watchers.add(npc);
            }
         }

         this._watcherRotation = true;
      }

      this.teleportAllPlayers();
      long time = 1200000L;
      this._eventTask = ThreadPool.schedule(this::endEvent, 1200000L);
      this._timeTask = ThreadPool.schedule(this::timeLeftInfo, 1189000L);
      this._watcherTask = ThreadPool.schedule(this::changeWatchers, this.getParams().getInt("watcherRotationTime") * 1000);
      this._isActiveNow = true;
   }

   public void endEvent() {
      if (this._eventTask != null) {
         this._eventTask.cancel(true);
         this._eventTask = null;
      }

      if (this._watcherTask != null) {
         this._watcherTask.cancel(true);
         this._watcherTask = null;
      }

      if (this._doorTask != null) {
         this._doorTask.cancel(true);
         this._doorTask = null;
      }

      this._watcherRotation = false;
      this.changeDoorStatus(false);
      if (!this._watchers.isEmpty()) {
         for (Npc npc : this._watchers) {
            if (npc != null) {
               npc.deleteMe();
            }
         }

         this._watchers.clear();
      }

      Set<Player> participants = this.getPlayers().keySet();
      if (participants != null && !participants.isEmpty()) {
         for (Player player : participants) {
            if (player != null) {
               this.removeAllEffects(player);
               player.teleToLocation(-70381, -70937, -1428, 0, true);
               player.setKrateiArena(null);
               player.setRegisteredOnEvent(false);
               player.sendPacket(ExPVPMatchCCRetire.STATIC);
               player.setCanRevive(true);
            }
         }

         this.checkRewardPlayers(this.getPlayers());
      }

      this._players.clear();
      this._isBattleNow = false;
      this._isActiveNow = false;
      KrateisCubeManager.getInstance().setIsActivate(false);
   }

   private void changeWatchers() {
      if (this._watcherTask != null) {
         this._watcherTask.cancel(true);
         this._watcherTask = null;
      }

      if (!this._watchers.isEmpty()) {
         for (Npc npc : this._watchers) {
            if (npc != null) {
               npc.deleteMe();
            }
         }

         this._watchers.clear();
      }

      if (!this.getWatcherLoc().isEmpty()) {
         for (Location loc : this.getWatcherLoc()) {
            if (loc != null) {
               Npc npcx = Quest.addSpawn(this._watcherRotation ? 18602 : 18601, loc, false, 0L);
               npcx.disableCoreAI(true);
               npcx.setImmobilized(true);
               this._watchers.add(npcx);
            }
         }
      }

      if (this._watcherRotation) {
         this._watcherRotation = false;
      } else {
         this._watcherRotation = true;
      }

      this._watcherTask = ThreadPool.schedule(this::changeWatchers, this.getParams().getInt("watcherRotationTime") * 1000);
   }

   private Map<Player, Integer> getParticipants() {
      Map<Player, Integer> participants = new HashMap<>();
      if (!this.getPlayers().isEmpty()) {
         for (Player player : this.getPlayers().keySet()) {
            if (player != null && !participants.containsKey(player)) {
               participants.put(player, this.getPoints(player));
            }
         }

         if (participants.isEmpty()) {
            return null;
         }
      }

      return participants.entrySet()
         .stream()
         .sorted(Entry.comparingByValue(Comparator.reverseOrder()))
         .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, var1x) -> e1, LinkedHashMap::new));
   }

   private void teleportAllPlayers() {
      if (!this.getPlayers().isEmpty()) {
         Map<Player, Integer> participants = this.getParticipants();
         if (participants == null) {
            return;
         }

         ExPVPMatchCCRecord p = new ExPVPMatchCCRecord(1, participants);

         for (Player player : this.getPlayers().keySet()) {
            if (player != null) {
               player.teleToLocation(this.getBattleLoc().get(Rnd.get(this.getBattleLoc().size())), true);
               this.addEffects(player);
               player.sendPacket(new ExPVPMatchCCMyRecord(this.getPoints(player)));
               player.sendPacket(p);
               player.setCanRevive(false);
            }
         }
      }
   }

   public void teleportToBattle(Player player) {
      if (player != null) {
         if (this.getPlayers().containsKey(player)) {
            player.teleToLocation(this.getBattleLoc().get(Rnd.get(this.getBattleLoc().size())), true);
         }
      } else if (!this.getPlayers().isEmpty()) {
         for (Player pl : this.getPlayers().keySet()) {
            if (pl != null) {
               this.teleportToWaitingRoom(pl, false);
            }
         }
      }
   }

   public void teleportToWaitingRoom(Player player, boolean restoreEffects) {
      if (this.getPlayers().containsKey(player)) {
         if (restoreEffects) {
            this.respawnEffects(player);
         }

         player.teleToLocation(this.getWaitLoc().get(Rnd.get(this.getWaitLoc().size())), true);
      }
   }

   private void changeDoorStatus(boolean active) {
      if (this._doorTask != null) {
         this._doorTask.cancel(false);
      }

      if (this._doorsRotation) {
         this._doorsRotation = false;
      } else {
         this._doorsRotation = true;
      }

      if (!this.getDoorListA().isEmpty() && !this.getDoorListB().isEmpty()) {
         List<Door> doorsA = this._doorsRotation ? this.getDoorListA() : this.getDoorListB();
         List<Door> doorsB = this._doorsRotation ? this.getDoorListB() : this.getDoorListA();
         if (active) {
            this.changeDoorsStatus(doorsA, true);
            this.changeDoorsStatus(doorsB, false);
         } else {
            this.changeDoorsStatus(doorsA, false);
            this.changeDoorsStatus(doorsB, false);
         }
      }

      this._doorTask = ThreadPool.schedule(() -> this.changeDoorStatus(active), this.getParams().getInt("doorRotationTime") * 1000);
   }

   private void changeDoorsStatus(List<Door> doors, boolean isOpen) {
      for (Door door : doors) {
         if (door != null) {
            if (isOpen) {
               door.openMe();
            } else {
               door.closeMe();
            }
         }
      }
   }

   public void addEffects(Player player) {
      player.setCurrentHp(player.getMaxHp());
      player.setCurrentCp(player.getMaxCp());
      player.setCurrentMp(player.getMaxMp());
      if (player.getSummon() != null) {
         player.getSummon().setCurrentHp(player.getSummon().getMaxHp());
         player.getSummon().setCurrentCp(player.getSummon().getMaxCp());
         player.getSummon().setCurrentMp(player.getSummon().getMaxMp());
      }

      if (!this.getBuffList().isEmpty()) {
         for (Skill skill : this.getBuffList()) {
            if (skill != null) {
               skill.applyEffects(player, player);
               if (player.getSummon() != null) {
                  skill.applyEffects(player.getSummon(), player.getSummon());
               }
            }
         }
      }

      player.broadcastUserInfo();
      if (player.getSummon() != null) {
         player.getSummon().broadcastInfo();
      }
   }

   private void removeAllEffects(Player player) {
      player.getEffectList().stopAllEffects();
      player.stopAllEffects();
      if (player.getSummon() != null) {
         player.getSummon().stopAllEffects();
      }

      player.broadcastUserInfo();
      if (player.getSummon() != null) {
         player.getSummon().broadcastInfo();
      }
   }

   private void respawnEffects(Player player) {
      this.removeAllEffects(player);
      if (player.isDead()) {
         player.doRevive(100.0);
      }

      player.broadcastStatusUpdate();
      player.broadcastUserInfo();
   }

   private void checkRewardPlayers(Map<Player, KrateiCubePlayer> players) {
      List<KrateiCubePlayer> playerList = new ArrayList<>();

      for (KrateiCubePlayer info : players.values()) {
         if (info != null) {
            playerList.add(info);
         }
      }

      Comparator<KrateiCubePlayer> statsComparator = new KrateiArena.SortPointInfo();
      playerList.sort(statsComparator);
      ExPVPMatchCCRecord p = null;
      Map<Player, Integer> participants = this.getParticipants();
      if (participants != null) {
         p = new ExPVPMatchCCRecord(0, participants);
      }

      double dif = 0.05;
      int pos = 0;

      for (KrateiCubePlayer pl : playerList) {
         if (pl != null && pl.getPlayer() != null) {
            pos++;
            if (p != null) {
               pl.getPlayer().sendPacket(p);
            }

            if (pl.getPoints() >= this.getParams().getInt("minPoints")) {
               int count = (int)(pl.getPoints() * dif * (1.0 + players.size() / pos * 0.04));
               dif -= 0.0016;
               if (count > 0) {
                  int exp = count * this.getParams().getInt("expModifier");
                  int sp = count * this.getParams().getInt("spModifier");
                  pl.getPlayer().addExpAndSp(exp, sp);
                  if (!this.getRewards().isEmpty()) {
                     for (KrateisReward reward : this.getRewards()) {
                        if (reward != null) {
                           long amount = reward.isAllowMidifier() ? count : reward.getAmount();
                           if (reward.getId() == -100) {
                              if (pl.getPlayer().getPcCafePoints() + amount > PremiumSystemConfig.PC_CAFE_MAX_POINTS) {
                                 amount = PremiumSystemConfig.PC_CAFE_MAX_POINTS - pl.getPlayer().getPcCafePoints();
                              }

                              SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
                              sm.addInt((int)amount);
                              pl.getPlayer().sendPacket(sm);
                              pl.getPlayer().setPcCafePoints((int)(pl.getPlayer().getPcCafePoints() + amount));
                              pl.getPlayer().sendPacket(new ExPCCafePointInfo(pl.getPlayer().getPcCafePoints(), (int)amount, 1));
                           } else if (reward.getId() == -200) {
                              if (pl.getPlayer().getClan() != null) {
                                 pl.getPlayer().getClan().addReputationScore((int)amount);
                              }
                           } else if (reward.getId() == -300) {
                              pl.getPlayer().setFame((int)(pl.getPlayer().getFame() + amount));
                              SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_REPUTATION_SCORE);
                              sm.addInt((int)amount);
                              pl.getPlayer().sendPacket(sm);
                              pl.getPlayer().updateUserInfo();
                           } else if (reward.getId() > 0) {
                              pl.getPlayer().addItem(ItemProcessType.REWARD, reward.getId(), amount, pl.getPlayer(), true);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      playerList.clear();
   }

   public void addPoints(Player player, boolean isPlayer) {
      KrateiCubePlayer krateiPlayer = this.getPlayers().get(player);
      if (krateiPlayer != null) {
         krateiPlayer.addPoints(krateiPlayer.getPoints() + (isPlayer ? this.getParams().getInt("pointPerPlayer") : this.getParams().getInt("pointPerMob")));
         krateiPlayer.getPlayer().sendPacket(new ExPVPMatchCCMyRecord(krateiPlayer.getPoints()));
         Map<Player, Integer> participants = this.getParticipants();
         if (participants != null) {
            ExPVPMatchCCRecord p = new ExPVPMatchCCRecord(1, participants);
            if (!this.getPlayers().isEmpty()) {
               for (Player pl : this.getPlayers().keySet()) {
                  if (pl != null) {
                     pl.sendPacket(p);
                  }
               }
            }
         }
      }
   }

   public int getPoints(Player player) {
      int kills = 0;
      KrateiCubePlayer pl = this.getPlayers().get(player);
      if (pl != null) {
         kills = pl.getPoints();
      }

      return kills;
   }

   private void timeLeftInfo() {
      if (this._timeTask != null) {
         this._timeTask.cancel(true);
         this._timeTask = null;
      }

      if (!this.getPlayers().isEmpty()) {
         ThreadPool.schedule(new KrateiArena.MessageTask(this.getPlayers(), 10, 1), 0L);
      }
   }

   public void waitTimeInfo() {
      if (!this.getPlayers().isEmpty()) {
         ThreadPool.schedule(new KrateiArena.MessageTask(this.getPlayers(), 10, 0), 0L);
      }
   }

   public void setRespawnTask(Player player) {
      if (player != null) {
         ThreadPool.schedule(new KrateiArena.RespawnTask(player, this.getParams().getInt("respawnTime")), 0L);
      }
   }

   public class MessageTask implements Runnable {
      private final Map<Player, KrateiCubePlayer> _players;
      private int _time;
      private final int _type;

      public MessageTask(Map<Player, KrateiCubePlayer> players, int time, int type) {
         Objects.requireNonNull(KrateiArena.this);
         super();
         this._players = players;
         this._time = time;
         this._type = type;
      }

      @Override
      public void run() {
         if (this._players != null && !this._players.isEmpty() && this._time > 0) {
            SystemMessage msg = null;
            switch (this._type) {
               case 0:
                  msg = new SystemMessage(SystemMessageId.THE_MATCH_WILL_START_IN_S1_SECOND_S).addInt(this._time);
                  break;
               case 1:
                  msg = new SystemMessage(SystemMessageId.THE_GAME_WILL_END_IN_S1_SECOND_S_2).addInt(this._time);
            }

            for (Player player : this._players.keySet()) {
               if (player != null && msg != null) {
                  player.sendPacket(msg);
               }
            }

            this._time--;
            if (this._time > 0) {
               ThreadPool.schedule(KrateiArena.this.new MessageTask(this._players, this._time, this._type), 1000L);
            }
         }
      }
   }

   private class RespawnTask implements Runnable {
      private final Player _player;
      private int _time;

      public RespawnTask(Player player, int time) {
         Objects.requireNonNull(KrateiArena.this);
         super();
         this._player = player;
         this._time = time;
      }

      @Override
      public void run() {
         if (this._player != null && this._time > 0) {
            this._player.sendPacket(new SystemMessage(SystemMessageId.RESURRECTION_WILL_TAKE_PLACE_IN_THE_WAITING_ROOM_AFTER_S1_SECONDS).addInt(this._time));
            this._time--;
            if (this._time > 0) {
               ThreadPool.schedule(KrateiArena.this.new RespawnTask(this._player, this._time), 1000L);
            } else {
               KrateiArena.this.respawnEffects(this._player);
               this._player.teleToLocation(KrateiArena.this.getWaitLoc().get(Rnd.get(KrateiArena.this.getWaitLoc().size())), true);
            }
         }
      }
   }

   private class SortPointInfo implements Comparator<KrateiCubePlayer> {
      private SortPointInfo() {
         Objects.requireNonNull(KrateiArena.this);
         super();
      }

      public int compare(KrateiCubePlayer o1, KrateiCubePlayer o2) {
         return Integer.compare(o2.getPoints(), o1.getPoints());
      }
   }
}
