package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class CategoryData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(CategoryData.class.getName());
   private final Map<CategoryType, Set<Integer>> _categories = new EnumMap<>(CategoryType.class);

   protected CategoryData() {
      this.load();
   }

   @Override
   public void load() {
      this._categories.clear();
      this.parseDatapackFile("data/CategoryData.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._categories.size() + " categories.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling()) {
         if ("list".equalsIgnoreCase(node.getNodeName())) {
            for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling()) {
               if ("category".equalsIgnoreCase(list_node.getNodeName())) {
                  NamedNodeMap attrs = list_node.getAttributes();
                  CategoryType categoryType = CategoryType.findByName(attrs.getNamedItem("name").getNodeValue());
                  if (categoryType == null) {
                     LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Can't find category by name :" + attrs.getNamedItem("name").getNodeValue());
                  } else {
                     Set<Integer> ids = new HashSet<>();

                     for (Node category_node = list_node.getFirstChild(); category_node != null; category_node = category_node.getNextSibling()) {
                        if ("id".equalsIgnoreCase(category_node.getNodeName())) {
                           ids.add(Integer.parseInt(category_node.getTextContent()));
                        }
                     }

                     this._categories.put(categoryType, ids);
                  }
               }
            }
         }
      }
   }

   public boolean isInCategory(CategoryType type, int id) {
      Set<Integer> category = this.getCategoryByType(type);
      if (category == null) {
         LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Can't find category type :" + type);
         return false;
      } else {
         return category.contains(id);
      }
   }

   public Set<Integer> getCategoryByType(CategoryType type) {
      return this._categories.get(type);
   }

   public static CategoryData getInstance() {
      return CategoryData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CategoryData INSTANCE = new CategoryData();
   }
}
