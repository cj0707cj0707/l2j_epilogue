package org.l2jmobius.gameserver.data;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class HeroSkillTable {
   private static final Skill[] _heroSkills = new Skill[5];
   private static final int[] _heroSkillsId = new int[]{395, 396, 1374, 1375, 1376};

   HeroSkillTable() {
      for (int i = 0; i < _heroSkillsId.length; i++) {
         _heroSkills[i] = SkillData.getInstance().getSkill(_heroSkillsId[i], 1);
      }
   }

   public static HeroSkillTable getInstance() {
      return HeroSkillTable.SingletonHolder._instance;
   }

   public static Skill[] getHeroSkills() {
      return _heroSkills;
   }

   public static boolean isHeroSkill(int skillId) {
      for (int id : _heroSkillsId) {
         if (id == skillId) {
            return true;
         }
      }

      return false;
   }

   private static class SingletonHolder {
      protected static final HeroSkillTable _instance = new HeroSkillTable();
   }
}
