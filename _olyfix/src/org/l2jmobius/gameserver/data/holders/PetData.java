package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

public class PetData {
   private final Map<Integer, PetLevelData> _levelStats = new HashMap<>();
   private final List<PetData.PetSkillLearn> _skills = new ArrayList<>();
   private final int _npcId;
   private final int _itemId;
   private int _load = 20000;
   private int _hungryLimit = 1;
   private int _minLevel = 127;
   private int _maxLevel = 0;
   private boolean _syncLevel = false;
   private final Set<Integer> _food = new HashSet<>();

   public PetData(int npcId, int itemId) {
      this._npcId = npcId;
      this._itemId = itemId;
   }

   public int getNpcId() {
      return this._npcId;
   }

   public int getItemId() {
      return this._itemId;
   }

   public void addNewStat(int level, PetLevelData data) {
      if (this._minLevel > level) {
         this._minLevel = level;
      }

      if (this._maxLevel < level) {
         this._maxLevel = level;
      }

      this._levelStats.put(level, data);
   }

   public PetLevelData getPetLevelData(int petLevel) {
      return this._levelStats.get(petLevel);
   }

   public int getLoad() {
      return this._load;
   }

   public int getHungryLimit() {
      return this._hungryLimit;
   }

   public boolean isSynchLevel() {
      return this._syncLevel;
   }

   public int getMinLevel() {
      return this._minLevel;
   }

   public int getMaxLevel() {
      return this._maxLevel;
   }

   public Set<Integer> getFood() {
      return this._food;
   }

   public void addFood(Integer foodId) {
      this._food.add(foodId);
   }

   public void setLoad(int load) {
      this._load = load;
   }

   public void setHungryLimit(int limit) {
      this._hungryLimit = limit;
   }

   public void setSyncLevel(boolean value) {
      this._syncLevel = value;
   }

   public void addNewSkill(int skillId, int skillLevel, int petLvl) {
      this._skills.add(new PetData.PetSkillLearn(skillId, skillLevel, petLvl));
   }

   public int getAvailableLevel(int skillId, int petLvl) {
      int lvl = 0;
      boolean found = false;

      for (PetData.PetSkillLearn temp : this._skills) {
         if (temp.getSkillId() == skillId) {
            found = true;
            if (temp.getSkillLevel() == 0) {
               if (petLvl < 70) {
                  lvl = petLvl / 10;
                  if (lvl <= 0) {
                     lvl = 1;
                  }
               } else {
                  lvl = 7 + (petLvl - 70) / 5;
               }

               int maxLevel = SkillData.getInstance().getMaxLevel(temp.getSkillId());
               if (lvl > maxLevel) {
                  lvl = maxLevel;
               }
               break;
            }

            if (temp.getMinLevel() <= petLvl && temp.getSkillLevel() > lvl) {
               lvl = temp.getSkillLevel();
            }
         }
      }

      return found && lvl == 0 ? 1 : lvl;
   }

   public List<PetData.PetSkillLearn> getAvailableSkills() {
      return this._skills;
   }

   public static class PetSkillLearn extends SkillHolder {
      private final int _minLevel;

      public PetSkillLearn(int id, int lvl, int minLevel) {
         super(id, lvl);
         this._minLevel = minLevel;
      }

      public int getMinLevel() {
         return this._minLevel;
      }
   }
}
