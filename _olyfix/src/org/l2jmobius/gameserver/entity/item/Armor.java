package org.l2jmobius.gameserver.entity.item;

import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

public class Armor extends ItemTemplate {
   private SkillHolder _enchant4Skill = null;
   private ArmorType _type;

   public Armor(StatSet set) {
      super(set);
   }

   @Override
   public void set(StatSet set) {
      super.set(set);
      this._type = set.getEnum("armor_type", ArmorType.class, ArmorType.NONE);
      BodyPart bodyPart = this.getBodyPart();
      if (bodyPart != BodyPart.NECK
         && bodyPart != BodyPart.L_EAR
         && bodyPart != BodyPart.R_EAR
         && bodyPart != BodyPart.LR_EAR
         && bodyPart != BodyPart.L_FINGER
         && bodyPart != BodyPart.R_FINGER
         && bodyPart != BodyPart.LR_FINGER
         && bodyPart != BodyPart.R_BRACELET
         && bodyPart != BodyPart.L_BRACELET) {
         if (this._type == ArmorType.NONE && bodyPart == BodyPart.L_HAND) {
            this._type = ArmorType.SHIELD;
         }

         this._type1 = 1;
         this._type2 = 1;
      } else {
         this._type1 = 0;
         this._type2 = 2;
      }

      String skill = set.getString("enchant4_skill", null);
      if (skill != null) {
         String[] info = skill.split("-");
         if (info != null && info.length == 2) {
            int id = 0;
            int level = 0;

            try {
               id = Integer.parseInt(info[0]);
               level = Integer.parseInt(info[1]);
            } catch (Exception var8) {
               LOGGER.info("> Could not parse " + skill + " in armor enchant skills! item " + this);
            }

            if (id > 0 && level > 0) {
               this._enchant4Skill = new SkillHolder(id, level);
            }
         }
      }
   }

   public ArmorType getItemType() {
      return this._type;
   }

   @Override
   public int getItemMask() {
      return this._type.mask();
   }

   @Override
   public boolean isArmor() {
      return true;
   }

   @Override
   public Skill getEnchant4Skill() {
      return this._enchant4Skill == null ? null : this._enchant4Skill.getSkill();
   }
}
