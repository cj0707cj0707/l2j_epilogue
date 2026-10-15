package org.l2jmobius.gameserver.mechanics.skill.holders;

import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class SkillUseHolder extends SkillHolder {
   private final boolean _ctrlPressed;
   private final boolean _shiftPressed;

   public SkillUseHolder(Skill skill, boolean ctrlPressed, boolean shiftPressed) {
      super(skill);
      this._ctrlPressed = ctrlPressed;
      this._shiftPressed = shiftPressed;
   }

   public boolean isCtrlPressed() {
      return this._ctrlPressed;
   }

   public boolean isShiftPressed() {
      return this._shiftPressed;
   }
}
