package org.l2jmobius.gameserver.mechanics.siege.clanhalls;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.ConquerableHallSiegeConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.siege.Siegable;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClanType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public abstract class ClanHallSiegeEngine extends Script implements Siegable {
   private static final String SQL_LOAD_ATTACKERS = "SELECT attacker_id FROM clanhall_siege_attackers WHERE clanhall_id = ?";
   private static final String SQL_SAVE_ATTACKERS = "INSERT INTO clanhall_siege_attackers VALUES (?,?)";
   private static final String SQL_LOAD_GUARDS = "SELECT * FROM clanhall_siege_guards WHERE clanHallId = ?";
   public static final int FORTRESS_RESSISTANCE = 21;
   public static final int DEVASTATED_CASTLE = 34;
   public static final int BANDIT_STRONGHOLD = 35;
   public static final int RAINBOW_SPRINGS = 62;
   public static final int BEAST_FARM = 63;
   public static final int FORTRESS_OF_DEAD = 64;
   protected final Logger LOGGER;
   private final Map<Integer, SiegeClan> _attackers = new ConcurrentHashMap<>();
   private Collection<Spawn> _guards;
   public SiegableHall _hall;
   public ScheduledFuture<?> _siegeTask;
   public boolean _missionAccomplished = false;

   public ClanHallSiegeEngine(int hallId) {
      this.LOGGER = Logger.getLogger(this.getClass().getName());
      this._hall = CHSiegeManager.getInstance().getSiegableHall(hallId);
      this._hall.setSiege(this);
      this._siegeTask = ThreadPool.schedule(new ClanHallSiegeEngine.PrepareOwner(), this._hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000L);
      this.LOGGER.config(this._hall.getName() + " siege scheduled for " + this._hall.getSiegeDate().getTime() + ".");
      this.loadAttackers();
   }

   public void loadAttackers() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT attacker_id FROM clanhall_siege_attackers WHERE clanhall_id = ?");
      ) {
         ps.setInt(1, this._hall.getId());

         try (ResultSet rset = ps.executeQuery()) {
            while (rset.next()) {
               int id = rset.getInt("attacker_id");
               this._attackers.put(id, new SiegeClan(id, SiegeClanType.ATTACKER));
            }
         }
      } catch (Exception var12) {
         this.LOGGER.warning(this.getName() + ": Could not load siege attackers!");
      }
   }

   public void saveAttackers() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM clanhall_siege_attackers WHERE clanhall_id = ?");
      ) {
         ps.setInt(1, this._hall.getId());
         ps.execute();
         if (!this._attackers.isEmpty()) {
            try (PreparedStatement insert = con.prepareStatement("INSERT INTO clanhall_siege_attackers VALUES (?,?)")) {
               for (SiegeClan clan : this._attackers.values()) {
                  insert.setInt(1, this._hall.getId());
                  insert.setInt(2, clan.getClanId());
                  insert.execute();
                  insert.clearParameters();
               }
            }
         }

         this.LOGGER.config(this.getName() + ": Successfully saved attackers to database.");
      } catch (Exception var12) {
         this.LOGGER.warning(this.getName() + ": Couldn't save attacker list!");
      }
   }

   public void loadGuards() {
      if (this._guards != null) {
         this._guards.forEach(Spawn::startRespawn);
      } else {
         this._guards = new ArrayList<>();

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM clanhall_siege_guards WHERE clanHallId = ?");
         ) {
            ps.setInt(1, this._hall.getId());
            ResultSet rset = ps.executeQuery();

            while (rset.next()) {
               Spawn spawn = new Spawn(rset.getInt("npcId"));
               spawn.setXYZ(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
               spawn.setHeading(rset.getInt("heading"));
               spawn.setRespawnDelay(rset.getInt("respawnDelay"));
               spawn.setAmount(1);
               this._guards.add(spawn);
            }
         } catch (Exception var9) {
            this.LOGGER.warning(this.getName() + ": Could not load siege guards!");
         }
      }
   }

   private void spawnSiegeGuards() {
      for (Spawn guard : this._guards) {
         guard.init();
         if (guard.getRespawnDelay() == 0) {
            guard.stopRespawn();
         }
      }
   }

   private void unSpawnSiegeGuards() {
      if (this._guards != null) {
         for (Spawn guard : this._guards) {
            guard.stopRespawn();
            if (guard.getLastSpawn() != null) {
               guard.getLastSpawn().deleteMe();
            }
         }
      }
   }

   @Override
   public Set<Npc> getFlag(Clan clan) {
      Set<Npc> result = null;
      SiegeClan sClan = this.getAttackerClan(clan);
      if (sClan != null) {
         result = sClan.getFlag();
      }

      return result;
   }

   public Map<Integer, SiegeClan> getAttackers() {
      return this._attackers;
   }

   @Override
   public boolean checkIsAttacker(Clan clan) {
      return clan == null ? false : this._attackers.containsKey(clan.getId());
   }

   @Override
   public boolean checkIsDefender(Clan clan) {
      return false;
   }

   @Override
   public SiegeClan getAttackerClan(int clanId) {
      return this._attackers.get(clanId);
   }

   @Override
   public SiegeClan getAttackerClan(Clan clan) {
      return this.getAttackerClan(clan.getId());
   }

   @Override
   public Collection<SiegeClan> getAttackerClans() {
      return Collections.unmodifiableCollection(this._attackers.values());
   }

   @Override
   public List<Player> getAttackersInZone() {
      List<Player> attackers = new ArrayList<>();

      for (Player pc : this._hall.getSiegeZone().getPlayersInside()) {
         Clan clan = pc.getClan();
         if (clan != null && this._attackers.containsKey(clan.getId())) {
            attackers.add(pc);
         }
      }

      return attackers;
   }

   @Override
   public SiegeClan getDefenderClan(int clanId) {
      return null;
   }

   @Override
   public SiegeClan getDefenderClan(Clan clan) {
      return null;
   }

   public List<SiegeClan> getDefenderClans() {
      return null;
   }

   public void prepareOwner() {
      if (this._hall.getOwnerId() > 0) {
         SiegeClan clan = new SiegeClan(this._hall.getOwnerId(), SiegeClanType.ATTACKER);
         this._attackers.put(clan.getClanId(), new SiegeClan(clan.getClanId(), SiegeClanType.ATTACKER));
      }

      this._hall.free();
      this._hall.banishForeigners();
      SystemMessage msg = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
      msg.addString(this.getName());
      World.broadcastToAllOnlinePlayers(msg);
      this._hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
      this._siegeTask = ThreadPool.schedule(new ClanHallSiegeEngine.SiegeStarts(), 3600000L);
   }

   @Override
   public void startSiege() {
      if (this._attackers.isEmpty() && this._hall.getId() != 21) {
         this.onSiegeEnds();
         this._attackers.clear();
         this._hall.updateNextSiege();
         this._siegeTask = ThreadPool.schedule(new ClanHallSiegeEngine.PrepareOwner(), this._hall.getSiegeDate().getTimeInMillis());
         this._hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
         sm.addString(this._hall.getName());
         World.broadcastToAllOnlinePlayers(sm);
      } else {
         this._hall.spawnDoor();
         this.loadGuards();
         this.spawnSiegeGuards();
         this._hall.updateSiegeZone(true);
         byte state = 1;

         for (SiegeClan sClan : this._attackers.values()) {
            Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
            if (clan != null) {
               for (Player pc : clan.getOnlineMembers(0)) {
                  pc.setSiegeState((byte)1);
                  pc.broadcastUserInfo();
                  pc.setInHideoutSiege(true);
               }
            }
         }

         this._hall.updateSiegeStatus(SiegeStatus.RUNNING);
         this.onSiegeStarts();
         this._siegeTask = ThreadPool.schedule(new ClanHallSiegeEngine.SiegeEnds(), this._hall.getSiegeLength());
      }
   }

   @Override
   public void endSiege() {
      SystemMessage end = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_FINISHED);
      end.addString(this._hall.getName());
      World.broadcastToAllOnlinePlayers(end);
      Clan winner = this.getWinner();
      SystemMessage finalMsg = null;
      if (this._missionAccomplished && winner != null) {
         this._hall.setOwner(winner);
         winner.setHideoutId(this._hall.getId());
         finalMsg = new SystemMessage(SystemMessageId.CLAN_S1_IS_VICTORIOUS_OVER_S2_S_CASTLE_SIEGE);
         finalMsg.addString(winner.getName());
      } else {
         finalMsg = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW);
      }

      finalMsg.addString(this._hall.getName());
      World.broadcastToAllOnlinePlayers(finalMsg);
      this._missionAccomplished = false;
      this._hall.updateSiegeZone(false);
      this._hall.updateNextSiege();
      this._hall.spawnDoor(false);
      this._hall.banishForeigners();
      byte state = 0;

      for (SiegeClan sClan : this._attackers.values()) {
         Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
         if (clan != null) {
            for (Player player : clan.getOnlineMembers(0)) {
               player.setSiegeState((byte)0);
               player.broadcastUserInfo();
               player.setInHideoutSiege(false);
            }
         }
      }

      for (Creature chr : this._hall.getSiegeZone().getCharactersInside()) {
         if (chr != null && chr.isPlayer()) {
            chr.asPlayer().startPvPFlag();
         }
      }

      this._attackers.clear();
      this.onSiegeEnds();
      this._siegeTask = ThreadPool.schedule(new ClanHallSiegeEngine.PrepareOwner(), this._hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000L);
      this.LOGGER.config("Siege of " + this._hall.getName() + " scheduled for " + this._hall.getSiegeDate().getTime() + ".");
      this._hall.updateSiegeStatus(SiegeStatus.REGISTERING);
      this.unSpawnSiegeGuards();
   }

   @Override
   public void updateSiege() {
      this.cancelSiegeTask();
      this._siegeTask = ThreadPool.schedule(new ClanHallSiegeEngine.PrepareOwner(), this._hall.getNextSiegeTime() - 3600000L);
      this.LOGGER.config(this._hall.getName() + " siege scheduled for " + this._hall.getSiegeDate().getTime() + ".");
   }

   public void cancelSiegeTask() {
      if (this._siegeTask != null) {
         this._siegeTask.cancel(false);
      }
   }

   @Override
   public Calendar getSiegeDate() {
      return this._hall.getSiegeDate();
   }

   @Override
   public boolean giveFame() {
      return ConquerableHallSiegeConfig.CHS_ENABLE_FAME;
   }

   @Override
   public int getFameAmount() {
      return ConquerableHallSiegeConfig.CHS_FAME_AMOUNT;
   }

   @Override
   public int getFameFrequency() {
      return ConquerableHallSiegeConfig.CHS_FAME_FREQUENCY;
   }

   public void broadcastNpcSay(Npc npc, ChatType type, String message) {
      NpcSay npcSay = new NpcSay(npc.getObjectId(), type, npc.getId(), message);
      int sourceRegion = MapRegionData.getInstance().getMapRegionLocId(npc);

      for (Player pc : World.getPlayers()) {
         if (pc != null && MapRegionData.getInstance().getMapRegionLocId(pc) == sourceRegion) {
            pc.sendPacket(npcSay);
         }
      }
   }

   public Location getInnerSpawnLoc(Player player) {
      return null;
   }

   public boolean canPlantFlag() {
      return true;
   }

   public boolean doorIsAutoAttackable() {
      return true;
   }

   public void onSiegeStarts() {
   }

   public void onSiegeEnds() {
   }

   public abstract Clan getWinner();

   public class PrepareOwner implements Runnable {
      public PrepareOwner() {
         Objects.requireNonNull(ClanHallSiegeEngine.this);
         super();
      }

      @Override
      public void run() {
         ClanHallSiegeEngine.this.prepareOwner();
      }
   }

   public class SiegeEnds implements Runnable {
      public SiegeEnds() {
         Objects.requireNonNull(ClanHallSiegeEngine.this);
         super();
      }

      @Override
      public void run() {
         ClanHallSiegeEngine.this.endSiege();
      }
   }

   public class SiegeStarts implements Runnable {
      public SiegeStarts() {
         Objects.requireNonNull(ClanHallSiegeEngine.this);
         super();
      }

      @Override
      public void run() {
         ClanHallSiegeEngine.this.startSiege();
      }
   }
}
