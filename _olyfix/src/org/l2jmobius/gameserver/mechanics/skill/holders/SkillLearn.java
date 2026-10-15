package org.l2jmobius.gameserver.mechanics.skill.holders;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.SocialClass;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.util.StatSet;

public class SkillLearn {
   private final String _skillName;
   private final int _skillId;
   private final int _skillLevel;
   private final int _getLevel;
   private final boolean _autoGet;
   private final int _levelUpSp;
   private final Set<ItemHolder> _requiredItems = new HashSet<>(1);
   private final Set<Race> _races = EnumSet.noneOf(Race.class);
   private final Set<SkillHolder> _preReqSkills = new HashSet<>(1);
   private SocialClass _socialClass;
   private final boolean _residenceSkill;
   private final Set<Integer> _residenceIds = new HashSet<>(1);
   private final List<SkillLearn.SubClassData> _subClassLvlNumber = new ArrayList<>(1);
   private final boolean _learnedByNpc;
   private final boolean _learnedByFS;

   public SkillLearn(StatSet set) {
      this._skillName = set.getString("skillName");
      this._skillId = set.getInt("skillId");
      this._skillLevel = set.getInt("skillLevel");
      this._getLevel = set.getInt("getLevel");
      this._autoGet = set.getBoolean("autoGet", false);
      this._levelUpSp = set.getInt("levelUpSp", 0);
      this._residenceSkill = set.getBoolean("residenceSkill", false);
      this._learnedByNpc = set.getBoolean("learnedByNpc", false);
      this._learnedByFS = set.getBoolean("learnedByFS", false);
   }

   public String getName() {
      return this._skillName;
   }

   public int getSkillId() {
      return this._skillId;
   }

   public int getSkillLevel() {
      return this._skillLevel;
   }

   public int getGetLevel() {
      return this._getLevel;
   }

   public int getLevelUpSp() {
      return this._levelUpSp;
   }

   public boolean isAutoGet() {
      return this._autoGet;
   }

   public Set<ItemHolder> getRequiredItems() {
      return this._requiredItems;
   }

   public void addRequiredItem(ItemHolder item) {
      this._requiredItems.add(item);
   }

   public Set<Race> getRaces() {
      return this._races;
   }

   public void addRace(Race race) {
      this._races.add(race);
   }

   public Set<SkillHolder> getPreReqSkills() {
      return this._preReqSkills;
   }

   public void addPreReqSkill(SkillHolder skill) {
      this._preReqSkills.add(skill);
   }

   public SocialClass getSocialClass() {
      return this._socialClass;
   }

   public void setSocialClass(SocialClass socialClass) {
      if (this._socialClass == null) {
         this._socialClass = socialClass;
      }
   }

   public boolean isResidencialSkill() {
      return this._residenceSkill;
   }

   public Set<Integer> getResidenceIds() {
      return this._residenceIds;
   }

   public void addResidenceId(Integer id) {
      this._residenceIds.add(id);
   }

   public List<SkillLearn.SubClassData> getSubClassConditions() {
      return this._subClassLvlNumber;
   }

   public void addSubclassConditions(int slot, int level) {
      this._subClassLvlNumber.add(new SkillLearn.SubClassData(slot, level));
   }

   public boolean isLearnedByNpc() {
      return this._learnedByNpc;
   }

   public boolean isLearnedByFS() {
      return this._learnedByFS;
   }

   public int getCalculatedLevelUpSp(PlayerClass playerClass, PlayerClass learningClass) {
      if (playerClass != null && learningClass != null) {
         int levelUpSp = this._levelUpSp;
         if (PlayerConfig.ALT_GAME_SKILL_LEARN && playerClass != learningClass) {
            if (playerClass.isMage() != learningClass.isMage()) {
               levelUpSp *= 3;
            } else {
               levelUpSp *= 2;
            }
         }

         return levelUpSp;
      } else {
         return this._levelUpSp;
      }
   }

   public class SubClassData {
      private final int slot;
      private final int level;

      public SubClassData(int pSlot, int pLvl) {
         Objects.requireNonNull(SkillLearn.this);
         super();
         this.slot = pSlot;
         this.level = pLvl;
      }

      public int getSlot() {
         return this.slot;
      }

      public int getLvl() {
         return this.level;
      }
   }
}
