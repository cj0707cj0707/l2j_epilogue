package org.l2jmobius.gameserver.mechanics.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.StaticObjectData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.residences.AbstractResidence;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.type.FortZone;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Fort extends AbstractResidence {
   protected static final Logger LOGGER = Logger.getLogger(Fort.class.getName());
   private final List<Door> _doors = new ArrayList<>();
   private StaticObject _flagPole = null;
   private FortSiege _siege = null;
   private Calendar _siegeDate;
   private Calendar _lastOwnedTime;
   private SiegeZone _zone;
   private Clan _fortOwner = null;
   private int _fortType = 0;
   private int _state = 0;
   private int _castleId = 0;
   private int _supplyLeveL = 0;
   private final Map<Integer, Fort.FortFunction> _function;
   private final ScheduledFuture<?>[] _fortUpdater = new ScheduledFuture[2];
   private boolean _isSuspiciousMerchantSpawned = false;
   private final Collection<Spawn> _siegeNpcs = ConcurrentHashMap.newKeySet();
   private final Collection<Spawn> _npcCommanders = ConcurrentHashMap.newKeySet();
   private final Collection<Spawn> _specialEnvoys = ConcurrentHashMap.newKeySet();
   private final Map<Integer, Integer> _envoyCastles = new HashMap<>(2);
   private final Set<Integer> _availableCastles = new HashSet<>(1);
   public static final int FUNC_TELEPORT = 1;
   public static final int FUNC_RESTORE_HP = 2;
   public static final int FUNC_RESTORE_MP = 3;
   public static final int FUNC_RESTORE_EXP = 4;
   public static final int FUNC_SUPPORT = 5;

   public Fort(int fortId) {
      super(fortId);
      this.load();
      this.loadFlagPoles();
      this._function = new ConcurrentHashMap<>();
      if (this._fortOwner != null) {
         this.setVisibleFlag(true);
         this.loadFunctions();
      }

      this.initResidenceZone();
      this.initNpcs();
      this.initSiegeNpcs();
      this.initNpcCommanders();
      this.spawnNpcCommanders();
      this.initSpecialEnvoys();
      if (this._fortOwner != null && this._state == 0) {
         this.spawnSpecialEnvoys();
      }
   }

   public Fort.FortFunction getFunction(int type) {
      return this._function.get(type);
   }

   public void endOfSiege(Clan clan) {
      ThreadPool.execute(new Fort.endFortressSiege(this, clan));
   }

   public void banishForeigners() {
      this.getResidenceZone().banishForeigners(this._fortOwner.getId());
   }

   public boolean checkIfInZone(int x, int y, int z) {
      SiegeZone zone = this.getZone();
      return zone != null && zone.isInsideZone(x, y, z);
   }

   public SiegeZone getZone() {
      if (this._zone == null) {
         for (SiegeZone zone : ZoneManager.getInstance().getAllZones(SiegeZone.class)) {
            if (zone.getSiegeObjectId() == this.getResidenceId()) {
               this._zone = zone;
               break;
            }
         }
      }

      return this._zone;
   }

   public FortZone getResidenceZone() {
      return (FortZone)super.getResidenceZone();
   }

   public double getDistance(WorldObject obj) {
      return this.getZone().getDistanceToZone(obj);
   }

   public void closeDoor(Player player, int doorId) {
      this.openCloseDoor(player, doorId, false);
   }

   public void openDoor(Player player, int doorId) {
      this.openCloseDoor(player, doorId, true);
   }

   public void openCloseDoor(Player player, int doorId, boolean open) {
      if (player.getClan() == this._fortOwner) {
         Door door = this.getDoor(doorId);
         if (door != null) {
            if (open) {
               door.openMe();
            } else {
               door.closeMe();
            }
         }
      }
   }

   public void removeUpgrade() {
      this.removeDoorUpgrade();
   }

   public boolean setOwner(Clan clan, boolean updateClansReputation) {
      if (clan == null) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Updating Fort owner with null clan!!!");
         return false;
      } else {
         SystemMessage sm = new SystemMessage(SystemMessageId.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED);
         sm.addCastleId(this.getResidenceId());
         this.getSiege().announceToPlayer(sm);
         Clan oldowner = this._fortOwner;
         if (oldowner != null && clan != oldowner) {
            this.updateClansReputation(oldowner, true);

            try {
               Player oldleader = oldowner.getLeader().getPlayer();
               if (oldleader != null && oldleader.getMountType() == MountType.WYVERN) {
                  oldleader.dismount();
               }
            } catch (Exception var7) {
               LOGGER.log(Level.WARNING, "Exception in setOwner: " + var7.getMessage(), (Throwable)var7);
            }

            if (this.getSiege().isInProgress()) {
               this.getSiege().updatePlayerSiegeStateFlags(true);
            }

            this.removeOwner(true);
         }

         this.setFortState(0, 0);
         if (clan.getCastleId() > 0) {
            this.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.THE_REBEL_ARMY_RECAPTURED_THE_FORTRESS));
            return false;
         } else {
            if (updateClansReputation) {
               this.updateClansReputation(clan, false);
            }

            this.spawnSpecialEnvoys();
            if (clan.getFortId() > 0) {
               FortManager.getInstance().getFortByOwner(clan).removeOwner(true);
            }

            this.setSupplyLeveL(0);
            this.setOwnerClan(clan);
            this.updateOwnerInDB();
            this.saveFortVariables();
            if (this.getSiege().isInProgress()) {
               this.getSiege().endSiege();
            }

            for (Player member : clan.getOnlineMembers(0)) {
               this.giveResidentialSkills(member);
               member.sendSkillList();
            }

            return true;
         }
      }
   }

   public void removeOwner(boolean updateDB) {
      Clan clan = this._fortOwner;
      if (clan != null) {
         for (Player member : clan.getOnlineMembers(0)) {
            this.removeResidentialSkills(member);
            member.sendSkillList();
         }

         clan.setFortId(0);
         clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
         this.setOwnerClan(null);
         this.setSupplyLeveL(0);
         this.saveFortVariables();
         this.removeAllFunctions();
         if (updateDB) {
            this.updateOwnerInDB();
         }
      }
   }

   public void raiseSupplyLeveL() {
      this._supplyLeveL++;
      if (this._supplyLeveL > FeatureConfig.FS_MAX_SUPPLY_LEVEL) {
         this._supplyLeveL = FeatureConfig.FS_MAX_SUPPLY_LEVEL;
      }
   }

   public void setSupplyLeveL(int value) {
      if (value <= FeatureConfig.FS_MAX_SUPPLY_LEVEL) {
         this._supplyLeveL = value;
      }
   }

   public int getSupplyLeveL() {
      return this._supplyLeveL;
   }

   public void saveFortVariables() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE fort SET supplyLvL=? WHERE id = ?");
      ) {
         ps.setInt(1, this._supplyLeveL);
         ps.setInt(2, this.getResidenceId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: saveFortVariables(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void setVisibleFlag(boolean value) {
      StaticObject flagPole = this._flagPole;
      if (flagPole != null) {
         flagPole.setMeshIndex(value ? 1 : 0);
      }
   }

   public void resetDoors() {
      for (Door door : this._doors) {
         if (door.isOpen()) {
            door.closeMe();
         }

         if (door.isDead()) {
            door.doRevive();
         }

         if (door.getCurrentHp() < door.getMaxHp()) {
            door.setCurrentHp(door.getMaxHp());
         }
      }

      this.loadDoorUpgrade();
   }

   public void upgradeDoor(int doorId, int hp, int pDef, int mDef) {
      Door door = this.getDoor(doorId);
      if (door != null) {
         door.setCurrentHp(door.getMaxHp() + hp);
         this.saveDoorUpgrade(doorId, hp, pDef, mDef);
      }
   }

   @Override
   protected void load() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM fort WHERE id = ?");
      ) {
         ps.setInt(1, this.getResidenceId());
         int ownerId = 0;

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this.setName(rs.getString("name"));
               this._siegeDate = Calendar.getInstance();
               this._lastOwnedTime = Calendar.getInstance();
               this._siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
               this._lastOwnedTime.setTimeInMillis(rs.getLong("lastOwnedTime"));
               ownerId = rs.getInt("owner");
               this._fortType = rs.getInt("fortType");
               this._state = rs.getInt("state");
               this._castleId = rs.getInt("castleId");
               this._supplyLeveL = rs.getInt("supplyLvL");
            }
         }

         if (ownerId <= 0) {
            this.setOwnerClan(null);
         } else {
            Clan clan = ClanTable.getInstance().getClan(ownerId);
            clan.setFortId(this.getResidenceId());
            this.setOwnerClan(clan);
            int runCount = this.getOwnedTime() / (FeatureConfig.FS_UPDATE_FRQ * 60);
            long initial = System.currentTimeMillis() - this._lastOwnedTime.getTimeInMillis();

            while (initial > FeatureConfig.FS_UPDATE_FRQ * 60000) {
               initial -= FeatureConfig.FS_UPDATE_FRQ * 60000;
            }

            initial = FeatureConfig.FS_UPDATE_FRQ * 60000 - initial;
            if (FeatureConfig.FS_MAX_OWN_TIME > 0 && this.getOwnedTime() >= FeatureConfig.FS_MAX_OWN_TIME * 3600) {
               this._fortUpdater[1] = ThreadPool.schedule(new FortUpdater(this, clan, 0, FortUpdaterType.MAX_OWN_TIME), 60000L);
            } else {
               this._fortUpdater[0] = ThreadPool.scheduleAtFixedRate(
                  new FortUpdater(this, clan, runCount, FortUpdaterType.PERIODIC_UPDATE), initial, FeatureConfig.FS_UPDATE_FRQ * 60000
               );
               if (FeatureConfig.FS_MAX_OWN_TIME > 0) {
                  this._fortUpdater[1] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, runCount, FortUpdaterType.MAX_OWN_TIME), 3600000L, 3600000L);
               }
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Exception: loadFortData(): " + var14.getMessage(), (Throwable)var14);
      }
   }

   private void loadFunctions() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_functions WHERE fort_id = ?");
      ) {
         ps.setInt(1, this.getResidenceId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this._function
                  .put(
                     rs.getInt("type"),
                     new Fort.FortFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true)
                  );
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Exception: Fort.loadFunctions(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void removeFunction(int functionType) {
      this._function.remove(functionType);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM fort_functions WHERE fort_id=? AND type=?");
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.setInt(2, functionType);
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "Exception: Fort.removeFunctions(int functionType): " + var10.getMessage(), (Throwable)var10);
      }
   }

   private void removeAllFunctions() {
      for (int id : this._function.keySet()) {
         this.removeFunction(id);
      }
   }

   public boolean updateFunctions(Player player, int type, int level, int lease, long rate, boolean addNew) {
      if (player == null) {
         return false;
      } else if (lease > 0 && !player.destroyItemByItemId(null, 57, lease, null, true)) {
         return false;
      } else {
         if (addNew) {
            this._function.put(type, new Fort.FortFunction(type, level, lease, 0, rate, 0L, false));
         } else if (level == 0 && lease == 0) {
            this.removeFunction(type);
         } else if (lease - this._function.get(type).getLease() > 0) {
            this._function.remove(type);
            this._function.put(type, new Fort.FortFunction(type, level, lease, 0, rate, -1L, false));
         } else {
            this._function.get(type).setLease(lease);
            this._function.get(type).setLevel(level);
            this._function.get(type).dbSave();
         }

         return true;
      }
   }

   public void activateInstance() {
      this.loadDoor();
   }

   private void loadDoor() {
      for (Door door : DoorData.getInstance().getDoors()) {
         if (door.getFort() != null && door.getFort().getResidenceId() == this.getResidenceId()) {
            this._doors.add(door);
         }
      }
   }

   private void loadFlagPoles() {
      for (StaticObject obj : StaticObjectData.getInstance().getStaticObjects()) {
         if (obj.getType() == 3 && obj.getName().startsWith(this.getName())) {
            this._flagPole = obj;
            break;
         }
      }

      if (this._flagPole == null) {
         throw new NullPointerException("Can't find flagpole for Fort " + this);
      }
   }

   private void loadDoorUpgrade() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_doorupgrade WHERE fortId = ?");
      ) {
         ps.setInt(1, this.getResidenceId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this.upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: loadFortDoorUpgrade(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void removeDoorUpgrade() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM fort_doorupgrade WHERE fortId = ?");
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("INSERT INTO fort_doorupgrade (doorId, hp, pDef, mDef) VALUES (?,?,?,?)");
      ) {
         ps.setInt(1, doorId);
         ps.setInt(2, hp);
         ps.setInt(3, pDef);
         ps.setInt(4, mDef);
         ps.execute();
      } catch (Exception var13) {
         LOGGER.log(Level.WARNING, "Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + var13.getMessage(), (Throwable)var13);
      }
   }

   private void updateOwnerInDB() {
      Clan clan = this._fortOwner;
      int clanId = 0;
      if (clan != null) {
         clanId = clan.getId();
         this._lastOwnedTime.setTimeInMillis(System.currentTimeMillis());
      } else {
         this._lastOwnedTime.setTimeInMillis(0L);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE fort SET owner=?,lastOwnedTime=?,state=?,castleId=? WHERE id = ?");
      ) {
         ps.setInt(1, clanId);
         ps.setLong(2, this._lastOwnedTime.getTimeInMillis());
         ps.setInt(3, 0);
         ps.setInt(4, 0);
         ps.setInt(5, this.getResidenceId());
         ps.execute();
         if (clan != null) {
            clan.setFortId(this.getResidenceId());
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_IS_VICTORIOUS_IN_THE_FORTRESS_BATTLE_OF_S2);
            sm.addString(clan.getName());
            sm.addCastleId(this.getResidenceId());
            World.getPlayers().forEach(p -> p.sendPacket(sm));
            clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
            clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
            if (this._fortUpdater[0] != null) {
               this._fortUpdater[0].cancel(false);
            }

            if (this._fortUpdater[1] != null) {
               this._fortUpdater[1].cancel(false);
            }

            this._fortUpdater[0] = ThreadPool.scheduleAtFixedRate(
               new FortUpdater(this, clan, 0, FortUpdaterType.PERIODIC_UPDATE), FeatureConfig.FS_UPDATE_FRQ * 60000, FeatureConfig.FS_UPDATE_FRQ * 60000
            );
            if (FeatureConfig.FS_MAX_OWN_TIME > 0) {
               this._fortUpdater[1] = ThreadPool.scheduleAtFixedRate(new FortUpdater(this, clan, 0, FortUpdaterType.MAX_OWN_TIME), 3600000L, 3600000L);
            }
         } else {
            if (this._fortUpdater[0] != null) {
               this._fortUpdater[0].cancel(false);
            }

            this._fortUpdater[0] = null;
            if (this._fortUpdater[1] != null) {
               this._fortUpdater[1].cancel(false);
            }

            this._fortUpdater[1] = null;
         }
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + var11.getMessage(), (Throwable)var11);
      }
   }

   public Clan getOwnerClan() {
      return this._fortOwner;
   }

   public void setOwnerClan(Clan clan) {
      this.setVisibleFlag(clan != null);
      this._fortOwner = clan;
   }

   public Door getDoor(int doorId) {
      if (doorId <= 0) {
         return null;
      } else {
         for (Door door : this._doors) {
            if (door.getId() == doorId) {
               return door;
            }
         }

         return null;
      }
   }

   public List<Door> getDoors() {
      return this._doors;
   }

   public StaticObject getFlagPole() {
      return this._flagPole;
   }

   public FortSiege getSiege() {
      if (this._siege == null) {
         synchronized (this) {
            if (this._siege == null) {
               this._siege = new FortSiege(this);
            }
         }
      }

      return this._siege;
   }

   public Calendar getSiegeDate() {
      return this._siegeDate;
   }

   public void setSiegeDate(Calendar siegeDate) {
      this._siegeDate = siegeDate;
   }

   public int getOwnedTime() {
      return this._lastOwnedTime.getTimeInMillis() == 0L ? 0 : (int)((System.currentTimeMillis() - this._lastOwnedTime.getTimeInMillis()) / 1000L);
   }

   public int getTimeTillRebelArmy() {
      return this._lastOwnedTime.getTimeInMillis() == 0L
         ? 0
         : (int)((this._lastOwnedTime.getTimeInMillis() + FeatureConfig.FS_MAX_OWN_TIME * 3600000 - System.currentTimeMillis()) / 1000L);
   }

   public long getTimeTillNextFortUpdate() {
      return this._fortUpdater[0] == null ? 0L : this._fortUpdater[0].getDelay(TimeUnit.SECONDS);
   }

   public void updateClansReputation(Clan owner, boolean removePoints) {
      if (owner != null) {
         if (removePoints) {
            owner.takeReputationScore(FeatureConfig.LOOSE_FORT_POINTS);
         } else {
            owner.addReputationScore(FeatureConfig.TAKE_FORT_POINTS);
         }
      }
   }

   public int getFortState() {
      return this._state;
   }

   public void setFortState(int state, int castleId) {
      this._state = state;
      this._castleId = castleId;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE fort SET state=?,castleId=? WHERE id = ?");
      ) {
         ps.setInt(1, this._state);
         ps.setInt(2, this._castleId);
         ps.setInt(3, this.getResidenceId());
         ps.execute();
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, "Exception: setFortState(int state, int castleId): " + var11.getMessage(), (Throwable)var11);
      }
   }

   public int getFortType() {
      return this._fortType;
   }

   public int getCastleIdByAmbassador(int npcId) {
      return this._envoyCastles != null && this._envoyCastles.containsKey(npcId) ? this._envoyCastles.get(npcId) : -1;
   }

   public Castle getCastleByAmbassador(int npcId) {
      return CastleManager.getInstance().getCastleById(this.getCastleIdByAmbassador(npcId));
   }

   public int getContractedCastleId() {
      return this._castleId;
   }

   public Castle getContractedCastle() {
      return CastleManager.getInstance().getCastleById(this.getContractedCastleId());
   }

   public boolean isBorderFortress() {
      return this._availableCastles.size() > 1;
   }

   public int getFortSize() {
      return this._fortType == 0 ? 3 : 5;
   }

   public void spawnSuspiciousMerchant() {
      if (!this._isSuspiciousMerchantSpawned) {
         this._isSuspiciousMerchantSpawned = true;

         for (Spawn spawnDat : this._siegeNpcs) {
            spawnDat.doSpawn(false);
            spawnDat.startRespawn();
         }
      }
   }

   public void despawnSuspiciousMerchant() {
      if (this._isSuspiciousMerchantSpawned) {
         this._isSuspiciousMerchantSpawned = false;

         for (Spawn spawnDat : this._siegeNpcs) {
            spawnDat.stopRespawn();
            spawnDat.getLastSpawn().deleteMe();
         }
      }
   }

   public void spawnNpcCommanders() {
      for (Spawn spawnDat : this._npcCommanders) {
         spawnDat.doSpawn(false);
         spawnDat.startRespawn();
      }
   }

   public void despawnNpcCommanders() {
      for (Spawn spawnDat : this._npcCommanders) {
         spawnDat.stopRespawn();
         spawnDat.getLastSpawn().deleteMe();
      }
   }

   public void spawnSpecialEnvoys() {
      for (Spawn spawnDat : this._specialEnvoys) {
         spawnDat.doSpawn(false);
         spawnDat.startRespawn();
      }
   }

   private void initNpcs() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM fort_spawnlist WHERE fortId = ? AND spawnType = ?");
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.setInt(2, 0);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Spawn spawnDat = new Spawn(rs.getInt("npcId"));
               spawnDat.setAmount(1);
               spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
               spawnDat.setHeading(rs.getInt("heading"));
               spawnDat.setRespawnDelay(60);
               SpawnTable.getInstance().addSpawn(spawnDat);
               spawnDat.doSpawn(false);
               spawnDat.startRespawn();
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Fort " + this.getResidenceId() + " initNpcs: Spawn could not be initialized: " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void initSiegeNpcs() {
      this._siegeNpcs.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT id, npcId, x, y, z, heading FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id");
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.setInt(2, 2);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Spawn spawnDat = new Spawn(rs.getInt("npcId"));
               spawnDat.setAmount(1);
               spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
               spawnDat.setHeading(rs.getInt("heading"));
               spawnDat.setRespawnDelay(60);
               this._siegeNpcs.add(spawnDat);
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Fort " + this.getResidenceId() + " initSiegeNpcs: Spawn could not be initialized: " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void initNpcCommanders() {
      this._npcCommanders.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT id, npcId, x, y, z, heading FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id");
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.setInt(2, 1);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Spawn spawnDat = new Spawn(rs.getInt("npcId"));
               spawnDat.setAmount(1);
               spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
               spawnDat.setHeading(rs.getInt("heading"));
               spawnDat.setRespawnDelay(60);
               this._npcCommanders.add(spawnDat);
            }
         }
      } catch (Exception var12) {
         LOGGER.log(
            Level.WARNING, "Fort " + this.getResidenceId() + " initNpcCommanders: Spawn could not be initialized: " + var12.getMessage(), (Throwable)var12
         );
      }
   }

   private void initSpecialEnvoys() {
      this._specialEnvoys.clear();
      this._envoyCastles.clear();
      this._availableCastles.clear();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "SELECT id, npcId, x, y, z, heading, castleId FROM fort_spawnlist WHERE fortId = ? AND spawnType = ? ORDER BY id"
         );
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.setInt(2, 3);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               int castleId = rs.getInt("castleId");
               Spawn spawnDat = new Spawn(rs.getInt("npcId"));
               spawnDat.setAmount(1);
               spawnDat.setXYZ(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
               spawnDat.setHeading(rs.getInt("heading"));
               spawnDat.setRespawnDelay(60);
               this._specialEnvoys.add(spawnDat);
               this._envoyCastles.put(spawnDat.getId(), castleId);
               this._availableCastles.add(castleId);
            }
         }
      } catch (Exception var12) {
         LOGGER.log(
            Level.WARNING, "Fort " + this.getResidenceId() + " initSpecialEnvoys: Spawn could not be initialized: " + var12.getMessage(), (Throwable)var12
         );
      }
   }

   @Override
   protected void initResidenceZone() {
      for (FortZone zone : ZoneManager.getInstance().getAllZones(FortZone.class)) {
         if (zone.getResidenceId() == this.getResidenceId()) {
            this.setResidenceZone(zone);
            break;
         }
      }
   }

   public class FortFunction {
      final int _type;
      private int _level;
      protected int _fee;
      protected int _tempFee;
      final long _rate;
      long _endDate;
      protected boolean _inDebt;
      public boolean _cwh;

      public FortFunction(int type, int level, int lease, int tempLease, long rate, long time, boolean cwh) {
         Objects.requireNonNull(Fort.this);
         super();
         this._type = type;
         this._level = level;
         this._fee = lease;
         this._tempFee = tempLease;
         this._rate = rate;
         this._endDate = time;
         this.initializeTask(cwh);
      }

      public int getType() {
         return this._type;
      }

      public int getLevel() {
         return this._level;
      }

      public int getLease() {
         return this._fee;
      }

      public long getRate() {
         return this._rate;
      }

      public long getEndTime() {
         return this._endDate;
      }

      public void setLevel(int level) {
         this._level = level;
      }

      public void setLease(int lease) {
         this._fee = lease;
      }

      public void setEndTime(long time) {
         this._endDate = time;
      }

      private void initializeTask(boolean cwh) {
         if (Fort.this._fortOwner != null) {
            long currentTime = System.currentTimeMillis();
            if (this._endDate > currentTime) {
               ThreadPool.schedule(new Fort.FortFunction.FunctionTask(cwh), this._endDate - currentTime);
            } else {
               ThreadPool.schedule(new Fort.FortFunction.FunctionTask(cwh), 0L);
            }
         }
      }

      public void dbSave() {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("REPLACE INTO fort_functions (fort_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
         ) {
            ps.setInt(1, Fort.this.getResidenceId());
            ps.setInt(2, this._type);
            ps.setInt(3, this._level);
            ps.setInt(4, this._fee);
            ps.setLong(5, this._rate);
            ps.setLong(6, this._endDate);
            ps.execute();
         } catch (Exception var9) {
            Fort.LOGGER
               .log(
                  Level.SEVERE,
                  "Exception: Fort.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + var9.getMessage(),
                  (Throwable)var9
               );
         }
      }

      private class FunctionTask implements Runnable {
         public FunctionTask(boolean cwh) {
            Objects.requireNonNull(FortFunction.this);
            super();
            FortFunction.this._cwh = cwh;
         }

         @Override
         public void run() {
            try {
               if (Fort.this._fortOwner == null) {
                  return;
               }

               if (Fort.this._fortOwner.getWarehouse().getAdena() < FortFunction.this._fee && FortFunction.this._cwh) {
                  Fort.this.removeFunction(FortFunction.this._type);
               } else {
                  int fee = FortFunction.this._endDate == -1L ? FortFunction.this._tempFee : FortFunction.this._fee;
                  FortFunction.this.setEndTime(System.currentTimeMillis() + FortFunction.this._rate);
                  FortFunction.this.dbSave();
                  if (FortFunction.this._cwh) {
                     Fort.this._fortOwner.getWarehouse().destroyItemByItemId(ItemProcessType.FEE, 57, fee, null, null);
                  }

                  ThreadPool.schedule(FortFunction.this.new FunctionTask(true), FortFunction.this._rate);
               }
            } catch (Throwable var2) {
            }
         }
      }
   }

   private static class endFortressSiege implements Runnable {
      private final Fort _f;
      private final Clan _clan;

      public endFortressSiege(Fort f, Clan clan) {
         this._f = f;
         this._clan = clan;
      }

      @Override
      public void run() {
         try {
            this._f.setOwner(this._clan, true);
         } catch (Exception var2) {
            Fort.LOGGER.log(Level.WARNING, "Exception in endFortressSiege " + var2.getMessage(), (Throwable)var2);
         }
      }
   }
}
