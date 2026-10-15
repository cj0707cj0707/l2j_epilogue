package org.l2jmobius.gameserver.mechanics.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.FortCommander;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeGuardManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.fort.OnFortSiegeFinish;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.fort.OnFortSiegeStart;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class FortSiege implements Siegable {
   protected static final Logger LOGGER = Logger.getLogger(FortSiege.class.getName());
   private static final String DELETE_FORT_SIEGECLANS_BY_CLAN_ID = "DELETE FROM fortsiege_clans WHERE fort_id = ? AND clan_id = ?";
   private static final String DELETE_FORT_SIEGECLANS = "DELETE FROM fortsiege_clans WHERE fort_id = ?";
   private final Collection<SiegeClan> _attackerClans = ConcurrentHashMap.newKeySet();
   protected Collection<Spawn> _commanders = ConcurrentHashMap.newKeySet();
   protected final Fort _fort;
   boolean _isInProgress = false;
   private FortSiegeGuardManager _siegeGuardManager;
   ScheduledFuture<?> _siegeEnd = null;
   ScheduledFuture<?> _siegeRestore = null;
   ScheduledFuture<?> _siegeStartTask = null;

   public FortSiege(Fort fort) {
      this._fort = fort;
      this.checkAutoTask();
      FortSiegeManager.getInstance().addSiege(this);
   }

   @Override
   public void endSiege() {
      if (this._isInProgress) {
         this._isInProgress = false;
         this.removeFlags();
         this.unSpawnFlags();
         this.updatePlayerSiegeStateFlags(true);
         int ownerId = this._fort.getOwnerClan() != null ? this._fort.getOwnerClan().getId() : -1;
         this._fort.getZone().banishForeigners(ownerId);
         this._fort.getZone().setActive(false);
         this._fort.getZone().updateZoneStatusForCharactersInside();
         this._fort.getZone().setSiegeInstance(null);
         this.saveFortSiege();
         this.clearSiegeClan();
         this.removeCommanders();
         this._fort.spawnNpcCommanders();
         this.getSiegeGuardManager().unspawnSiegeGuard();
         this._fort.resetDoors();
         ThreadPool.schedule(new FortSiege.ScheduleSuspiciousMerchantSpawn(), FortSiegeManager.getInstance().getSuspiciousMerchantRespawnDelay() * 60 * 1000);
         this.setSiegeDateTime(true);
         if (this._siegeEnd != null) {
            this._siegeEnd.cancel(true);
            this._siegeEnd = null;
         }

         if (this._siegeRestore != null) {
            this._siegeRestore.cancel(true);
            this._siegeRestore = null;
         }

         if (this._fort.getOwnerClan() != null && this._fort.getFlagPole().getMeshIndex() == 0) {
            this._fort.setVisibleFlag(true);
         }

         LOGGER.info("Siege of " + this._fort.getName() + " fort finished.");
         if (EventDispatcher.getInstance().hasListener(EventType.ON_FORT_SIEGE_FINISH, this.getFort())) {
            EventDispatcher.getInstance().notifyEventAsync(new OnFortSiegeFinish(this), this.getFort());
         }
      }
   }

   @Override
   public void startSiege() {
      if (!this._isInProgress) {
         if (this._siegeStartTask != null) {
            this._siegeStartTask.cancel(true);
            this._fort.despawnSuspiciousMerchant();
         }

         this._siegeStartTask = null;
         if (!this._attackerClans.isEmpty()) {
            this._isInProgress = true;
            this.loadSiegeClan();
            this.updatePlayerSiegeStateFlags(false);
            this.teleportPlayer(FortTeleportWhoType.Attacker, TeleportWhereType.TOWN);
            this._fort.despawnNpcCommanders();
            this.spawnCommanders();
            this._fort.resetDoors();
            this.spawnSiegeGuard();
            this._fort.setVisibleFlag(false);
            this._fort.getZone().setSiegeInstance(this);
            this._fort.getZone().setActive(true);
            this._fort.getZone().updateZoneStatusForCharactersInside();
            this._siegeEnd = ThreadPool.schedule(new FortSiege.ScheduleEndSiegeTask(), FortSiegeManager.getInstance().getSiegeLength() * 60 * 1000);
            SystemMessage sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_S1_HAS_BEGUN);
            sm.addCastleId(this._fort.getResidenceId());
            this.announceToPlayer(sm);
            this.saveFortSiege();
            LOGGER.info("Siege of " + this._fort.getName() + " fort started.");
            if (EventDispatcher.getInstance().hasListener(EventType.ON_FORT_SIEGE_START, this.getFort())) {
               EventDispatcher.getInstance().notifyEventAsync(new OnFortSiegeStart(this), this.getFort());
            }
         }
      }
   }

   public void announceToPlayer(SystemMessage sm) {
      for (SiegeClan siegeclan : this._attackerClans) {
         Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

         for (Player member : clan.getOnlineMembers(0)) {
            member.sendPacket(sm);
         }
      }

      if (this._fort.getOwnerClan() != null) {
         Clan clan = ClanTable.getInstance().getClan(this.getFort().getOwnerClan().getId());

         for (Player member : clan.getOnlineMembers(0)) {
            if (member != null) {
               member.sendPacket(sm);
            }
         }
      }
   }

   public void announceToPlayer(SystemMessage sm, String s) {
      sm.addString(s);
      this.announceToPlayer(sm);
   }

   public void updatePlayerSiegeStateFlags(boolean clear) {
      for (SiegeClan siegeclan : this._attackerClans) {
         Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

         for (Player member : clan.getOnlineMembers(0)) {
            if (clear) {
               member.setSiegeState((byte)0);
               member.setSiegeSide(0);
               member.setInSiege(false);
               member.stopFameTask();
            } else {
               member.setSiegeState((byte)1);
               member.setSiegeSide(this._fort.getResidenceId());
               if (this.checkIfInZone(member)) {
                  member.setInSiege(true);
                  member.startFameTask(PlayerConfig.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
               }
            }

            member.broadcastUserInfo();
         }
      }

      if (this._fort.getOwnerClan() != null) {
         Clan clan = ClanTable.getInstance().getClan(this.getFort().getOwnerClan().getId());

         for (Player member : clan.getOnlineMembers(0)) {
            if (member != null) {
               if (clear) {
                  member.setSiegeState((byte)0);
                  member.setSiegeSide(0);
                  member.setInSiege(false);
                  member.stopFameTask();
               } else {
                  member.setSiegeState((byte)2);
                  member.setSiegeSide(this._fort.getResidenceId());
                  if (this.checkIfInZone(member)) {
                     member.setInSiege(true);
                     member.startFameTask(PlayerConfig.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
                  }
               }

               member.broadcastUserInfo();
            }
         }
      }
   }

   public boolean checkIfInZone(WorldObject object) {
      return this.checkIfInZone(object.getX(), object.getY(), object.getZ());
   }

   public boolean checkIfInZone(int x, int y, int z) {
      return this._isInProgress && this._fort.checkIfInZone(x, y, z);
   }

   @Override
   public boolean checkIsAttacker(Clan clan) {
      return this.getAttackerClan(clan) != null;
   }

   @Override
   public boolean checkIsDefender(Clan clan) {
      return clan != null && this._fort.getOwnerClan() == clan;
   }

   public void clearSiegeClan() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=?");
      ) {
         ps.setInt(1, this._fort.getResidenceId());
         ps.execute();
         if (this._fort.getOwnerClan() != null) {
            try (PreparedStatement delete = con.prepareStatement("DELETE FROM fortsiege_clans WHERE clan_id=?")) {
               delete.setInt(1, this._fort.getOwnerClan().getId());
               delete.execute();
            }
         }

         this._attackerClans.clear();
         if (this._isInProgress) {
            this.endSiege();
         }

         if (this._siegeStartTask != null) {
            this._siegeStartTask.cancel(true);
            this._siegeStartTask = null;
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: clearSiegeClan(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void clearSiegeDate() {
      this._fort.getSiegeDate().setTimeInMillis(0L);
   }

   @Override
   public List<Player> getAttackersInZone() {
      List<Player> players = new ArrayList<>();

      for (SiegeClan siegeclan : this._attackerClans) {
         for (Player player : ClanTable.getInstance().getClan(siegeclan.getClanId()).getOnlineMembers(0)) {
            if (player.isInSiege()) {
               players.add(player);
            }
         }
      }

      return players;
   }

   public List<Player> getPlayersInZone() {
      return this._fort.getZone().getPlayersInside();
   }

   public List<Player> getOwnersInZone() {
      List<Player> players = new ArrayList<>();
      if (this._fort.getOwnerClan() != null) {
         Clan clan = ClanTable.getInstance().getClan(this.getFort().getOwnerClan().getId());
         if (clan != this._fort.getOwnerClan()) {
            return null;
         }

         for (Player player : clan.getOnlineMembers(0)) {
            if (player.isInSiege()) {
               players.add(player);
            }
         }
      }

      return players;
   }

   public void killedCommander(FortCommander instance) {
      if (!this._commanders.isEmpty() && this._fort != null) {
         Spawn spawn = instance.getSpawn();
         if (spawn != null) {
            for (FortSpawnHolder spawn2 : FortSiegeManager.getInstance().getCommanderSpawnList(this.getFort().getResidenceId())) {
               if (spawn2.getId() == spawn.getId()) {
                  String npcString = null;
                  switch (spawn2.getMessageId()) {
                     case 1:
                        npcString = "You may have broken our arrows, but you will never break our will! Archers, retreat!";
                        break;
                     case 2:
                        npcString = "Aiieeee! Command Center! This is guard unit! We need backup right away!";
                        break;
                     case 3:
                        npcString = "At last! The Magic Field that protects the fortress has weakened! Volunteers, stand back!";
                        break;
                     case 4:
                        npcString = "I feel so much grief that I can't even take care of myself. There isn't any reason for me to stay here any longer.";
                  }

                  if (npcString != null) {
                     instance.broadcastPacket(new NpcSay(instance.getObjectId(), ChatType.NPC_SHOUT, instance.getId(), npcString));
                  }
               }
            }

            this._commanders.remove(spawn);
            if (this._commanders.isEmpty()) {
               this.spawnFlag(this._fort.getResidenceId());
               if (this._siegeRestore != null) {
                  this._siegeRestore.cancel(true);
               }

               for (Door door : this._fort.getDoors()) {
                  if (!door.isShowHp()) {
                     door.openMe();
                  }
               }

               this._fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.ALL_BARRACKS_ARE_OCCUPIED));
            } else {
               this._fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.THE_BARRACKS_HAVE_BEEN_SEIZED));
               if (this._siegeRestore == null) {
                  this._siegeRestore = ThreadPool.schedule(
                     new FortSiege.ScheduleSiegeRestore(), FortSiegeManager.getInstance().getCountDownLength() * 60 * 1000
                  );
               }
            }
         } else {
            LOGGER.warning(
               "FortSiege.killedCommander(): killed commander, but commander not registered for fortress. NpcId: "
                  + instance.getId()
                  + " FortId: "
                  + this._fort.getResidenceId()
            );
         }
      }
   }

   public void killedFlag(Npc flag) {
      if (flag != null) {
         for (SiegeClan clan : this._attackerClans) {
            if (clan.removeFlag(flag)) {
               return;
            }
         }
      }
   }

   public int addAttacker(Player player, boolean checkConditions) {
      Clan clan = player.getClan();
      if (clan == null) {
         return 0;
      } else {
         if (checkConditions) {
            if (this._fort.getSiege().getAttackerClans().isEmpty() && player.getInventory().getAdena() < 250000L) {
               return 1;
            }

            if (System.currentTimeMillis() < TerritoryWarManager.getInstance().getTWStartTimeInMillis()
               && TerritoryWarManager.getInstance().isRegistrationOver()) {
               return 2;
            }

            if (System.currentTimeMillis() > TerritoryWarManager.getInstance().getTWStartTimeInMillis() && TerritoryWarManager.getInstance().isTWChannelOpen()) {
               return 2;
            }

            for (Fort fort : FortManager.getInstance().getForts()) {
               if (fort.getSiege().getAttackerClan(player.getClanId()) != null) {
                  return 3;
               }

               if (fort.getOwnerClan() == clan && (fort.getSiege().isInProgress() || fort.getSiege()._siegeStartTask != null)) {
                  return 3;
               }
            }
         }

         this.saveSiegeClan(clan);
         if (this._attackerClans.size() == 1) {
            if (checkConditions) {
               player.reduceAdena(ItemProcessType.FEE, 250000L, null, true);
            }

            this.startAutoTask(true);
         }

         return 4;
      }
   }

   public void removeAttacker(Clan clan) {
      if (clan != null
         && clan.getFortId() != this.getFort().getResidenceId()
         && FortSiegeManager.getInstance().checkIsRegistered(clan, this.getFort().getResidenceId())) {
         this.removeSiegeClan(clan.getId());
      }
   }

   private void removeSiegeClan(int clanId) {
      String query = clanId != 0 ? "DELETE FROM fortsiege_clans WHERE fort_id = ? AND clan_id = ?" : "DELETE FROM fortsiege_clans WHERE fort_id = ?";

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(query);
      ) {
         ps.setInt(1, this._fort.getResidenceId());
         if (clanId != 0) {
            ps.setInt(2, clanId);
         }

         ps.execute();
         this.loadSiegeClan();
         if (this._attackerClans.isEmpty()) {
            if (this._isInProgress) {
               this.endSiege();
            } else {
               this.saveFortSiege();
            }

            if (this._siegeStartTask != null) {
               this._siegeStartTask.cancel(true);
               this._siegeStartTask = null;
            }
         }
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, "Exception on removeSiegeClan: " + var11.getMessage(), (Throwable)var11);
      }
   }

   public void checkAutoTask() {
      if (this._siegeStartTask == null) {
         long delay = this.getFort().getSiegeDate().getTimeInMillis() - System.currentTimeMillis();
         if (delay < 0L) {
            this.saveFortSiege();
            this.clearSiegeClan();
            ThreadPool.execute(new FortSiege.ScheduleSuspiciousMerchantSpawn());
         } else {
            this.loadSiegeClan();
            if (this._attackerClans.isEmpty()) {
               ThreadPool.schedule(new FortSiege.ScheduleSuspiciousMerchantSpawn(), delay);
            } else {
               if (delay > 3600000L) {
                  ThreadPool.execute(new FortSiege.ScheduleSuspiciousMerchantSpawn());
                  this._siegeStartTask = ThreadPool.schedule(new FortSiege.ScheduleStartSiegeTask(3600), delay - 3600000L);
               }

               if (delay > 600000L) {
                  ThreadPool.execute(new FortSiege.ScheduleSuspiciousMerchantSpawn());
                  this._siegeStartTask = ThreadPool.schedule(new FortSiege.ScheduleStartSiegeTask(600), delay - 600000L);
               } else if (delay > 300000L) {
                  this._siegeStartTask = ThreadPool.schedule(new FortSiege.ScheduleStartSiegeTask(300), delay - 300000L);
               } else if (delay > 60000L) {
                  this._siegeStartTask = ThreadPool.schedule(new FortSiege.ScheduleStartSiegeTask(60), delay - 60000L);
               } else {
                  this._siegeStartTask = ThreadPool.schedule(new FortSiege.ScheduleStartSiegeTask(60), 0L);
               }

               LOGGER.info("Siege of " + this._fort.getName() + " fort: " + this._fort.getSiegeDate().getTime());
            }
         }
      }
   }

   public void startAutoTask(boolean setTime) {
      if (this._siegeStartTask == null) {
         if (setTime) {
            this.setSiegeDateTime(false);
         }

         if (this._fort.getOwnerClan() != null) {
            this._fort.getOwnerClan().broadcastToOnlineMembers(new SystemMessage(SystemMessageId.A_FORTRESS_IS_UNDER_ATTACK));
         }

         this._siegeStartTask = ThreadPool.schedule(new FortSiege.ScheduleStartSiegeTask(3600), 0L);
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public void teleportPlayer(FortTeleportWhoType teleportWho, TeleportWhereType teleportWhere) {
      <unknown> var3;
      for (Player player : var3_1) {
         if (!player.isGM() && !player.isJailed()) {
            player.teleToLocation(teleportWhere);
         }
      }
   }

   private void addAttacker(int clanId) {
      this._attackerClans.add(new SiegeClan(clanId, SiegeClanType.ATTACKER));
   }

   public boolean checkIfAlreadyRegisteredForSameDay(Clan clan) {
      for (FortSiege siege : FortSiegeManager.getInstance().getSieges()) {
         if (siege != this && siege.getSiegeDate().get(7) == this.getSiegeDate().get(7)) {
            if (siege.checkIsAttacker(clan)) {
               return true;
            }

            if (siege.checkIsDefender(clan)) {
               return true;
            }
         }
      }

      return false;
   }

   private void setSiegeDateTime(boolean merchant) {
      Calendar newDate = Calendar.getInstance();
      if (merchant) {
         newDate.add(12, FortSiegeManager.getInstance().getSuspiciousMerchantRespawnDelay());
      } else {
         newDate.add(12, 60);
      }

      this._fort.setSiegeDate(newDate);
      this.saveSiegeDate();
   }

   private void loadSiegeClan() {
      this._attackerClans.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT clan_id FROM fortsiege_clans WHERE fort_id=?");
      ) {
         ps.setInt(1, this._fort.getResidenceId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this.addAttacker(rs.getInt("clan_id"));
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: loadSiegeClan(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void removeCommanders() {
      if (this._commanders != null && !this._commanders.isEmpty()) {
         for (Spawn spawn : this._commanders) {
            if (spawn != null) {
               spawn.stopRespawn();
               if (spawn.getLastSpawn() != null) {
                  spawn.getLastSpawn().deleteMe();
               }
            }
         }

         this._commanders.clear();
      }
   }

   private void removeFlags() {
      for (SiegeClan sc : this._attackerClans) {
         if (sc != null) {
            sc.removeFlags();
         }
      }
   }

   private void saveFortSiege() {
      this.clearSiegeDate();
      this.saveSiegeDate();
   }

   private void saveSiegeDate() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE fort SET siegeDate = ? WHERE id = ?");
      ) {
         ps.setLong(1, this.getFort().getSiegeDate().getTimeInMillis());
         ps.setInt(2, this._fort.getResidenceId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: saveSiegeDate(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   private void saveSiegeClan(Clan clan) {
      if (this.getAttackerClans().size() < FortSiegeManager.getInstance().getAttackerMaxClans()) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO fortsiege_clans (clan_id,fort_id) values (?,?)");
         ) {
            ps.setInt(1, clan.getId());
            ps.setInt(2, this._fort.getResidenceId());
            ps.execute();
            this.addAttacker(clan.getId());
         } catch (Exception var10) {
            LOGGER.log(Level.WARNING, "Exception: saveSiegeClan(Pledge clan): " + var10.getMessage(), (Throwable)var10);
         }
      }
   }

   private void spawnCommanders() {
      try {
         this._commanders.clear();

         for (FortSpawnHolder _sp : FortSiegeManager.getInstance().getCommanderSpawnList(this.getFort().getResidenceId())) {
            Spawn spawnDat = new Spawn(_sp.getId());
            spawnDat.setAmount(1);
            spawnDat.setXYZ(_sp.getLocation());
            spawnDat.setHeading(_sp.getLocation().getHeading());
            spawnDat.setRespawnDelay(60);
            spawnDat.doSpawn(false);
            spawnDat.stopRespawn();
            this._commanders.add(spawnDat);
         }
      } catch (Exception var4) {
         LOGGER.log(Level.WARNING, "FortSiege.spawnCommander: Spawn could not be initialized: " + var4.getMessage(), (Throwable)var4);
      }
   }

   private void spawnFlag(int id) {
      for (CombatFlag cf : FortSiegeManager.getInstance().getFlagList(id)) {
         cf.spawnMe();
      }
   }

   private void unSpawnFlags() {
      if (FortSiegeManager.getInstance().getFlagList(this.getFort().getResidenceId()) != null) {
         for (CombatFlag cf : FortSiegeManager.getInstance().getFlagList(this.getFort().getResidenceId())) {
            cf.unSpawnMe();
         }
      }
   }

   private void spawnSiegeGuard() {
      this.getSiegeGuardManager().spawnSiegeGuard();
   }

   @Override
   public SiegeClan getAttackerClan(Clan clan) {
      return clan == null ? null : this.getAttackerClan(clan.getId());
   }

   @Override
   public SiegeClan getAttackerClan(int clanId) {
      for (SiegeClan sc : this._attackerClans) {
         if (sc != null && sc.getClanId() == clanId) {
            return sc;
         }
      }

      return null;
   }

   @Override
   public Collection<SiegeClan> getAttackerClans() {
      return this._attackerClans;
   }

   public Fort getFort() {
      return this._fort;
   }

   public boolean isInProgress() {
      return this._isInProgress;
   }

   @Override
   public Calendar getSiegeDate() {
      return this._fort.getSiegeDate();
   }

   @Override
   public Set<Npc> getFlag(Clan clan) {
      if (clan != null) {
         SiegeClan sc = this.getAttackerClan(clan);
         if (sc != null) {
            return sc.getFlag();
         }
      }

      return null;
   }

   public FortSiegeGuardManager getSiegeGuardManager() {
      if (this._siegeGuardManager == null) {
         this._siegeGuardManager = new FortSiegeGuardManager(this._fort);
      }

      return this._siegeGuardManager;
   }

   public void resetSiege() {
      this.removeCommanders();
      this.spawnCommanders();
      this._fort.resetDoors();
   }

   public Collection<Spawn> getCommanders() {
      return this._commanders;
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

   @Override
   public boolean giveFame() {
      return true;
   }

   @Override
   public int getFameFrequency() {
      return PlayerConfig.FORTRESS_ZONE_FAME_TASK_FREQUENCY;
   }

   @Override
   public int getFameAmount() {
      return PlayerConfig.FORTRESS_ZONE_FAME_AQUIRE_POINTS;
   }

   @Override
   public void updateSiege() {
   }

   public class ScheduleEndSiegeTask implements Runnable {
      public ScheduleEndSiegeTask() {
         Objects.requireNonNull(FortSiege.this);
         super();
      }

      @Override
      public void run() {
         if (FortSiege.this._isInProgress) {
            try {
               FortSiege.this._siegeEnd = null;
               FortSiege.this.endSiege();
            } catch (Exception var2) {
               FortSiege.LOGGER
                  .log(
                     Level.WARNING, "Exception: ScheduleEndSiegeTask() for Fort: " + FortSiege.this._fort.getName() + " " + var2.getMessage(), (Throwable)var2
                  );
            }
         }
      }
   }

   public class ScheduleSiegeRestore implements Runnable {
      public ScheduleSiegeRestore() {
         Objects.requireNonNull(FortSiege.this);
         super();
      }

      @Override
      public void run() {
         if (FortSiege.this._isInProgress) {
            try {
               FortSiege.this._siegeRestore = null;
               FortSiege.this.resetSiege();
               FortSiege.this.announceToPlayer(new SystemMessage(SystemMessageId.THE_BARRACKS_FUNCTION_HAS_BEEN_RESTORED));
            } catch (Exception var2) {
               FortSiege.LOGGER
                  .log(
                     Level.WARNING, "Exception: ScheduleSiegeRestore() for Fort: " + FortSiege.this._fort.getName() + " " + var2.getMessage(), (Throwable)var2
                  );
            }
         }
      }
   }

   public class ScheduleStartSiegeTask implements Runnable {
      private final Fort _fortInst;
      private final int _time;

      public ScheduleStartSiegeTask(int time) {
         Objects.requireNonNull(FortSiege.this);
         super();
         this._fortInst = FortSiege.this._fort;
         this._time = time;
      }

      @Override
      public void run() {
         if (!FortSiege.this._isInProgress) {
            try {
               if (this._time == 3600) {
                  ThreadPool.schedule(FortSiege.this.new ScheduleStartSiegeTask(600), 3000000L);
               } else if (this._time == 600) {
                  FortSiege.this._fort.despawnSuspiciousMerchant();
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_THE_FORTRESS_BATTLE_STARTS);
                  sm.addInt(10);
                  FortSiege.this.announceToPlayer(sm);
                  ThreadPool.schedule(FortSiege.this.new ScheduleStartSiegeTask(300), 300000L);
               } else if (this._time == 300) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_THE_FORTRESS_BATTLE_STARTS);
                  sm.addInt(5);
                  FortSiege.this.announceToPlayer(sm);
                  ThreadPool.schedule(FortSiege.this.new ScheduleStartSiegeTask(60), 240000L);
               } else if (this._time == 60) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_THE_FORTRESS_BATTLE_STARTS);
                  sm.addInt(1);
                  FortSiege.this.announceToPlayer(sm);
                  ThreadPool.schedule(FortSiege.this.new ScheduleStartSiegeTask(30), 30000L);
               } else if (this._time == 30) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_SECOND_S_UNTIL_THE_FORTRESS_BATTLE_STARTS);
                  sm.addInt(30);
                  FortSiege.this.announceToPlayer(sm);
                  ThreadPool.schedule(FortSiege.this.new ScheduleStartSiegeTask(10), 20000L);
               } else if (this._time == 10) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_SECOND_S_UNTIL_THE_FORTRESS_BATTLE_STARTS);
                  sm.addInt(10);
                  FortSiege.this.announceToPlayer(sm);
                  ThreadPool.schedule(FortSiege.this.new ScheduleStartSiegeTask(5), 5000L);
               } else if (this._time == 5) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_SECOND_S_UNTIL_THE_FORTRESS_BATTLE_STARTS);
                  sm.addInt(5);
                  FortSiege.this.announceToPlayer(sm);
                  ThreadPool.schedule(FortSiege.this.new ScheduleStartSiegeTask(1), 4000L);
               } else if (this._time == 1) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_SECOND_S_UNTIL_THE_FORTRESS_BATTLE_STARTS);
                  sm.addInt(1);
                  FortSiege.this.announceToPlayer(sm);
                  ThreadPool.schedule(FortSiege.this.new ScheduleStartSiegeTask(0), 1000L);
               } else if (this._time == 0) {
                  this._fortInst.getSiege().startSiege();
               } else {
                  FortSiege.LOGGER.warning("Exception: ScheduleStartSiegeTask(): unknown siege time: " + this._time);
               }
            } catch (Exception var2) {
               FortSiege.LOGGER
                  .log(Level.WARNING, "Exception: ScheduleStartSiegeTask() for Fort: " + this._fortInst.getName() + " " + var2.getMessage(), (Throwable)var2);
            }
         }
      }
   }

   public class ScheduleSuspiciousMerchantSpawn implements Runnable {
      public ScheduleSuspiciousMerchantSpawn() {
         Objects.requireNonNull(FortSiege.this);
         super();
      }

      @Override
      public void run() {
         if (!FortSiege.this._isInProgress) {
            try {
               FortSiege.this._fort.spawnSuspiciousMerchant();
            } catch (Exception var2) {
               FortSiege.LOGGER
                  .log(
                     Level.WARNING,
                     "Exception: ScheduleSuspicoiusMerchantSpawn() for Fort: " + FortSiege.this._fort.getName() + " " + var2.getMessage(),
                     (Throwable)var2
                  );
            }
         }
      }
   }
}
