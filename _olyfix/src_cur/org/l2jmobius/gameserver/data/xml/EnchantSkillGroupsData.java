package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillGroup;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class EnchantSkillGroupsData implements IXmlReader {
   public static final int NORMAL_ENCHANT_COST_MULTIPLIER = GeneralConfig.NORMAL_ENCHANT_COST_MULTIPLIER;
   public static final int SAFE_ENCHANT_COST_MULTIPLIER = GeneralConfig.SAFE_ENCHANT_COST_MULTIPLIER;
   public static final int NORMAL_ENCHANT_BOOK = 6622;
   public static final int SAFE_ENCHANT_BOOK = 9627;
   public static final int CHANGE_ENCHANT_BOOK = 9626;
   public static final int UNTRAIN_ENCHANT_BOOK = 9625;
   private final Map<Integer, EnchantSkillGroup> _enchantSkillGroups = new HashMap<>();
   private final Map<Integer, EnchantSkillLearn> _enchantSkillTrees = new HashMap<>();

   protected EnchantSkillGroupsData() {
      this.load();
   }

   @Override
   public void load() {
      this._enchantSkillGroups.clear();
      this._enchantSkillTrees.clear();
      this.parseDatapackFile("data/EnchantSkillGroups.xml");
      int routes = 0;

      for (EnchantSkillGroup group : this._enchantSkillGroups.values()) {
         routes += group.getEnchantGroupDetails().size();
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._enchantSkillGroups.size() + " groups and " + routes + " routes.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("group".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  int id = this.parseInteger(attrs, "id");
                  EnchantSkillGroup group = this._enchantSkillGroups.get(id);
                  if (group == null) {
                     group = new EnchantSkillGroup(id);
                     this._enchantSkillGroups.put(id, group);
                  }

                  for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling()) {
                     if ("enchant".equalsIgnoreCase(b.getNodeName())) {
                        attrs = b.getAttributes();
                        StatSet set = new StatSet();

                        for (int i = 0; i < attrs.getLength(); i++) {
                           Node att = attrs.item(i);
                           set.set(att.getNodeName(), att.getNodeValue());
                        }

                        group.addEnchantDetail(new EnchantSkillGroup.EnchantSkillHolder(set));
                     }
                  }
               }
            }
         }
      }
   }

   public int addNewRouteForSkill(int skillId, int maxLvL, int route, int group) {
      EnchantSkillLearn enchantableSkill = this._enchantSkillTrees.get(skillId);
      if (enchantableSkill == null) {
         enchantableSkill = new EnchantSkillLearn(skillId, maxLvL);
         this._enchantSkillTrees.put(skillId, enchantableSkill);
      }

      EnchantSkillGroup groupData = this._enchantSkillGroups.get(group);
      if (groupData != null) {
         enchantableSkill.addNewEnchantRoute(route, group);
         return groupData.getEnchantGroupDetails().size();
      } else {
         LOGGER.log(
            Level.SEVERE,
            this.getClass().getSimpleName()
               + ": Error while loading generating enchant skill id: "
               + skillId
               + "; route: "
               + route
               + "; missing group: "
               + group
         );
         return 0;
      }
   }

   public EnchantSkillLearn getSkillEnchantmentForSkill(Skill skill) {
      EnchantSkillLearn esl = this.getSkillEnchantmentBySkillId(skill.getId());
      return esl != null && skill.getLevel() >= esl.getBaseLevel() ? esl : null;
   }

   public EnchantSkillLearn getSkillEnchantmentBySkillId(int skillId) {
      return this._enchantSkillTrees.get(skillId);
   }

   public EnchantSkillGroup getEnchantSkillGroupById(int id) {
      return this._enchantSkillGroups.get(id);
   }

   public int getEnchantSkillSpCost(Skill skill) {
      EnchantSkillLearn enchantSkillLearn = this._enchantSkillTrees.get(skill.getId());
      if (enchantSkillLearn != null) {
         EnchantSkillGroup.EnchantSkillHolder esh = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
         if (esh != null) {
            return esh.getSpCost();
         }
      }

      return Integer.MAX_VALUE;
   }

   public int getEnchantSkillAdenaCost(Skill skill) {
      EnchantSkillLearn enchantSkillLearn = this._enchantSkillTrees.get(skill.getId());
      if (enchantSkillLearn != null) {
         EnchantSkillGroup.EnchantSkillHolder esh = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
         if (esh != null) {
            return esh.getAdenaCost();
         }
      }

      return Integer.MAX_VALUE;
   }

   public byte getEnchantSkillRate(Player player, Skill skill) {
      EnchantSkillLearn enchantSkillLearn = this._enchantSkillTrees.get(skill.getId());
      if (enchantSkillLearn != null) {
         EnchantSkillGroup.EnchantSkillHolder esh = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
         if (esh != null) {
            return esh.getRate(player);
         }
      }

      return 0;
   }

   public static EnchantSkillGroupsData getInstance() {
      return EnchantSkillGroupsData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final EnchantSkillGroupsData INSTANCE = new EnchantSkillGroupsData();
   }
}
