package org.l2jmobius.gameserver.mechanics.skill;

public enum SkillOperateType {
   A1,
   A2,
   A3,
   A4,
   CA1,
   CA5,
   DA1,
   DA2,
   P,
   T;

   public boolean isActive() {
      switch (this) {
         case A1:
         case A2:
         case A3:
         case A4:
         case CA1:
         case CA5:
         case DA1:
         case DA2:
            return true;
         default:
            return false;
      }
   }

   public boolean isContinuous() {
      switch (this) {
         case A2:
         case A4:
         case DA2:
            return true;
         default:
            return false;
      }
   }

   public boolean isSelfContinuous() {
      return this == A3;
   }

   public boolean isPassive() {
      return this == P;
   }

   public boolean isToggle() {
      return this == T;
   }

   public boolean isChanneling() {
      switch (this) {
         case CA1:
         case CA5:
            return true;
         default:
            return false;
      }
   }
}
