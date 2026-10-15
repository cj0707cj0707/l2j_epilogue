package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantSupportItem;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class EnchantItemData implements IXmlReader {
   private final Map<Integer, EnchantScroll> _scrolls = new HashMap<>();
   private final Map<Integer, EnchantSupportItem> _supports = new HashMap<>();

   public EnchantItemData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this._scrolls.clear();
      this._supports.clear();
      this.parseDatapackFile("data/EnchantItemData.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._scrolls.size() + " enchant scrolls.");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._supports.size() + " support items.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("enchant".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  StatSet set = new StatSet();

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  try {
                     EnchantScroll item = new EnchantScroll(set);

                     for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                        if ("item".equalsIgnoreCase(cd.getNodeName())) {
                           item.addItem(this.parseInteger(cd.getAttributes(), "id"), this.parseInteger(cd.getAttributes(), "altScrollGroupId", -1));
                        }
                     }

                     this._scrolls.put(item.getId(), item);
                  } catch (NullPointerException var12) {
                     LOGGER.log(
                        Level.WARNING, this.getClass().getSimpleName() + ": Unexistent enchant scroll: " + set.getString("id") + " defined in enchant data!"
                     );
                  } catch (IllegalAccessError var13) {
                     LOGGER.log(
                        Level.WARNING,
                        this.getClass().getSimpleName() + ": Wrong enchant scroll item type: " + set.getString("id") + " defined in enchant data!"
                     );
                  }
               } else if ("support".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  StatSet set = new StatSet();

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  try {
                     EnchantSupportItem item = new EnchantSupportItem(set);
                     this._supports.put(item.getId(), item);
                  } catch (NullPointerException var10) {
                     LOGGER.log(
                        Level.WARNING,
                        this.getClass().getSimpleName() + ": Unexistent enchant support item: " + set.getString("id") + " defined in enchant data!"
                     );
                  } catch (IllegalAccessError var11) {
                     LOGGER.log(
                        Level.WARNING,
                        this.getClass().getSimpleName() + ": Wrong enchant support item type: " + set.getString("id") + " defined in enchant data!"
                     );
                  }
               }
            }
         }
      }
   }

   public Collection<EnchantScroll> getScrolls() {
      return this._scrolls.values();
   }

   public EnchantScroll getEnchantScroll(Item item) {
      return item == null ? null : this._scrolls.get(item.getId());
   }

   public EnchantSupportItem getSupportItem(Item item) {
      return item == null ? null : this._supports.get(item.getId());
   }

   public static EnchantItemData getInstance() {
      return EnchantItemData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final EnchantItemData INSTANCE = new EnchantItemData();
   }
}
