package org.l2jmobius.gameserver.handler;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.entity.actor.Player;

public interface IVoicedCommandHandler {
   Logger LOGGER = Logger.getLogger(IVoicedCommandHandler.class.getName());

   boolean onCommand(String var1, Player var2, String var3);

   String[] getCommandList();
}
