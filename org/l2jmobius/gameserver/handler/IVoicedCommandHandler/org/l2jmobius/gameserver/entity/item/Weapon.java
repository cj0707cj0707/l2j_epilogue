package org.l2jmobius.gameserver.entity.item;

import java.util.Collections;
import java.util.List;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionGameChance;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class Weapon extends ItemTemplate {
   private WeaponType _type;
   private boolean _isMagicWeapon;
   private int _soulShotCount;
   private int _spiritShotCount;
   private int _mpConsume;
   private int _baseAttackRange;
   private int _baseAttackRadius;
   private int _baseAttackAngle;
   private SkillHolder _enchant4Skill = null;
   private int _changeWeaponId;
   private SkillHolder _skillsOnMagic;
   private Condition _skillsOnMagicCondition = null;
   private SkillHolder _skillsOnCrit;
   private Condition _skillsOnCritCondition = null;
   private int _reducedSoulshot;
   private int _reducedSoulshotChance;
   private int _reducedMpConsume;
   private int _reducedMpConsumeChance;
   private boolean _isForceEquip;
   private boolean _isAttackWeapon;
   private boolean _useWeaponSkillsOnly;

   public Weapon(StatSet set) {
      super(set);
   }

   @Override
   public void set(StatSet set) {
      super.set(set);
      this._type = WeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
      this._type1 = 0;
      this._type2 = 0;
      this._isMagicWeapon = set.getBoolean("is_magic_weapon", false);
      this._soulShotCount = set.getInt("soulshots", 0);
      this._spiritShotCount = set.getInt("spiritshots", 0);
      this._mpConsume = set.getInt("mp_consume", 0);
      this._baseAttackRange = set.getInt("attack_range", 40);
      String[] damageRange = set.getString("damage_range", "").split(";");
      if (damageRange.length > 1 && StringUtil.isNumeric(damageRange[2]) && StringUtil.isNumeric(damageRange[3])) {
         this._baseAttackRadius = Integer.parseInt(damageRange[2]);
         this._baseAttackAngle = Integer.parseInt(damageRange[3]);
      } else {
         this._baseAttackRadius = 40;
         this._baseAttackAngle = 0;
      }

      String[] reducedSoulshots = set.getString("reduced_soulshot", "").split(",");
      this._reducedSoulshotChance = reducedSoulshots.length == 2 ? Integer.parseInt(reducedSoulshots[0]) : 0;
      this._reducedSoulshot = reducedSoulshots.length == 2 ? Integer.parseInt(reducedSoulshots[1]) : 0;
      String[] reducedMpConsume = set.getString("reduced_mp_consume", "").split(",");
      this._reducedMpConsumeChance = reducedMpConsume.length == 2 ? Integer.parseInt(reducedMpConsume[0]) : 0;
      this._reducedMpConsume = reducedMpConsume.length == 2 ? Integer.parseInt(reducedMpConsume[1]) : 0;
      String skill = set.getString("enchant4_skill", null);
      if (skill != null) {
         String[] info = skill.split("-");
         if (info != null && info.length == 2) {
            int id = 0;
            int level = 0;

            try {
               id = Integer.parseInt(info[0]);
               level = Integer.parseInt(info[1]);
            } catch (Exception var13) {
               LOGGER.info("> Could not parse " + skill + " in weapon enchant skills! item " + this);
            }

            if (id > 0 && level > 0) {
               this._enchant4Skill = new SkillHolder(id, level);
            }
         }
      }

      skill = set.getString("onmagic_skill", null);
      if (skill != null) {
         String[] info = skill.split("-");
         int chance = set.getInt("onmagic_chance", 100);
         if (info != null && info.length == 2) {
            int id = 0;
            int level = 0;

            try {
               id = Integer.parseInt(info[0]);
               level = Integer.parseInt(info[1]);
            } catch (Exception var12) {
               LOGGER.info("> Could not parse " + skill + " in weapon onmagic skills! item " + this);
            }

            if (id > 0 && level > 0 && chance > 0) {
               this._skillsOnMagic = new SkillHolder(id, level);
               this._skillsOnMagicCondition = new ConditionGameChance(chance);
            }
         }
      }

      skill = set.getString("oncrit_skill", null);
      if (skill != null) {
         String[] info = skill.split("-");
         int chance = set.getInt("oncrit_chance", 100);
         if (info != null && info.length == 2) {
            int id = 0;
            int level = 0;

            try {
               id = Integer.parseInt(info[0]);
               level = Integer.parseInt(info[1]);
            } catch (Exception var11) {
               LOGGER.info("> Could not parse " + skill + " in weapon oncrit skills! item " + this);
            }

            if (id > 0 && level > 0 && chance > 0) {
               this._skillsOnCrit = new SkillHolder(id, level);
               this._skillsOnCritCondition = new ConditionGameChance(chance);
            }
         }
      }

      this._changeWeaponId = set.getInt("change_weaponId", 0);
      this._isForceEquip = set.getBoolean("isForceEquip", false);
      this._isAttackWeapon = set.getBoolean("isAttackWeapon", true);
      this._useWeaponSkillsOnly = set.getBoolean("useWeaponSkillsOnly", false);
      if (this._reuseDelay == 0 && this._type.isRanged()) {
         this._reuseDelay = 1500;
      }
   }

   public WeaponType getItemType() {
      return this._type;
   }

   @Override
   public int getItemMask() {
      return this._type.mask();
   }

   @Override
   public boolean isWeapon() {
      return true;
   }

   @Override
   public boolean isMagicWeapon() {
      return this._isMagicWeapon;
   }

   public int getSoulShotCount() {
      return this._soulShotCount;
   }

   public int getSpiritShotCount() {
      return this._spiritShotCount;
   }

   public int getReducedSoulShot() {
      return this._reducedSoulshot;
   }

   public int getReducedSoulShotChance() {
      return this._reducedSoulshotChance;
   }

   public int getMpConsume() {
      return this._mpConsume;
   }

   public int getBaseAttackRange() {
      return this._baseAttackRange;
   }

   public int getBaseAttackRadius() {
      return this._baseAttackRadius;
   }

   public int getBaseAttackAngle() {
      return this._baseAttackAngle;
   }

   public int getReducedMpConsume() {
      return this._reducedMpConsume;
   }

   public int getReducedMpConsumeChance() {
      return this._reducedMpConsumeChance;
   }

   @Override
   public Skill getEnchant4Skill() {
      return this._enchant4Skill == null ? null : this._enchant4Skill.getSkill();
   }

   public int getChangeWeaponId() {
      return this._changeWeaponId;
   }

   public boolean isForceEquip() {
      return this._isForceEquip;
   }

   public boolean isAttackWeapon() {
      return this._isAttackWeapon;
   }

   public boolean useWeaponSkillsOnly() {
      return this._useWeaponSkillsOnly;
   }

   public void castOnCriticalSkill(Creature caster, Creature target) {
      if (this._skillsOnCrit != null) {
         Skill onCritSkill = this._skillsOnCrit.getSkill();
         if (this._skillsOnCritCondition == null || this._skillsOnCritCondition.test(caster, target, onCritSkill)) {
            if (onCritSkill.checkCondition(caster, target, false)) {
               onCritSkill.activateSkill(caster, Collections.singletonList(target));
            }
         }
      }
   }

   public void castOnMagicSkill(Creature caster, Creature target, Skill trigger) {
      if (this._skillsOnMagic != null) {
         Skill onMagicSkill = this._skillsOnMagic.getSkill();
         if (trigger.hasNegativeEffect() == onMagicSkill.hasNegativeEffect()) {
            if (trigger.isMagic() || onMagicSkill.isMagic()) {
               if (!trigger.isToggle()) {
                  if (caster.getAI().getCastTarget() == target) {
                     if (this._skillsOnMagicCondition == null || this._skillsOnMagicCondition.test(caster, target, onMagicSkill)) {
                        if (onMagicSkill.checkCondition(caster, target, false)) {
                           if (!onMagicSkill.hasNegativeEffect() || Formulas.calcShldUse(caster, target, onMagicSkill) != 2) {
                              onMagicSkill.activateSkill(caster, Collections.singletonList(target));
                              if (caster.isPlayer()) {
                                 List<WorldObject> targets = Collections.singletonList(target);
                                 World.forEachVisibleObjectInRange(
                                    caster,
                                    Npc.class,
                                    1000,
                                    npc -> {
                                       if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_SEE, npc)) {
                                          EventDispatcher.getInstance()
                                             .notifyEventAsync(new OnNpcSkillSee(npc, caster.asPlayer(), onMagicSkill, targets, false), npc);
                                       }
                                    }
                                 );
                              }

                              if (caster.isPlayer()) {
                                 SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED);
                                 sm.addSkillName(onMagicSkill);
                                 caster.sendPacket(sm);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean isRange() {
      return this.isBow() || this.isCrossBow();
   }

   public boolean isBow() {
      return this._type == WeaponType.BOW;
   }

   public boolean isCrossBow() {
      return this._type == WeaponType.CROSSBOW;
   }
}
