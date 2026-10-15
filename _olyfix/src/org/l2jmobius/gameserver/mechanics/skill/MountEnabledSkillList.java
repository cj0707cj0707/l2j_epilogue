package org.l2jmobius.gameserver.mechanics.skill;

public class MountEnabledSkillList {
   private static final int STRIDER_SIEGE_ASSAULT = 325;
   private static final int WYVERN_BREATH = 4289;

   public static boolean contains(int skillId) {
      return skillId == 325 || skillId == 4289;
   }
}
