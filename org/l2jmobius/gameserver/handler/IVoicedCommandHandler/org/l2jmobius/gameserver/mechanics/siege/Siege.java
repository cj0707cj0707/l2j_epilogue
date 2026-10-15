package org.l2jmobius.gameserver.mechanics.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
import org.l2jmobius.gameserver.data.xml.SiegeScheduleData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.ControlTower;
import org.l2jmobius.gameserver.entity.actor.instance.FlameTower;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.MercTicketManager;
import org.l2jmobius.gameserver.managers.SiegeGuardManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.castle.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.castle.OnCastleSiegeOwnerChange;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.castle.OnCastleSiegeStart;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Siege implements Siegable {
   protected static final Logger LOGGER = Logger.getLogger(Siege.class.getName());
   public static final byte OWNER = -1;
   public static final byte DEFENDER = 0;
   public static final byte ATTACKER = 1;
   public static final byte DEFENDER_NOT_APPROVED = 2;
   private int _controlTowerCount;
   private final Collection<SiegeClan> _attackerClans = ConcurrentHashMap.newKeySet();
   private final Collection<SiegeClan> _defenderClans = ConcurrentHashMap.newKeySet();
   private final Collection<SiegeClan> _defenderWaitingClans = ConcurrentHashMap.newKeySet();
   private final List<ControlTower> _controlTowers = new ArrayList<>();
   private final List<FlameTower> _flameTowers = new ArrayList<>();
   private final Castle _castle;
   boolean _isInProgress = false;
   private boolean _isNormalSide = true;
   protected boolean _isRegistrationOver = false;
   protected Calendar _siegeEndDate;
   private SiegeGuardManager _siegeGuardManager;
   protected ScheduledFuture<?> _scheduledStartSiegeTask = null;
   protected int _firstOwnerClanId = -1;

   public Siege(Castle castle) {
      this._castle = castle;
      this._siegeGuardManager = new SiegeGuardManager(this.getCastle());
      SiegeScheduleDate schedule = SiegeScheduleData.getInstance().getScheduleDateForCastleId(this._castle.getResidenceId());
      if (schedule != null && schedule.siegeEnabled()) {
         this.startAutoTask();
      }
   }

   @Override
   public void endSiege() {
      if (this._isInProgress) {
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_FINISHED);
         sm.addCastleId(this._castle.getResidenceId());
         World.broadcastToAllOnlinePlayers(sm);
         if (this._castle.getOwnerId() > 0) {
            Clan clan = ClanTable.getInstance().getClan(this._castle.getOwnerId());
            sm = new SystemMessage(SystemMessageId.CLAN_S1_IS_VICTORIOUS_OVER_S2_S_CASTLE_SIEGE);
            sm.addString(clan.getName());
            sm.addString(this._castle.getName());
            World.broadcastToAllOnlinePlayers(sm);
            if (clan.getId() == this._firstOwnerClanId) {
               clan.increaseBloodAllianceCount();
            } else {
               this._castle.setTicketBuyCount(0);

               for (ClanMember member : clan.getMembers()) {
                  if (member != null) {
                     Player player = member.getPlayer();
                     if (player != null && player.isNoble()) {
                        Hero.getInstance().setCastleTaken(player.getObjectId(), this._castle.getResidenceId());
                     }
                  }
               }
            }
         } else {
            sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW);
            sm.addCastleId(this._castle.getResidenceId());
            World.broadcastToAllOnlinePlayers(sm);
         }

         for (SiegeClan attackerClan : this.getAttackerClans()) {
            Clan clan = ClanTable.getInstance().getClan(attackerClan.getClanId());
            if (clan != null) {
               clan.clearSiegeKills();
               clan.clearSiegeDeaths();
            }
         }

         for (SiegeClan defenderClan : this.getDefenderClans()) {
            Clan clan = ClanTable.getInstance().getClan(defenderClan.getClanId());
            if (clan != null) {
               clan.clearSiegeKills();
               clan.clearSiegeDeaths();
            }
         }

         this._castle.updateClansReputation();
         this.removeFlags();
         this.teleportPlayer(SiegeTeleportWhoType.NotOwner, TeleportWhereType.TOWN);
         this._isInProgress = false;
         this.updatePlayerSiegeStateFlags(true);
         this.saveCastleSiege();
         this.clearSiegeClan();
         this.removeTowers();
         this._siegeGuardManager.unspawnSiegeGuard();
         if (this._castle.getOwnerId() > 0) {
            this._siegeGuardManager.removeMercs();
         }

         this._castle.spawnDoor();
         this._castle.setFirstMidVictory(false);
         this._castle.getZone().setActive(false);
         this._castle.getZone().updateZoneStatusForCharactersInside();
         this._castle.getZone().setSiegeInstance(null);
         if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_FINISH, this._castle)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeFinish(this), this._castle);
         }
      }
   }

   private void removeDefender(SiegeClan sc) {
      if (sc != null) {
         this.getDefenderClans().remove(sc);
      }
   }

   private void removeAttacker(SiegeClan sc) {
      if (sc != null) {
         this.getAttackerClans().remove(sc);
      }
   }

   private void addDefender(SiegeClan sc, SiegeClanType type) {
      if (sc != null) {
         sc.setType(type);
         this.getDefenderClans().add(sc);
      }
   }

   private void addAttacker(SiegeClan sc) {
      if (sc != null) {
         sc.setType(SiegeClanType.ATTACKER);
         this.getAttackerClans().add(sc);
      }
   }

   public void midVictory() {
      if (this._isInProgress) {
         if (this._castle.getOwnerId() > 0) {
            this._siegeGuardManager.removeMercs();
         }

         if (this.getDefenderClans().isEmpty() && this.getAttackerClans().size() == 1) {
            SiegeClan scNewOwner = this.getAttackerClan(this._castle.getOwnerId());
            this.removeAttacker(scNewOwner);
            this.addDefender(scNewOwner, SiegeClanType.OWNER);
            this.endSiege();
            return;
         }

         if (this._castle.getOwnerId() > 0) {
            int allyId = ClanTable.getInstance().getClan(this._castle.getOwnerId()).getAllyId();
            if (this.getDefenderClans().isEmpty() && allyId != 0) {
               boolean allinsamealliance = true;

               for (SiegeClan sc : this.getAttackerClans()) {
                  if (sc != null && ClanTable.getInstance().getClan(sc.getClanId()).getAllyId() != allyId) {
                     allinsamealliance = false;
                  }
               }

               if (allinsamealliance) {
                  SiegeClan scNewOwner = this.getAttackerClan(this._castle.getOwnerId());
                  this.removeAttacker(scNewOwner);
                  this.addDefender(scNewOwner, SiegeClanType.OWNER);
                  this.endSiege();
                  return;
               }
            }

            for (SiegeClan scx : this.getDefenderClans()) {
               if (scx != null) {
                  this.removeDefender(scx);
                  this.addAttacker(scx);
               }
            }

            SiegeClan scNewOwner = this.getAttackerClan(this._castle.getOwnerId());
            this.removeAttacker(scNewOwner);
            this.addDefender(scNewOwner, SiegeClanType.OWNER);

            for (Clan clan : ClanTable.getInstance().getClanAllies(allyId)) {
               SiegeClan scxx = this.getAttackerClan(clan.getId());
               if (scxx != null) {
                  this.removeAttacker(scxx);
                  this.addDefender(scxx, SiegeClanType.DEFENDER);
               }
            }

            this._castle.setFirstMidVictory(true);
            this.teleportPlayer(SiegeTeleportWhoType.Attacker, TeleportWhereType.SIEGEFLAG);
            this.teleportPlayer(SiegeTeleportWhoType.Spectator, TeleportWhereType.TOWN);
            this.removeDefenderFlags();
            this._castle.removeUpgrade();
            this._castle.spawnDoor(true);
            this.removeTowers();
            this._controlTowerCount = 0;
            this.spawnControlTower();
            this.spawnFlameTower();
            this.updatePlayerSiegeStateFlags(false);
            if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, this._castle)) {
               EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeOwnerChange(this), this._castle);
            }
         }
      }
   }

   @Override
   public void startSiege() {
      if (!this._isInProgress) {
         this._firstOwnerClanId = this._castle.getOwnerId();
         if (this.getAttackerClans().isEmpty()) {
            SystemMessage sm;
            if (this._firstOwnerClanId <= 0) {
               sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
            } else {
               sm = new SystemMessage(SystemMessageId.S1_S_SIEGE_WAS_CANCELED_BECAUSE_THERE_WERE_NO_CLANS_THAT_PARTICIPATED);
               Clan ownerClan = ClanTable.getInstance().getClan(this._firstOwnerClanId);
               ownerClan.increaseBloodAllianceCount();
            }

            sm.addCastleId(this._castle.getResidenceId());
            World.broadcastToAllOnlinePlayers(sm);
            this.saveCastleSiege();
            return;
         }

         this._isNormalSide = true;
         this._isInProgress = true;
         this.loadSiegeClan();
         this.updatePlayerSiegeStateFlags(false);
         this.teleportPlayer(SiegeTeleportWhoType.NotOwner, TeleportWhereType.TOWN);
         this._controlTowerCount = 0;
         this.spawnControlTower();
         this.spawnFlameTower();
         this._castle.spawnDoor();
         this.spawnSiegeGuard();
         MercTicketManager.getInstance().deleteTickets(this._castle.getResidenceId());
         this._castle.getZone().setSiegeInstance(this);
         this._castle.getZone().setActive(true);
         this._castle.getZone().updateZoneStatusForCharactersInside();
         this._siegeEndDate = Calendar.getInstance();
         this._siegeEndDate.add(12, SiegeManager.getInstance().getSiegeLength());
         ThreadPool.schedule(new Siege.ScheduleEndSiegeTask(this._castle), 1000L);
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_STARTED);
         sm.addCastleId(this._castle.getResidenceId());
         World.broadcastToAllOnlinePlayers(sm);
         if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_START, this._castle)) {
            EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeStart(this), this._castle);
         }
      }
   }

   public void announceToPlayer(SystemMessage message, boolean bothSides) {
      for (SiegeClan siegeClans : this.getDefenderClans()) {
         for (Player member : ClanTable.getInstance().getClan(siegeClans.getClanId()).getOnlineMembers(0)) {
            member.sendPacket(message);
         }
      }

      if (bothSides) {
         for (SiegeClan siegeClans : this.getAttackerClans()) {
            for (Player member : ClanTable.getInstance().getClan(siegeClans.getClanId()).getOnlineMembers(0)) {
               if (member != null) {
                  member.sendPacket(message);
               }
            }
         }
      }
   }

   public void updatePlayerSiegeStateFlags(boolean clear) {
      for (SiegeClan siegeclan : this.getAttackerClans()) {
         if (siegeclan != null) {
            Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

            for (Player member : clan.getOnlineMembers(0)) {
               if (clear) {
                  member.setSiegeState((byte)0);
                  member.setSiegeSide(0);
                  member.setInSiege(false);
                  member.stopFameTask();
               } else {
                  member.setSiegeState((byte)1);
                  member.setSiegeSide(this.getCastle().getResidenceId());
                  if (this.checkIfInZone(member)) {
                     member.setInSiege(true);
                     member.startFameTask(PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS);
                  }
               }

               member.updateUserInfo();
               World.forEachVisibleObject(member, Player.class, player -> {
                  if (member.isVisibleFor(player)) {
                     player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
                     if (member.hasSummon()) {
                        player.sendPacket(new RelationChanged(member.getSummon(), member.getRelation(player), member.isAutoAttackable(player)));
                     }
                  }
               });
            }
         }
      }

      for (SiegeClan siegeclanx : this.getDefenderClans()) {
         if (siegeclanx != null) {
            Clan clan = ClanTable.getInstance().getClan(siegeclanx.getClanId());

            for (Player member : clan.getOnlineMembers(0)) {
               if (member != null) {
                  if (clear) {
                     member.setSiegeState((byte)0);
                     member.setSiegeSide(0);
                     member.setInSiege(false);
                     member.stopFameTask();
                  } else {
                     member.setSiegeState((byte)2);
                     member.setSiegeSide(this.getCastle().getResidenceId());
                     if (this.checkIfInZone(member)) {
                        member.setInSiege(true);
                        member.startFameTask(PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS);
                     }
                  }

                  member.updateUserInfo();
                  World.forEachVisibleObject(member, Player.class, player -> {
                     if (member.isVisibleFor(player)) {
                        player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
                        if (member.hasSummon()) {
                           player.sendPacket(new RelationChanged(member.getSummon(), member.getRelation(player), member.isAutoAttackable(player)));
                        }
                     }
                  });
               }
            }
         }
      }
   }

   public void approveSiegeDefenderClan(int clanId) {
      if (clanId > 0) {
         this.saveSiegeClan(ClanTable.getInstance().getClan(clanId), (byte)0, true);
         this.loadSiegeClan();
      }
   }

   public boolean checkIfInZone(WorldObject object) {
      return this.checkIfInZone(object.getX(), object.getY(), object.getZ());
   }

   public boolean checkIfInZone(int x, int y, int z) {
      return this._isInProgress && this.getCastle().checkIfInZone(x, y, z);
   }

   @Override
   public boolean checkIsAttacker(Clan clan) {
      return this.getAttackerClan(clan) != null;
   }

   @Override
   public boolean checkIsDefender(Clan clan) {
      return this.getDefenderClan(clan) != null;
   }

   public boolean checkIsDefenderWaiting(Clan clan) {
      return this.getDefenderWaitingClan(clan) != null;
   }

   public void clearSiegeClan() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=?");
      ) {
         ps.setInt(1, this.getCastle().getResidenceId());
         ps.execute();
         if (this.getCastle().getOwnerId() > 0) {
            try (PreparedStatement delete = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?")) {
               delete.setInt(1, this.getCastle().getOwnerId());
               delete.execute();
            }
         }

         this.getAttackerClans().clear();
         this.getDefenderClans().clear();
         this._defenderWaitingClans.clear();
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: clearSiegeClan(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void clearSiegeWaitingClan() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and type = 2");
      ) {
         ps.setInt(1, this.getCastle().getResidenceId());
         ps.execute();
         this._defenderWaitingClans.clear();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: clearSiegeWaitingClan(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   @Override
   public List<Player> getAttackersInZone() {
      List<Player> players = new ArrayList<>();

      for (SiegeClan siegeclan : this.getAttackerClans()) {
         Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

         for (Player player : clan.getOnlineMembers(0)) {
            if (player.isInSiege()) {
               players.add(player);
            }
         }
      }

      return players;
   }

   public List<Player> getPlayersInZone() {
      return this.getCastle().getZone().getPlayersInside();
   }

   public List<Player> getOwnersInZone() {
      List<Player> players = new ArrayList<>();

      for (SiegeClan siegeclan : this.getDefenderClans()) {
         Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
         if (clan.getId() == this.getCastle().getOwnerId()) {
            for (Player player : clan.getOnlineMembers(0)) {
               if (player.isInSiege()) {
                  players.add(player);
               }
            }
         }
      }

      return players;
   }

   public List<Player> getSpectatorsInZone() {
      List<Player> players = new ArrayList<>();

      for (Player player : this.getCastle().getZone().getPlayersInside()) {
         if (player != null && !player.isInSiege()) {
            players.add(player);
         }
      }

      return players;
   }

   public void killedCT() {
      this._controlTowerCount--;
      if (this._controlTowerCount < 0) {
         this._controlTowerCount = 0;
      }
   }

   public void killedFlag(Npc flag) {
      if (flag != null) {
         for (SiegeClan clan : this.getAttackerClans()) {
            if (clan.removeFlag(flag)) {
               return;
            }
         }
      }
   }

   public void listRegisterClan(Player player) {
      player.sendPacket(new SiegeInfo(this.getCastle(), player));
   }

   public void registerAttacker(Player player) {
      this.registerAttacker(player, false);
   }

   public void registerAttacker(Player player, boolean force) {
      if (player.getClan() != null) {
         int allyId = this.getCastle().getOwnerId() != 0 ? ClanTable.getInstance().getClan(this.getCastle().getOwnerId()).getAllyId() : 0;
         if (allyId != 0 && player.getClan().getAllyId() == allyId && !force) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_ON_THE_ATTACKING_SIDE_BECAUSE_YOU_ARE_PART_OF_AN_ALLIANCE_WITH_THE_CLAN_THAT_OWNS_THE_CASTLE);
         } else if (force) {
            if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), this.getCastle().getResidenceId())) {
               player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_SIEGE_BATTLE);
            } else {
               this.saveSiegeClan(player.getClan(), (byte)1, false);
            }
         } else {
            if (this.checkIfCanRegister(player, (byte)1)) {
               this.saveSiegeClan(player.getClan(), (byte)1, false);
            }
         }
      }
   }

   public void registerDefender(Player player) {
      this.registerDefender(player, false);
   }

   public void registerDefender(Player player, boolean force) {
      if (this.getCastle().getOwnerId() <= 0) {
         player.sendMessage("You cannot register as a defender because " + this.getCastle().getName() + " is owned by NPC.");
      } else if (force) {
         if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), this.getCastle().getResidenceId())) {
            player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_SIEGE_BATTLE);
         } else {
            this.saveSiegeClan(player.getClan(), (byte)2, false);
         }
      } else {
         if (this.checkIfCanRegister(player, (byte)2)) {
            this.saveSiegeClan(player.getClan(), (byte)2, false);
         }
      }
   }

   public void removeSiegeClan(int clanId) {
      if (clanId > 0) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and clan_id=?");
         ) {
            ps.setInt(1, this.getCastle().getResidenceId());
            ps.setInt(2, clanId);
            ps.execute();
            this.loadSiegeClan();
         } catch (Exception var10) {
            LOGGER.log(Level.WARNING, "Exception: removeSiegeClan(): " + var10.getMessage(), (Throwable)var10);
         }
      }
   }

   public void removeSiegeClan(Clan clan) {
      if (clan != null
         && clan.getCastleId() != this.getCastle().getResidenceId()
         && SiegeManager.getInstance().checkIsRegistered(clan, this.getCastle().getResidenceId())) {
         this.removeSiegeClan(clan.getId());
      }
   }

   public void removeSiegeClan(Player player) {
      this.removeSiegeClan(player.getClan());
   }

   public void startAutoTask() {
      this.correctSiegeDateTime();
      LOGGER.info("Siege of " + this.getCastle().getName() + ": " + this.getCastle().getSiegeDate().getTime());
      this.loadSiegeClan();
      if (this._scheduledStartSiegeTask != null) {
         this._scheduledStartSiegeTask.cancel(false);
      }

      this._scheduledStartSiegeTask = ThreadPool.schedule(new Siege.ScheduleStartSiegeTask(this.getCastle()), 1000L);
   }

   public void teleportPlayer(SiegeTeleportWhoType teleportWho, TeleportWhereType teleportWhere) {
      label51: {
         List players;

         players = switch (teleportWho) {
            case Owner -> this.getOwnersInZone();
            case NotOwner -> {
               yield this.getPlayersInZone();
               Iterator<Player> it = players.iterator();

               while (it.hasNext()) {
                  Player player = it.next();
                  if (player == null || player.inObserverMode() || player.getClanId() > 0 && player.getClanId() == this.getCastle().getOwnerId()) {
                     it.remove();
                  }
               }

               for (Player player : players) {
                  if (!player.isGM() && !player.isJailed()) {
                     player.teleToLocation(teleportWhere);
                  }
               }
            }
            case Attacker -> this.getAttackersInZone();
            case Spectator -> this.getSpectatorsInZone();
            default -> Collections.emptyList();
         };
      }
   }

   private void addAttacker(int clanId) {
      this.getAttackerClans().add(new SiegeClan(clanId, SiegeClanType.ATTACKER));
   }

   private void addDefender(int clanId) {
      this.getDefenderClans().add(new SiegeClan(clanId, SiegeClanType.DEFENDER));
   }

   private void addDefender(int clanId, SiegeClanType type) {
      this.getDefenderClans().add(new SiegeClan(clanId, type));
   }

   private void addDefenderWaiting(int clanId) {
      this._defenderWaitingClans.add(new SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING));
   }

   private boolean checkIfCanRegister(Player player, byte typeId) {
      if (this._isRegistrationOver) {
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_DEADLINE_TO_REGISTER_FOR_THE_SIEGE_OF_S1_HAS_PASSED);
         sm.addCastleId(this.getCastle().getResidenceId());
         player.sendPacket(sm);
      } else if (this._isInProgress) {
         player.sendPacket(SystemMessageId.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE);
      } else if (player.getClan() == null || player.getClan().getLevel() < SiegeManager.getInstance().getSiegeClanMinLevel()) {
         player.sendPacket(SystemMessageId.ONLY_CLANS_OF_LEVEL_5_OR_HIGHER_MAY_REGISTER_FOR_A_CASTLE_SIEGE);
      } else if (player.getClan().getId() == this.getCastle().getOwnerId()) {
         player.sendPacket(SystemMessageId.THE_CLAN_THAT_OWNS_THE_CASTLE_IS_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE);
      } else if (player.getClan().getCastleId() > 0) {
         player.sendPacket(SystemMessageId.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE);
      } else if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), this.getCastle().getResidenceId())) {
         player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_SIEGE_BATTLE);
      } else if (this.checkIfAlreadyRegisteredForSameDay(player.getClan())) {
         player.sendPacket(SystemMessageId.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE);
      } else if (typeId == 1 && this.getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans()) {
         player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_ATTACKER_SIDE);
      } else {
         if (typeId != 0 && typeId != 2 && typeId != -1
            || this.getDefenderClans().size() + this.getDefenderWaitingClans().size() < SiegeManager.getInstance().getDefenderMaxClans()) {
            return true;
         }

         player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_DEFENDER_SIDE);
      }

      return false;
   }

   public boolean checkIfAlreadyRegisteredForSameDay(Clan clan) {
      for (Siege siege : SiegeManager.getInstance().getSieges()) {
         if (siege != this && siege.getSiegeDate().get(7) == this.getSiegeDate().get(7)) {
            if (siege.checkIsAttacker(clan)) {
               return true;
            }

            if (siege.checkIsDefender(clan)) {
               return true;
            }

            if (siege.checkIsDefenderWaiting(clan)) {
               return true;
            }
         }
      }

      return false;
   }

   public void correctSiegeDateTime() {
      boolean corrected = false;
      if (this.getCastle().getSiegeDate().getTimeInMillis() < System.currentTimeMillis()) {
         corrected = true;
         this.setNextSiegeDate();
      }

      if (corrected) {
         this.saveSiegeDate();
      }
   }

   private void loadSiegeClan() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT clan_id,type FROM siege_clans where castle_id=?");
      ) {
         this.getAttackerClans().clear();
         this.getDefenderClans().clear();
         this._defenderWaitingClans.clear();
         if (this.getCastle().getOwnerId() > 0) {
            this.addDefender(this.getCastle().getOwnerId(), SiegeClanType.OWNER);
         }

         ps.setInt(1, this.getCastle().getResidenceId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               int typeId = rs.getInt("type");
               if (typeId == 0) {
                  this.addDefender(rs.getInt("clan_id"));
               } else if (typeId == 1) {
                  this.addAttacker(rs.getInt("clan_id"));
               } else if (typeId == 2) {
                  this.addDefenderWaiting(rs.getInt("clan_id"));
               }
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: loadSiegeClan(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void removeTowers() {
      for (FlameTower ct : this._flameTowers) {
         ct.deleteMe();
      }

      for (ControlTower ct : this._controlTowers) {
         ct.deleteMe();
      }

      this._flameTowers.clear();
      this._controlTowers.clear();
   }

   private void removeFlags() {
      for (SiegeClan sc : this.getAttackerClans()) {
         if (sc != null) {
            sc.removeFlags();
         }
      }

      for (SiegeClan scx : this.getDefenderClans()) {
         if (scx != null) {
            scx.removeFlags();
         }
      }
   }

   private void removeDefenderFlags() {
      for (SiegeClan sc : this.getDefenderClans()) {
         if (sc != null) {
            sc.removeFlags();
         }
      }
   }

   private void saveCastleSiege() {
      this.setNextSiegeDate();
      this.getTimeRegistrationOverDate().setTimeInMillis(System.currentTimeMillis());
      this.getTimeRegistrationOverDate().add(5, 1);
      this.getCastle().setTimeRegistrationOver(false);
      this.saveSiegeDate();
      this.startAutoTask();
   }

   public void saveSiegeDate() {
      if (this._scheduledStartSiegeTask != null) {
         this._scheduledStartSiegeTask.cancel(true);
         this._scheduledStartSiegeTask = ThreadPool.schedule(new Siege.ScheduleStartSiegeTask(this.getCastle()), 1000L);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE castle SET siegeDate = ?, regTimeEnd = ?, regTimeOver = ?  WHERE id = ?");
      ) {
         ps.setLong(1, this.getSiegeDate().getTimeInMillis());
         ps.setLong(2, this.getTimeRegistrationOverDate().getTimeInMillis());
         ps.setString(3, String.valueOf(this.isTimeRegistrationOver()));
         ps.setInt(4, this.getCastle().getResidenceId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: saveSiegeDate(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   private void saveSiegeClan(Clan clan, byte typeId, boolean isUpdateRegistration) {
      if (clan.getCastleId() <= 0) {
         try (Connection con = DatabaseFactory.getConnection()) {
            if (typeId != 0 && typeId != 2 && typeId != -1) {
               if (this.getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans()) {
                  return;
               }
            } else if (this.getDefenderClans().size() + this.getDefenderWaitingClans().size() >= SiegeManager.getInstance().getDefenderMaxClans()) {
               return;
            }

            if (!isUpdateRegistration) {
               try (PreparedStatement ps = con.prepareStatement("INSERT INTO siege_clans (clan_id,castle_id,type,castle_owner) values (?,?,?,0)")) {
                  ps.setInt(1, clan.getId());
                  ps.setInt(2, this.getCastle().getResidenceId());
                  ps.setInt(3, typeId);
                  ps.execute();
               }
            } else {
               try (PreparedStatement ps = con.prepareStatement("UPDATE siege_clans SET type = ? WHERE castle_id = ? AND clan_id = ?")) {
                  ps.setInt(1, typeId);
                  ps.setInt(2, this.getCastle().getResidenceId());
                  ps.setInt(3, clan.getId());
                  ps.execute();
               }
            }

            if (typeId == 0 || typeId == -1) {
               this.addDefender(clan.getId());
               return;
            } else if (typeId == 1) {
               this.addAttacker(clan.getId());
            } else if (typeId == 2) {
               this.addDefenderWaiting(clan.getId());
            }
         } catch (Exception var14) {
            LOGGER.log(
               Level.WARNING, "Exception: saveSiegeClan(Pledge clan, int typeId, boolean isUpdateRegistration): " + var14.getMessage(), (Throwable)var14
            );
         }
      }
   }

   private void setNextSiegeDate() {
      SiegeScheduleDate holder = SiegeScheduleData.getInstance().getScheduleDateForCastleId(this._castle.getResidenceId());
      if (holder != null && holder.siegeEnabled()) {
         Calendar calendar = this._castle.getSiegeDate();
         if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.setTimeInMillis(System.currentTimeMillis());
         }

         calendar.set(7, holder.getDay());
         calendar.set(11, holder.getHour());
         calendar.set(12, 0);
         calendar.set(13, 0);
         if (calendar.before(Calendar.getInstance())) {
            calendar.add(3, SiegeManager.getInstance().getSiegeCycle());
         }

         if (CastleManager.getInstance().getSiegeDates(calendar.getTimeInMillis()) < holder.getMaxConcurrent()) {
            CastleManager.getInstance().registerSiegeDate(this.getCastle().getResidenceId(), calendar.getTimeInMillis());
            World.broadcastToAllOnlinePlayers(
               new SystemMessage(SystemMessageId.S1_HAS_ANNOUNCED_THE_CASTLE_SIEGE_TIME).addCastleId(this._castle.getResidenceId())
            );
            this._isRegistrationOver = false;
         } else {
            this._isRegistrationOver = true;
         }
      }
   }

   private void spawnControlTower() {
      for (TowerSpawn ts : SiegeManager.getInstance().getControlTowers(this.getCastle().getResidenceId())) {
         try {
            Spawn spawn = new Spawn(ts.getId());
            spawn.setLocation(ts.getLocation());
            this._controlTowers.add((ControlTower)spawn.doSpawn(false));
         } catch (Exception var4) {
            LOGGER.warning(this.getClass().getName() + ": Cannot spawn control tower! " + var4);
         }
      }

      this._controlTowerCount = this._controlTowers.size();
   }

   private void spawnFlameTower() {
      for (TowerSpawn ts : SiegeManager.getInstance().getFlameTowers(this.getCastle().getResidenceId())) {
         try {
            Spawn spawn = new Spawn(ts.getId());
            spawn.setLocation(ts.getLocation());
            FlameTower tower = (FlameTower)spawn.doSpawn(false);
            tower.setUpgradeLevel(ts.getUpgradeLevel());
            tower.setZoneList(ts.getZoneList());
            this._flameTowers.add(tower);
         } catch (Exception var5) {
            LOGGER.warning(this.getClass().getName() + ": Cannot spawn flame tower! " + var5);
         }
      }
   }

   private void spawnSiegeGuard() {
      this.getSiegeGuardManager().spawnSiegeGuard();

      for (Spawn spawn : this.getSiegeGuardManager().getSiegeGuardSpawn()) {
         if (spawn != null) {
            ControlTower closestCt = null;
            double distanceClosest = 2.147483647E9;

            for (ControlTower ct : this._controlTowers) {
               if (ct != null) {
                  double distance = ct.calculateDistance3D(spawn);
                  if (distance < distanceClosest) {
                     closestCt = ct;
                     distanceClosest = distance;
                  }
               }
            }

            if (closestCt != null) {
               closestCt.registerGuard(spawn);
            }
         }
      }
   }

   @Override
   public SiegeClan getAttackerClan(Clan clan) {
      return clan == null ? null : this.getAttackerClan(clan.getId());
   }

   @Override
   public SiegeClan getAttackerClan(int clanId) {
      for (SiegeClan sc : this.getAttackerClans()) {
         if (sc != null && sc.getClanId() == clanId) {
            return sc;
         }
      }

      return null;
   }

   @Override
   public Collection<SiegeClan> getAttackerClans() {
      return this._isNormalSide ? this._attackerClans : this._defenderClans;
   }

   public int getAttackerRespawnDelay() {
      return SiegeManager.getInstance().getAttackerRespawnDelay();
   }

   public Castle getCastle() {
      return this._castle == null ? null : this._castle;
   }

   @Override
   public SiegeClan getDefenderClan(Clan clan) {
      return clan == null ? null : this.getDefenderClan(clan.getId());
   }

   @Override
   public SiegeClan getDefenderClan(int clanId) {
      for (SiegeClan sc : this.getDefenderClans()) {
         if (sc != null && sc.getClanId() == clanId) {
            return sc;
         }
      }

      return null;
   }

   @Override
   public Collection<SiegeClan> getDefenderClans() {
      return this._isNormalSide ? this._defenderClans : this._attackerClans;
   }

   public SiegeClan getDefenderWaitingClan(Clan clan) {
      return clan == null ? null : this.getDefenderWaitingClan(clan.getId());
   }

   public SiegeClan getDefenderWaitingClan(int clanId) {
      for (SiegeClan sc : this._defenderWaitingClans) {
         if (sc != null && sc.getClanId() == clanId) {
            return sc;
         }
      }

      return null;
   }

   public Collection<SiegeClan> getDefenderWaitingClans() {
      return this._defenderWaitingClans;
   }

   public boolean isInProgress() {
      return this._isInProgress;
   }

   public boolean isRegistrationOver() {
      return this._isRegistrationOver;
   }

   public boolean isTimeRegistrationOver() {
      return this.getCastle().isTimeRegistrationOver();
   }

   @Override
   public Calendar getSiegeDate() {
      return this.getCastle().getSiegeDate();
   }

   public Calendar getTimeRegistrationOverDate() {
      return this.getCastle().getTimeRegistrationOverDate();
   }

   public void endTimeRegistration(boolean automatic) {
      this.getCastle().setTimeRegistrationOver(true);
      if (!automatic) {
         this.saveSiegeDate();
      }
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

   public SiegeGuardManager getSiegeGuardManager() {
      if (this._siegeGuardManager == null) {
         this._siegeGuardManager = new SiegeGuardManager(this.getCastle());
      }

      return this._siegeGuardManager;
   }

   public int getControlTowerCount() {
      return this._controlTowerCount;
   }

   @Override
   public boolean giveFame() {
      return true;
   }

   @Override
   public int getFameFrequency() {
      return PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY;
   }

   @Override
   public int getFameAmount() {
      return PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS;
   }

   @Override
   public void updateSiege() {
   }

   public class ScheduleEndSiegeTask implements Runnable {
      private final Castle _castleInst;

      public ScheduleEndSiegeTask(Castle pCastle) {
         Objects.requireNonNull(Siege.this);
         super();
         this._castleInst = pCastle;
      }

      @Override
      public void run() {
         if (Siege.this._isInProgress) {
            try {
               long timeRemaining = Siege.this._siegeEndDate.getTimeInMillis() - System.currentTimeMillis();
               if (timeRemaining > 3600000L) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_HOUR_S_UNTIL_CASTLE_SIEGE_CONCLUSION);
                  sm.addInt(2);
                  Siege.this.announceToPlayer(sm, true);
                  ThreadPool.schedule(Siege.this.new ScheduleEndSiegeTask(this._castleInst), timeRemaining - 3600000L);
               } else if (timeRemaining <= 3600000L && timeRemaining > 600000L) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION);
                  sm.addInt((int)timeRemaining / 60000);
                  Siege.this.announceToPlayer(sm, true);
                  ThreadPool.schedule(Siege.this.new ScheduleEndSiegeTask(this._castleInst), timeRemaining - 600000L);
               } else if (timeRemaining <= 600000L && timeRemaining > 300000L) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION);
                  sm.addInt((int)timeRemaining / 60000);
                  Siege.this.announceToPlayer(sm, true);
                  ThreadPool.schedule(Siege.this.new ScheduleEndSiegeTask(this._castleInst), timeRemaining - 300000L);
               } else if (timeRemaining <= 300000L && timeRemaining > 10000L) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION);
                  sm.addInt((int)timeRemaining / 60000);
                  Siege.this.announceToPlayer(sm, true);
                  ThreadPool.schedule(Siege.this.new ScheduleEndSiegeTask(this._castleInst), timeRemaining - 10000L);
               } else if (timeRemaining <= 10000L && timeRemaining > 0L) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.CASTLE_SIEGE_S1_SECOND_S_LEFT);
                  sm.addInt((int)timeRemaining / 1000);
                  Siege.this.announceToPlayer(sm, true);
                  ThreadPool.schedule(Siege.this.new ScheduleEndSiegeTask(this._castleInst), timeRemaining);
               } else {
                  this._castleInst.getSiege().endSiege();
               }
            } catch (Exception var4) {
               Siege.LOGGER.log(Level.SEVERE, "", (Throwable)var4);
            }
         }
      }
   }

   public class ScheduleStartSiegeTask implements Runnable {
      private final Castle _castleInst;

      public ScheduleStartSiegeTask(Castle pCastle) {
         Objects.requireNonNull(Siege.this);
         super();
         this._castleInst = pCastle;
      }

      @Override
      public void run() {
         Siege.this._scheduledStartSiegeTask.cancel(false);
         if (!Siege.this._isInProgress) {
            try {
               long currentTime = System.currentTimeMillis();
               if (!Siege.this.isTimeRegistrationOver()) {
                  long regTimeRemaining = Siege.this.getTimeRegistrationOverDate().getTimeInMillis() - currentTime;
                  if (regTimeRemaining > 0L) {
                     Siege.this._scheduledStartSiegeTask = ThreadPool.schedule(Siege.this.new ScheduleStartSiegeTask(this._castleInst), regTimeRemaining);
                     return;
                  }

                  Siege.this.endTimeRegistration(true);
               }

               long timeRemaining = Siege.this.getSiegeDate().getTimeInMillis() - currentTime;
               if (timeRemaining > 86400000L) {
                  Siege.this._scheduledStartSiegeTask = ThreadPool.schedule(Siege.this.new ScheduleStartSiegeTask(this._castleInst), timeRemaining - 86400000L);
               } else if (timeRemaining <= 86400000L && timeRemaining > 13600000L) {
                  SystemMessage sm = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
                  sm.addCastleId(Siege.this.getCastle().getResidenceId());
                  World.broadcastToAllOnlinePlayers(sm);
                  Siege.this._isRegistrationOver = true;
                  Siege.this.clearSiegeWaitingClan();
                  Siege.this._scheduledStartSiegeTask = ThreadPool.schedule(Siege.this.new ScheduleStartSiegeTask(this._castleInst), timeRemaining - 13600000L);
               } else if (timeRemaining <= 13600000L && timeRemaining > 600000L) {
                  Siege.this._scheduledStartSiegeTask = ThreadPool.schedule(Siege.this.new ScheduleStartSiegeTask(this._castleInst), timeRemaining - 600000L);
               } else if (timeRemaining <= 600000L && timeRemaining > 300000L) {
                  Siege.this._scheduledStartSiegeTask = ThreadPool.schedule(Siege.this.new ScheduleStartSiegeTask(this._castleInst), timeRemaining - 300000L);
               } else if (timeRemaining <= 300000L && timeRemaining > 10000L) {
                  Siege.this._scheduledStartSiegeTask = ThreadPool.schedule(Siege.this.new ScheduleStartSiegeTask(this._castleInst), timeRemaining - 10000L);
               } else if (timeRemaining <= 10000L && timeRemaining > 0L) {
                  Siege.this._scheduledStartSiegeTask = ThreadPool.schedule(Siege.this.new ScheduleStartSiegeTask(this._castleInst), timeRemaining);
               } else {
                  this._castleInst.getSiege().startSiege();
               }
            } catch (Exception var6) {
               Siege.LOGGER.log(Level.SEVERE, "", (Throwable)var6);
            }
         }
      }
   }
}
