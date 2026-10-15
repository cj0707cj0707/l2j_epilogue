package org.l2jmobius.gameserver.mechanics.options;

import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class OptionSkillHolder {
   private final Skill _skill;
   private final double _chance;
   private final OptionSkillType _type;

   public OptionSkillHolder(Skill skill, double chance, OptionSkillType type) {
      this._skill = skill;
      this._chance = chance;
      this._type = type;
   }

   public Skill getSkill() {
      return this._skill;
   }

   public double getChance() {
      return this._chance;
   }

   public OptionSkillType getSkillType() {
      return this._type;
   }
}
