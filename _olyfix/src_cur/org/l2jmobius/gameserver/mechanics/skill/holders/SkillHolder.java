package org.l2jmobius.gameserver.mechanics.skill.holders;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class SkillHolder {
   private final int _skillId;
   private final int _skillLevel;
   private Skill _skill;

   public SkillHolder(int skillId, int skillLevel) {
      this._skillId = skillId;
      this._skillLevel = skillLevel;
      this._skill = null;
   }

   public SkillHolder(Skill skill) {
      this._skillId = skill.getId();
      this._skillLevel = skill.getLevel();
      this._skill = skill;
   }

   public int getSkillId() {
      return this._skillId;
   }

   public int getSkillLevel() {
      return this._skillLevel;
   }

   public Skill getSkill() {
      if (this._skill == null) {
         this._skill = SkillData.getInstance().getSkill(this._skillId, Math.max(this._skillLevel, 1));
      }

      return this._skill;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return !(obj instanceof SkillHolder holder) ? false : holder.getSkillId() == this._skillId && holder.getSkillLevel() == this._skillLevel;
      }
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + this._skillId;
      return 31 * result + this._skillLevel;
   }

   @Override
   public String toString() {
      return "[SkillId: " + this._skillId + " Level: " + this._skillLevel + "]";
   }
}
