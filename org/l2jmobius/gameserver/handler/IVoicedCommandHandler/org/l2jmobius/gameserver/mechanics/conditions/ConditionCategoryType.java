package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionCategoryType extends Condition {
   private final Set<CategoryType> _categoryTypes;

   public ConditionCategoryType(Set<CategoryType> categoryTypes) {
      this._categoryTypes = categoryTypes;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      for (CategoryType type : this._categoryTypes) {
         if (effector.isInCategory(type)) {
            return true;
         }
      }

      return false;
   }
}
