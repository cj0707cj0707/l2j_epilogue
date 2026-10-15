package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ArmorSetData implements IXmlReader {
   private ArmorSet[] _armorSets;
   private final Map<Integer, ArmorSet> _armorSetMap = new ConcurrentHashMap<>();

   protected ArmorSetData() {
      this.load();
   }

   @Override
   public void load() {
      this.parseDatapackDirectory("data/stats/armorsets", false);
      this._armorSets = new ArmorSet[Collections.max(this._armorSetMap.keySet()) + 1];

      for (Entry<Integer, ArmorSet> armorSet : this._armorSetMap.entrySet()) {
         this._armorSets[armorSet.getKey()] = armorSet.getValue();
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._armorSetMap.size() + " armor sets.");
      this._armorSetMap.clear();
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("set".equalsIgnoreCase(d.getNodeName())) {
                  ArmorSet set = new ArmorSet();

                  for (Node a = d.getFirstChild(); a != null; a = a.getNextSibling()) {
                     NamedNodeMap attrs = a.getAttributes();
                     String var8 = a.getNodeName();
                     switch (var8) {
                        case "chest":
                           set.addChest(this.parseInteger(attrs, "id"));
                           break;
                        case "feet":
                           set.addFeet(this.parseInteger(attrs, "id"));
                           break;
                        case "gloves":
                           set.addGloves(this.parseInteger(attrs, "id"));
                           break;
                        case "head":
                           set.addHead(this.parseInteger(attrs, "id"));
                           break;
                        case "legs":
                           set.addLegs(this.parseInteger(attrs, "id"));
                           break;
                        case "shield":
                           set.addShield(this.parseInteger(attrs, "id"));
                           break;
                        case "skill": {
                           int skillId = this.parseInteger(attrs, "id");
                           int skillLevel = this.parseInteger(attrs, "level");
                           set.addSkill(new SkillHolder(skillId, skillLevel));
                           break;
                        }
                        case "shield_skill": {
                           int skillId = this.parseInteger(attrs, "id");
                           int skillLevel = this.parseInteger(attrs, "level");
                           set.addShieldSkill(new SkillHolder(skillId, skillLevel));
                           break;
                        }
                        case "enchant6skill": {
                           int skillId = this.parseInteger(attrs, "id");
                           int skillLevel = this.parseInteger(attrs, "level");
                           set.addEnchant6Skill(new SkillHolder(skillId, skillLevel));
                           break;
                        }
                        case "con":
                           set.addCon(this.parseInteger(attrs, "val"));
                           break;
                        case "dex":
                           set.addDex(this.parseInteger(attrs, "val"));
                           break;
                        case "str":
                           set.addStr(this.parseInteger(attrs, "val"));
                           break;
                        case "men":
                           set.addMen(this.parseInteger(attrs, "val"));
                           break;
                        case "wit":
                           set.addWit(this.parseInteger(attrs, "val"));
                           break;
                        case "int":
                           set.addInt(this.parseInteger(attrs, "val"));
                     }
                  }

                  this._armorSetMap.put(set.getChestId(), set);
               }
            }
         }
      }
   }

   public boolean isArmorSet(int chestId) {
      return this._armorSets.length > chestId && this._armorSets[chestId] != null;
   }

   public ArmorSet getSet(int chestId) {
      return this._armorSets.length > chestId ? this._armorSets[chestId] : null;
   }

   public static ArmorSetData getInstance() {
      return ArmorSetData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ArmorSetData INSTANCE = new ArmorSetData();
   }
}
