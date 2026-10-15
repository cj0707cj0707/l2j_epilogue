package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.Siegable;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.siege.TerritoryWard;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

public class TerritoryWarManager implements Siegable {
   private static final Logger LOGGER = Logger.getLogger(TerritoryWarManager.class.getName());
   private static final String TW_CONFIG_FILE = "./config/TerritoryWar.ini";
   private static final String DELETE = "DELETE FROM territory_registrations WHERE castleId = ? and registeredId = ?";
   private static final String INSERT = "INSERT INTO territory_registrations (castleId, registeredId) values (?, ?)";
   public static final Map<Integer, Integer> TERRITORY_ITEM_IDS = new HashMap<>();
   public static String qn = "TerritoryWarSuperClass";
   public static String GLOBAL_VARIABLE = "nextTWStartDate";
   public static int DEFENDERMAXCLANS;
   public static int DEFENDERMAXPLAYERS;
   public static int CLANMINLEVEL;
   public static int PLAYERMINLEVEL;
   public static int MINTWBADGEFORNOBLESS;
   public static int MINTWBADGEFORSTRIDERS;
   public static int MINTWBADGEFORBIGSTRIDER;
   public static Long WARLENGTH;
   public static boolean PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE;
   public static boolean SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS;
   public static boolean RETURN_WARDS_WHEN_TW_STARTS;
   private final Map<Integer, List<Clan>> _registeredClans = new ConcurrentHashMap<>();
   private final Map<Integer, List<Integer>> _registeredMercenaries = new ConcurrentHashMap<>();
   private final Map<Integer, TerritoryWarManager.Territory> _territoryList = new ConcurrentHashMap<>();
   protected Collection<Integer> _disguisedPlayers = ConcurrentHashMap.newKeySet();
   private final Collection<TerritoryWard> _territoryWards = ConcurrentHashMap.newKeySet();
   private final Map<Clan, SiegeFlag> _clanFlags = new ConcurrentHashMap<>();
   private final Map<Integer, Integer[]> _participantPoints = new ConcurrentHashMap<>();
   protected Calendar _startTWDate = Calendar.getInstance();
   protected boolean _isRegistrationOver = true;
   protected boolean _isTWChannelOpen = false;
   boolean _isTWInProgress = false;
   protected ScheduledFuture<?> _scheduledStartTWTask = null;
   protected ScheduledFuture<?> _scheduledEndTWTask = null;
   protected ScheduledFuture<?> _scheduledRewardOnlineTask = null;

   protected TerritoryWarManager() {
      this.load();
   }

   public int getRegisteredTerritoryId(Player player) {
      if (player != null && this._isTWChannelOpen && player.getLevel() >= PLAYERMINLEVEL) {
         Clan clan = player.getClan();
         if (clan != null) {
            if (clan.getCastleId() > 0) {
               return clan.getCastleId() + 80;
            }

            for (int cId : this._registeredClans.keySet()) {
               if (this._registeredClans.get(cId).contains(clan)) {
                  return cId + 80;
               }
            }
         }

         for (int cIdx : this._registeredMercenaries.keySet()) {
            if (this._registeredMercenaries.get(cIdx).contains(player.getObjectId())) {
               return cIdx + 80;
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public boolean isAllyField(Player player, int fieldId) {
      if (player != null && player.getSiegeSide() != 0) {
         return player.getSiegeSide() - 80 == fieldId
            ? true
            : fieldId > 100
               && this._territoryList.containsKey(player.getSiegeSide() - 80)
               && this._territoryList.get(player.getSiegeSide() - 80).getFortId() == fieldId;
      } else {
         return false;
      }
   }

   public boolean checkIsRegistered(int castleId, Clan clan) {
      if (clan == null) {
         return false;
      } else if (clan.getCastleId() > 0) {
         return castleId == -1 ? true : clan.getCastleId() == castleId;
      } else if (castleId == -1) {
         for (int cId : this._registeredClans.keySet()) {
            if (this._registeredClans.get(cId).contains(clan)) {
               return true;
            }
         }

         return false;
      } else {
         return this._registeredClans.get(castleId).contains(clan);
      }
   }

   public boolean checkIsRegistered(int castleId, int objId) {
      if (castleId == -1) {
         for (int cId : this._registeredMercenaries.keySet()) {
            if (this._registeredMercenaries.get(cId).contains(objId)) {
               return true;
            }
         }

         return false;
      } else {
         return this._registeredMercenaries.get(castleId).contains(objId);
      }
   }

   public TerritoryWarManager.Territory getTerritory(int castleId) {
      return this._territoryList.get(castleId);
   }

   public List<TerritoryWarManager.Territory> getAllTerritories() {
      List<TerritoryWarManager.Territory> ret = new ArrayList<>();

      for (TerritoryWarManager.Territory t : this._territoryList.values()) {
         if (t.getOwnerClan() != null) {
            ret.add(t);
         }
      }

      return ret;
   }

   public List<Clan> getRegisteredClans(int castleId) {
      return this._registeredClans.get(castleId);
   }

   public void addDisguisedPlayer(int playerObjId) {
      this._disguisedPlayers.add(playerObjId);
   }

   public boolean isDisguised(int playerObjId) {
      return this._disguisedPlayers.contains(playerObjId);
   }

   public List<Integer> getRegisteredMercenaries(int castleId) {
      return this._registeredMercenaries.get(castleId);
   }

   public long getTWStartTimeInMillis() {
      return this._startTWDate.getTimeInMillis();
   }

   public Calendar getTWStart() {
      return this._startTWDate;
   }

   public void setTWStartTimeInMillis(long time) {
      this._startTWDate.setTimeInMillis(time);
      if (this._isTWInProgress) {
         if (this._scheduledEndTWTask != null) {
            this._scheduledEndTWTask.cancel(false);
         }

         this._scheduledEndTWTask = ThreadPool.schedule(new TerritoryWarManager.ScheduleEndTWTask(), 1000L);
      } else {
         if (this._scheduledStartTWTask != null) {
            this._scheduledStartTWTask.cancel(false);
         }

         this._scheduledStartTWTask = ThreadPool.schedule(new TerritoryWarManager.ScheduleStartTWTask(), 1000L);
      }
   }

   public boolean isTWChannelOpen() {
      return this._isTWChannelOpen;
   }

   public void registerClan(int castleId, Clan clan) {
      if (clan != null && (this._registeredClans.get(castleId) == null || !this._registeredClans.get(castleId).contains(clan))) {
         this._registeredClans.putIfAbsent(castleId, new CopyOnWriteArrayList<>());
         this._registeredClans.get(castleId).add(clan);
         this.changeRegistration(castleId, clan.getId(), false);
      }
   }

   public void registerMerc(int castleId, Player player) {
      if (player != null
         && player.getLevel() >= PLAYERMINLEVEL
         && (this._registeredMercenaries.get(castleId) == null || !this._registeredMercenaries.get(castleId).contains(player.getObjectId()))) {
         this._registeredMercenaries.putIfAbsent(castleId, new CopyOnWriteArrayList<>());
         this._registeredMercenaries.get(castleId).add(player.getObjectId());
         this.changeRegistration(castleId, player.getObjectId(), false);
      }
   }

   public void removeClan(int castleId, Clan clan) {
      if (clan != null && this._registeredClans.get(castleId) != null && this._registeredClans.get(castleId).remove(clan)) {
         this.changeRegistration(castleId, clan.getId(), true);
      }
   }

   public void removeMerc(int castleId, Player player) {
      if (player != null && this._registeredMercenaries.get(castleId) != null && this._registeredMercenaries.get(castleId).contains(player.getObjectId())) {
         this._registeredMercenaries.get(castleId).remove(this._registeredMercenaries.get(castleId).indexOf(player.getObjectId()));
         this.changeRegistration(castleId, player.getObjectId(), true);
      }
   }

   public boolean isRegistrationOver() {
      return this._isRegistrationOver;
   }

   public boolean isTWInProgress() {
      return this._isTWInProgress;
   }

   public void territoryCatapultDestroyed(int castleId) {
      if (this._territoryList.get(castleId) != null) {
         this._territoryList.get(castleId).changeNPCsSpawn(2, false);
      }

      for (Door door : CastleManager.getInstance().getCastleById(castleId).getDoors()) {
         door.openMe();
      }
   }

   public Npc addTerritoryWard(int territoryId, int newOwnerId, int oldOwnerId, boolean broadcastMessage) {
      Npc ret = null;
      TerritoryWarManager.Territory terNew = this._territoryList.get(newOwnerId);
      if (terNew != null) {
         TerritoryWarManager.TerritoryNPCSpawn ward = terNew.getFreeWardSpawnPlace();
         if (ward != null) {
            ward._npcId = territoryId;
            ret = this.spawnNPC(36491 + territoryId, ward.getLocation());
            ward.setNPC(ret);
            if (!this._isTWInProgress && !SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS) {
               ret.decayMe();
            }

            if (terNew.getOwnerClan() != null && terNew.getOwnedWardIds().contains(newOwnerId + 80)) {
               for (int wardId : terNew.getOwnedWardIds()) {
                  for (SkillLearn s : SkillTreeData.getInstance().getAvailableResidentialSkills(wardId)) {
                     Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
                     if (sk != null) {
                        for (Player member : terNew.getOwnerClan().getOnlineMembers(0)) {
                           if (!member.isInOlympiadMode()) {
                              member.addSkill(sk, false);
                           }
                        }
                     }
                  }
               }
            }
         }

         TerritoryWarManager.Territory terOld = this._territoryList.get(oldOwnerId);
         if (terOld != null) {
            terOld.removeWard(territoryId);
            this.updateTerritoryData(terOld);
            this.updateTerritoryData(terNew);
            if (broadcastMessage) {
               SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_S1_HAS_SUCCEEDED_IN_CAPTURING_S2_S_TERRITORY_WARD);
               sm.addString(terNew.getOwnerClan().getName());
               sm.addCastleId(territoryId);
               this.announceToParticipants(sm, 135000, 13500);
            }

            if (terOld.getOwnerClan() != null) {
               for (SkillLearn sx : SkillTreeData.getInstance().getAvailableResidentialSkills(territoryId)) {
                  Skill sk = SkillData.getInstance().getSkill(sx.getSkillId(), sx.getSkillLevel());
                  if (sk != null) {
                     for (Player memberx : terOld.getOwnerClan().getOnlineMembers(0)) {
                        memberx.removeSkill(sk, false);
                     }
                  }
               }

               if (!terOld.getOwnedWardIds().isEmpty() && !terOld.getOwnedWardIds().contains(oldOwnerId + 80)) {
                  for (int wardId : terOld.getOwnedWardIds()) {
                     for (SkillLearn sxx : SkillTreeData.getInstance().getAvailableResidentialSkills(wardId)) {
                        Skill sk = SkillData.getInstance().getSkill(sxx.getSkillId(), sxx.getSkillLevel());
                        if (sk != null) {
                           for (Player memberx : terOld.getOwnerClan().getOnlineMembers(0)) {
                              memberx.removeSkill(sk, false);
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         LOGGER.warning(this.getClass().getSimpleName() + ": Missing territory for new Ward owner: " + newOwnerId + ";" + territoryId);
      }

      return ret;
   }

   public SiegeFlag getHQForClan(Clan clan) {
      return clan.getCastleId() > 0 ? this._territoryList.get(clan.getCastleId()).getHQ() : null;
   }

   public SiegeFlag getHQForTerritory(int territoryId) {
      return this._territoryList.get(territoryId - 80).getHQ();
   }

   public void setHQForClan(Clan clan, SiegeFlag hq) {
      if (clan.getCastleId() > 0) {
         this._territoryList.get(clan.getCastleId()).setHQ(hq);
      }
   }

   public void addClanFlag(Clan clan, SiegeFlag flag) {
      this._clanFlags.put(clan, flag);
   }

   public boolean isClanHasFlag(Clan clan) {
      return this._clanFlags.containsKey(clan);
   }

   public SiegeFlag getFlagForClan(Clan clan) {
      return this._clanFlags.get(clan);
   }

   public void removeClanFlag(Clan clan) {
      this._clanFlags.remove(clan);
   }

   public Collection<TerritoryWard> getAllTerritoryWards() {
      return this._territoryWards;
   }

   public TerritoryWard getTerritoryWardForOwner(int castleId) {
      for (TerritoryWard twWard : this._territoryWards) {
         if (twWard.getTerritoryId() == castleId) {
            return twWard;
         }
      }

      return null;
   }

   public TerritoryWard getTerritoryWard(int territoryId) {
      for (TerritoryWard twWard : this._territoryWards) {
         if (twWard.getTerritoryId() == territoryId) {
            return twWard;
         }
      }

      return null;
   }

   public TerritoryWard getTerritoryWard(Player player) {
      for (TerritoryWard twWard : this._territoryWards) {
         if (twWard.playerId == player.getObjectId()) {
            return twWard;
         }
      }

      return null;
   }

   public void dropCombatFlag(Player player, boolean isKilled, boolean isSpawnBack) {
      for (TerritoryWard twWard : this._territoryWards) {
         if (twWard.playerId == player.getObjectId()) {
            twWard.dropIt();
            if (this._isTWInProgress) {
               if (isKilled) {
                  twWard.spawnMe();
               } else if (isSpawnBack) {
                  twWard.spawnBack();
               } else {
                  for (TerritoryWarManager.TerritoryNPCSpawn wardSpawn : this._territoryList.get(twWard.getOwnerCastleId()).getOwnedWard()) {
                     if (wardSpawn.getId() == twWard.getTerritoryId()) {
                        wardSpawn.setNPC(wardSpawn.getNpc().getSpawn().doSpawn(false));
                        twWard.unSpawnMe();
                        twWard.setNpc(wardSpawn.getNpc());
                     }
                  }
               }
            }

            if (isKilled) {
               SystemMessage sm = new SystemMessage(SystemMessageId.THE_CHARACTER_THAT_ACQUIRED_S1_WARD_HAS_BEEN_KILLED);
               sm.addString(twWard.getNpc().getName().replace(" Ward", ""));
               this.announceToParticipants(sm, 0, 0);
            }
         }
      }
   }

   public void giveTWQuestPoint(Player player) {
      this._participantPoints.putIfAbsent(player.getObjectId(), new Integer[]{player.getSiegeSide(), 0, 0, 0, 0, 0, 0});
      Integer[] var2 = this._participantPoints.get(player.getObjectId());
      Integer var3 = var2[2];
      var2[2] = var2[2] + 1;
   }

   public void giveTWPoint(Player killer, int victimSide, int type) {
      if (victimSide != 0) {
         if (killer.getParty() != null && type < 5) {
            for (Player pl : killer.getParty().getMembers()) {
               if (pl.getSiegeSide() != victimSide && pl.getSiegeSide() != 0 && LocationUtil.checkIfInRange(2000, killer, pl, false)) {
                  this._participantPoints.putIfAbsent(pl.getObjectId(), new Integer[]{pl.getSiegeSide(), 0, 0, 0, 0, 0, 0});
                  Integer[] var10 = this._participantPoints.get(pl.getObjectId());
                  Integer var8 = var10[type];
                  var10[type] = var10[type] + 1;
               }
            }
         } else {
            if (killer.getSiegeSide() == victimSide || killer.getSiegeSide() == 0) {
               return;
            }

            this._participantPoints.putIfAbsent(killer.getObjectId(), new Integer[]{killer.getSiegeSide(), 0, 0, 0, 0, 0, 0});
            Integer[] var4 = this._participantPoints.get(killer.getObjectId());
            Integer var6 = var4[type];
            var4[type] = var4[type] + 1;
         }
      }
   }

   public int[] calcReward(Player player) {
      if (this._participantPoints.containsKey(player.getObjectId())) {
         int[] reward = new int[2];
         Integer[] temp = this._participantPoints.get(player.getObjectId());
         reward[0] = temp[0];
         reward[1] = 0;
         if (temp[6] < 10) {
            return reward;
         } else {
            reward[1] += temp[6] > 70 ? 7 : (int)(temp[6].intValue() * 0.1);
            reward[1] += temp[2] * 7;
            if (temp[1] < 50) {
               reward[1] = (int)(reward[1] + temp[1].intValue() * 0.1);
            } else if (temp[1] < 120) {
               reward[1] += 5 + (temp[1] - 50) / 14;
            } else {
               reward[1] += 10;
            }

            reward[1] += temp[3];
            reward[1] += temp[4] * 2;
            reward[1] += temp[5] > 0 ? 5 : 0;
            reward[1] += Math.min(this._territoryList.get(temp[0] - 80).getQuestDone()[0], 10);
            reward[1] += this._territoryList.get(temp[0] - 80).getQuestDone()[1];
            reward[1] += this._territoryList.get(temp[0] - 80).getOwnedWardIds().size();
            return reward;
         }
      } else {
         return new int[]{0, 0};
      }
   }

   public void debugReward(Player player) {
      player.sendMessage("Registred TerrId: " + player.getSiegeSide());
      if (this._participantPoints.containsKey(player.getObjectId())) {
         Integer[] temp = this._participantPoints.get(player.getObjectId());
         player.sendMessage("TerrId: " + temp[0]);
         player.sendMessage("PcKill: " + temp[1]);
         player.sendMessage("PcQuests: " + temp[2]);
         player.sendMessage("npcKill: " + temp[3]);
         player.sendMessage("CatatKill: " + temp[4]);
         player.sendMessage("WardKill: " + temp[5]);
         player.sendMessage("onlineTime: " + temp[6]);
      } else {
         player.sendMessage("No points for you!");
      }

      if (this._territoryList.containsKey(player.getSiegeSide() - 80)) {
         player.sendMessage("Your Territory's jobs:");
         player.sendMessage("npcKill: " + this._territoryList.get(player.getSiegeSide() - 80).getQuestDone()[0]);
         player.sendMessage("WardCaptured: " + this._territoryList.get(player.getSiegeSide() - 80).getQuestDone()[1]);
      }
   }

   public void resetReward(Player player) {
      if (this._participantPoints.containsKey(player.getObjectId())) {
         this._participantPoints.get(player.getObjectId())[6] = 0;
      }
   }

   public Npc spawnNPC(int npcId, Location loc) {
      try {
         Spawn spawnDat = new Spawn(npcId);
         spawnDat.setAmount(1);
         spawnDat.setXYZ(loc);
         spawnDat.setHeading(loc.getHeading());
         spawnDat.stopRespawn();
         return spawnDat.doSpawn(false);
      } catch (Exception var4) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": " + var4.getMessage(), (Throwable)var4);
         return null;
      }
   }

   private void changeRegistration(int castleId, int objId, boolean delete) {
      String query = delete
         ? "DELETE FROM territory_registrations WHERE castleId = ? and registeredId = ?"
         : "INSERT INTO territory_registrations (castleId, registeredId) values (?, ?)";

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(query);
      ) {
         ps.setInt(1, castleId);
         ps.setInt(2, objId);
         ps.execute();
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Exception: Territory War registration: " + var13.getMessage(), (Throwable)var13);
      }
   }

   private void updateTerritoryData(TerritoryWarManager.Territory ter) {
      StringBuilder wardList = new StringBuilder();

      for (int i : ter.getOwnedWardIds()) {
         wardList.append(i + ";");
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE territories SET ownedWardIds=? WHERE territoryId=?");
      ) {
         ps.setString(1, wardList.toString());
         ps.setInt(2, ter.getTerritoryId());
         ps.execute();
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, "Exception: Territory Data update: " + var11.getMessage(), (Throwable)var11);
      }
   }

   private void load() {
      ConfigReader territoryWarConfig = new ConfigReader("./config/TerritoryWar.ini");
      DEFENDERMAXCLANS = territoryWarConfig.getInt("DefenderMaxClans", 500);
      DEFENDERMAXPLAYERS = territoryWarConfig.getInt("DefenderMaxPlayers", 500);
      CLANMINLEVEL = territoryWarConfig.getInt("ClanMinLevel", 0);
      PLAYERMINLEVEL = territoryWarConfig.getInt("PlayerMinLevel", 40);
      WARLENGTH = territoryWarConfig.getLong("WarLength", 120L) * 60000L;
      PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE = territoryWarConfig.getBoolean("PlayerWithWardCanBeKilledInPeaceZone", false);
      SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS = territoryWarConfig.getBoolean("SpawnWardsWhenTWIsNotInProgress", false);
      RETURN_WARDS_WHEN_TW_STARTS = territoryWarConfig.getBoolean("ReturnWardsWhenTWStarts", false);
      MINTWBADGEFORNOBLESS = territoryWarConfig.getInt("MinTerritoryBadgeForNobless", 100);
      MINTWBADGEFORSTRIDERS = territoryWarConfig.getInt("MinTerritoryBadgeForStriders", 50);
      MINTWBADGEFORBIGSTRIDER = territoryWarConfig.getInt("MinTerritoryBadgeForBigStrider", 80);

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM territory_spawnlist");
      ) {
         while (rs.next()) {
            int castleId = rs.getInt("castleId");
            int npcId = rs.getInt("npcId");
            Location loc = new Location(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("heading"));
            int spawnType = rs.getInt("spawnType");
            this._territoryList.computeIfAbsent(castleId, k -> new TerritoryWarManager.Territory(castleId));
            switch (spawnType) {
               case 0:
               case 1:
               case 2:
                  this._territoryList.get(castleId).getSpawnList().add(new TerritoryWarManager.TerritoryNPCSpawn(castleId, loc, npcId, spawnType, null));
                  break;
               case 3:
                  this._territoryList.get(castleId).addWardSpawnPlace(loc);
                  break;
               default:
                  LOGGER.warning(this.getClass().getSimpleName() + ": Unknown npc type for " + rs.getInt("id"));
            }
         }
      } catch (Exception var33) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": SpawnList error: " + var33.getMessage(), (Throwable)var33);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM territories");
      ) {
         while (rs.next()) {
            int castleId = rs.getInt("castleId");
            int fortId = rs.getInt("fortId");
            String ownedWardIds = rs.getString("OwnedWardIds");
            TerritoryWarManager.Territory t = this._territoryList.get(castleId);
            if (t != null) {
               t._fortId = fortId;
               if (CastleManager.getInstance().getCastleById(castleId).getOwnerId() > 0) {
                  t.setOwnerClan(ClanTable.getInstance().getClan(CastleManager.getInstance().getCastleById(castleId).getOwnerId()));
                  t.changeNPCsSpawn(0, true);
               }

               if (!ownedWardIds.isEmpty()) {
                  for (String wardId : ownedWardIds.split(";")) {
                     if (Integer.parseInt(wardId) > 0) {
                        this.addTerritoryWard(Integer.parseInt(wardId), castleId, 0, false);
                     }
                  }
               }
            }
         }
      } catch (Exception var29) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": territory list error(): " + var29.getMessage(), (Throwable)var29);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM territory_registrations");
      ) {
         while (rs.next()) {
            int castleId = rs.getInt("castleId");
            int registeredId = rs.getInt("registeredId");
            if (ClanTable.getInstance().getClan(registeredId) != null) {
               if (this._registeredClans.get(castleId) == null) {
                  this._registeredClans.putIfAbsent(castleId, new CopyOnWriteArrayList<>());
               }

               this._registeredClans.get(castleId).add(ClanTable.getInstance().getClan(registeredId));
            } else {
               if (this._registeredMercenaries.get(castleId) == null) {
                  this._registeredMercenaries.put(castleId, new CopyOnWriteArrayList<>());
               }

               this._registeredMercenaries.get(castleId).add(registeredId);
            }
         }
      } catch (Exception var25) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": registration list error: " + var25.getMessage(), (Throwable)var25);
      }
   }

   protected void startTerritoryWar() {
      if (this._territoryList == null) {
         LOGGER.warning(this.getClass().getSimpleName() + ": TerritoryList is NULL!");
      } else {
         List<TerritoryWarManager.Territory> activeTerritoryList = new ArrayList<>();

         for (TerritoryWarManager.Territory t : this._territoryList.values()) {
            Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
            if (castle != null) {
               if (castle.getOwnerId() > 0) {
                  activeTerritoryList.add(t);
               }
            } else {
               LOGGER.warning(this.getClass().getSimpleName() + ": Castle missing! CastleId: " + t.getCastleId());
            }
         }

         if (activeTerritoryList.size() >= 2) {
            this._isTWInProgress = true;
            this.updatePlayerTWStateFlags(false);

            for (TerritoryWarManager.Territory tx : activeTerritoryList) {
               Castle castle = CastleManager.getInstance().getCastleById(tx.getCastleId());
               Fort fort = FortManager.getInstance().getFortById(tx.getFortId());
               if (castle != null) {
                  tx.changeNPCsSpawn(2, true);
                  castle.spawnDoor();
                  castle.getZone().setSiegeInstance(this);
                  castle.getZone().setActive(true);
                  castle.getZone().updateZoneStatusForCharactersInside();
               } else {
                  LOGGER.warning(this.getClass().getSimpleName() + ": Castle missing! CastleId: " + tx.getCastleId());
               }

               if (fort != null) {
                  tx.changeNPCsSpawn(1, true);
                  fort.resetDoors();
                  fort.getZone().setSiegeInstance(this);
                  fort.getZone().setActive(true);
                  fort.getZone().updateZoneStatusForCharactersInside();
               } else {
                  LOGGER.warning(this.getClass().getSimpleName() + ": Fort missing! FortId: " + tx.getFortId());
               }

               for (TerritoryWarManager.TerritoryNPCSpawn ward : tx.getOwnedWard()) {
                  if (ward.getNpc() != null && tx.getOwnerClan() != null) {
                     if (!ward.getNpc().isSpawned()) {
                        ward.setNPC(ward.getNpc().getSpawn().doSpawn(false));
                     }

                     this._territoryWards
                        .add(
                           new TerritoryWard(
                              ward.getId(),
                              ward.getLocation().getX(),
                              ward.getLocation().getY(),
                              ward.getLocation().getZ(),
                              0,
                              ward.getId() + 13479,
                              tx.getCastleId(),
                              ward.getNpc()
                           )
                        );
                  }
               }

               tx.getQuestDone()[0] = 0;
               tx.getQuestDone()[1] = 0;
            }

            this._participantPoints.clear();
            if (RETURN_WARDS_WHEN_TW_STARTS) {
               for (TerritoryWard wardx : this._territoryWards) {
                  if (wardx.getOwnerCastleId() != wardx.getTerritoryId() - 80) {
                     wardx.unSpawnMe();
                     wardx.setNpc(this.addTerritoryWard(wardx.getTerritoryId(), wardx.getTerritoryId() - 80, wardx.getOwnerCastleId(), false));
                     wardx.setOwnerCastleId(wardx.getTerritoryId() - 80);
                  }
               }
            }

            World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.TERRITORY_WAR_HAS_BEGUN));
         }
      }
   }

   protected void endTerritoryWar() {
      this._isTWInProgress = false;
      if (this._territoryList == null) {
         LOGGER.warning(this.getClass().getSimpleName() + ": TerritoryList is NULL!");
      } else {
         List<TerritoryWarManager.Territory> activeTerritoryList = new ArrayList<>();

         for (TerritoryWarManager.Territory t : this._territoryList.values()) {
            Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
            if (castle != null) {
               if (castle.getOwnerId() > 0) {
                  activeTerritoryList.add(t);
               }
            } else {
               LOGGER.warning(this.getClass().getSimpleName() + ": Castle missing! CastleId: " + t.getCastleId());
            }
         }

         this.updatePlayerTWStateFlags(true);

         for (TerritoryWard twWard : this._territoryWards) {
            twWard.unSpawnMe();
         }

         this._territoryWards.clear();
         if (activeTerritoryList.size() > 1) {
            for (TerritoryWarManager.Territory tx : activeTerritoryList) {
               Castle castle = CastleManager.getInstance().getCastleById(tx.getCastleId());
               Fort fort = FortManager.getInstance().getFortById(tx.getFortId());
               if (castle != null) {
                  castle.spawnDoor();
                  tx.changeNPCsSpawn(2, false);
                  castle.getZone().setActive(false);
                  castle.getZone().updateZoneStatusForCharactersInside();
                  castle.getZone().setSiegeInstance(null);
               } else {
                  LOGGER.warning(this.getClass().getSimpleName() + ": Castle missing! CastleId: " + tx.getCastleId());
               }

               if (fort != null) {
                  tx.changeNPCsSpawn(1, false);
                  fort.getZone().setActive(false);
                  fort.getZone().updateZoneStatusForCharactersInside();
                  fort.getZone().setSiegeInstance(null);
               } else {
                  LOGGER.warning(this.getClass().getSimpleName() + ": Fort missing! FortId: " + tx.getFortId());
               }

               if (tx.getHQ() != null) {
                  tx.getHQ().deleteMe();
               }

               for (TerritoryWarManager.TerritoryNPCSpawn ward : tx.getOwnedWard()) {
                  if (ward.getNpc() != null) {
                     if (!ward.getNpc().isSpawned() && SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS) {
                        ward.setNPC(ward.getNpc().getSpawn().doSpawn(false));
                     } else if (ward.getNpc().isSpawned() && !SPAWN_WARDS_WHEN_TW_IS_NOT_IN_PROGRESS) {
                        ward.getNpc().decayMe();
                     }
                  }
               }
            }
         }

         for (SiegeFlag flag : this._clanFlags.values()) {
            flag.deleteMe();
         }

         this._clanFlags.clear();

         for (Integer castleId : this._registeredClans.keySet()) {
            for (Clan clan : this._registeredClans.get(castleId)) {
               this.changeRegistration(castleId, clan.getId(), true);
            }
         }

         for (Integer castleId : this._registeredMercenaries.keySet()) {
            for (Integer pl_objId : this._registeredMercenaries.get(castleId)) {
               this.changeRegistration(castleId, pl_objId, true);
            }
         }

         this.setNextTWDate();
         World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.TERRITORY_WAR_HAS_ENDED));
      }
   }

   public void setNextTWDate() {
      Calendar cal = Calendar.getInstance();
      long nextSiegeDate = GlobalVariablesManager.getInstance().getLong(GLOBAL_VARIABLE, 0L);
      if (nextSiegeDate > System.currentTimeMillis()) {
         cal.setTimeInMillis(nextSiegeDate);
      } else {
         if (cal.before(Calendar.getInstance())) {
            cal.setTimeInMillis(System.currentTimeMillis());
         }

         boolean hasOwnedCastle = CastleManager.getInstance().hasOwnedCastle();
         cal.set(7, hasOwnedCastle ? 7 : 1);
         cal.set(11, hasOwnedCastle ? 20 : 22);
         cal.set(12, 0);
         cal.set(13, 0);
         if (cal.before(Calendar.getInstance())) {
            cal.add(3, 2);
         }

         GlobalVariablesManager.getInstance().set(GLOBAL_VARIABLE, cal.getTimeInMillis());
      }

      this.setTWStartTimeInMillis(cal.getTimeInMillis());
   }

   protected void updatePlayerTWStateFlags(boolean clear) {
      Quest twQuest = ScriptManager.getInstance().getScript(qn);
      if (twQuest != null) {
         twQuest.setOnEnterWorld(this._isTWInProgress);
      }

      for (int castleId : this._registeredClans.keySet()) {
         for (Clan clan : this._registeredClans.get(castleId)) {
            for (Player player : clan.getOnlineMembers(0)) {
               if (clear) {
                  player.setSiegeState((byte)0);
                  if (!this._isTWChannelOpen) {
                     player.setSiegeSide(0);
                  }
               } else {
                  if (player.getLevel() < PLAYERMINLEVEL || player.getPlayerClass().level() < 2) {
                     continue;
                  }

                  if (this._isTWInProgress) {
                     player.setSiegeState((byte)1);
                  }

                  player.setSiegeSide(80 + castleId);
               }

               player.broadcastUserInfo();
            }
         }
      }

      for (int castleId : this._registeredMercenaries.keySet()) {
         for (int objId : this._registeredMercenaries.get(castleId)) {
            Player player = World.getPlayer(objId);
            if (player != null) {
               if (clear) {
                  player.setSiegeState((byte)0);
                  if (!this._isTWChannelOpen) {
                     player.setSiegeSide(0);
                  }
               } else {
                  if (this._isTWInProgress) {
                     player.setSiegeState((byte)1);
                  }

                  player.setSiegeSide(80 + castleId);
               }

               player.broadcastUserInfo();
            }
         }
      }

      for (TerritoryWarManager.Territory terr : this._territoryList.values()) {
         if (terr.getOwnerClan() != null) {
            for (Player player : terr.getOwnerClan().getOnlineMembers(0)) {
               if (player != null) {
                  if (clear) {
                     player.setSiegeState((byte)0);
                     if (!this._isTWChannelOpen) {
                        player.setSiegeSide(0);
                     }
                  } else {
                     if (player.getLevel() < PLAYERMINLEVEL || player.getPlayerClass().level() < 2) {
                        continue;
                     }

                     if (this._isTWInProgress) {
                        player.setSiegeState((byte)1);
                     }

                     player.setSiegeSide(80 + terr.getCastleId());
                  }

                  player.broadcastUserInfo();
               }
            }
         }
      }
   }

   public void announceToParticipants(ServerPacket sm, int exp, int sp) {
      for (TerritoryWarManager.Territory ter : this._territoryList.values()) {
         if (ter.getOwnerClan() != null) {
            for (Player member : ter.getOwnerClan().getOnlineMembers(0)) {
               member.sendPacket(sm);
               if (exp > 0 || sp > 0) {
                  member.addExpAndSp(exp, sp);
               }
            }
         }
      }

      for (List<Clan> list : this._registeredClans.values()) {
         for (Clan c : list) {
            for (Player memberx : c.getOnlineMembers(0)) {
               memberx.sendPacket(sm);
               if (exp > 0 || sp > 0) {
                  memberx.addExpAndSp(exp, sp);
               }
            }
         }
      }

      for (List<Integer> list : this._registeredMercenaries.values()) {
         for (int objId : list) {
            Player player = World.getPlayer(objId);
            if (player != null && (player.getClan() == null || !this.checkIsRegistered(-1, player.getClan()))) {
               player.sendPacket(sm);
               if (exp > 0 || sp > 0) {
                  player.addExpAndSp(exp, sp);
               }
            }
         }
      }
   }

   @Override
   public void startSiege() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void endSiege() {
      throw new UnsupportedOperationException();
   }

   @Override
   public SiegeClan getAttackerClan(int clanId) {
      throw new UnsupportedOperationException();
   }

   @Override
   public SiegeClan getAttackerClan(Clan clan) {
      throw new UnsupportedOperationException();
   }

   public List<SiegeClan> getAttackerClans() {
      throw new UnsupportedOperationException();
   }

   @Override
   public List<Player> getAttackersInZone() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean checkIsAttacker(Clan clan) {
      throw new UnsupportedOperationException();
   }

   @Override
   public SiegeClan getDefenderClan(int clanId) {
      throw new UnsupportedOperationException();
   }

   @Override
   public SiegeClan getDefenderClan(Clan clan) {
      throw new UnsupportedOperationException();
   }

   public List<SiegeClan> getDefenderClans() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean checkIsDefender(Clan clan) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Set<Npc> getFlag(Clan clan) {
      throw new UnsupportedOperationException();
   }

   @Override
   public Calendar getSiegeDate() {
      throw new UnsupportedOperationException();
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

   public static TerritoryWarManager getInstance() {
      return TerritoryWarManager.SingletonHolder.INSTANCE;
   }

   static {
      TERRITORY_ITEM_IDS.put(81, 13757);
      TERRITORY_ITEM_IDS.put(82, 13758);
      TERRITORY_ITEM_IDS.put(83, 13759);
      TERRITORY_ITEM_IDS.put(84, 13760);
      TERRITORY_ITEM_IDS.put(85, 13761);
      TERRITORY_ITEM_IDS.put(86, 13762);
      TERRITORY_ITEM_IDS.put(87, 13763);
      TERRITORY_ITEM_IDS.put(88, 13764);
      TERRITORY_ITEM_IDS.put(89, 13765);
   }

   protected class RewardOnlineParticipants implements Runnable {
      protected RewardOnlineParticipants() {
         Objects.requireNonNull(TerritoryWarManager.this);
         super();
      }

      @Override
      public void run() {
         if (TerritoryWarManager.this._isTWInProgress) {
            for (Player player : World.getPlayers()) {
               if (player != null && player.getSiegeSide() > 0) {
                  TerritoryWarManager.this.giveTWPoint(player, 1000, 6);
               }
            }
         } else {
            TerritoryWarManager.this._scheduledRewardOnlineTask.cancel(false);
         }
      }
   }

   private class ScheduleEndTWTask implements Runnable {
      private final Logger LOGGER;

      protected ScheduleEndTWTask() {
         Objects.requireNonNull(TerritoryWarManager.this);
         super();
         this.LOGGER = Logger.getLogger(TerritoryWarManager.ScheduleEndTWTask.class.getName());
      }

      @Override
      public void run() {
         try {
            TerritoryWarManager.this._scheduledEndTWTask.cancel(false);
            long timeRemaining = TerritoryWarManager.this._startTWDate.getTimeInMillis() + TerritoryWarManager.WARLENGTH - System.currentTimeMillis();
            if (timeRemaining > 3600000L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_HOUR_S);
               sm.addInt(2);
               TerritoryWarManager.this.announceToParticipants(sm, 0, 0);
               TerritoryWarManager.this._scheduledEndTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleEndTWTask(), timeRemaining - 3600000L);
            } else if (timeRemaining <= 3600000L && timeRemaining > 600000L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTE_S);
               sm.addInt((int)(timeRemaining / 60000L));
               TerritoryWarManager.this.announceToParticipants(sm, 0, 0);
               TerritoryWarManager.this._scheduledEndTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleEndTWTask(), timeRemaining - 600000L);
            } else if (timeRemaining <= 600000L && timeRemaining > 300000L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTE_S);
               sm.addInt((int)(timeRemaining / 60000L));
               TerritoryWarManager.this.announceToParticipants(sm, 0, 0);
               TerritoryWarManager.this._scheduledEndTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleEndTWTask(), timeRemaining - 300000L);
            } else if (timeRemaining <= 300000L && timeRemaining > 10000L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTE_S);
               sm.addInt((int)(timeRemaining / 60000L));
               TerritoryWarManager.this.announceToParticipants(sm, 0, 0);
               TerritoryWarManager.this._scheduledEndTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleEndTWTask(), timeRemaining - 10000L);
            } else if (timeRemaining <= 10000L && timeRemaining > 0L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.S1_SECOND_S_TO_THE_END_OF_TERRITORY_WAR);
               sm.addInt((int)(timeRemaining / 1000L));
               TerritoryWarManager.this.announceToParticipants(sm, 0, 0);
               TerritoryWarManager.this._scheduledEndTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleEndTWTask(), timeRemaining);
            } else {
               TerritoryWarManager.this.endTerritoryWar();
               ThreadPool.schedule(TerritoryWarManager.this.new closeTerritoryChannelTask(), 600000L);
            }
         } catch (Exception var4) {
            this.LOGGER.log(Level.SEVERE, "", (Throwable)var4);
         }
      }
   }

   protected class ScheduleStartTWTask implements Runnable {
      private final Logger LOGGER;

      protected ScheduleStartTWTask() {
         Objects.requireNonNull(TerritoryWarManager.this);
         super();
         this.LOGGER = Logger.getLogger(TerritoryWarManager.ScheduleStartTWTask.class.getName());
      }

      @Override
      public void run() {
         TerritoryWarManager.this._scheduledStartTWTask.cancel(false);

         try {
            long timeRemaining = TerritoryWarManager.this._startTWDate.getTimeInMillis() - System.currentTimeMillis();
            if (timeRemaining > 7200000L) {
               TerritoryWarManager.this._isRegistrationOver = false;
               TerritoryWarManager.this._scheduledStartTWTask = ThreadPool.schedule(
                  TerritoryWarManager.this.new ScheduleStartTWTask(), timeRemaining - 7200000L
               );
            } else if (timeRemaining <= 7200000L && timeRemaining > 1200000L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_REQUEST_PERIOD_HAS_ENDED);
               World.broadcastToAllOnlinePlayers(sm);
               TerritoryWarManager.this._isRegistrationOver = true;
               TerritoryWarManager.this._scheduledStartTWTask = ThreadPool.schedule(
                  TerritoryWarManager.this.new ScheduleStartTWTask(), timeRemaining - 1200000L
               );
            } else if (timeRemaining <= 1200000L && timeRemaining > 600000L) {
               SystemMessage sm = new SystemMessage(
                  SystemMessageId.THE_TERRITORY_WAR_WILL_BEGIN_IN_20_MINUTES_TERRITORY_RELATED_FUNCTIONS_IE_BATTLEFIELD_CHANNEL_DISGUISE_SCROLLS_TRANSFORMATIONS_ETC_CAN_NOW_BE_USED
               );
               World.broadcastToAllOnlinePlayers(sm);
               TerritoryWarManager.this._isTWChannelOpen = true;
               TerritoryWarManager.this._isRegistrationOver = true;
               TerritoryWarManager.this.updatePlayerTWStateFlags(false);
               TerritoryWarManager.this._scheduledStartTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleStartTWTask(), timeRemaining - 600000L);
            } else if (timeRemaining <= 600000L && timeRemaining > 300000L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_10_MINUTES);
               World.broadcastToAllOnlinePlayers(sm);
               TerritoryWarManager.this._isTWChannelOpen = true;
               TerritoryWarManager.this._isRegistrationOver = true;
               TerritoryWarManager.this.updatePlayerTWStateFlags(false);
               TerritoryWarManager.this._scheduledStartTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleStartTWTask(), timeRemaining - 300000L);
            } else if (timeRemaining <= 300000L && timeRemaining > 60000L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_5_MINUTES);
               World.broadcastToAllOnlinePlayers(sm);
               TerritoryWarManager.this._isTWChannelOpen = true;
               TerritoryWarManager.this._isRegistrationOver = true;
               TerritoryWarManager.this.updatePlayerTWStateFlags(false);
               TerritoryWarManager.this._scheduledStartTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleStartTWTask(), timeRemaining - 60000L);
            } else if (timeRemaining <= 60000L && timeRemaining > 0L) {
               SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_1_MINUTE);
               World.broadcastToAllOnlinePlayers(sm);
               TerritoryWarManager.this._isTWChannelOpen = true;
               TerritoryWarManager.this._isRegistrationOver = true;
               TerritoryWarManager.this.updatePlayerTWStateFlags(false);
               TerritoryWarManager.this._scheduledStartTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleStartTWTask(), timeRemaining);
            } else if (timeRemaining + TerritoryWarManager.WARLENGTH > 0L) {
               TerritoryWarManager.this._isTWChannelOpen = true;
               TerritoryWarManager.this._isRegistrationOver = true;
               TerritoryWarManager.this.startTerritoryWar();
               TerritoryWarManager.this._scheduledEndTWTask = ThreadPool.schedule(TerritoryWarManager.this.new ScheduleEndTWTask(), 1000L);
               TerritoryWarManager.this._scheduledRewardOnlineTask = ThreadPool.scheduleAtFixedRate(
                  TerritoryWarManager.this.new RewardOnlineParticipants(), 60000L, 60000L
               );
            }
         } catch (Exception var4) {
            this.LOGGER.log(Level.SEVERE, "", (Throwable)var4);
         }
      }
   }

   private static class SingletonHolder {
      protected static final TerritoryWarManager INSTANCE = new TerritoryWarManager();
   }

   public class Territory {
      private final Logger LOGGER;
      private final int _territoryId;
      private final int _castleId;
      protected int _fortId;
      private Clan _ownerClan;
      private final List<TerritoryWarManager.TerritoryNPCSpawn> _spawnList;
      private final TerritoryWarManager.TerritoryNPCSpawn[] _territoryWardSpawnPlaces;
      private boolean _isInProgress;
      private SiegeFlag _territoryHQ;
      private final int[] _questDone;

      public Territory(int castleId) {
         Objects.requireNonNull(TerritoryWarManager.this);
         super();
         this.LOGGER = Logger.getLogger(TerritoryWarManager.Territory.class.getName());
         this._spawnList = new ArrayList<>();
         this._isInProgress = false;
         this._territoryHQ = null;
         this._castleId = castleId;
         this._territoryId = castleId + 80;
         this._territoryWardSpawnPlaces = new TerritoryWarManager.TerritoryNPCSpawn[9];
         this._questDone = new int[2];
      }

      protected void addWardSpawnPlace(Location loc) {
         for (int i = 0; i < this._territoryWardSpawnPlaces.length; i++) {
            if (this._territoryWardSpawnPlaces[i] == null) {
               this._territoryWardSpawnPlaces[i] = new TerritoryWarManager.TerritoryNPCSpawn(this._castleId, loc, 0, 4, null);
               return;
            }
         }
      }

      protected TerritoryWarManager.TerritoryNPCSpawn getFreeWardSpawnPlace() {
         for (TerritoryWarManager.TerritoryNPCSpawn _territoryWardSpawnPlace : this._territoryWardSpawnPlaces) {
            if (_territoryWardSpawnPlace != null && _territoryWardSpawnPlace.getNpc() == null) {
               return _territoryWardSpawnPlace;
            }
         }

         this.LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": no free Ward spawn found for territory: " + this._territoryId);

         for (int i = 0; i < this._territoryWardSpawnPlaces.length; i++) {
            if (this._territoryWardSpawnPlaces[i] == null) {
               this.LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": territory ward spawn place " + i + " is null!");
            } else if (this._territoryWardSpawnPlaces[i].getNpc() != null) {
               this.LOGGER
                  .log(
                     Level.WARNING,
                     this.getClass().getSimpleName()
                        + ": territory ward spawn place "
                        + i
                        + " has npc name: "
                        + this._territoryWardSpawnPlaces[i].getNpc().getName()
                  );
            } else {
               this.LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": territory ward spawn place " + i + " is empty!");
            }
         }

         return null;
      }

      public List<TerritoryWarManager.TerritoryNPCSpawn> getSpawnList() {
         return this._spawnList;
      }

      protected void changeNPCsSpawn(int type, boolean isSpawn) {
         if (type >= 0 && type <= 3) {
            for (TerritoryWarManager.TerritoryNPCSpawn twSpawn : this._spawnList) {
               if (twSpawn.getType() == type) {
                  if (isSpawn) {
                     twSpawn.setNPC(TerritoryWarManager.this.spawnNPC(twSpawn.getId(), twSpawn.getLocation()));
                  } else {
                     Npc npc = twSpawn.getNpc();
                     if (npc != null && !npc.isDead()) {
                        npc.deleteMe();
                     }

                     twSpawn.setNPC(null);
                  }
               }
            }
         } else {
            this.LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": wrong type(" + type + ") for NPCs spawn change!");
         }
      }

      protected void removeWard(int wardId) {
         for (TerritoryWarManager.TerritoryNPCSpawn wardSpawn : this._territoryWardSpawnPlaces) {
            if (wardSpawn.getId() == wardId) {
               wardSpawn.getNpc().deleteMe();
               wardSpawn.setNPC(null);
               wardSpawn._npcId = 0;
               return;
            }
         }

         this.LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Can't delete wardId: " + wardId + " for territory: " + this._territoryId);
      }

      public int getTerritoryId() {
         return this._territoryId;
      }

      public int getCastleId() {
         return this._castleId;
      }

      public int getFortId() {
         return this._fortId;
      }

      public Clan getOwnerClan() {
         return this._ownerClan;
      }

      public void setOwnerClan(Clan newOwner) {
         this._ownerClan = newOwner;
      }

      public void setHQ(SiegeFlag hq) {
         this._territoryHQ = hq;
      }

      public SiegeFlag getHQ() {
         return this._territoryHQ;
      }

      public TerritoryWarManager.TerritoryNPCSpawn[] getOwnedWard() {
         return this._territoryWardSpawnPlaces;
      }

      public int[] getQuestDone() {
         return this._questDone;
      }

      public List<Integer> getOwnedWardIds() {
         List<Integer> ret = new ArrayList<>();

         for (TerritoryWarManager.TerritoryNPCSpawn wardSpawn : this._territoryWardSpawnPlaces) {
            if (wardSpawn.getId() > 0) {
               ret.add(wardSpawn.getId());
            }
         }

         return ret;
      }

      public boolean isInProgress() {
         return this._isInProgress;
      }

      public void setInProgress(boolean value) {
         this._isInProgress = value;
      }
   }

   public static class TerritoryNPCSpawn {
      private final Location _location;
      protected int _npcId;
      private final int _castleId;
      private final int _type;
      private Npc _npc;

      public TerritoryNPCSpawn(int castleId, Location loc, int npcId, int type, Npc npc) {
         this._castleId = castleId;
         this._location = loc;
         this._npcId = npcId;
         this._type = type;
         this._npc = npc;
      }

      public int getCastleId() {
         return this._castleId;
      }

      public int getId() {
         return this._npcId;
      }

      public int getType() {
         return this._type;
      }

      public void setNPC(Npc npc) {
         if (this._npc != null) {
            this._npc.deleteMe();
         }

         this._npc = npc;
      }

      public Npc getNpc() {
         return this._npc;
      }

      public Location getLocation() {
         return this._location;
      }
   }

   private class closeTerritoryChannelTask implements Runnable {
      public closeTerritoryChannelTask() {
         Objects.requireNonNull(TerritoryWarManager.this);
         super();
      }

      @Override
      public void run() {
         TerritoryWarManager.this._isTWChannelOpen = false;
         TerritoryWarManager.this._disguisedPlayers.clear();
         TerritoryWarManager.this.updatePlayerTWStateFlags(true);
      }
   }
}
