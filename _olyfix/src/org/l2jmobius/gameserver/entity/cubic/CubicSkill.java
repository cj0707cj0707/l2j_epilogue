package org.l2jmobius.gameserver.entity.cubic;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.cubic.conditions.ICubicCondition;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

public class CubicSkill extends SkillHolder implements ICubicConditionHolder {
   private final int _triggerRate;
   private final CubicTargetType _targetType;
   private final boolean _targetDebuff;
   private final boolean _priority;
   private final List<ICubicCondition> _conditions = new ArrayList<>();

   public CubicSkill(StatSet set) {
      super(set.getInt("id"), set.getInt("level"));
      this._triggerRate = set.getInt("triggerRate", 100);
      this._targetType = set.getEnum("target", CubicTargetType.class, CubicTargetType.TARGET);
      this._targetDebuff = set.getBoolean("targetDebuff", false);
      this._priority = set.getBoolean("priority", false);
   }

   public int getTriggerRate() {
      return this._triggerRate;
   }

   public CubicTargetType getTargetType() {
      return this._targetType;
   }

   public boolean isPriority() {
      return this._priority;
   }

   @Override
   public boolean validateConditions(Cubic cubic, Creature owner, WorldObject target) {
      if (target == null) {
         return false;
      } else if (this._targetDebuff && !this.hasDispellableDebuff(target)) {
         return false;
      } else if (this._conditions.isEmpty()) {
         return true;
      } else {
         for (ICubicCondition condition : this._conditions) {
            if (!condition.test(cubic, owner, target)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean hasDispellableDebuff(WorldObject target) {
      if (!target.isCreature()) {
         return false;
      } else {
         for (BuffInfo info : target.asCreature().getEffectList().getDebuffs()) {
            if (info.getSkill().canBeDispeled()) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public void addCondition(ICubicCondition condition) {
      this._conditions.add(condition);
   }
}
