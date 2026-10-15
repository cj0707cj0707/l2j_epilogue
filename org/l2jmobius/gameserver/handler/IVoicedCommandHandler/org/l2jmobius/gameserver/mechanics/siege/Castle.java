package org.l2jmobius.gameserver.mechanics.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.instance.Artefact;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.residences.AbstractResidence;
import org.l2jmobius.gameserver.entity.zone.type.CastleZone;
import org.l2jmobius.gameserver.entity.zone.type.ResidenceTeleportZone;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Castle extends AbstractResidence {
   protected static final Logger LOGGER = Logger.getLogger(Castle.class.getName());
   private final List<Door> _doors = new ArrayList<>();
   int _ownerId = 0;
   private Siege _siege = null;
   private Calendar _siegeDate;
   private boolean _isTimeRegistrationOver = true;
   private Calendar _siegeTimeRegistrationEndDate;
   private int _taxPercent = 0;
   private double _taxRate = 0.0;
   private long _treasury = 0L;
   private boolean _showNpcCrest = false;
   private SiegeZone _zone = null;
   private ResidenceTeleportZone _teleZone;
   private Clan _formerOwner = null;
   private final Set<Artefact> _artefacts = new HashSet<>(1);
   private final Map<Integer, Castle.CastleFunction> _function = new ConcurrentHashMap<>();
   private int _ticketBuyCount = 0;
   private boolean _isFirstMidVictory = false;
   public static final int FUNC_TELEPORT = 1;
   public static final int FUNC_RESTORE_HP = 2;
   public static final int FUNC_RESTORE_MP = 3;
   public static final int FUNC_RESTORE_EXP = 4;
   public static final int FUNC_SUPPORT = 5;

   public Castle(int castleId) {
      super(castleId);
      this.load();
      this.initResidenceZone();
      if (this._ownerId != 0) {
         this.loadFunctions();
         this.loadDoorUpgrade();
      }
   }

   public Castle.CastleFunction getFunction(int type) {
      return this._function.get(type);
   }

   public synchronized void engrave(Clan clan, WorldObject target) {
      if (this._artefacts.contains(target)) {
         this.setOwner(clan);
         SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_S1_HAS_SUCCEEDED_IN_ENGRAVING_THE_RULER);
         msg.addString(clan.getName());
         World.broadcastToAllOnlinePlayers(msg);
      }
   }

   public void addToTreasury(long amountValue) {
      if (this._ownerId > 0) {
         long amount = amountValue;
         if (this.getName().equalsIgnoreCase("Schuttgart") || this.getName().equalsIgnoreCase("Goddard")) {
            Castle rune = CastleManager.getInstance().getCastle("rune");
            if (rune != null) {
               long runeTax = (long)(amountValue * rune.getTaxRate());
               if (rune.getOwnerId() > 0) {
                  rune.addToTreasury(runeTax);
               }

               amount = amountValue - runeTax;
            }
         }

         if (!this.getName().equalsIgnoreCase("aden")
            && !this.getName().equalsIgnoreCase("Rune")
            && !this.getName().equalsIgnoreCase("Schuttgart")
            && !this.getName().equalsIgnoreCase("Goddard")) {
            Castle aden = CastleManager.getInstance().getCastle("aden");
            if (aden != null) {
               long adenTax = (long)(amount * aden.getTaxRate());
               if (aden.getOwnerId() > 0) {
                  aden.addToTreasury(adenTax);
               }

               amount -= adenTax;
            }
         }

         this.addToTreasuryNoTax(amount);
      }
   }

   public boolean addToTreasuryNoTax(long amountValue) {
      if (this._ownerId <= 0) {
         return false;
      } else {
         if (amountValue < 0L) {
            long amount = amountValue * -1L;
            if (this._treasury < amount) {
               return false;
            }

            this._treasury -= amount;
         } else if (this._treasury + amountValue > Inventory.MAX_ADENA) {
            this._treasury = Inventory.MAX_ADENA;
         } else {
            this._treasury += amountValue;
         }

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE castle SET treasury = ? WHERE id = ?");
         ) {
            ps.setLong(1, this._treasury);
            ps.setInt(2, this.getResidenceId());
            ps.execute();
         } catch (Exception var13) {
            LOGGER.log(Level.WARNING, var13.getMessage(), (Throwable)var13);
         }

         return true;
      }
   }

   public void banishForeigners() {
      this.getResidenceZone().banishForeigners(this._ownerId);
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

   public boolean isFirstMidVictory() {
      return this._isFirstMidVictory;
   }

   public void setFirstMidVictory(boolean value) {
      this._isFirstMidVictory = value;
   }

   public CastleZone getResidenceZone() {
      return (CastleZone)super.getResidenceZone();
   }

   public ResidenceTeleportZone getTeleZone() {
      if (this._teleZone == null) {
         for (ResidenceTeleportZone zone : ZoneManager.getInstance().getAllZones(ResidenceTeleportZone.class)) {
            if (zone.getResidenceId() == this.getResidenceId()) {
               this._teleZone = zone;
               break;
            }
         }
      }

      return this._teleZone;
   }

   public void oustAllPlayers() {
      this.getTeleZone().oustAllPlayers();
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
      if (player.getClanId() == this._ownerId) {
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
      this.removeTrapUpgrade();

      for (Integer fc : this._function.keySet()) {
         this.removeFunction(fc);
      }

      this._function.clear();
   }

   public void setOwner(Clan clan) {
      if (this._ownerId > 0 && (clan == null || clan.getId() != this._ownerId)) {
         Clan oldOwner = ClanTable.getInstance().getClan(this.getOwnerId());
         if (oldOwner != null) {
            if (this._formerOwner == null) {
               this._formerOwner = oldOwner;
               if (PlayerConfig.REMOVE_CASTLE_CIRCLETS) {
                  CastleManager.getInstance().removeCirclet(this._formerOwner, this.getResidenceId());
               }
            }

            try {
               Player oldleader = oldOwner.getLeader().getPlayer();
               if (oldleader != null && oldleader.getMountType() == MountType.WYVERN) {
                  oldleader.dismount();
               }
            } catch (Exception var5) {
               LOGGER.log(Level.WARNING, "Exception in setOwner: " + var5.getMessage(), (Throwable)var5);
            }

            oldOwner.setCastleId(0);

            for (Player member : oldOwner.getOnlineMembers(0)) {
               this.removeResidentialSkills(member);
               member.sendSkillList();
            }
         }
      }

      this.updateOwnerInDB(clan);
      this.setShowNpcCrest(false);
      if (clan != null && clan.getFortId() > 0) {
         FortManager.getInstance().getFortByOwner(clan).removeOwner(true);
      }

      if (this.getSiege().isInProgress()) {
         this.getSiege().midVictory();
      }

      TerritoryWarManager.getInstance().getTerritory(this.getResidenceId()).setOwnerClan(clan);
      if (clan != null) {
         for (Player member : clan.getOnlineMembers(0)) {
            this.giveResidentialSkills(member);
            member.sendSkillList();
         }
      }
   }

   public void removeOwner(Clan clan) {
      if (clan != null) {
         this._formerOwner = clan;
         if (PlayerConfig.REMOVE_CASTLE_CIRCLETS) {
            CastleManager.getInstance().removeCirclet(this._formerOwner, this.getResidenceId());
         }

         for (Player member : clan.getOnlineMembers(0)) {
            this.removeResidentialSkills(member);
            member.sendSkillList();
         }

         clan.setCastleId(0);
         clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
      }

      this.updateOwnerInDB(null);
      if (this.getSiege().isInProgress()) {
         this.getSiege().midVictory();
      }

      for (Integer fc : this._function.keySet()) {
         this.removeFunction(fc);
      }

      this._function.clear();
   }

   public void setTaxPercent(int taxPercent) {
      this._taxPercent = taxPercent;
      this._taxRate = this._taxPercent / 100.0;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE castle SET taxPercent = ? WHERE id = ?");
      ) {
         ps.setInt(1, taxPercent);
         ps.setInt(2, this.getResidenceId());
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, var10.getMessage(), (Throwable)var10);
      }
   }

   public void spawnDoor() {
      this.spawnDoor(false);
   }

   public void spawnDoor(boolean isDoorWeak) {
      for (Door door : this._doors) {
         if (door.isDead()) {
            door.doRevive();
            door.setCurrentHp(isDoorWeak ? door.getMaxHp() / 2 : door.getMaxHp());
         }

         if (door.isOpen()) {
            door.closeMe();
         }
      }
   }

   @Override
   protected void load() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps1 = con.prepareStatement("SELECT * FROM castle WHERE id = ?");
         PreparedStatement ps2 = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ?");
      ) {
         ps1.setInt(1, this.getResidenceId());

         try (ResultSet rs = ps1.executeQuery()) {
            while (rs.next()) {
               this.setName(rs.getString("name"));
               this._siegeDate = Calendar.getInstance();
               this._siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
               this._siegeTimeRegistrationEndDate = Calendar.getInstance();
               this._siegeTimeRegistrationEndDate.setTimeInMillis(rs.getLong("regTimeEnd"));
               this._isTimeRegistrationOver = Boolean.parseBoolean(rs.getString("regTimeOver"));
               this._taxPercent = rs.getInt("taxPercent");
               this._treasury = rs.getLong("treasury");
               this._showNpcCrest = Boolean.parseBoolean(rs.getString("showNpcCrest"));
               this._ticketBuyCount = rs.getInt("ticketBuyCount");
            }
         }

         this._taxRate = this._taxPercent / 100.0;
         ps2.setInt(1, this.getResidenceId());

         try (ResultSet rs = ps2.executeQuery()) {
            while (rs.next()) {
               this._ownerId = rs.getInt("clan_id");
            }
         }
      } catch (Exception var17) {
         LOGGER.log(Level.WARNING, "Exception: loadCastleData(): " + var17.getMessage(), (Throwable)var17);
      }
   }

   private void loadFunctions() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_functions WHERE castle_id = ?");
      ) {
         ps.setInt(1, this.getResidenceId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this._function
                  .put(
                     rs.getInt("type"),
                     new Castle.CastleFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true)
                  );
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Exception: Castle.loadFunctions(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void removeFunction(int functionType) {
      this._function.remove(functionType);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM castle_functions WHERE castle_id=? AND type=?");
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.setInt(2, functionType);
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "Exception: Castle.removeFunctions(int functionType): " + var10.getMessage(), (Throwable)var10);
      }
   }

   public boolean updateFunctions(Player player, int type, int lvl, int lease, long rate, boolean addNew) {
      if (player == null) {
         return false;
      } else if (lease > 0 && !player.destroyItemByItemId(null, 57, lease, null, true)) {
         return false;
      } else {
         if (addNew) {
            this._function.put(type, new Castle.CastleFunction(type, lvl, lease, 0, rate, 0L, false));
         } else if (lvl == 0 && lease == 0) {
            this.removeFunction(type);
         } else {
            int diffLease = lease - this._function.get(type).getLease();
            if (diffLease > 0) {
               this._function.remove(type);
               this._function.put(type, new Castle.CastleFunction(type, lvl, lease, 0, rate, -1L, false));
            } else {
               this._function.get(type).setLease(lease);
               this._function.get(type).setLvl(lvl);
               this._function.get(type).dbSave();
            }
         }

         return true;
      }
   }

   public void activateInstance() {
      this.loadDoor();
   }

   private void loadDoor() {
      for (Door door : DoorData.getInstance().getDoors()) {
         if (door.getCastle() != null && door.getCastle().getResidenceId() == this.getResidenceId()) {
            this._doors.add(door);
         }
      }
   }

   private void loadDoorUpgrade() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_doorupgrade WHERE castleId=?");
      ) {
         ps.setInt(1, this.getResidenceId());

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this.setDoorUpgrade(rs.getInt("doorId"), rs.getInt("ratio"), false);
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: loadCastleDoorUpgrade(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void removeDoorUpgrade() {
      for (Door door : this._doors) {
         door.getStat().setUpgradeHpRatio(1);
         door.setCurrentHp(door.getCurrentHp());
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM castle_doorupgrade WHERE castleId=?");
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void setDoorUpgrade(int doorId, int ratio, boolean save) {
      Door door = this.getDoors().isEmpty() ? DoorData.getInstance().getDoor(doorId) : this.getDoor(doorId);
      if (door != null) {
         door.getStat().setUpgradeHpRatio(ratio);
         door.setCurrentHp(door.getMaxHp());
         if (save) {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_doorupgrade (doorId, ratio, castleId) values (?,?,?)");
            ) {
               ps.setInt(1, doorId);
               ps.setInt(2, ratio);
               ps.setInt(3, this.getResidenceId());
               ps.execute();
            } catch (Exception var13) {
               LOGGER.log(Level.WARNING, "Exception: setDoorUpgrade(int doorId, int ratio, int castleId): " + var13.getMessage(), (Throwable)var13);
            }
         }
      }
   }

   private void updateOwnerInDB(Clan clan) {
      if (clan != null) {
         this._ownerId = clan.getId();
      } else {
         this._ownerId = 0;
         CastleManorManager.getInstance().resetManorData(this.getResidenceId());
      }

      try (Connection con = DatabaseFactory.getConnection()) {
         try (PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET hasCastle = 0 WHERE hasCastle = ?")) {
            ps.setInt(1, this.getResidenceId());
            ps.execute();
         }

         try (PreparedStatement ps = con.prepareStatement("UPDATE clan_data SET hasCastle = ? WHERE clan_id = ?")) {
            ps.setInt(1, this.getResidenceId());
            ps.setInt(2, this._ownerId);
            ps.execute();
         }

         if (clan != null) {
            clan.setCastleId(this.getResidenceId());
            clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
            clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + var12.getMessage(), (Throwable)var12);
      }
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

   public int getOwnerId() {
      return this._ownerId;
   }

   public Clan getOwner() {
      return this._ownerId != 0 ? ClanTable.getInstance().getClan(this._ownerId) : null;
   }

   public Siege getSiege() {
      if (this._siege == null) {
         this._siege = new Siege(this);
      }

      return this._siege;
   }

   public Calendar getSiegeDate() {
      return this._siegeDate;
   }

   public boolean isTimeRegistrationOver() {
      return this._isTimeRegistrationOver;
   }

   public void setTimeRegistrationOver(boolean value) {
      this._isTimeRegistrationOver = value;
   }

   public Calendar getTimeRegistrationOverDate() {
      if (this._siegeTimeRegistrationEndDate == null) {
         this._siegeTimeRegistrationEndDate = Calendar.getInstance();
      }

      return this._siegeTimeRegistrationEndDate;
   }

   public int getTaxPercent() {
      return this._taxPercent;
   }

   public double getTaxRate() {
      return this._taxRate;
   }

   public long getTreasury() {
      return this._treasury;
   }

   public boolean getShowNpcCrest() {
      return this._showNpcCrest;
   }

   public void setShowNpcCrest(boolean showNpcCrest) {
      if (this._showNpcCrest != showNpcCrest) {
         this._showNpcCrest = showNpcCrest;
         this.updateShowNpcCrest();
      }
   }

   public void updateClansReputation() {
      Clan owner = ClanTable.getInstance().getClan(this.getOwnerId());
      if (this._formerOwner != null) {
         if (this._formerOwner != owner) {
            int maxreward = Math.max(0, this._formerOwner.getReputationScore());
            this._formerOwner.takeReputationScore(FeatureConfig.LOOSE_CASTLE_POINTS);
            if (owner != null) {
               owner.addReputationScore(Math.min(FeatureConfig.TAKE_CASTLE_POINTS, maxreward));
               owner.broadcastToOnlineMembers(
                  new SystemMessage(SystemMessageId.SINCE_YOUR_CLAN_EMERGED_VICTORIOUS_FROM_THE_SIEGE_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_S_REPUTATION_SCORE)
                     .addInt(Math.min(FeatureConfig.TAKE_CASTLE_POINTS, maxreward))
               );
            }

            this._formerOwner
               .broadcastToOnlineMembers(
                  new SystemMessage(
                        SystemMessageId.SINCE_YOUR_CLAN_WAS_DEFEATED_IN_A_SIEGE_S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_CLAN_S_REPUTATION_SCORE_AND_GIVEN_TO_THE_OPPOSING_CLAN
                     )
                     .addInt(FeatureConfig.LOOSE_CASTLE_POINTS)
               );
         } else {
            this._formerOwner.addReputationScore(FeatureConfig.CASTLE_DEFENDED_POINTS);
            this._formerOwner
               .broadcastToOnlineMembers(
                  new SystemMessage(SystemMessageId.SINCE_YOUR_CLAN_EMERGED_VICTORIOUS_FROM_THE_SIEGE_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_S_REPUTATION_SCORE)
                     .addInt(FeatureConfig.CASTLE_DEFENDED_POINTS)
               );
         }
      } else if (owner != null) {
         owner.addReputationScore(FeatureConfig.TAKE_CASTLE_POINTS);
         owner.broadcastToOnlineMembers(
            new SystemMessage(SystemMessageId.SINCE_YOUR_CLAN_EMERGED_VICTORIOUS_FROM_THE_SIEGE_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_S_REPUTATION_SCORE)
               .addInt(FeatureConfig.TAKE_CASTLE_POINTS)
         );
      }
   }

   public void updateShowNpcCrest() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE castle SET showNpcCrest = ? WHERE id = ?");
      ) {
         ps.setString(1, String.valueOf(this._showNpcCrest));
         ps.setInt(2, this.getResidenceId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.info("Error saving showNpcCrest for castle " + this.getName() + ": " + var9.getMessage());
      }
   }

   @Override
   public void giveResidentialSkills(Player player) {
      TerritoryWarManager.Territory territory = TerritoryWarManager.getInstance().getTerritory(this.getResidenceId());
      if (territory != null && territory.getOwnedWardIds().contains(this.getResidenceId() + 80)) {
         for (int wardId : territory.getOwnedWardIds()) {
            for (SkillLearn s : SkillTreeData.getInstance().getAvailableResidentialSkills(wardId)) {
               Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
               if (sk != null) {
                  player.addSkill(sk, false);
               } else {
                  LOGGER.warning(
                     "Trying to add a null skill for Territory Ward Id: " + wardId + ", skill Id: " + s.getSkillId() + " level: " + s.getSkillLevel() + "!"
                  );
               }
            }
         }
      }

      super.giveResidentialSkills(player);
   }

   @Override
   public void removeResidentialSkills(Player player) {
      if (TerritoryWarManager.getInstance().getTerritory(this.getResidenceId()) != null) {
         for (int wardId : TerritoryWarManager.getInstance().getTerritory(this.getResidenceId()).getOwnedWardIds()) {
            for (SkillLearn s : SkillTreeData.getInstance().getAvailableResidentialSkills(wardId)) {
               Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
               if (sk != null) {
                  player.removeSkill(sk, false, true);
               } else {
                  LOGGER.warning(
                     "Trying to remove a null skill for Territory Ward Id: " + wardId + ", skill Id: " + s.getSkillId() + " level: " + s.getSkillLevel() + "!"
                  );
               }
            }
         }
      }

      super.removeResidentialSkills(player);
   }

   public void registerArtefact(Artefact artefact) {
      this._artefacts.add(artefact);
   }

   public Set<Artefact> getArtefacts() {
      return this._artefacts;
   }

   public int getTicketBuyCount() {
      return this._ticketBuyCount;
   }

   public void setTicketBuyCount(int count) {
      this._ticketBuyCount = count;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE castle SET ticketBuyCount = ? WHERE id = ?");
      ) {
         ps.setInt(1, this._ticketBuyCount);
         ps.setInt(2, this.getResidenceId());
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.WARNING, var10.getMessage(), (Throwable)var10);
      }
   }

   public int getTrapUpgradeLevel(int towerIndex) {
      TowerSpawn spawn = SiegeManager.getInstance().getFlameTowers(this.getResidenceId()).get(towerIndex);
      return spawn != null ? spawn.getUpgradeLevel() : 0;
   }

   public void setTrapUpgrade(int towerIndex, int level, boolean save) {
      if (save) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_trapupgrade (castleId, towerIndex, level) values (?,?,?)");
         ) {
            ps.setInt(1, this.getResidenceId());
            ps.setInt(2, towerIndex);
            ps.setInt(3, level);
            ps.execute();
         } catch (Exception var12) {
            LOGGER.log(Level.WARNING, "Exception: setTrapUpgradeLevel(int towerIndex, int level, int castleId): " + var12.getMessage(), (Throwable)var12);
         }
      }

      TowerSpawn spawn = SiegeManager.getInstance().getFlameTowers(this.getResidenceId()).get(towerIndex);
      if (spawn != null) {
         spawn.setUpgradeLevel(level);
      }
   }

   private void removeTrapUpgrade() {
      for (TowerSpawn ts : SiegeManager.getInstance().getFlameTowers(this.getResidenceId())) {
         ts.setUpgradeLevel(0);
      }

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM castle_trapupgrade WHERE castleId=?");
      ) {
         ps.setInt(1, this.getResidenceId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   @Override
   protected void initResidenceZone() {
      for (CastleZone zone : ZoneManager.getInstance().getAllZones(CastleZone.class)) {
         if (zone.getResidenceId() == this.getResidenceId()) {
            this.setResidenceZone(zone);
            break;
         }
      }
   }

   public class CastleFunction {
      final int _type;
      private int _lvl;
      protected int _fee;
      protected int _tempFee;
      final long _rate;
      long _endDate;
      protected boolean _inDebt;
      public boolean _cwh;

      public CastleFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh) {
         Objects.requireNonNull(Castle.this);
         super();
         this._type = type;
         this._lvl = lvl;
         this._fee = lease;
         this._tempFee = tempLease;
         this._rate = rate;
         this._endDate = time;
         this.initializeTask(cwh);
      }

      public int getType() {
         return this._type;
      }

      public int getLvl() {
         return this._lvl;
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

      public void setLvl(int lvl) {
         this._lvl = lvl;
      }

      public void setLease(int lease) {
         this._fee = lease;
      }

      public void setEndTime(long time) {
         this._endDate = time;
      }

      private void initializeTask(boolean cwh) {
         if (Castle.this._ownerId > 0) {
            long currentTime = System.currentTimeMillis();
            if (this._endDate > currentTime) {
               ThreadPool.schedule(new Castle.CastleFunction.FunctionTask(cwh), this._endDate - currentTime);
            } else {
               ThreadPool.schedule(new Castle.CastleFunction.FunctionTask(cwh), 0L);
            }
         }
      }

      public void dbSave() {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_functions (castle_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
         ) {
            ps.setInt(1, Castle.this.getResidenceId());
            ps.setInt(2, this._type);
            ps.setInt(3, this._lvl);
            ps.setInt(4, this._fee);
            ps.setLong(5, this._rate);
            ps.setLong(6, this._endDate);
            ps.execute();
         } catch (Exception var9) {
            Castle.LOGGER
               .log(
                  Level.SEVERE,
                  "Exception: Castle.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + var9.getMessage(),
                  (Throwable)var9
               );
         }
      }

      private class FunctionTask implements Runnable {
         public FunctionTask(boolean cwh) {
            Objects.requireNonNull(CastleFunction.this);
            super();
            CastleFunction.this._cwh = cwh;
         }

         @Override
         public void run() {
            try {
               if (Castle.this._ownerId <= 0) {
                  return;
               }

               if (ClanTable.getInstance().getClan(Castle.this.getOwnerId()).getWarehouse().getAdena() < CastleFunction.this._fee && CastleFunction.this._cwh) {
                  Castle.this.removeFunction(CastleFunction.this._type);
               } else {
                  int fee = CastleFunction.this._fee;
                  if (CastleFunction.this._endDate == -1L) {
                     fee = CastleFunction.this._tempFee;
                  }

                  CastleFunction.this.setEndTime(System.currentTimeMillis() + CastleFunction.this._rate);
                  CastleFunction.this.dbSave();
                  if (CastleFunction.this._cwh) {
                     ClanTable.getInstance().getClan(Castle.this.getOwnerId()).getWarehouse().destroyItemByItemId(ItemProcessType.FEE, 57, fee, null, null);
                  }

                  ThreadPool.schedule(CastleFunction.this.new FunctionTask(true), CastleFunction.this._rate);
               }
            } catch (Exception var2) {
               Castle.LOGGER.log(Level.SEVERE, "", (Throwable)var2);
            }
         }
      }
   }
}
