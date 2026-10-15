package org.l2jmobius.gameserver.entity.cubic;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.cubic.conditions.ICubicCondition;

public interface ICubicConditionHolder {
   boolean validateConditions(Cubic var1, Creature var2, WorldObject var3);

   void addCondition(ICubicCondition var1);
}
