package org.l2jmobius.gameserver.mechanics.skill;

import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

public enum CommonSkill {
   FAKE_DEATH(60, 1),
   RAID_CURSE(4215, 1),
   RAID_CURSE2(4515, 1),
   SEAL_OF_RULER(246, 1),
   BUILD_HEADQUARTERS(247, 1),
   SHADOW_SENSE(294, 1),
   WYVERN_BREATH(4289, 1),
   STRIDER_SIEGE_ASSAULT(325, 1),
   FIREWORK(5965, 1),
   LARGE_FIREWORK(2025, 1),
   BLESSING_OF_PROTECTION(5182, 1),
   VOID_BURST(3630, 1),
   VOID_FLOW(3631, 1),
   THE_VICTOR_OF_WAR(5074, 1),
   THE_VANQUISHED_OF_WAR(5075, 1),
   SPECIAL_TREE_RECOVERY_BONUS(2139, 1),
   WEAPON_GRADE_PENALTY(6209, 1),
   ARMOR_GRADE_PENALTY(6213, 1),
   CREATE_DWARVEN(172, 1),
   LUCKY(194, 1),
   EXPERTISE(239, 1),
   CRYSTALLIZE(248, 1),
   ONYX_BEAST_TRANSFORMATION(617, 1),
   CREATE_COMMON(1320, 1),
   DIVINE_INSPIRATION(1405, 1),
   SERVITOR_SHARE(1557, 1),
   CARAVANS_SECRET_MEDICINE(2341, 1),
   FROG_TRANSFORM(6201, 1),
   CHILD_TRANSFORM(6202, 1),
   NATIVE_TRANSFORM(6203, 1);

   private final SkillHolder _holder;

   private CommonSkill(int id, int level) {
      this._holder = new SkillHolder(id, level);
   }

   public int getId() {
      return this._holder.getSkillId();
   }

   public int getLevel() {
      return this._holder.getSkillLevel();
   }

   public Skill getSkill() {
      return this._holder.getSkill();
   }
}
