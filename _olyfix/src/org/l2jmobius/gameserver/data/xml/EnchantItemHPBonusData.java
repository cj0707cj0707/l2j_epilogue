package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.mechanics.stats.functions.StatFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class EnchantItemHPBonusData implements IXmlReader {
   private final Map<CrystalType, List<Integer>> _armorHPBonuses = new EnumMap<>(CrystalType.class);
   private static final float FULL_ARMOR_MODIFIER = 1.5F;

   protected EnchantItemHPBonusData() {
      this.load();
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("enchantHP".equalsIgnoreCase(d.getNodeName())) {
                  List<Integer> bonuses = new ArrayList<>(12);

                  for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling()) {
                     if ("bonus".equalsIgnoreCase(e.getNodeName())) {
                        bonuses.add(Integer.parseInt(e.getTextContent()));
                     }
                  }

                  this._armorHPBonuses.put(this.parseEnum(d.getAttributes(), CrystalType.class, "grade"), bonuses);
               }
            }
         }
      }

      if (!this._armorHPBonuses.isEmpty()) {
         ItemData it = ItemData.getInstance();

         for (Integer itemId : it.getAllArmorsId()) {
            ItemTemplate item = it.getTemplate(itemId);
            if (item != null && item.getCrystalType() != CrystalType.NONE) {
               switch (item.getBodyPart()) {
                  case CHEST:
                  case FEET:
                  case GLOVES:
                  case HEAD:
                  case LEGS:
                  case BACK:
                  case FULL_ARMOR:
                  case UNDERWEAR:
                  case L_HAND:
                  case BELT:
                     item.attach(new FuncTemplate(null, null, StatFunction.ENCHANTHP.getName(), -1, Stat.MAX_HP, 0.0));
               }
            }
         }
      }
   }

   @Override
   public void load() {
      this._armorHPBonuses.clear();
      this.parseDatapackFile("data/stats/enchantHPBonus.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._armorHPBonuses.size() + " enchant HP bonuses.");
   }

   public int getHPBonus(Item item) {
      List<Integer> values = this._armorHPBonuses.get(item.getTemplate().getCrystalTypePlus());
      if (values != null && !values.isEmpty() && item.getOlyEnchantLevel() > 0) {
         int bonus = values.get(Math.min(item.getOlyEnchantLevel(), values.size()) - 1);
         return item.getTemplate().getBodyPart() == BodyPart.FULL_ARMOR ? (int)(bonus * 1.5F) : bonus;
      } else {
         return 0;
      }
   }

   public static EnchantItemHPBonusData getInstance() {
      return EnchantItemHPBonusData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final EnchantItemHPBonusData INSTANCE = new EnchantItemHPBonusData();
   }
}
