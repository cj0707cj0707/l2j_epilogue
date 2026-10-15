package org.l2jmobius.gameserver.entity.residences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.util.StatSet;

public abstract class ClanHall {
   protected static final Logger LOGGER = Logger.getLogger(ClanHall.class.getName());
   final int _clanHallId;
   private List<Door> _doors;
   private final String _name;
   private int _ownerId;
   private final String _desc;
   private final String _location;
   private ClanHallZone _zone;
   protected boolean _isFree = true;
   private final Map<Integer, ClanHall.ClanHallFunction> _functions;
   public static final int FUNC_TELEPORT = 1;
   public static final int FUNC_ITEM_CREATE = 2;
   public static final int FUNC_RESTORE_HP = 3;
   public static final int FUNC_RESTORE_MP = 4;
   public static final int FUNC_RESTORE_EXP = 5;
   public static final int FUNC_SUPPORT = 6;
   public static final int FUNC_DECO_FRONTPLATEFORM = 7;
   public static final int FUNC_DECO_CURTAINS = 8;

   public ClanHall(StatSet set) {
      this._clanHallId = set.getInt("id");
      this._name = set.getString("name");
      this._ownerId = set.getInt("ownerId");
      this._desc = set.getString("desc");
      this._location = set.getString("location");
      this._functions = new ConcurrentHashMap<>();
      if (this._ownerId > 0) {
         Clan clan = ClanTable.getInstance().getClan(this._ownerId);
         if (clan != null) {
            clan.setHideoutId(this._clanHallId);
         } else {
            this.free();
         }
      }
   }

   public int getId() {
      return this._clanHallId;
   }

   public String getName() {
      return this._name;
   }

   public int getOwnerId() {
      return this._ownerId;
   }

   public String getDesc() {
      return this._desc;
   }

   public String getLocation() {
      return this._location;
   }

   public List<Door> getDoors() {
      if (this._doors == null) {
         this._doors = new ArrayList<>();
      }

      return this._doors;
   }

   public Door getDoor(int doorId) {
      if (doorId <= 0) {
         return null;
      } else {
         for (Door door : this.getDoors()) {
            if (door.getId() == doorId) {
               return door;
            }
         }

         return null;
      }
   }

   public ClanHall.ClanHallFunction getFunction(int type) {
      return this._functions.get(type);
   }

   public void setZone(ClanHallZone zone) {
      this._zone = zone;
   }

   public boolean checkIfInZone(int x, int y, int z) {
      return this._zone.isInsideZone(x, y, z);
   }

   public ClanHallZone getZone() {
      return this._zone;
   }

   public void free() {
      this._ownerId = 0;
      this._isFree = true;

      for (Integer fc : this._functions.keySet()) {
         this.removeFunction(fc);
      }

      this._functions.clear();
      this.updateDb();
   }

   public void setOwner(Clan clan) {
      if (this._ownerId <= 0 && clan != null) {
         this._ownerId = clan.getId();
         this._isFree = false;
         clan.setHideoutId(this._clanHallId);
         clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
         this.updateDb();
      }
   }

   public void openCloseDoor(Player player, int doorId, boolean open) {
      if (player != null && player.getClanId() == this._ownerId) {
         this.openCloseDoor(doorId, open);
      }
   }

   public void openCloseDoor(int doorId, boolean open) {
      this.openCloseDoor(this.getDoor(doorId), open);
   }

   public void openCloseDoor(Door door, boolean open) {
      if (door != null) {
         if (open) {
            door.openMe();
         } else {
            door.closeMe();
         }
      }
   }

   public void openCloseDoors(Player player, boolean open) {
      if (player != null && player.getClanId() == this._ownerId) {
         this.openCloseDoors(open);
      }
   }

   public void openCloseDoors(boolean open) {
      for (Door door : this.getDoors()) {
         if (door != null) {
            if (open) {
               door.openMe();
            } else {
               door.closeMe();
            }
         }
      }
   }

   public void banishForeigners() {
      if (this._zone != null) {
         this._zone.banishForeigners(this._ownerId);
      } else {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Zone is null for clan hall: " + this._clanHallId + " " + this._name);
      }
   }

   protected void loadFunctions() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM clanhall_functions WHERE hall_id = ?");
      ) {
         ps.setInt(1, this._clanHallId);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this._functions
                  .put(
                     rs.getInt("type"),
                     new ClanHall.ClanHallFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true)
                  );
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "Exception: ClanHall.loadFunctions(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void removeFunction(int functionType) {
      this._functions.remove(functionType);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id=? AND type=?");
      ) {
         ps.setInt(1, this._clanHallId);
         ps.setInt(2, functionType);
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "Exception: ClanHall.removeFunctions(int functionType): " + var10.getMessage(), (Throwable)var10);
      }
   }

   public boolean updateFunctions(Player player, int type, int level, int lease, long rate, boolean addNew) {
      if (player != null && (lease <= 0 || player.destroyItemByItemId(null, 57, lease, null, true))) {
         if (addNew) {
            this._functions.put(type, new ClanHall.ClanHallFunction(type, level, lease, 0, rate, 0L, false));
         } else if (level == 0 && lease == 0) {
            this.removeFunction(type);
         } else if (lease - this._functions.get(type).getLease() > 0) {
            this._functions.remove(type);
            this._functions.put(type, new ClanHall.ClanHallFunction(type, level, lease, 0, rate, -1L, false));
         } else {
            this._functions.get(type).setLease(lease);
            this._functions.get(type).setLevel(level);
            this._functions.get(type).dbSave();
         }

         return true;
      } else {
         return false;
      }
   }

   public int getGrade() {
      return 0;
   }

   public long getPaidUntil() {
      return 0L;
   }

   public int getLease() {
      return 0;
   }

   public boolean isSiegableHall() {
      return false;
   }

   public boolean isFree() {
      return this._isFree;
   }

   public abstract void updateDb();

   public class ClanHallFunction {
      final int _type;
      private int _level;
      protected int _fee;
      protected int _tempFee;
      final long _rate;
      long _endDate;
      protected boolean _inDebt;
      public boolean _cwh;

      public ClanHallFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh) {
         Objects.requireNonNull(ClanHall.this);
         super();
         this._type = type;
         this._level = lvl;
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
         if (!ClanHall.this._isFree) {
            long currentTime = System.currentTimeMillis();
            if (this._endDate > currentTime) {
               ThreadPool.schedule(new ClanHall.ClanHallFunction.FunctionTask(cwh), this._endDate - currentTime);
            } else {
               ThreadPool.schedule(new ClanHall.ClanHallFunction.FunctionTask(cwh), 0L);
            }
         }
      }

      public void dbSave() {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("REPLACE INTO clanhall_functions (hall_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
         ) {
            ps.setInt(1, ClanHall.this._clanHallId);
            ps.setInt(2, this._type);
            ps.setInt(3, this._level);
            ps.setInt(4, this._fee);
            ps.setLong(5, this._rate);
            ps.setLong(6, this._endDate);
            ps.execute();
         } catch (Exception var9) {
            ClanHall.LOGGER
               .log(
                  Level.SEVERE,
                  "Exception: ClanHall.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + var9.getMessage(),
                  (Throwable)var9
               );
         }
      }

      private class FunctionTask implements Runnable {
         public FunctionTask(boolean cwh) {
            Objects.requireNonNull(ClanHallFunction.this);
            super();
            ClanHallFunction.this._cwh = cwh;
         }

         @Override
         public void run() {
            try {
               if (ClanHall.this._isFree) {
                  return;
               }

               if (ClanTable.getInstance().getClan(ClanHall.this.getOwnerId()).getWarehouse().getAdena() < ClanHallFunction.this._fee
                  && ClanHallFunction.this._cwh) {
                  ClanHall.this.removeFunction(ClanHallFunction.this._type);
               } else {
                  int fee = ClanHallFunction.this._endDate == -1L ? ClanHallFunction.this._tempFee : ClanHallFunction.this._fee;
                  ClanHallFunction.this.setEndTime(System.currentTimeMillis() + ClanHallFunction.this._rate);
                  ClanHallFunction.this.dbSave();
                  if (ClanHallFunction.this._cwh) {
                     ClanTable.getInstance().getClan(ClanHall.this.getOwnerId()).getWarehouse().destroyItemByItemId(ItemProcessType.FEE, 57, fee, null, null);
                  }

                  ThreadPool.schedule(ClanHallFunction.this.new FunctionTask(true), ClanHallFunction.this._rate);
               }
            } catch (Exception var2) {
               ClanHall.LOGGER.log(Level.SEVERE, "", (Throwable)var2);
            }
         }
      }
   }
}
