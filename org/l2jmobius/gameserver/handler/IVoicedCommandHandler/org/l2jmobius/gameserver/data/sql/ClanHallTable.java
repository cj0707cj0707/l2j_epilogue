package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.AuctionableHall;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.util.StatSet;

public class ClanHallTable {
   protected static final Logger LOGGER = Logger.getLogger(ClanHallTable.class.getName());
   private final Map<Integer, AuctionableHall> _clanHall = new ConcurrentHashMap<>();
   private final Map<Integer, AuctionableHall> _freeClanHall = new ConcurrentHashMap<>();
   private final Map<Integer, AuctionableHall> _allAuctionableClanHalls = new HashMap<>();
   private static Map<Integer, ClanHall> _allClanHalls = new HashMap<>();
   private boolean _loaded = false;

   public boolean loaded() {
      return this._loaded;
   }

   protected ClanHallTable() {
      this.load();
   }

   private void load() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM clanhall ORDER BY id");
      ) {
         while (rs.next()) {
            StatSet set = new StatSet();
            int id = rs.getInt("id");
            int ownerId = rs.getInt("ownerId");
            int lease = rs.getInt("lease");
            set.set("id", id);
            set.set("name", rs.getString("name"));
            set.set("ownerId", ownerId);
            set.set("lease", lease);
            set.set("desc", rs.getString("desc"));
            set.set("location", rs.getString("location"));
            set.set("paidUntil", rs.getLong("paidUntil"));
            set.set("grade", rs.getInt("Grade"));
            set.set("paid", rs.getBoolean("paid"));
            AuctionableHall ch = new AuctionableHall(set);
            this._allAuctionableClanHalls.put(id, ch);
            addClanHall(ch);
            if (ch.getOwnerId() > 0) {
               this._clanHall.put(id, ch);
            } else {
               this._freeClanHall.put(id, ch);
               if (ClanHallAuctionManager.getInstance().getAuction(id) == null && lease > 0) {
                  ClanHallAuctionManager.getInstance().initNPC(id);
               }
            }
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._clanHall.size() + " clan halls");
         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._freeClanHall.size() + " free clan halls");
         this._loaded = true;
      } catch (Exception var15) {
         LOGGER.log(Level.WARNING, "Exception: ClanHallManager.load(): " + var15.getMessage(), (Throwable)var15);
      }
   }

   public static Map<Integer, ClanHall> getAllClanHalls() {
      return _allClanHalls;
   }

   public Map<Integer, AuctionableHall> getFreeClanHalls() {
      return this._freeClanHall;
   }

   public Map<Integer, AuctionableHall> getClanHalls() {
      return this._clanHall;
   }

   public Map<Integer, AuctionableHall> getAllAuctionableClanHalls() {
      return this._allAuctionableClanHalls;
   }

   public static void addClanHall(ClanHall hall) {
      _allClanHalls.put(hall.getId(), hall);
   }

   public boolean isFree(int chId) {
      return this._freeClanHall.containsKey(chId);
   }

   public synchronized void setFree(int chId) {
      this._freeClanHall.put(chId, this._clanHall.get(chId));
      ClanTable.getInstance().getClan(this._freeClanHall.get(chId).getOwnerId()).setHideoutId(0);
      this._freeClanHall.get(chId).free();
      this._clanHall.remove(chId);
   }

   public synchronized void setOwner(int chId, Clan clan) {
      if (!this._clanHall.containsKey(chId)) {
         this._clanHall.put(chId, this._freeClanHall.get(chId));
         this._freeClanHall.remove(chId);
      } else {
         this._clanHall.get(chId).free();
      }

      ClanTable.getInstance().getClan(clan.getId()).setHideoutId(chId);
      this._clanHall.get(chId).setOwner(clan);
   }

   public ClanHall getClanHallById(int clanHallId) {
      return _allClanHalls.get(clanHallId);
   }

   public AuctionableHall getAuctionableHallById(int clanHallId) {
      return this._allAuctionableClanHalls.get(clanHallId);
   }

   public ClanHall getClanHall(int x, int y, int z) {
      for (ClanHall temp : _allClanHalls.values()) {
         if (temp.checkIfInZone(x, y, z)) {
            return temp;
         }
      }

      return null;
   }

   public ClanHall getClanHall(WorldObject activeObject) {
      return this.getClanHall(activeObject.getX(), activeObject.getY(), activeObject.getZ());
   }

   public AuctionableHall getNearbyClanHall(int x, int y, int maxDist) {
      ClanHallZone zone = null;

      for (Entry<Integer, AuctionableHall> ch : this._clanHall.entrySet()) {
         zone = ch.getValue().getZone();
         if (zone != null && zone.getDistanceToZone(x, y) < maxDist) {
            return ch.getValue();
         }
      }

      for (Entry<Integer, AuctionableHall> chx : this._freeClanHall.entrySet()) {
         zone = chx.getValue().getZone();
         if (zone != null && zone.getDistanceToZone(x, y) < maxDist) {
            return chx.getValue();
         }
      }

      return null;
   }

   public ClanHall getNearbyAbstractHall(int x, int y, int maxDist) {
      ClanHallZone zone = null;

      for (Entry<Integer, ClanHall> ch : _allClanHalls.entrySet()) {
         zone = ch.getValue().getZone();
         if (zone != null && zone.getDistanceToZone(x, y) < maxDist) {
            return ch.getValue();
         }
      }

      return null;
   }

   public AuctionableHall getClanHallByOwner(Clan clan) {
      for (Entry<Integer, AuctionableHall> ch : this._clanHall.entrySet()) {
         if (clan.getId() == ch.getValue().getOwnerId()) {
            return ch.getValue();
         }
      }

      return null;
   }

   public ClanHall getAbstractHallByOwner(Clan clan) {
      for (Entry<Integer, AuctionableHall> ch : this._clanHall.entrySet()) {
         if (clan.getId() == ch.getValue().getOwnerId()) {
            return ch.getValue();
         }
      }

      for (Entry<Integer, SiegableHall> chx : CHSiegeManager.getInstance().getConquerableHalls().entrySet()) {
         if (clan.getId() == chx.getValue().getOwnerId()) {
            return chx.getValue();
         }
      }

      return null;
   }

   public static ClanHallTable getInstance() {
      return ClanHallTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ClanHallTable INSTANCE = new ClanHallTable();
   }
}
