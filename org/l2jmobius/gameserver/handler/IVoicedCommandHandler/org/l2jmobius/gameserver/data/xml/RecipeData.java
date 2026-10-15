package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.data.holders.RecipeStatHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.recipe.RecipeList;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class RecipeData implements IXmlReader {
   private final Map<Integer, RecipeList> _recipes = new HashMap<>();

   protected RecipeData() {
      this.load();
   }

   @Override
   public void load() {
      this._recipes.clear();
      this.parseDatapackFile("data/Recipes.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._recipes.size() + " recipes.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      List<RecipeHolder> recipePartList = new ArrayList<>();
      List<RecipeStatHolder> recipeStatUseList = new ArrayList<>();
      List<RecipeStatHolder> recipeAltStatChangeList = new ArrayList<>();

      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            label116:
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("item".equalsIgnoreCase(d.getNodeName())) {
                  recipePartList.clear();
                  recipeStatUseList.clear();
                  recipeAltStatChangeList.clear();
                  NamedNodeMap attrs = d.getAttributes();
                  int id = -1;
                  boolean haveRare = false;
                  Node att = attrs.getNamedItem("id");
                  if (att == null) {
                     LOGGER.severe(this.getClass().getSimpleName() + ": Missing id for recipe item, skipping");
                  } else {
                     id = Integer.parseInt(att.getNodeValue());
                     StatSet set = new StatSet();
                     set.set("id", id);
                     att = attrs.getNamedItem("recipeId");
                     if (att == null) {
                        LOGGER.severe(this.getClass().getSimpleName() + ": Missing recipeId for recipe item id: " + id + ", skipping");
                     } else {
                        set.set("recipeId", Integer.parseInt(att.getNodeValue()));
                        att = attrs.getNamedItem("name");
                        if (att == null) {
                           LOGGER.severe(this.getClass().getSimpleName() + ": Missing name for recipe item id: " + id + ", skipping");
                        } else {
                           set.set("recipeName", att.getNodeValue());
                           att = attrs.getNamedItem("craftLevel");
                           if (att == null) {
                              LOGGER.severe(this.getClass().getSimpleName() + ": Missing level for recipe item id: " + id + ", skipping");
                           } else {
                              set.set("craftLevel", Integer.parseInt(att.getNodeValue()));
                              att = attrs.getNamedItem("type");
                              if (att == null) {
                                 LOGGER.severe(this.getClass().getSimpleName() + ": Missing type for recipe item id: " + id + ", skipping");
                              } else {
                                 set.set("isDwarvenRecipe", att.getNodeValue().equalsIgnoreCase("dwarven"));
                                 att = attrs.getNamedItem("successRate");
                                 if (att == null) {
                                    LOGGER.severe(this.getClass().getSimpleName() + ": Missing successRate for recipe item id: " + id + ", skipping");
                                 } else {
                                    set.set("successRate", Integer.parseInt(att.getNodeValue()));

                                    for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
                                       if ("statUse".equalsIgnoreCase(c.getNodeName())) {
                                          String statName = c.getAttributes().getNamedItem("name").getNodeValue();
                                          int value = Integer.parseInt(c.getAttributes().getNamedItem("value").getNodeValue());

                                          try {
                                             recipeStatUseList.add(new RecipeStatHolder(statName, value));
                                          } catch (Exception var18) {
                                             LOGGER.severe(
                                                this.getClass().getSimpleName() + ": Error in StatUse parameter for recipe item id: " + id + ", skipping"
                                             );
                                             continue label116;
                                          }
                                       } else if ("altStatChange".equalsIgnoreCase(c.getNodeName())) {
                                          String statName = c.getAttributes().getNamedItem("name").getNodeValue();
                                          int value = Integer.parseInt(c.getAttributes().getNamedItem("value").getNodeValue());

                                          try {
                                             recipeAltStatChangeList.add(new RecipeStatHolder(statName, value));
                                          } catch (Exception var17) {
                                             LOGGER.severe(
                                                this.getClass().getSimpleName() + ": Error in AltStatChange parameter for recipe item id: " + id + ", skipping"
                                             );
                                             continue label116;
                                          }
                                       } else if ("ingredient".equalsIgnoreCase(c.getNodeName())) {
                                          int ingId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
                                          int ingCount = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
                                          recipePartList.add(new RecipeHolder(ingId, ingCount));
                                       } else if ("production".equalsIgnoreCase(c.getNodeName())) {
                                          set.set("itemId", Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue()));
                                          set.set("count", Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue()));
                                       } else if ("productionRare".equalsIgnoreCase(c.getNodeName())) {
                                          set.set("rareItemId", Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue()));
                                          set.set("rareCount", Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue()));
                                          set.set("rarity", Integer.parseInt(c.getAttributes().getNamedItem("rarity").getNodeValue()));
                                          haveRare = true;
                                       }
                                    }

                                    RecipeList recipeList = new RecipeList(set, haveRare);

                                    for (RecipeHolder recipePart : recipePartList) {
                                       recipeList.addRecipe(recipePart);
                                    }

                                    for (RecipeStatHolder recipeStatUse : recipeStatUseList) {
                                       recipeList.addStatUse(recipeStatUse);
                                    }

                                    for (RecipeStatHolder recipeAltStatChange : recipeAltStatChangeList) {
                                       recipeList.addAltStatChange(recipeAltStatChange);
                                    }

                                    this._recipes.put(id, recipeList);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public RecipeList getRecipeList(int listId) {
      return this._recipes.get(listId);
   }

   public RecipeList getRecipeByItemId(int itemId) {
      for (RecipeList find : this._recipes.values()) {
         if (find.getRecipeId() == itemId) {
            return find;
         }
      }

      return null;
   }

   public int[] getAllItemIds() {
      int[] idList = new int[this._recipes.size()];
      int i = 0;

      for (RecipeList rec : this._recipes.values()) {
         idList[i++] = rec.getRecipeId();
      }

      return idList;
   }

   public RecipeList getValidRecipeList(Player player, int id) {
      RecipeList recipeList = this._recipes.get(id);
      if (recipeList != null && recipeList.getRecipes().length != 0) {
         return recipeList;
      } else {
         player.sendMessage(this.getClass().getSimpleName() + ": No recipe for: " + id);
         player.setCrafting(false);
         return null;
      }
   }

   public static RecipeData getInstance() {
      return RecipeData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final RecipeData INSTANCE = new RecipeData();
   }
}
