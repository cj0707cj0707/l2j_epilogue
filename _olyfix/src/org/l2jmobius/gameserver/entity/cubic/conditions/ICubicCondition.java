package org.l2jmobius.gameserver.entity.cubic.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;

public interface ICubicCondition {
   boolean test(Cubic var1, Creature var2, WorldObject var3);
}
