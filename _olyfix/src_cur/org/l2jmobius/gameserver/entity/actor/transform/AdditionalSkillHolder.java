package org.l2jmobius.gameserver.entity.actor.transform;

import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

public class AdditionalSkillHolder extends SkillHolder {
   private final int _minLevel;

   public AdditionalSkillHolder(int skillId, int skillLevel, int minLevel) {
      super(skillId, skillLevel);
      this._minLevel = minLevel;
   }

   public int getMinLevel() {
      return this._minLevel;
   }
}
