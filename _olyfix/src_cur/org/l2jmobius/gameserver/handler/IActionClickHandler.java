package org.l2jmobius.gameserver.handler;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;

public interface IActionClickHandler {
   Logger LOGGER = Logger.getLogger(IActionClickHandler.class.getName());

   boolean onAction(Player var1, WorldObject var2, boolean var3);

   InstanceType getInstanceType();
}
