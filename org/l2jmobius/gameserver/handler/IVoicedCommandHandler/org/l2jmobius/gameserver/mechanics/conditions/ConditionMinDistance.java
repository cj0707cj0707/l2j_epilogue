package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public class ConditionMinDistance extends Condition {
   private final int _distance;

   public ConditionMinDistance(int distance) {
      this._distance = distance;
   }

   @Override
   public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item) {
      return effected != null
         && effector.calculateDistance3D(effected) >= this._distance
         && GeoEngine.getInstance().canSeeTarget(effector, (WorldObject)effected);
   }
}
