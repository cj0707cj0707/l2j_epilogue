package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.buylist.BuyListHolder;
import org.l2jmobius.gameserver.mechanics.buylist.Product;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class BuyListData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(BuyListData.class.getName());
   private static final Pattern XML_FILE_PATTERN = Pattern.compile("\\d+\\.xml");
   private final Map<Integer, BuyListHolder> _buyLists = new ConcurrentHashMap<>();

   protected BuyListData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this._buyLists.clear();
      this.parseDatapackDirectory("data/buylists", false);
      if (GeneralConfig.CUSTOM_BUYLIST_LOAD) {
         this.parseDatapackDirectory("data/buylists/custom", false);
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._buyLists.size() + " buyLists.");

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement statement = con.createStatement();
         ResultSet rs = statement.executeQuery("SELECT * FROM `buylists`");
      ) {
         while (rs.next()) {
            int buyListId = rs.getInt("buylist_id");
            BuyListHolder buyList = this.getBuyList(buyListId);
            if (buyList == null) {
               LOGGER.warning("BuyList found in database but not loaded from xml! BuyListId: " + buyListId);
            } else {
               int itemId = rs.getInt("item_id");
               Product product = buyList.getProductByItemId(itemId);
               if (product == null) {
                  LOGGER.warning("ItemId found in database but not loaded from xml! BuyListId: " + buyListId + " ItemId: " + itemId);
               } else {
                  long count = rs.getLong("count");
                  if (count < product.getMaxCount()) {
                     product.setCount(count);
                     product.restartRestockTask(rs.getLong("next_restock_time"));
                  }
               }
            }
         }
      } catch (Exception var16) {
         LOGGER.log(Level.WARNING, "Failed to load buyList data from database.", (Throwable)var16);
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      try {
         int buyListId = Integer.parseInt(file.getName().replace(".xml", ""));

         for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling()) {
            if ("list".equalsIgnoreCase(node.getNodeName())) {
               BuyListHolder buyList = new BuyListHolder(buyListId);

               for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling()) {
                  if ("item".equalsIgnoreCase(list_node.getNodeName())) {
                     int itemId = -1;
                     long price = -1L;
                     long restockDelay = -1L;
                     long count = -1L;
                     NamedNodeMap attrs = list_node.getAttributes();
                     Node attr = attrs.getNamedItem("id");
                     itemId = Integer.parseInt(attr.getNodeValue());
                     attr = attrs.getNamedItem("price");
                     if (attr != null) {
                        price = Long.parseLong(attr.getNodeValue());
                     }

                     attr = attrs.getNamedItem("restock_delay");
                     if (attr != null) {
                        restockDelay = Long.parseLong(attr.getNodeValue());
                     }

                     attr = attrs.getNamedItem("count");
                     if (attr != null) {
                        count = Long.parseLong(attr.getNodeValue());
                     }

                     ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
                     if (item != null) {
                        int sellPrice = item.getReferencePrice() / 2;
                        if (GeneralConfig.CORRECT_PRICES && price > -1L && sellPrice > price && buyList.getNpcsAllowed() != null) {
                           LOGGER.warning(
                              "Buy price "
                                 + price
                                 + " is less than sell price "
                                 + sellPrice
                                 + " for ItemID:"
                                 + itemId
                                 + " of buylist "
                                 + buyList.getListId()
                                 + "."
                           );
                           buyList.addProduct(new Product(buyList.getListId(), item, sellPrice, restockDelay, count));
                        } else {
                           buyList.addProduct(new Product(buyList.getListId(), item, price, restockDelay, count));
                        }
                     } else {
                        LOGGER.warning("Item not found. BuyList:" + buyList.getListId() + " ItemID:" + itemId + " File:" + file.getName());
                     }
                  } else if ("npcs".equalsIgnoreCase(list_node.getNodeName())) {
                     for (Node npcs_node = list_node.getFirstChild(); npcs_node != null; npcs_node = npcs_node.getNextSibling()) {
                        if ("npc".equalsIgnoreCase(npcs_node.getNodeName())) {
                           buyList.addAllowedNpc(Integer.parseInt(npcs_node.getTextContent()));
                        }
                     }
                  }
               }

               this._buyLists.put(buyList.getListId(), buyList);
            }
         }
      } catch (Exception var18) {
         LOGGER.log(Level.WARNING, "Failed to load buyList data from xml File:" + file.getName(), (Throwable)var18);
      }
   }

   @Override
   public boolean isValidXmlFile(File file) {
      return file != null && file.isFile() && XML_FILE_PATTERN.matcher(file.getName().toLowerCase()).matches();
   }

   public BuyListHolder getBuyList(int listId) {
      return this._buyLists.get(listId);
   }

   public static BuyListData getInstance() {
      return BuyListData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final BuyListData INSTANCE = new BuyListData();
   }
}
