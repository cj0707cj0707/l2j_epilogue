package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerRace extends Condition {
   private final Set<Race> _races;

   public ConditionPlayerRace(Set<Race> races) {
      this._races = races;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effector != null && effector.isPlayer() ? this._races.contains(effector.asPlayer().getRace()) : false;
   }
}
