package org.l2jmobius.gameserver.handler;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;

public interface IBypassHandler {
   Logger LOGGER = Logger.getLogger(IBypassHandler.class.getName());

   boolean onCommand(String var1, Player var2, Creature var3);

   String[] getCommandList();
}
