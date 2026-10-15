package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuctionInstance;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ItemAuctionManager {
   private static final Logger LOGGER = Logger.getLogger(ItemAuctionManager.class.getName());
   private final Map<Integer, ItemAuctionInstance> _managerInstances = new HashMap<>();
   private final AtomicInteger _auctionIds = new AtomicInteger(1);

   protected ItemAuctionManager() {
      if (!GeneralConfig.ALT_ITEM_AUCTION_ENABLED) {
         LOGGER.info(this.getClass().getSimpleName() + ": Disabled.");
      } else {
         try (
            Connection con = DatabaseFactory.getConnection();
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT auctionId FROM item_auction ORDER BY auctionId DESC LIMIT 0, 1");
         ) {
            if (rs.next()) {
               this._auctionIds.set(rs.getInt(1) + 1);
            }
         } catch (SQLException var15) {
            LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Failed loading auctions.", (Throwable)var15);
         }

         File file = new File(ServerConfig.DATAPACK_ROOT, "data/ItemAuctions.xml");
         if (!file.exists()) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Missing ItemAuctions.xml!");
         } else {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);

            try {
               Document document = factory.newDocumentBuilder().parse(file);

               for (Node na = document.getFirstChild(); na != null; na = na.getNextSibling()) {
                  if ("list".equalsIgnoreCase(na.getNodeName())) {
                     for (Node nb = na.getFirstChild(); nb != null; nb = nb.getNextSibling()) {
                        if ("instance".equalsIgnoreCase(nb.getNodeName())) {
                           NamedNodeMap nab = nb.getAttributes();
                           int instanceId = Integer.parseInt(nab.getNamedItem("id").getNodeValue());
                           if (this._managerInstances.containsKey(instanceId)) {
                              throw new Exception("Dublicated instanceId " + instanceId);
                           }

                           this._managerInstances.put(instanceId, new ItemAuctionInstance(instanceId, this._auctionIds, nb));
                        }
                     }
                  }
               }

               LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded " + this._managerInstances.size() + " instance(s).");
            } catch (Exception var11) {
               LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Failed loading auctions from xml.", (Throwable)var11);
            }
         }
      }
   }

   public void shutdown() {
      for (ItemAuctionInstance instance : this._managerInstances.values()) {
         instance.shutdown();
      }
   }

   public ItemAuctionInstance getManagerInstance(int instanceId) {
      return this._managerInstances.get(instanceId);
   }

   public int getNextAuctionId() {
      return this._auctionIds.getAndIncrement();
   }

   public static void deleteAuction(int auctionId) {
      try (Connection con = DatabaseFactory.getConnection()) {
         try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_auction WHERE auctionId=?")) {
            ps.setInt(1, auctionId);
            ps.execute();
         }

         try (PreparedStatement ps = con.prepareStatement("DELETE FROM item_auction_bid WHERE auctionId=?")) {
            ps.setInt(1, auctionId);
            ps.execute();
         }
      } catch (SQLException var11) {
         LOGGER.log(Level.SEVERE, "ItemAuctionManagerInstance: Failed deleting auction: " + auctionId, (Throwable)var11);
      }
   }

   public static ItemAuctionManager getInstance() {
      return ItemAuctionManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ItemAuctionManager INSTANCE = new ItemAuctionManager();
   }
}
