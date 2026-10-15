package org.l2jmobius.gameserver.entity.residences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class AuctionableHall extends ClanHall {
   private static final int CH_RATE = 604800000;
   protected long _paidUntil;
   private final int _grade;
   protected boolean _paid;
   final int _lease;

   public AuctionableHall(StatSet set) {
      super(set);
      this._paidUntil = set.getLong("paidUntil");
      this._grade = set.getInt("grade");
      this._paid = set.getBoolean("paid");
      this._lease = set.getInt("lease");
      if (this.getOwnerId() != 0) {
         this._isFree = false;
         this.initialyzeTask(false);
         this.loadFunctions();
      }
   }

   public boolean getPaid() {
      return this._paid;
   }

   @Override
   public int getLease() {
      return this._lease;
   }

   @Override
   public long getPaidUntil() {
      return this._paidUntil;
   }

   @Override
   public int getGrade() {
      return this._grade;
   }

   @Override
   public void free() {
      super.free();
      this._paidUntil = 0L;
      this._paid = false;
   }

   @Override
   public void setOwner(Clan clan) {
      super.setOwner(clan);
      this._paidUntil = System.currentTimeMillis();
      this.initialyzeTask(true);
   }

   private void initialyzeTask(boolean forced) {
      long currentTime = System.currentTimeMillis();
      if (this._paidUntil > currentTime) {
         ThreadPool.schedule(new AuctionableHall.FeeTask(), this._paidUntil - currentTime);
      } else if (!this._paid && !forced) {
         if (System.currentTimeMillis() + 86400000L <= this._paidUntil + 604800000L) {
            ThreadPool.schedule(new AuctionableHall.FeeTask(), System.currentTimeMillis() + 86400000L);
         } else {
            ThreadPool.schedule(new AuctionableHall.FeeTask(), this._paidUntil + 604800000L - System.currentTimeMillis());
         }
      } else {
         ThreadPool.schedule(new AuctionableHall.FeeTask(), 0L);
      }
   }

   @Override
   public void updateDb() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE clanhall SET ownerId=?, paidUntil=?, paid=? WHERE id=?");
      ) {
         ps.setInt(1, this.getOwnerId());
         ps.setLong(2, this._paidUntil);
         ps.setInt(3, this._paid ? 1 : 0);
         ps.setInt(4, this.getId());
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + var9.getMessage(), (Throwable)var9);
      }
   }

   protected class FeeTask implements Runnable {
      private final Logger LOGGER;

      protected FeeTask() {
         Objects.requireNonNull(AuctionableHall.this);
         super();
         this.LOGGER = Logger.getLogger(AuctionableHall.FeeTask.class.getName());
      }

      @Override
      public void run() {
         try {
            if (AuctionableHall.this.isFree()) {
               return;
            }

            long _time = System.currentTimeMillis();
            if (AuctionableHall.this._paidUntil > _time) {
               ThreadPool.schedule(AuctionableHall.this.new FeeTask(), AuctionableHall.this._paidUntil - _time);
               return;
            }

            Clan clan = ClanTable.getInstance().getClan(AuctionableHall.this.getOwnerId());
            if (ClanTable.getInstance().getClan(AuctionableHall.this.getOwnerId()).getWarehouse().getAdena() >= AuctionableHall.this.getLease()) {
               if (AuctionableHall.this._paidUntil != 0L) {
                  while (AuctionableHall.this._paidUntil <= _time) {
                     AuctionableHall.this._paidUntil += 604800000L;
                  }
               } else {
                  AuctionableHall.this._paidUntil = _time + 604800000L;
               }

               ClanTable.getInstance()
                  .getClan(AuctionableHall.this.getOwnerId())
                  .getWarehouse()
                  .destroyItemByItemId(ItemProcessType.FEE, 57, AuctionableHall.this.getLease(), null, null);
               ThreadPool.schedule(AuctionableHall.this.new FeeTask(), AuctionableHall.this._paidUntil - _time);
               AuctionableHall.this._paid = true;
               AuctionableHall.this.updateDb();
            } else {
               AuctionableHall.this._paid = false;
               if (_time > AuctionableHall.this._paidUntil + 604800000L) {
                  if (ClanHallTable.getInstance().loaded()) {
                     ClanHallAuctionManager.getInstance().initNPC(AuctionableHall.this.getId());
                     ClanHallTable.getInstance().setFree(AuctionableHall.this.getId());
                     clan.broadcastToOnlineMembers(
                        new SystemMessage(SystemMessageId.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED)
                     );
                  } else {
                     ThreadPool.schedule(AuctionableHall.this.new FeeTask(), 3000L);
                  }
               } else {
                  AuctionableHall.this.updateDb();
                  SystemMessage sm = new SystemMessage(
                     SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW
                  );
                  sm.addInt(AuctionableHall.this._lease);
                  clan.broadcastToOnlineMembers(sm);
                  if (_time + 86400000L <= AuctionableHall.this._paidUntil + 604800000L) {
                     ThreadPool.schedule(AuctionableHall.this.new FeeTask(), _time + 86400000L);
                  } else {
                     ThreadPool.schedule(AuctionableHall.this.new FeeTask(), AuctionableHall.this._paidUntil + 604800000L - _time);
                  }
               }
            }
         } catch (Exception var5) {
            this.LOGGER.log(Level.SEVERE, "", (Throwable)var5);
         }
      }
   }
}
