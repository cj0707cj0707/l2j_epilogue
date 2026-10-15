package org.l2jmobius.gameserver.mechanics.sevensigns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.managers.DimensionalRiftManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;

public class DimensionalRift {
   protected byte _type;
   protected Party _party;
   protected List<Byte> _completedRooms = new ArrayList<>();
   private static final long FIVE_SECONDS = 5000L;
   protected byte currentJump = 1;
   private Timer teleporterTimer;
   private TimerTask teleporterTimerTask;
   private Timer spawnTimer;
   private TimerTask spawnTimerTask;
   private Future<?> earthQuakeTask;
   protected byte _choosenRoom = -1;
   protected Collection<Player> _deadPlayers = ConcurrentHashMap.newKeySet();
   protected Collection<Player> _revivedInWaitingRoom = ConcurrentHashMap.newKeySet();
   private boolean isBossRoom = false;

   public DimensionalRift(Party party, byte type, byte room) {
      DimensionalRiftManager.getInstance().getRoom(type, room).setPartyInside(true);
      this._type = type;
      this._party = party;
      this._choosenRoom = room;
      Location coords = this.getRoomCoord(room);
      party.setDimensionalRift(this);

      for (Player p : party.getMembers()) {
         Quest riftQuest = ScriptManager.getInstance().getQuest(635);
         if (riftQuest != null) {
            QuestState qs = p.getQuestState(riftQuest.getName());
            if (qs == null) {
               qs = riftQuest.newQuestState(p);
            }

            if (!qs.isStarted()) {
               qs.startQuest();
            }
         }

         p.teleToLocation(coords);
      }

      this.createSpawnTimer(this._choosenRoom);
      this.createTeleporterTimer(true);
   }

   public byte getType() {
      return this._type;
   }

   public byte getCurrentRoom() {
      return this._choosenRoom;
   }

   protected void createTeleporterTimer(final boolean reasonTP) {
      if (this._party != null) {
         if (this.teleporterTimerTask != null) {
            this.teleporterTimerTask.cancel();
            this.teleporterTimerTask = null;
         }

         if (this.teleporterTimer != null) {
            this.teleporterTimer.cancel();
            this.teleporterTimer = null;
         }

         if (this.earthQuakeTask != null) {
            this.earthQuakeTask.cancel(false);
            this.earthQuakeTask = null;
         }

         this.teleporterTimer = new Timer();
         this.teleporterTimerTask = new TimerTask() {
            {
               Objects.requireNonNull(DimensionalRift.this);
            }

            @Override
            public void run() {
               if (DimensionalRift.this._choosenRoom > -1) {
                  DimensionalRiftManager.getInstance().getRoom(DimensionalRift.this._type, DimensionalRift.this._choosenRoom).unspawn().setPartyInside(false);
               }

               if (reasonTP
                  && DimensionalRift.this.currentJump < DimensionalRift.this.getMaxJumps()
                  && DimensionalRift.this._party.getMemberCount() > DimensionalRift.this._deadPlayers.size()) {
                  DimensionalRift.this.currentJump++;
                  DimensionalRift.this._completedRooms.add(DimensionalRift.this._choosenRoom);
                  DimensionalRift.this._choosenRoom = -1;

                  for (Player p : DimensionalRift.this._party.getMembers()) {
                     if (!DimensionalRift.this._revivedInWaitingRoom.contains(p)) {
                        DimensionalRift.this.teleportToNextRoom(p);
                     }
                  }

                  DimensionalRift.this.createTeleporterTimer(true);
                  DimensionalRift.this.createSpawnTimer(DimensionalRift.this._choosenRoom);
               } else {
                  for (Player px : DimensionalRift.this._party.getMembers()) {
                     if (!DimensionalRift.this._revivedInWaitingRoom.contains(px)) {
                        DimensionalRift.this.teleportToWaitingRoom(px);
                     }
                  }

                  DimensionalRift.this.killRift();
                  this.cancel();
               }
            }
         };
         if (reasonTP) {
            long jumpTime = this.calcTimeToNextJump();
            this.teleporterTimer.schedule(this.teleporterTimerTask, jumpTime);
            this.earthQuakeTask = ThreadPool.schedule(() -> {
               for (Player p : this._party.getMembers()) {
                  if (!this._revivedInWaitingRoom.contains(p)) {
                     p.sendPacket(new Earthquake(p.getX(), p.getY(), p.getZ(), 65, 9));
                  }
               }
            }, jumpTime - 7000L);
         } else {
            this.teleporterTimer.schedule(this.teleporterTimerTask, 5000L);
         }
      }
   }

   public void createSpawnTimer(final byte room) {
      if (this.spawnTimerTask != null) {
         this.spawnTimerTask.cancel();
         this.spawnTimerTask = null;
      }

      if (this.spawnTimer != null) {
         this.spawnTimer.cancel();
         this.spawnTimer = null;
      }

      this.spawnTimer = new Timer();
      this.spawnTimerTask = new TimerTask() {
         {
            Objects.requireNonNull(DimensionalRift.this);
         }

         @Override
         public void run() {
            DimensionalRiftManager.getInstance().getRoom(DimensionalRift.this._type, room).spawn();
         }
      };
      this.spawnTimer.schedule(this.spawnTimerTask, GeneralConfig.RIFT_SPAWN_DELAY);
   }

   public void partyMemberInvited() {
      this.createTeleporterTimer(false);
   }

   public void partyMemberExited(Player player) {
      this._deadPlayers.remove(player);
      this._revivedInWaitingRoom.remove(player);
      if (this._party.getMemberCount() < GeneralConfig.RIFT_MIN_PARTY_SIZE || this._party.getMemberCount() == 1) {
         for (Player p : this._party.getMembers()) {
            this.teleportToWaitingRoom(p);
         }

         this.killRift();
      }
   }

   public void manualTeleport(Player player, Npc npc) {
      Party party = player.getParty();
      if (party != null && party.isInDimensionalRift()) {
         if (player.getObjectId() != party.getLeaderObjectId()) {
            DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
         } else if (this.currentJump >= this.getMaxJumps()) {
            DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/AlreadyTeleported.htm", npc);
         } else {
            this.currentJump++;
            DimensionalRiftManager.getInstance().getRoom(this._type, this._choosenRoom).unspawn().setPartyInside(false);
            this._completedRooms.add(this._choosenRoom);
            this._choosenRoom = -1;

            for (Player p : this._party.getMembers()) {
               this.teleportToNextRoom(p);
            }

            DimensionalRiftManager.getInstance().getRoom(this._type, this._choosenRoom).setPartyInside(true);
            this.createSpawnTimer(this._choosenRoom);
            this.createTeleporterTimer(true);
         }
      }
   }

   public void manualExitRift(Player player, Npc npc) {
      Party party = player.getParty();
      if (party != null && party.isInDimensionalRift()) {
         if (player.getObjectId() != party.getLeaderObjectId()) {
            DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
         } else {
            for (Player p : party.getMembers()) {
               this.teleportToWaitingRoom(p);
            }

            this.killRift();
         }
      }
   }

   protected void teleportToNextRoom(Player player) {
      if (this._choosenRoom == -1) {
         do {
            List<Byte> emptyRooms = DimensionalRiftManager.getInstance().getFreeRooms(this._type);
            emptyRooms.removeAll(this._completedRooms);
            if (emptyRooms.isEmpty()) {
               emptyRooms = DimensionalRiftManager.getInstance().getFreeRooms(this._type);
            }

            this._choosenRoom = emptyRooms.get(Rnd.get(1, emptyRooms.size()) - 1);
         } while (DimensionalRiftManager.getInstance().getRoom(this._type, this._choosenRoom).isPartyInside());
      }

      DimensionalRiftManager.getInstance().getRoom(this._type, this._choosenRoom).setPartyInside(true);
      this.checkBossRoom(this._choosenRoom);
      player.teleToLocation(this.getRoomCoord(this._choosenRoom));
   }

   protected void teleportToWaitingRoom(Player player) {
      DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
      Quest riftQuest = ScriptManager.getInstance().getQuest(635);
      if (riftQuest != null) {
         QuestState qs = player.getQuestState(riftQuest.getName());
         if (qs != null && qs.isCond(1)) {
            qs.exitQuest(true, true);
         }
      }
   }

   public void killRift() {
      this._completedRooms.clear();
      if (this._party != null) {
         this._party.setDimensionalRift(null);
      }

      this._party = null;
      this._revivedInWaitingRoom = null;
      this._deadPlayers = null;
      if (this.earthQuakeTask != null) {
         this.earthQuakeTask.cancel(false);
         this.earthQuakeTask = null;
      }

      DimensionalRiftManager.getInstance().getRoom(this._type, this._choosenRoom).unspawn().setPartyInside(false);
      DimensionalRiftManager.getInstance().killRift(this);
   }

   public Timer getTeleportTimer() {
      return this.teleporterTimer;
   }

   public TimerTask getTeleportTimerTask() {
      return this.teleporterTimerTask;
   }

   public Timer getSpawnTimer() {
      return this.spawnTimer;
   }

   public TimerTask getSpawnTimerTask() {
      return this.spawnTimerTask;
   }

   public void setTeleportTimer(Timer t) {
      this.teleporterTimer = t;
   }

   public void setTeleportTimerTask(TimerTask tt) {
      this.teleporterTimerTask = tt;
   }

   public void setSpawnTimer(Timer t) {
      this.spawnTimer = t;
   }

   public void setSpawnTimerTask(TimerTask st) {
      this.spawnTimerTask = st;
   }

   private long calcTimeToNextJump() {
      int time = Rnd.get(GeneralConfig.RIFT_AUTO_JUMPS_TIME_MIN, GeneralConfig.RIFT_AUTO_JUMPS_TIME_MAX) * 1000;
      return this.isBossRoom ? (long)(time * GeneralConfig.RIFT_BOSS_ROOM_TIME_MUTIPLY) : time;
   }

   public void memberDead(Player player) {
      if (!this._deadPlayers.contains(player)) {
         this._deadPlayers.add(player);
      }
   }

   public void memberRessurected(Player player) {
      this._deadPlayers.remove(player);
   }

   public void usedTeleport(Player player) {
      if (!this._revivedInWaitingRoom.contains(player)) {
         this._revivedInWaitingRoom.add(player);
      }

      if (!this._deadPlayers.contains(player)) {
         this._deadPlayers.add(player);
      }

      if (this._party.getMemberCount() - this._revivedInWaitingRoom.size() < GeneralConfig.RIFT_MIN_PARTY_SIZE) {
         for (Player p : this._party.getMembers()) {
            if (p != null && !this._revivedInWaitingRoom.contains(p)) {
               this.teleportToWaitingRoom(p);
            }
         }

         this.killRift();
      }
   }

   public Collection<Player> getDeadMemberList() {
      return this._deadPlayers;
   }

   public Collection<Player> getRevivedAtWaitingRoom() {
      return this._revivedInWaitingRoom;
   }

   public void checkBossRoom(byte room) {
      this.isBossRoom = DimensionalRiftManager.getInstance().getRoom(this._type, room).isBossRoom();
   }

   public Location getRoomCoord(byte room) {
      return DimensionalRiftManager.getInstance().getRoom(this._type, room).getTeleportCoorinates();
   }

   public byte getMaxJumps() {
      return GeneralConfig.RIFT_MAX_JUMPS <= 8 && GeneralConfig.RIFT_MAX_JUMPS >= 1 ? (byte)GeneralConfig.RIFT_MAX_JUMPS : 4;
   }
}
