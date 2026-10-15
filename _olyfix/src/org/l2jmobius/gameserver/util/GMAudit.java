package org.l2jmobius.gameserver.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GMAudit {
   private static final Logger LOGGER = Logger.getLogger("gmaudit");
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy H:mm:ss");
   private static final char[] ILLEGAL_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};

   public static void logAction(String gmName, String action, String target, String params) {
      String timestamp = DATE_FORMAT.format(LocalDateTime.now());
      String sanitizedGmName = sanitizeFileName(gmName);
      if (!isValidFileName(sanitizedGmName)) {
         sanitizedGmName = "INVALID_GM_NAME_" + timestamp;
      }

      File logFile = new File("log/GMAudit/" + sanitizedGmName + ".txt");

      try (FileWriter writer = new FileWriter(logFile, true)) {
         writer.write(timestamp + ">" + gmName + ">" + action + ">" + target + ">" + params + System.lineSeparator());
      } catch (IOException var12) {
         LOGGER.log(Level.SEVERE, "Could not save GMAudit log for GM " + gmName + ":", (Throwable)var12);
      }
   }

   public static void logAction(String gmName, String action, String target) {
      logAction(gmName, action, target, "");
   }

   private static String sanitizeFileName(String name) {
      String sanitized = name;

      for (char illegalChar : ILLEGAL_CHARACTERS) {
         sanitized = sanitized.replace(illegalChar, '_');
      }

      return sanitized;
   }

   private static boolean isValidFileName(String name) {
      File file = new File(name);

      try {
         file.getCanonicalPath();
         return true;
      } catch (IOException var3) {
         return false;
      }
   }

   static {
      new File("log/GMAudit").mkdirs();
   }
}
