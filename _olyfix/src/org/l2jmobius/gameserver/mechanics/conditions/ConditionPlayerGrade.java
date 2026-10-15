package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerGrade extends Condition {
   protected static final Logger LOGGER = Logger.getLogger(ConditionPlayerGrade.class.getName());
   public static final int COND_NO_GRADE = 1;
   public static final int COND_D_GRADE = 2;
   public static final int COND_C_GRADE = 4;
   public static final int COND_B_GRADE = 8;
   public static final int COND_A_GRADE = 16;
   public static final int COND_S_GRADE = 32;
   public static final int COND_S80_GRADE = 64;
   public static final int COND_S84_GRADE = 128;
   private final int _value;

   public ConditionPlayerGrade(int value) {
      this._value = value;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effector.isPlayer() && this._value == (byte)effector.asPlayer().getExpertiseLevel();
   }
}
