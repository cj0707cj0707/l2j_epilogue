package org.l2jmobius.gameserver.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncEnchant;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class DocumentItem extends DocumentBase implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(DocumentItem.class.getName());
   private DocumentItem.DocumentItemDataHolder _currentItem = null;
   private final List<ItemTemplate> _itemsInFile = new ArrayList<>();

   public DocumentItem(File file) {
      super(file);
   }

   @Override
   protected StatSet getStatSet() {
      return this._currentItem.set;
   }

   @Override
   protected String getTableValue(String name) {
      return this._tables.get(name)[this._currentItem.currentLevel];
   }

   @Override
   protected String getTableValue(String name, int idx) {
      return this._tables.get(name)[idx - 1];
   }

   @Override
   protected void parseDocument(Document document) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("item".equalsIgnoreCase(d.getNodeName())) {
                  try {
                     this._currentItem = new DocumentItem.DocumentItemDataHolder();
                     this.parseItem(d);
                     this._itemsInFile.add(this._currentItem.item);
                     this.resetTable();
                  } catch (Exception var5) {
                     LOGGER.log(Level.WARNING, "Cannot create item " + this._currentItem.id, (Throwable)var5);
                  }
               }
            }
         }
      }
   }

   private void parseItem(Node node) throws InvocationTargetException {
      int itemId = Integer.parseInt(node.getAttributes().getNamedItem("id").getNodeValue());
      String className = node.getAttributes().getNamedItem("type").getNodeValue();
      String itemName = node.getAttributes().getNamedItem("name").getNodeValue();
      this._currentItem.id = itemId;
      this._currentItem.type = className;
      this._currentItem.set = new StatSet();
      this._currentItem.set.set("item_id", itemId);
      this._currentItem.set.set("name", itemName);
      Node first = node.getFirstChild();

      for (Node n = first; n != null; n = n.getNextSibling()) {
         if (n.getNodeType() == 1 && n.getFirstChild() != null && n.getAttributes().getLength() == 0) {
            boolean hasChildElements = false;

            for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
               if (child.getNodeType() == 1) {
                  hasChildElements = true;
                  break;
               }
            }

            if (!hasChildElements && n.getFirstChild().getNodeType() == 3) {
               this.parseElementValue(n, this._currentItem.set, 1);
               if (this._currentItem.item != null) {
                  this._currentItem.item.set(this._currentItem.set);
               }
               continue;
            }
         }

         if ("table".equalsIgnoreCase(n.getNodeName())) {
            if (this._currentItem.item != null) {
               throw new IllegalStateException("Item created but table node found! Item " + itemId);
            }

            this.parseTable(n);
         } else if ("set".equalsIgnoreCase(n.getNodeName())) {
            if (this._currentItem.item != null) {
               throw new IllegalStateException("Item created but set node found! Item " + itemId);
            }

            this.parseBeanSet(n, this._currentItem.set, 1);
         } else if ("stats".equalsIgnoreCase(n.getNodeName())) {
            this.makeItem();

            for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling()) {
               if ("stat".equals(b.getNodeName())) {
                  Stat type = Stat.valueOfXml(b.getAttributes().getNamedItem("type").getNodeValue());
                  double value = Double.parseDouble(b.getTextContent());
                  switch (type) {
                     case POWER_ATTACK:
                     case POWER_ATTACK_SPEED:
                     case POWER_ATTACK_RANGE:
                     case CRITICAL_RATE:
                     case MAGIC_ATTACK:
                     case SHIELD_DEFENCE:
                     case SHIELD_RATE:
                        this._currentItem.item.attach(new FuncTemplate(null, null, "set", 0, type, value));
                        break;
                     case ACCURACY_COMBAT:
                     case EVASION_RATE:
                     case RANDOM_DAMAGE:
                     case POWER_DEFENCE:
                     case MAGIC_DEFENCE:
                     case MAX_MP:
                     case FIRE_POWER:
                     case WATER_POWER:
                     case WIND_POWER:
                     case EARTH_POWER:
                     case HOLY_POWER:
                     case DARK_POWER:
                     case FIRE_RES:
                     case WATER_RES:
                     case WIND_RES:
                     case EARTH_RES:
                     case HOLY_RES:
                     case DARK_RES:
                        this._currentItem.item.attach(new FuncTemplate(null, null, "add", 0, type, value));
                        break;
                     case MAGIC_SUCCESS_RES:
                        this._currentItem.item.attach(new FuncTemplate(null, null, "mul", 0, type, value));
                        break;
                     default:
                        LOGGER.warning("Unhandled stat type " + type);
                  }
               }
            }

            if (this._currentItem.item.isArmor()) {
               if (!this._currentItem.item.hasFunction(FuncEnchant.class)) {
                  if (this._currentItem.item.getItemType() == ArmorType.SHIELD) {
                     this._currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0, Stat.SHIELD_DEFENCE, 0.0));
                  } else if (this._currentItem.item.getType1() == 0 && this._currentItem.item.getType2() == 2) {
                     this._currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0, Stat.MAGIC_DEFENCE, 0.0));
                  } else {
                     this._currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0, Stat.POWER_DEFENCE, 0.0));
                  }
               }
            } else if (this._currentItem.item.isWeapon() && !this._currentItem.item.hasFunction(FuncEnchant.class)) {
               this._currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0, Stat.POWER_ATTACK, 0.0));
               this._currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0, Stat.MAGIC_ATTACK, 0.0));
            }
         } else if ("skills".equalsIgnoreCase(n.getNodeName())) {
            for (Node bx = n.getFirstChild(); bx != null; bx = bx.getNextSibling()) {
               if ("skill".equalsIgnoreCase(bx.getNodeName())) {
                  int id = this.parseInteger(bx.getAttributes(), "id");
                  int level = this.parseInteger(bx.getAttributes(), "level");
                  int chance = this.parseInteger(bx.getAttributes(), "type_chance", 100);
                  ItemSkillType type = this.parseEnum(bx.getAttributes(), ItemSkillType.class, "type", ItemSkillType.NORMAL);
                  switch (type) {
                     case NORMAL:
                        if (this._currentItem.set.getString("item_skill", null) == null) {
                           this._currentItem.set.set("item_skill", id + "-" + level);
                        } else {
                           this._currentItem.set.set("item_skill", this._currentItem.set.getString("item_skill") + ";" + id + "-" + level);
                        }
                        break;
                     case ON_ENCHANT_4:
                        this._currentItem.set.set("enchant4_skill", id + "-" + level);
                        break;
                     case ON_UNEQUIP:
                        this._currentItem.set.set("unequip_skill", id + "-" + level);
                        break;
                     case ON_CRITICAL_SKILL:
                        this._currentItem.set.set("oncrit_skill", id + "-" + level);
                        this._currentItem.set.set("oncrit_chance", chance);
                        break;
                     case ON_MAGIC_SKILL:
                        this._currentItem.set.set("onmagic_skill", id + "-" + level);
                        this._currentItem.set.set("onmagic_chance", chance);
                  }
               }
            }
         } else if ("capsuled_items".equalsIgnoreCase(n.getNodeName())) {
            this.makeItem();

            for (Node bxx = n.getFirstChild(); bxx != null; bxx = bxx.getNextSibling()) {
               if ("item".equals(bxx.getNodeName())) {
                  int id = this.parseInteger(bxx.getAttributes(), "id");
                  long min = this.parseLong(bxx.getAttributes(), "min");
                  long max = this.parseLong(bxx.getAttributes(), "max");
                  double chance = this.parseDouble(bxx.getAttributes(), "chance");
                  int minEnchant = this.parseInteger(bxx.getAttributes(), "minEnchant", 0);
                  int maxEnchant = this.parseInteger(bxx.getAttributes(), "maxEnchant", 0);
                  this._currentItem.item.addCapsuledItem(new ExtractableProduct(id, min, max, chance, minEnchant, maxEnchant));
               }
            }
         } else if ("conditions".equalsIgnoreCase(n.getNodeName())) {
            this.makeItem();
            Condition condition = this.parseCondition(n.getFirstChild(), this._currentItem.item);
            Node msg = n.getAttributes().getNamedItem("msg");
            Node msgId = n.getAttributes().getNamedItem("msgId");
            if (condition != null && msg != null) {
               condition.setMessage(msg.getNodeValue());
            } else if (condition != null && msgId != null) {
               condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
               Node addName = n.getAttributes().getNamedItem("addName");
               if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                  condition.addName();
               }
            }

            this._currentItem.item.attach(condition);
         }
      }

      this.makeItem();
   }

   private void makeItem() throws InvocationTargetException {
      if (this._currentItem.item != null) {
         this._currentItem.item.set(this._currentItem.set);
      } else {
         try {
            Constructor<?> itemClass = Class.forName("org.l2jmobius.gameserver.entity.item." + this._currentItem.type).getConstructor(StatSet.class);
            this._currentItem.item = (ItemTemplate)itemClass.newInstance(this._currentItem.set);
         } catch (Exception var2) {
            throw new InvocationTargetException(var2);
         }
      }
   }

   public List<ItemTemplate> getItemList() {
      return this._itemsInFile;
   }

   @Override
   public void load() {
   }

   @Override
   public void parseDocument(Document document, File file) {
   }

   private class DocumentItemDataHolder {
      int id;
      String type;
      StatSet set;
      int currentLevel;
      ItemTemplate item;

      public DocumentItemDataHolder() {
         Objects.requireNonNull(DocumentItem.this);
         super();
      }
   }
}
