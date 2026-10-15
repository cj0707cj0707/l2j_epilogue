package org.l2jmobius.gameserver.handler;

import java.util.logging.Logger;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;

public interface IPunishmentHandler {
   Logger LOGGER = Logger.getLogger(IPunishmentHandler.class.getName());

   void onStart(PunishmentTask var1);

   void onEnd(PunishmentTask var1);

   PunishmentType getType();
}
