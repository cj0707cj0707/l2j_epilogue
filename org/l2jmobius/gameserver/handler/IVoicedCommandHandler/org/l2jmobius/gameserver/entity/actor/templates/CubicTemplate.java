package org.l2jmobius.gameserver.entity.actor.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.cubic.CubicSelectionMode;
import org.l2jmobius.gameserver.entity.cubic.CubicSkill;
import org.l2jmobius.gameserver.entity.cubic.CubicTargetType;
import org.l2jmobius.gameserver.entity.cubic.ICubicConditionHolder;
import org.l2jmobius.gameserver.entity.cubic.conditions.ICubicCondition;
import org.l2jmobius.gameserver.util.StatSet;

public class CubicTemplate extends CreatureTemplate implements ICubicConditionHolder {
   private final int _id;
   private final int _level;
   private final int _slot;
   private final int _duration;
   private final int _delay;
   private final int _maxCount;
   private final int _useUp;
   private final double _power;
   private final CubicTargetType _targetType;
   private final CubicSelectionMode _selectionMode;
   private final List<ICubicCondition> _conditions = new ArrayList<>();
   private final List<CubicSkill> _skills = new ArrayList<>();

   public CubicTemplate(StatSet set) {
      super(set);
      this._id = set.getInt("id");
      this._level = set.getInt("level");
      this._slot = set.getInt("slot");
      this._duration = set.getInt("duration");
      this._delay = set.getInt("delay");
      this._maxCount = set.getInt("maxCount");
      this._useUp = set.getInt("useUp");
      this._power = set.getDouble("power", 0.0);
      this._targetType = set.getEnum("targetType", CubicTargetType.class, CubicTargetType.TARGET);
      this._selectionMode = set.getEnum("selectionMode", CubicSelectionMode.class, CubicSelectionMode.RANDOM);
   }

   public int getId() {
      return this._id;
   }

   public int getLevel() {
      return this._level;
   }

   public int getSlot() {
      return this._slot;
   }

   public int getDuration() {
      return this._duration;
   }

   public int getDelay() {
      return this._delay;
   }

   public int getMaxCount() {
      return this._maxCount;
   }

   public int getUseUp() {
      return this._useUp;
   }

   public CubicTargetType getTargetType() {
      return this._targetType;
   }

   public CubicSelectionMode getSelectionMode() {
      return this._selectionMode;
   }

   public List<CubicSkill> getCubicSkills() {
      return Collections.unmodifiableList(this._skills);
   }

   public void addCubicSkill(CubicSkill skill) {
      this._skills.add(skill);
   }

   @Override
   public int getBasePAtk() {
      return (int)this._power;
   }

   @Override
   public int getBaseMAtk() {
      return (int)this._power;
   }

   @Override
   public boolean validateConditions(Cubic cubic, Creature owner, WorldObject target) {
      if (this._conditions.isEmpty()) {
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

   @Override
   public void addCondition(ICubicCondition condition) {
      this._conditions.add(condition);
   }
}
