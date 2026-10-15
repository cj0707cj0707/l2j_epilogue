package org.l2jmobius.gameserver.network;

import java.util.logging.Logger;

public class PacketLogger {
   private static final Logger LOGGER = Logger.getLogger(PacketLogger.class.getName());

   public static synchronized void warning(String message) {
      LOGGER.warning(message);
   }

   public static synchronized void info(String message) {
      LOGGER.info(message);
   }

   public static synchronized void finer(String message) {
      LOGGER.finer(message);
   }
}
