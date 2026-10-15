package org.l2jmobius.gameserver.handler;

import java.util.List;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

public interface ITargetTypeHandler {
   List<WorldObject> getTargetList(Skill var1, Creature var2, boolean var3, Creature var4);

   Enum<TargetType> getTargetType();
}
