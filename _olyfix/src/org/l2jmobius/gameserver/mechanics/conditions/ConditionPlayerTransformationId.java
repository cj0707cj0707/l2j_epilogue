package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionPlayerTransformationId extends Condition {
   private final int _id;

   public ConditionPlayerTransformationId(int id) {
      this._id = id;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      Player player = effector.asPlayer();
      return player != null && (this._id == -1 ? player.isTransformed() : player.getTransformationId() == this._id);
   }
}
