package org.l2jmobius.gameserver.handler;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Player;

public interface IUserCommandHandler {
   Logger LOGGER = Logger.getLogger(IUserCommandHandler.class.getName());

   boolean onCommand(int var1, Player var2);

   int[] getCommandList();
}
