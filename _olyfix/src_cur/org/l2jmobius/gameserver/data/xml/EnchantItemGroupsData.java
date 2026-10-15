package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.RangeChanceHolder;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantItemGroup;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantRateItem;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantScrollGroup;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class EnchantItemGroupsData implements IXmlReader {
   private final Map<String, EnchantItemGroup> _itemGroups = new HashMap<>();
   private final Map<Integer, EnchantScrollGroup> _scrollGroups = new HashMap<>();
   private int _maxWeaponEnchant = 0;
   private int _maxArmorEnchant = 0;
   private int _maxAccessoryEnchant = 0;

   protected EnchantItemGroupsData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this._itemGroups.clear();
      this._scrollGroups.clear();
      this.parseDatapackFile("data/EnchantItemGroups.xml");
      LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded " + this._itemGroups.size() + " item group templates.");
      LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded " + this._scrollGroups.size() + " scroll group templates.");
      if (PlayerConfig.OVER_ENCHANT_PROTECTION) {
         LOGGER.info(this.getClass().getSimpleName() + ": Max weapon enchant is set to " + this._maxWeaponEnchant + ".");
         LOGGER.info(this.getClass().getSimpleName() + ": Max armor enchant is set to " + this._maxArmorEnchant + ".");
         LOGGER.info(this.getClass().getSimpleName() + ": Max accessory enchant is set to " + this._maxAccessoryEnchant + ".");
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("enchantRateGroup".equalsIgnoreCase(d.getNodeName())) {
                  String name = this.parseString(d.getAttributes(), "name");
                  EnchantItemGroup group = new EnchantItemGroup(name);

                  for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                     if ("current".equalsIgnoreCase(cd.getNodeName())) {
                        String range = this.parseString(cd.getAttributes(), "enchant");
                        double chance = this.parseDouble(cd.getAttributes(), "chance");
                        int min = -1;
                        int max = 0;
                        if (range.contains("-")) {
                           String[] split = range.split("-");
                           if (split.length == 2 && StringUtil.isNumeric(split[0]) && StringUtil.isNumeric(split[1])) {
                              min = Integer.parseInt(split[0]);
                              max = Integer.parseInt(split[1]);
                           }
                        } else if (StringUtil.isNumeric(range)) {
                           min = Integer.parseInt(range);
                           max = min;
                        }

                        if (min > -1 && max > -1) {
                           group.addChance(new RangeChanceHolder(min, max, chance));
                        }

                        if (chance > 0.0) {
                           if (name.contains("WEAPON")) {
                              if (this._maxWeaponEnchant < max) {
                                 this._maxWeaponEnchant = max;
                              }
                           } else if (!name.contains("ACCESSORIES") && !name.contains("RING") && !name.contains("EARRING") && !name.contains("NECK")) {
                              if (this._maxArmorEnchant < max) {
                                 this._maxArmorEnchant = max;
                              }
                           } else if (this._maxAccessoryEnchant < max) {
                              this._maxAccessoryEnchant = max;
                           }
                        }
                     }
                  }

                  this._itemGroups.put(name, group);
               } else if ("enchantScrollGroup".equals(d.getNodeName())) {
                  int id = this.parseInteger(d.getAttributes(), "id");
                  EnchantScrollGroup group = new EnchantScrollGroup(id);

                  for (Node cdx = d.getFirstChild(); cdx != null; cdx = cdx.getNextSibling()) {
                     if ("enchantRate".equalsIgnoreCase(cdx.getNodeName())) {
                        EnchantRateItem rateGroup = new EnchantRateItem(this.parseString(cdx.getAttributes(), "group"));

                        for (Node z = cdx.getFirstChild(); z != null; z = z.getNextSibling()) {
                           if ("item".equals(z.getNodeName())) {
                              NamedNodeMap attrs = z.getAttributes();
                              if (attrs.getNamedItem("slot") != null) {
                                 rateGroup.addSlot(BodyPart.fromName(this.parseString(attrs, "slot")).getMask());
                              }

                              if (attrs.getNamedItem("magicWeapon") != null) {
                                 rateGroup.setMagicWeapon(this.parseBoolean(attrs, "magicWeapon"));
                              }

                              if (attrs.getNamedItem("itemId") != null) {
                                 rateGroup.addItemId(this.parseInteger(attrs, "itemId"));
                              }
                           }
                        }

                        group.addRateGroup(rateGroup);
                     }
                  }

                  this._scrollGroups.put(id, group);
               }
            }
         }
      }

      if (this._maxAccessoryEnchant == 0) {
         this._maxAccessoryEnchant = this._maxArmorEnchant;
      }

      this._maxWeaponEnchant++;
      this._maxArmorEnchant++;
      this._maxAccessoryEnchant++;
   }

   public EnchantItemGroup getItemGroup(ItemTemplate item, int scrollGroup) {
      EnchantScrollGroup group = this._scrollGroups.get(scrollGroup);
      EnchantRateItem rateGroup = group.getRateGroup(item);
      return rateGroup != null ? this._itemGroups.get(rateGroup.getName()) : null;
   }

   public EnchantItemGroup getItemGroup(String name) {
      return this._itemGroups.get(name);
   }

   public EnchantScrollGroup getScrollGroup(int id) {
      return this._scrollGroups.get(id);
   }

   public int getMaxWeaponEnchant() {
      return this._maxWeaponEnchant;
   }

   public int getMaxArmorEnchant() {
      return this._maxArmorEnchant;
   }

   public int getMaxAccessoryEnchant() {
      return this._maxAccessoryEnchant;
   }

   public static EnchantItemGroupsData getInstance() {
      return EnchantItemGroupsData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final EnchantItemGroupsData INSTANCE = new EnchantItemGroupsData();
   }
}
