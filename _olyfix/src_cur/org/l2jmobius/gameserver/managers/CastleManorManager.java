package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.manor.CropProcure;
import org.l2jmobius.gameserver.mechanics.siege.manor.ManorMode;
import org.l2jmobius.gameserver.mechanics.siege.manor.Seed;
import org.l2jmobius.gameserver.mechanics.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class CastleManorManager implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(CastleManorManager.class.getName());
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
   private static final String INSERT_PRODUCT = "INSERT INTO castle_manor_production VALUES (?, ?, ?, ?, ?, ?)";
   private static final String INSERT_CROP = "INSERT INTO castle_manor_procure VALUES (?, ?, ?, ?, ?, ?, ?)";
   private ManorMode _mode = ManorMode.APPROVED;
   private Calendar _nextModeChange = null;
   private static final Map<Integer, Seed> _seeds = new HashMap<>();
   private final Map<Integer, List<CropProcure>> _procure = new HashMap<>();
   private final Map<Integer, List<CropProcure>> _procureNext = new HashMap<>();
   private final Map<Integer, List<SeedProduction>> _production = new HashMap<>();
   private final Map<Integer, List<SeedProduction>> _productionNext = new HashMap<>();

   public CastleManorManager() {
      if (GeneralConfig.ALLOW_MANOR) {
         this.load();
         this.loadDb();
         Calendar currentTime = Calendar.getInstance();
         int hour = currentTime.get(11);
         int min = currentTime.get(12);
         int maintenanceMin = GeneralConfig.ALT_MANOR_REFRESH_MIN + GeneralConfig.ALT_MANOR_MAINTENANCE_MIN;
         if ((hour < GeneralConfig.ALT_MANOR_REFRESH_TIME || min < maintenanceMin)
            && hour >= GeneralConfig.ALT_MANOR_APPROVE_TIME
            && (hour != GeneralConfig.ALT_MANOR_APPROVE_TIME || min > GeneralConfig.ALT_MANOR_APPROVE_MIN)) {
            if (hour == GeneralConfig.ALT_MANOR_REFRESH_TIME && min >= GeneralConfig.ALT_MANOR_REFRESH_MIN && min < maintenanceMin) {
               this._mode = ManorMode.MAINTENANCE;
            } else {
               this._mode = ManorMode.APPROVED;
            }
         } else {
            this._mode = ManorMode.MODIFIABLE;
         }

         this.scheduleModeChange();
         if (!GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
            ThreadPool.scheduleAtFixedRate(
               this::storeMe, GeneralConfig.ALT_MANOR_SAVE_PERIOD_RATE * 60 * 60 * 1000, GeneralConfig.ALT_MANOR_SAVE_PERIOD_RATE * 60 * 60 * 1000
            );
         }
      } else {
         this._mode = ManorMode.DISABLED;
         this._nextModeChange = null;
         LOGGER.info(this.getClass().getSimpleName() + ": Manor system is deactivated.");
      }
   }

   @Override
   public void load() {
      this.parseDatapackFile("data/Seeds.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + _seeds.size() + " seeds.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("castle".equalsIgnoreCase(d.getNodeName())) {
                  int castleId = this.parseInteger(d.getAttributes(), "id");

                  for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
                     if ("crop".equalsIgnoreCase(c.getNodeName())) {
                        StatSet set = new StatSet();
                        set.set("castleId", castleId);
                        NamedNodeMap attrs = c.getAttributes();

                        for (int i = 0; i < attrs.getLength(); i++) {
                           Node att = attrs.item(i);
                           set.set(att.getNodeName(), att.getNodeValue());
                        }

                        _seeds.put(set.getInt("seedId"), new Seed(set));
                     }
                  }
               }
            }
         }
      }
   }

   private void loadDb() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement stProduction = con.prepareStatement("SELECT * FROM castle_manor_production WHERE castle_id=?");
         PreparedStatement stProcure = con.prepareStatement("SELECT * FROM castle_manor_procure WHERE castle_id=?");
      ) {
         for (Castle castle : CastleManager.getInstance().getCastles()) {
            int castleId = castle.getResidenceId();
            stProduction.clearParameters();
            stProcure.clearParameters();
            List<SeedProduction> currentProduction = new ArrayList<>();
            List<SeedProduction> nextProduction = new ArrayList<>();
            stProduction.setInt(1, castleId);

            try (ResultSet rs = stProduction.executeQuery()) {
               while (rs.next()) {
                  int seedId = rs.getInt("seed_id");
                  if (_seeds.containsKey(seedId)) {
                     SeedProduction seedProduction = new SeedProduction(seedId, rs.getLong("amount"), rs.getLong("price"), rs.getInt("start_amount"));
                     if (rs.getBoolean("next_period")) {
                        nextProduction.add(seedProduction);
                     } else {
                        currentProduction.add(seedProduction);
                     }
                  } else {
                     LOGGER.warning(this.getClass().getSimpleName() + ": Unknown seed ID: " + seedId);
                  }
               }
            }

            this._production.put(castleId, currentProduction);
            this._productionNext.put(castleId, nextProduction);
            List<CropProcure> currentProcure = new ArrayList<>();
            List<CropProcure> nextProcure = new ArrayList<>();
            stProcure.setInt(1, castleId);

            try (ResultSet rs = stProcure.executeQuery()) {
               Set<Integer> knownCropIds = this.getCropIds();

               while (rs.next()) {
                  int cropId = rs.getInt("crop_id");
                  if (knownCropIds.contains(cropId)) {
                     CropProcure cropProcure = new CropProcure(
                        cropId, rs.getLong("amount"), rs.getInt("reward_type"), rs.getLong("start_amount"), rs.getLong("price")
                     );
                     if (rs.getBoolean("next_period")) {
                        nextProcure.add(cropProcure);
                     } else {
                        currentProcure.add(cropProcure);
                     }
                  } else {
                     LOGGER.warning(this.getClass().getSimpleName() + ": Unknown crop ID: " + cropId);
                  }
               }
            }

            this._procure.put(castleId, currentProcure);
            this._procureNext.put(castleId, nextProcure);
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Manor data loaded.");
      } catch (Exception var25) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Unable to load manor data!", (Throwable)var25);
      }
   }

   private void scheduleModeChange() {
      this._nextModeChange = Calendar.getInstance();
      this._nextModeChange.set(13, 0);
      switch (this._mode) {
         case MODIFIABLE:
            this._nextModeChange.set(11, GeneralConfig.ALT_MANOR_APPROVE_TIME);
            this._nextModeChange.set(12, GeneralConfig.ALT_MANOR_APPROVE_MIN);
            if (this._nextModeChange.before(Calendar.getInstance())) {
               this._nextModeChange.add(5, 1);
            }
            break;
         case MAINTENANCE:
            this._nextModeChange.set(11, GeneralConfig.ALT_MANOR_REFRESH_TIME);
            this._nextModeChange.set(12, GeneralConfig.ALT_MANOR_REFRESH_MIN + GeneralConfig.ALT_MANOR_MAINTENANCE_MIN);
            if (this._nextModeChange.before(Calendar.getInstance())) {
               this._nextModeChange.add(5, 1);
            }
            break;
         case APPROVED:
            this._nextModeChange.set(11, GeneralConfig.ALT_MANOR_REFRESH_TIME);
            this._nextModeChange.set(12, GeneralConfig.ALT_MANOR_REFRESH_MIN);
            if (this._nextModeChange.before(Calendar.getInstance())) {
               this._nextModeChange.add(5, 1);
            }
            break;
         case DISABLED:
            return;
         default:
            return;
      }

      long delay = this._nextModeChange.getTimeInMillis() - System.currentTimeMillis();
      if (delay < 1000L) {
         delay = 1000L;
         this._nextModeChange.setTimeInMillis(System.currentTimeMillis() + delay);
      }

      ThreadPool.schedule(this::changeMode, delay);
   }

   public void changeMode() {
      switch (this._mode) {
         case MODIFIABLE:
            this._mode = ManorMode.APPROVED;

            for (Castle castle : CastleManager.getInstance().getCastles()) {
               Clan owner = castle.getOwner();
               if (owner != null) {
                  int castleId = castle.getResidenceId();
                  ItemContainer cwh = owner.getWarehouse();
                  int requiredSlots = 0;

                  for (CropProcure crop : this._procureNext.get(castleId)) {
                     if (crop.getStartAmount() > 0L && cwh.getAllItemsByItemId(this.getSeedByCrop(crop.getId()).getMatureId()) == null) {
                        requiredSlots++;
                     }
                  }

                  long manorCost = this.getManorCost(castleId, true);
                  if (!cwh.validateCapacity(requiredSlots) && castle.getTreasury() < manorCost) {
                     this._productionNext.get(castleId).clear();
                     this._procureNext.get(castleId).clear();
                     ClanMember clanLeader = owner.getLeader();
                     if (clanLeader != null && clanLeader.isOnline()) {
                        clanLeader.getPlayer().sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_FUNDS_IN_THE_CLAN_WAREHOUSE_FOR_THE_MANOR_TO_OPERATE);
                     }
                  } else {
                     castle.addToTreasuryNoTax(-manorCost);
                  }
               }
            }

            if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
               this.storeMe();
            }
            break;
         case MAINTENANCE:
            for (Castle castlex : CastleManager.getInstance().getCastles()) {
               Clan owner = castlex.getOwner();
               if (owner != null) {
                  ClanMember clanLeader = owner.getLeader();
                  if (clanLeader != null && clanLeader.isOnline()) {
                     clanLeader.getPlayer().sendPacket(SystemMessageId.THE_MANOR_INFORMATION_HAS_BEEN_UPDATED);
                  }
               }
            }

            this._mode = ManorMode.MODIFIABLE;
            break;
         case APPROVED:
            this._mode = ManorMode.MAINTENANCE;

            for (Castle castlexx : CastleManager.getInstance().getCastles()) {
               Clan owner = castlexx.getOwner();
               if (owner != null) {
                  int castleId = castlexx.getResidenceId();
                  ItemContainer cwh = owner.getWarehouse();

                  for (CropProcure cropx : this._procure.get(castleId)) {
                     if (cropx.getStartAmount() > 0L) {
                        if (cropx.getStartAmount() != cropx.getAmount()) {
                           long harvestedAmount = (long)((cropx.getStartAmount() - cropx.getAmount()) * 0.9);
                           if (harvestedAmount < 1L && Rnd.get(99) < 90) {
                              harvestedAmount = 1L;
                           }

                           if (harvestedAmount > 0L) {
                              cwh.addItem(ItemProcessType.REWARD, this.getSeedByCrop(cropx.getId()).getMatureId(), harvestedAmount, null, null);
                           }
                        }

                        if (cropx.getAmount() > 0L) {
                           castlexx.addToTreasuryNoTax(cropx.getAmount() * cropx.getPrice());
                        }
                     }
                  }

                  this._production.put(castleId, this._productionNext.get(castleId));
                  this._procure.put(castleId, this._procureNext.get(castleId));
                  if (castlexx.getTreasury() < this.getManorCost(castleId, false)) {
                     this._productionNext.put(castleId, Collections.emptyList());
                     this._procureNext.put(castleId, Collections.emptyList());
                  } else {
                     List<SeedProduction> productionList = new ArrayList<>(this._productionNext.get(castleId));

                     for (SeedProduction seed : productionList) {
                        seed.setAmount(seed.getStartAmount());
                     }

                     this._productionNext.put(castleId, productionList);
                     List<CropProcure> procureList = new ArrayList<>(this._procureNext.get(castleId));

                     for (CropProcure cropxx : procureList) {
                        cropxx.setAmount(cropxx.getStartAmount());
                     }

                     this._procureNext.put(castleId, procureList);
                  }
               }
            }

            this.storeMe();
            break;
         case DISABLED:
            return;
      }

      this.scheduleModeChange();
   }

   public void setNextSeedProduction(List<SeedProduction> list, int castleId) {
      this._productionNext.put(castleId, list);
      if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement deleteStmt = con.prepareStatement("DELETE FROM castle_manor_production WHERE castle_id = ? AND next_period = 1");
            PreparedStatement insertStmt = con.prepareStatement("INSERT INTO castle_manor_production VALUES (?, ?, ?, ?, ?, ?)");
         ) {
            deleteStmt.setInt(1, castleId);
            deleteStmt.executeUpdate();

            for (SeedProduction sp : list) {
               insertStmt.setInt(1, castleId);
               insertStmt.setInt(2, sp.getId());
               insertStmt.setLong(3, sp.getAmount());
               insertStmt.setLong(4, sp.getStartAmount());
               insertStmt.setLong(5, sp.getPrice());
               insertStmt.setBoolean(6, true);
               insertStmt.addBatch();
            }

            if (!list.isEmpty()) {
               insertStmt.executeBatch();
            }
         } catch (Exception var14) {
            LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Unable to store manor data!", (Throwable)var14);
         }
      }
   }

   public void setNextCropProcure(List<CropProcure> list, int castleId) {
      this._procureNext.put(castleId, list);
      if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement deleteStmt = con.prepareStatement("DELETE FROM castle_manor_procure WHERE castle_id = ? AND next_period = 1");
            PreparedStatement insertStmt = con.prepareStatement("INSERT INTO castle_manor_procure VALUES (?, ?, ?, ?, ?, ?, ?)");
         ) {
            deleteStmt.setInt(1, castleId);
            deleteStmt.executeUpdate();

            for (CropProcure cp : list) {
               insertStmt.setInt(1, castleId);
               insertStmt.setInt(2, cp.getId());
               insertStmt.setLong(3, cp.getAmount());
               insertStmt.setLong(4, cp.getStartAmount());
               insertStmt.setLong(5, cp.getPrice());
               insertStmt.setInt(6, cp.getReward());
               insertStmt.setBoolean(7, true);
               insertStmt.addBatch();
            }

            if (!list.isEmpty()) {
               insertStmt.executeBatch();
            }
         } catch (Exception var14) {
            LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Unable to store manor data!", (Throwable)var14);
         }
      }
   }

   public void updateCurrentProduction(int castleId, Collection<SeedProduction> items) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE castle_manor_production SET amount = ? WHERE castle_id = ? AND seed_id = ? AND next_period = 0");
      ) {
         for (SeedProduction sp : items) {
            ps.setLong(1, sp.getAmount());
            ps.setInt(2, castleId);
            ps.setInt(3, sp.getId());
            ps.addBatch();
         }

         ps.executeBatch();
      } catch (Exception var11) {
         LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Unable to update current production data!", (Throwable)var11);
      }
   }

   public void updateCurrentProcure(int castleId, Collection<CropProcure> items) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE castle_manor_procure SET amount = ? WHERE castle_id = ? AND crop_id = ? AND next_period = 0");
      ) {
         for (CropProcure cp : items) {
            ps.setLong(1, cp.getAmount());
            ps.setInt(2, castleId);
            ps.setInt(3, cp.getId());
            ps.addBatch();
         }

         ps.executeBatch();
      } catch (Exception var11) {
         LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Unable to update current procure data!", (Throwable)var11);
      }
   }

   public List<SeedProduction> getSeedProduction(int castleId, boolean nextPeriod) {
      return nextPeriod ? this._productionNext.get(castleId) : this._production.get(castleId);
   }

   public SeedProduction getSeedProduct(int castleId, int seedId, boolean nextPeriod) {
      for (SeedProduction sp : this.getSeedProduction(castleId, nextPeriod)) {
         if (sp.getId() == seedId) {
            return sp;
         }
      }

      return null;
   }

   public List<CropProcure> getCropProcure(int castleId, boolean nextPeriod) {
      return nextPeriod ? this._procureNext.get(castleId) : this._procure.get(castleId);
   }

   public CropProcure getCropProcure(int castleId, int cropId, boolean nextPeriod) {
      for (CropProcure cp : this.getCropProcure(castleId, nextPeriod)) {
         if (cp.getId() == cropId) {
            return cp;
         }
      }

      return null;
   }

   public long getManorCost(int castleId, boolean nextPeriod) {
      List<CropProcure> procureList = this.getCropProcure(castleId, nextPeriod);
      List<SeedProduction> productionList = this.getSeedProduction(castleId, nextPeriod);
      long totalCost = 0L;

      for (SeedProduction seed : productionList) {
         Seed s = this.getSeed(seed.getId());
         totalCost += s != null ? s.getSeedReferencePrice() * seed.getStartAmount() : 1L;
      }

      for (CropProcure crop : procureList) {
         totalCost += crop.getPrice() * crop.getStartAmount();
      }

      return totalCost;
   }

   public boolean storeMe() {
      try {
         boolean var24;
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement deleteProductionStmt = con.prepareStatement("DELETE FROM castle_manor_production");
            PreparedStatement insertProductionStmt = con.prepareStatement("INSERT INTO castle_manor_production VALUES (?, ?, ?, ?, ?, ?)");
            PreparedStatement deleteProcureStmt = con.prepareStatement("DELETE FROM castle_manor_procure");
            PreparedStatement insertProcureStmt = con.prepareStatement("INSERT INTO castle_manor_procure VALUES (?, ?, ?, ?, ?, ?, ?)");
         ) {
            deleteProductionStmt.executeUpdate();

            for (Entry<Integer, List<SeedProduction>> entry : this._production.entrySet()) {
               for (SeedProduction sp : entry.getValue()) {
                  insertProductionStmt.setInt(1, entry.getKey());
                  insertProductionStmt.setInt(2, sp.getId());
                  insertProductionStmt.setLong(3, sp.getAmount());
                  insertProductionStmt.setLong(4, sp.getStartAmount());
                  insertProductionStmt.setLong(5, sp.getPrice());
                  insertProductionStmt.setBoolean(6, false);
                  insertProductionStmt.addBatch();
               }
            }

            for (Entry<Integer, List<SeedProduction>> entry : this._productionNext.entrySet()) {
               for (SeedProduction sp : entry.getValue()) {
                  insertProductionStmt.setInt(1, entry.getKey());
                  insertProductionStmt.setInt(2, sp.getId());
                  insertProductionStmt.setLong(3, sp.getAmount());
                  insertProductionStmt.setLong(4, sp.getStartAmount());
                  insertProductionStmt.setLong(5, sp.getPrice());
                  insertProductionStmt.setBoolean(6, true);
                  insertProductionStmt.addBatch();
               }
            }

            insertProductionStmt.executeBatch();
            deleteProcureStmt.executeUpdate();

            for (Entry<Integer, List<CropProcure>> entry : this._procure.entrySet()) {
               for (CropProcure cp : entry.getValue()) {
                  insertProcureStmt.setInt(1, entry.getKey());
                  insertProcureStmt.setInt(2, cp.getId());
                  insertProcureStmt.setLong(3, cp.getAmount());
                  insertProcureStmt.setLong(4, cp.getStartAmount());
                  insertProcureStmt.setLong(5, cp.getPrice());
                  insertProcureStmt.setInt(6, cp.getReward());
                  insertProcureStmt.setBoolean(7, false);
                  insertProcureStmt.addBatch();
               }
            }

            for (Entry<Integer, List<CropProcure>> entry : this._procureNext.entrySet()) {
               for (CropProcure cp : entry.getValue()) {
                  insertProcureStmt.setInt(1, entry.getKey());
                  insertProcureStmt.setInt(2, cp.getId());
                  insertProcureStmt.setLong(3, cp.getAmount());
                  insertProcureStmt.setLong(4, cp.getStartAmount());
                  insertProcureStmt.setLong(5, cp.getPrice());
                  insertProcureStmt.setInt(6, cp.getReward());
                  insertProcureStmt.setBoolean(7, true);
                  insertProcureStmt.addBatch();
               }
            }

            insertProcureStmt.executeBatch();
            var24 = true;
         }

         return var24;
      } catch (Exception var20) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Unable to store manor data!", (Throwable)var20);
         return false;
      }
   }

   public void resetManorData(int castleId) {
      if (GeneralConfig.ALLOW_MANOR) {
         this._procure.get(castleId).clear();
         this._procureNext.get(castleId).clear();
         this._production.get(castleId).clear();
         this._productionNext.get(castleId).clear();
         if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS) {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement deleteProductionStmt = con.prepareStatement("DELETE FROM castle_manor_production WHERE castle_id = ?");
               PreparedStatement deleteProcureStmt = con.prepareStatement("DELETE FROM castle_manor_procure WHERE castle_id = ?");
            ) {
               deleteProductionStmt.setInt(1, castleId);
               deleteProductionStmt.executeUpdate();
               deleteProcureStmt.setInt(1, castleId);
               deleteProcureStmt.executeUpdate();
            } catch (Exception var13) {
               LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Unable to store manor data!", (Throwable)var13);
            }
         }
      }
   }

   public boolean isUnderMaintenance() {
      return this._mode == ManorMode.MAINTENANCE;
   }

   public boolean isManorApproved() {
      return this._mode == ManorMode.APPROVED;
   }

   public boolean isModifiablePeriod() {
      return this._mode == ManorMode.MODIFIABLE;
   }

   public String getCurrentModeName() {
      return this._mode.toString();
   }

   public String getNextModeChange() {
      return this._nextModeChange == null
         ? "Disabled"
         : DATE_FORMAT.format(Instant.ofEpochMilli(this._nextModeChange.getTimeInMillis()).atZone(ZoneId.systemDefault()));
   }

   public List<Seed> getCrops() {
      List<Seed> seeds = new ArrayList<>();
      List<Integer> cropIds = new ArrayList<>();

      for (Seed seed : _seeds.values()) {
         if (!cropIds.contains(seed.getCropId())) {
            seeds.add(seed);
            cropIds.add(seed.getCropId());
         }
      }

      cropIds.clear();
      return seeds;
   }

   public Set<Seed> getSeedsForCastle(int castleId) {
      Set<Seed> result = new HashSet<>();

      for (Seed seed : _seeds.values()) {
         if (seed.getCastleId() == castleId) {
            result.add(seed);
         }
      }

      return result;
   }

   public Set<Integer> getSeedIds() {
      return _seeds.keySet();
   }

   public Set<Integer> getCropIds() {
      Set<Integer> result = new HashSet<>();

      for (Seed seed : _seeds.values()) {
         result.add(seed.getCropId());
      }

      return result;
   }

   public Seed getSeed(int seedId) {
      return _seeds.get(seedId);
   }

   public Seed getSeedByCrop(int cropId, int castleId) {
      for (Seed seed : this.getSeedsForCastle(castleId)) {
         if (seed.getCropId() == cropId) {
            return seed;
         }
      }

      return null;
   }

   public Seed getSeedByCrop(int cropId) {
      for (Seed seed : _seeds.values()) {
         if (seed.getCropId() == cropId) {
            return seed;
         }
      }

      return null;
   }

   public static CastleManorManager getInstance() {
      return CastleManorManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CastleManorManager INSTANCE = new CastleManorManager();
   }
}
