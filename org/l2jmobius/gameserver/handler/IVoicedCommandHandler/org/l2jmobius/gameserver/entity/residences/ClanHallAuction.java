package org.l2jmobius.gameserver.entity.residences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class ClanHallAuction {
   protected static final Logger LOGGER = Logger.getLogger(ClanHallAuction.class.getName());
   private int _id = 0;
   private long _endDate;
   private int _highestBidderId = 0;
   private String _highestBidderName = "";
   private long _highestBidderMaxBid = 0L;
   private int _itemId = 0;
   private String _itemName = "";
   private int _itemObjectId = 0;
   private final long _itemQuantity = 0L;
   private String _itemType = "";
   private int _sellerId = 0;
   private String _sellerClanName = "";
   private String _sellerName = "";
   private long _currentBid = 0L;
   private long _startingBid = 0L;
   private final Map<Integer, ClanHallAuction.Bidder> _bidders = new ConcurrentHashMap<>();
   private static final String[] ItemTypeName = new String[]{"ClanHall"};

   public ClanHallAuction(int auctionId) {
      this._id = auctionId;
      this.load();
      this.startAutoTask();
   }

   public ClanHallAuction(int itemId, Clan clan, long delay, long bid, String name) {
      this._id = itemId;
      this._endDate = System.currentTimeMillis() + delay;
      this._itemId = itemId;
      this._itemName = name;
      this._itemType = "ClanHall";
      this._sellerId = clan.getLeaderId();
      this._sellerName = clan.getLeaderName();
      this._sellerClanName = clan.getName();
      this._startingBid = bid;
   }

   private void load() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("Select * from auction where id = ?");
      ) {
         ps.setInt(1, this._id);

         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               this._currentBid = rs.getLong("currentBid");
               this._endDate = rs.getLong("endDate");
               this._itemId = rs.getInt("itemId");
               this._itemName = rs.getString("itemName");
               this._itemObjectId = rs.getInt("itemObjectId");
               this._itemType = rs.getString("itemType");
               this._sellerId = rs.getInt("sellerId");
               this._sellerClanName = rs.getString("sellerClanName");
               this._sellerName = rs.getString("sellerName");
               this._startingBid = rs.getLong("startingBid");
            }
         }

         this.loadBid();
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: Auction.load(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void loadBid() {
      this._highestBidderId = 0;
      this._highestBidderName = "";
      this._highestBidderMaxBid = 0L;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "SELECT bidderId, bidderName, maxBid, clan_name, time_bid FROM auction_bid WHERE auctionId = ? ORDER BY maxBid DESC"
         );
      ) {
         ps.setInt(1, this._id);

         try (ResultSet rs = ps.executeQuery()) {
            for (;
               rs.next();
               this._bidders
                  .put(
                     rs.getInt("bidderId"),
                     new ClanHallAuction.Bidder(rs.getString("bidderName"), rs.getString("clan_name"), rs.getLong("maxBid"), rs.getLong("time_bid"))
                  )
            ) {
               if (rs.isFirst()) {
                  this._highestBidderId = rs.getInt("bidderId");
                  this._highestBidderName = rs.getString("bidderName");
                  this._highestBidderMaxBid = rs.getLong("maxBid");
               }
            }
         }
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: Auction.loadBid(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   private void startAutoTask() {
      long currentTime = System.currentTimeMillis();
      long taskDelay = 0L;
      if (this._endDate <= currentTime) {
         this._endDate = currentTime + 604800000L;
         this.saveAuctionDate();
      } else {
         taskDelay = this._endDate - currentTime;
      }

      ThreadPool.schedule(new ClanHallAuction.AutoEndTask(), taskDelay);
   }

   public static String getItemTypeName(AuctionItemType value) {
      return ItemTypeName[value.ordinal()];
   }

   private void saveAuctionDate() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("Update auction set endDate = ? where id = ?");
      ) {
         ps.setLong(1, this._endDate);
         ps.setInt(2, this._id);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Exception: saveAuctionDate(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public synchronized void setBid(Player bidder, long bid) {
      long requiredAdena = this._highestBidderName.equals(bidder.getClan().getLeaderName()) ? bid - this._highestBidderMaxBid : bid;
      if ((this._highestBidderId > 0 && bid > this._highestBidderMaxBid || this._highestBidderId == 0 && bid >= this._startingBid)
         && this.takeItem(bidder, requiredAdena)) {
         this.updateInDB(bidder, bid);
         bidder.getClan().setAuctionBiddedAt(this._id, true);
      } else {
         if (bid < this._startingBid || bid <= this._highestBidderMaxBid) {
            bidder.sendPacket(SystemMessageId.YOUR_BID_PRICE_MUST_BE_HIGHER_THAN_THE_MINIMUM_PRICE_THAT_CAN_BE_BID);
         }
      }
   }

   private void returnItem(String clanName, long quantity, boolean penalty) {
      long amount = quantity;
      if (penalty) {
         amount = (long)(quantity * 0.9);
      }

      Clan clan = ClanTable.getInstance().getClanByName(clanName);
      if (clan == null) {
         LOGGER.warning("Clan " + clanName + " doesn't exist!");
      } else {
         ItemContainer cwh = clan.getWarehouse();
         if (cwh == null) {
            LOGGER.warning("There has been a problem with " + clanName + "'s clan warehouse!");
         } else {
            long limit = Inventory.MAX_ADENA - cwh.getAdena();
            amount = Math.min(amount, limit);
            cwh.addItem(ItemProcessType.REFUND, 57, amount, null, null);
         }
      }
   }

   private boolean takeItem(Player bidder, long quantity) {
      if (bidder.getClan() != null && bidder.getClan().getWarehouse().getAdena() >= quantity) {
         bidder.getClan().getWarehouse().destroyItemByItemId(ItemProcessType.BUY, 57, quantity, bidder, bidder);
         return true;
      } else {
         bidder.sendPacket(SystemMessageId.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE);
         return false;
      }
   }

   private void updateInDB(Player bidder, long bid) {
      try (Connection con = DatabaseFactory.getConnection()) {
         if (this._bidders.get(bidder.getClanId()) != null) {
            try (PreparedStatement ps = con.prepareStatement(
                  "UPDATE auction_bid SET bidderId=?, bidderName=?, maxBid=?, time_bid=? WHERE auctionId=? AND bidderId=?"
               )) {
               ps.setInt(1, bidder.getClanId());
               ps.setString(2, bidder.getClan().getLeaderName());
               ps.setLong(3, bid);
               ps.setLong(4, System.currentTimeMillis());
               ps.setInt(5, this._id);
               ps.setInt(6, bidder.getClanId());
               ps.execute();
            }
         } else {
            try (PreparedStatement ps = con.prepareStatement(
                  "INSERT INTO auction_bid (id, auctionId, bidderId, bidderName, maxBid, clan_name, time_bid) VALUES (?, ?, ?, ?, ?, ?, ?)"
               )) {
               ps.setInt(1, IdManager.getInstance().getNextId());
               ps.setInt(2, this._id);
               ps.setInt(3, bidder.getClanId());
               ps.setString(4, bidder.getName());
               ps.setLong(5, bid);
               ps.setString(6, bidder.getClan().getName());
               ps.setLong(7, System.currentTimeMillis());
               ps.execute();
            }

            if (World.getPlayer(this._highestBidderName) != null) {
               World.getPlayer(this._highestBidderName).sendMessage("You have been out bidded");
            }
         }

         long currentTime = System.currentTimeMillis();
         this._highestBidderId = bidder.getClanId();
         this._highestBidderMaxBid = bid;
         this._highestBidderName = bidder.getClan().getLeaderName();
         if (this._bidders.get(this._highestBidderId) == null) {
            this._bidders.put(this._highestBidderId, new ClanHallAuction.Bidder(this._highestBidderName, bidder.getClan().getName(), bid, currentTime));
         } else {
            this._bidders.get(this._highestBidderId).setBid(bid);
            this._bidders.get(this._highestBidderId).setTimeBid(currentTime);
         }

         bidder.sendPacket(SystemMessageId.YOU_HAVE_BID_IN_A_CLAN_HALL_AUCTION);
      } catch (Exception var14) {
         LOGGER.log(Level.SEVERE, "Exception: Auction.updateInDB(Player bidder, int bid): " + var14.getMessage(), (Throwable)var14);
      }
   }

   private void removeBids() {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=?");
      ) {
         ps.setInt(1, this._id);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Exception: Auction.deleteFromDB(): " + var9.getMessage(), (Throwable)var9);
      }

      for (ClanHallAuction.Bidder b : this._bidders.values()) {
         if (ClanTable.getInstance().getClanByName(b.getClanName()).getHideoutId() == 0) {
            this.returnItem(b.getClanName(), b.getBid(), true);
         } else if (World.getPlayer(b.getName()) != null) {
            World.getPlayer(b.getName()).sendMessage("Congratulation you have won ClanHall!");
         }

         ClanTable.getInstance().getClanByName(b.getClanName()).setAuctionBiddedAt(0, true);
      }

      this._bidders.clear();
   }

   public void deleteAuctionFromDB() {
      ClanHallAuctionManager.getInstance().getAuctions().remove(this);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM auction WHERE itemId=?");
      ) {
         ps.setInt(1, this._itemId);
         ps.execute();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Exception: Auction.deleteFromDB(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public void endAuction() {
      if (ClanHallTable.getInstance().loaded()) {
         if (this._highestBidderId == 0 && this._sellerId == 0) {
            this.startAutoTask();
            return;
         }

         if (this._highestBidderId == 0 && this._sellerId > 0) {
            int aucId = ClanHallAuctionManager.getInstance().getAuctionIndex(this._id);
            ClanHallAuctionManager.getInstance().getAuctions().remove(aucId);
            return;
         }

         if (this._sellerId > 0) {
            this.returnItem(this._sellerClanName, this._highestBidderMaxBid, true);
            this.returnItem(this._sellerClanName, ClanHallTable.getInstance().getAuctionableHallById(this._itemId).getLease(), false);
         }

         this.deleteAuctionFromDB();
         Clan clan = ClanTable.getInstance().getClanByName(this._bidders.get(this._highestBidderId).getClanName());
         this._bidders.remove(this._highestBidderId);
         clan.setAuctionBiddedAt(0, true);
         this.removeBids();
         ClanHallTable.getInstance().setOwner(this._itemId, clan);
      } else {
         ThreadPool.schedule(new ClanHallAuction.AutoEndTask(), 3000L);
      }
   }

   public synchronized void cancelBid(int bidder) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=? AND bidderId=?");
      ) {
         ps.setInt(1, this._id);
         ps.setInt(2, bidder);
         ps.execute();
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "Exception: Auction.cancelBid(String bidder): " + var10.getMessage(), (Throwable)var10);
      }

      this.returnItem(this._bidders.get(bidder).getClanName(), this._bidders.get(bidder).getBid(), true);
      ClanTable.getInstance().getClanByName(this._bidders.get(bidder).getClanName()).setAuctionBiddedAt(0, true);
      this._bidders.clear();
      this.loadBid();
   }

   public void cancelAuction() {
      this.deleteAuctionFromDB();
      this.removeBids();
   }

   public void confirmAuction() {
      ClanHallAuctionManager.getInstance().getAuctions().add(this);

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement(
            "INSERT INTO auction (id, sellerId, sellerName, sellerClanName, itemType, itemId, itemObjectId, itemName, itemQuantity, startingBid, currentBid, endDate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
         );
      ) {
         ps.setInt(1, this._id);
         ps.setInt(2, this._sellerId);
         ps.setString(3, this._sellerName);
         ps.setString(4, this._sellerClanName);
         ps.setString(5, this._itemType);
         ps.setInt(6, this._itemId);
         ps.setInt(7, this._itemObjectId);
         ps.setString(8, this._itemName);
         ps.setLong(9, 0L);
         ps.setLong(10, this._startingBid);
         ps.setLong(11, this._currentBid);
         ps.setLong(12, this._endDate);
         ps.execute();
         ps.close();
      } catch (Exception var9) {
         LOGGER.log(Level.SEVERE, "Exception: Auction.load(): " + var9.getMessage(), (Throwable)var9);
      }
   }

   public int getId() {
      return this._id;
   }

   public long getCurrentBid() {
      return this._currentBid;
   }

   public long getEndDate() {
      return this._endDate;
   }

   public int getHighestBidderId() {
      return this._highestBidderId;
   }

   public String getHighestBidderName() {
      return this._highestBidderName;
   }

   public long getHighestBidderMaxBid() {
      return this._highestBidderMaxBid;
   }

   public int getItemId() {
      return this._itemId;
   }

   public String getItemName() {
      return this._itemName;
   }

   public int getItemObjectId() {
      return this._itemObjectId;
   }

   public long getItemQuantity() {
      return 0L;
   }

   public String getItemType() {
      return this._itemType;
   }

   public int getSellerId() {
      return this._sellerId;
   }

   public String getSellerName() {
      return this._sellerName;
   }

   public String getSellerClanName() {
      return this._sellerClanName;
   }

   public long getStartingBid() {
      return this._startingBid;
   }

   public Map<Integer, ClanHallAuction.Bidder> getBidders() {
      return this._bidders;
   }

   public class AutoEndTask implements Runnable {
      public AutoEndTask() {
         Objects.requireNonNull(ClanHallAuction.this);
         super();
      }

      @Override
      public void run() {
         try {
            ClanHallAuction.this.endAuction();
         } catch (Exception var2) {
            ClanHallAuction.LOGGER.log(Level.SEVERE, "", (Throwable)var2);
         }
      }
   }

   public static class Bidder {
      private final String _name;
      private final String _clanName;
      private long _bid;
      private final Calendar _timeBid;

      public Bidder(String name, String clanName, long bid, long timeBid) {
         this._name = name;
         this._clanName = clanName;
         this._bid = bid;
         this._timeBid = Calendar.getInstance();
         this._timeBid.setTimeInMillis(timeBid);
      }

      public String getName() {
         return this._name;
      }

      public String getClanName() {
         return this._clanName;
      }

      public long getBid() {
         return this._bid;
      }

      public Calendar getTimeBid() {
         return this._timeBid;
      }

      public void setTimeBid(long timeBid) {
         this._timeBid.setTimeInMillis(timeBid);
      }

      public void setBid(long bid) {
         this._bid = bid;
      }
   }
}
