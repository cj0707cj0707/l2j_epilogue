package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.PrimeShopProductHolder;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class PrimeShopData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(PrimeShopData.class.getName());
   private final Map<Integer, PrimeShopProductHolder> _products = new HashMap<>();

   protected PrimeShopData() {
      this.load();
   }

   @Override
   public void load() {
      this._products.clear();
      if (GeneralConfig.ENABLE_PRIME_SHOP) {
         this.parseDatapackFile("data/PrimeShop.xml");
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      StatSet set = null;

      for (Node a = document.getFirstChild(); a != null; a = a.getNextSibling()) {
         if ("list".equalsIgnoreCase(a.getNodeName())) {
            for (Node b = a.getFirstChild(); b != null; b = b.getNextSibling()) {
               if ("product".equalsIgnoreCase(b.getNodeName())) {
                  NamedNodeMap attrs = b.getAttributes();
                  set = new StatSet();

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  PrimeShopProductHolder product = new PrimeShopProductHolder(
                     set.getInt("id"), set.getInt("category"), set.getInt("points"), set.getInt("item"), set.getInt("count")
                  );
                  this._products.put(set.getInt("id"), product);
               }
            }
         }
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._products.size() + " products.");
   }

   public Collection<PrimeShopProductHolder> getAllItems() {
      return this._products.values();
   }

   public PrimeShopProductHolder getProduct(int id) {
      return this._products.get(id);
   }

   public static PrimeShopData getInstance() {
      return PrimeShopData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PrimeShopData INSTANCE = new PrimeShopData();
   }
}
