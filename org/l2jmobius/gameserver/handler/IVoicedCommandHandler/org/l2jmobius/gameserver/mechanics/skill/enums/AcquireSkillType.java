package org.l2jmobius.gameserver.mechanics.skill.enums;

public enum AcquireSkillType {
   CLASS,
   FISHING,
   PLEDGE,
   SUBPLEDGE,
   TRANSFORM,
   TRANSFER,
   SUBCLASS,
   COLLECT;

   public static AcquireSkillType getAcquireSkillType(int id) {
      return values()[id];
   }
}
