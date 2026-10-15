package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.options.EnchantOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class EnchantItemOptionsData implements IXmlReader {
   private final Map<Integer, Map<Integer, EnchantOptions>> _data = new HashMap<>();

   protected EnchantItemOptionsData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this._data.clear();
      this.parseDatapackFile("data/EnchantItemOptions.xml");
   }

   @Override
   public void parseDocument(Document document, File file) {
      int counter = 0;

      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            label64:
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("item".equalsIgnoreCase(d.getNodeName())) {
                  int itemId = this.parseInteger(d.getAttributes(), "id");
                  ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
                  if (template == null) {
                     LOGGER.warning(this.getClass().getSimpleName() + ": Could not find item template for id " + itemId);
                  } else {
                     for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                        if ("options".equalsIgnoreCase(cd.getNodeName())) {
                           EnchantOptions op = new EnchantOptions(this.parseInteger(cd.getAttributes(), "level"));

                           for (byte i = 0; i < 3; i++) {
                              Node att = cd.getAttributes().getNamedItem("option" + (i + 1));
                              if (att != null && StringUtil.isNumeric(att.getNodeValue())) {
                                 int id = this.parseInteger(att);
                                 if (OptionData.getInstance().getOptions(id) == null) {
                                    LOGGER.warning(this.getClass().getSimpleName() + ": Could not find option " + id + " for item " + template);
                                    continue label64;
                                 }

                                 Map<Integer, EnchantOptions> data = this._data.get(itemId);
                                 if (data == null) {
                                    data = new HashMap<>();
                                    this._data.put(itemId, data);
                                 }

                                 data.putIfAbsent(op.getLevel(), op);
                                 op.setOption(i, id);
                              }
                           }

                           counter++;
                        }
                     }
                  }
               }
            }
         }
      }

      LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded " + this._data.size() + " items and " + counter + " options.");
   }

   public EnchantOptions getOptions(int itemId, int enchantLevel) {
      Map<Integer, EnchantOptions> levelMap = this._data.get(itemId);
      return levelMap != null ? levelMap.get(enchantLevel) : null;
   }

   public EnchantOptions getOptions(Item item) {
      return item != null ? this.getOptions(item.getId(), item.getEnchantLevel()) : null;
   }

   public static EnchantItemOptionsData getInstance() {
      return EnchantItemOptionsData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final EnchantItemOptionsData INSTANCE = new EnchantItemOptionsData();
   }
}
