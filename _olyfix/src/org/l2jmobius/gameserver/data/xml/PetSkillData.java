package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class PetSkillData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(PetSkillData.class.getName());
   private final Map<Integer, Map<Long, SkillHolder>> _skillTrees = new HashMap<>();

   protected PetSkillData() {
      this.load();
   }

   @Override
   public void load() {
      this._skillTrees.clear();
      this.parseDatapackFile("data/PetSkillData.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._skillTrees.size() + " skills.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("skill".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  int npcId = this.parseInteger(attrs, "npcId");
                  int skillId = this.parseInteger(attrs, "skillId");
                  int skillLevel = this.parseInteger(attrs, "skillLevel");
                  Map<Long, SkillHolder> skillTree = this._skillTrees.get(npcId);
                  if (skillTree == null) {
                     skillTree = new HashMap<>();
                     this._skillTrees.put(npcId, skillTree);
                  }

                  if (SkillData.getInstance().getSkill(skillId, skillLevel == 0 ? 1 : skillLevel) != null) {
                     skillTree.put((long)SkillData.getSkillHashCode(skillId, skillLevel + 1), new SkillHolder(skillId, skillLevel));
                  } else {
                     LOGGER.info(
                        this.getClass().getSimpleName() + ": Could not find skill with id " + skillId + ", level " + skillLevel + " for NPC " + npcId + "."
                     );
                  }
               }
            }
         }
      }
   }

   public int getAvailableLevel(Summon pet, int skillId) {
      int level = 0;
      if (!this._skillTrees.containsKey(pet.getId())) {
         return level;
      } else {
         for (SkillHolder skillHolder : this._skillTrees.get(pet.getId()).values()) {
            if (skillHolder.getSkillId() == skillId) {
               if (skillHolder.getSkillLevel() == 0) {
                  if (pet.getLevel() < 70) {
                     level = pet.getLevel() / 10;
                     if (level <= 0) {
                        level = 1;
                     }
                  } else {
                     level = 7 + (pet.getLevel() - 70) / 5;
                  }

                  int maxLevel = SkillData.getInstance().getMaxLevel(skillHolder.getSkillId());
                  if (level > maxLevel) {
                     level = maxLevel;
                  }
                  break;
               }

               if (1 <= pet.getLevel() && skillHolder.getSkillLevel() > level) {
                  level = skillHolder.getSkillLevel();
               }
            }
         }

         return level;
      }
   }

   public List<Integer> getAvailableSkills(Summon pet) {
      List<Integer> skillIds = new ArrayList<>();
      if (!this._skillTrees.containsKey(pet.getId())) {
         return skillIds;
      } else {
         for (SkillHolder skillHolder : this._skillTrees.get(pet.getId()).values()) {
            if (!skillIds.contains(skillHolder.getSkillId())) {
               skillIds.add(skillHolder.getSkillId());
            }
         }

         return skillIds;
      }
   }

   public List<Skill> getKnownSkills(Summon pet) {
      List<Skill> skills = new ArrayList<>();
      if (!this._skillTrees.containsKey(pet.getId())) {
         return skills;
      } else {
         for (SkillHolder skillHolder : this._skillTrees.get(pet.getId()).values()) {
            Skill skill = skillHolder.getSkill();
            if (!skills.contains(skill)) {
               skills.add(skill);
            }
         }

         return skills;
      }
   }

   public Skill getKnownSkill(Summon pet, int skillId) {
      if (!this._skillTrees.containsKey(pet.getId())) {
         return null;
      } else {
         for (SkillHolder skillHolder : this._skillTrees.get(pet.getId()).values()) {
            if (skillHolder.getSkillId() == skillId) {
               return skillHolder.getSkill();
            }
         }

         return null;
      }
   }

   public static PetSkillData getInstance() {
      return PetSkillData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PetSkillData INSTANCE = new PetSkillData();
   }
}
