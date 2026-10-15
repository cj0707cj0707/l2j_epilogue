package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.ElementalItemHolder;
import org.l2jmobius.gameserver.entity.item.enums.ElementalItemType;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;

public class ElementalAttributeData implements IXmlReader {
   private static final Map<Integer, ElementalItemHolder> ELEMENTAL_ITEMS = new HashMap<>();

   protected ElementalAttributeData() {
      this.load();
   }

   @Override
   public void load() {
      ELEMENTAL_ITEMS.clear();
      this.parseDatapackFile("data/ElementalAttributeData.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + ELEMENTAL_ITEMS.size() + " elemental attribute items.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(document, "list", listNode -> this.forEach(listNode, "item", itemNode -> {
         StatSet set = new StatSet(this.parseAttributes(itemNode));
         int id = set.getInt("id");
         if (ItemData.getInstance().getTemplate(id) == null) {
            LOGGER.info(this.getClass().getSimpleName() + ": Could not find item with id " + id + ".");
         } else {
            int elementalId = -1;
            String s0$ = set.getString("elemental");
            switch (s0$) {
               case "FIRE":
                  elementalId = 0;
                  break;
               case "WATER":
                  elementalId = 1;
                  break;
               case "WIND":
                  elementalId = 2;
                  break;
               case "EARTH":
                  elementalId = 3;
                  break;
               case "HOLY":
                  elementalId = 4;
                  break;
               case "DARK":
                  elementalId = 5;
            }

            ELEMENTAL_ITEMS.put(id, new ElementalItemHolder(id, elementalId, set.getEnum("type", ElementalItemType.class)));
         }
      }));
   }

   public ElementalItemHolder getElementalItem(int itemId) {
      return ELEMENTAL_ITEMS.get(itemId);
   }

   public static ElementalAttributeData getInstance() {
      return ElementalAttributeData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ElementalAttributeData INSTANCE = new ElementalAttributeData();
   }
}
