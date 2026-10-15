package org.l2jmobius.gameserver.config;

import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.commons.util.ConfigReader;

public class DevelopmentConfig {
   private static final String DEVELOPMENT_CONFIG_FILE = "./config/Development.ini";
   public static boolean LOG_SERVER_LOAD_TIMES;
   public static boolean HTML_ACTION_CACHE_DEBUG;
   public static boolean NO_QUESTS;
   public static boolean NO_SPAWNS;
   public static boolean SHOW_QUEST_LOAD_IN_LOGS;
   public static boolean SHOW_SCRIPT_LOAD_IN_LOGS;
   public static boolean DEBUG_CLIENT_PACKETS;
   public static boolean DEBUG_EX_CLIENT_PACKETS;
   public static boolean DEBUG_SERVER_PACKETS;
   public static boolean DEBUG_UNKNOWN_PACKETS;
   public static Set<String> EXCLUDED_DEBUG_PACKETS;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Development.ini");
      LOG_SERVER_LOAD_TIMES = config.getBoolean("LogServerLoadTimes", false);
      HTML_ACTION_CACHE_DEBUG = config.getBoolean("HtmlActionCacheDebug", false);
      NO_QUESTS = config.getBoolean("NoQuests", false);
      NO_SPAWNS = config.getBoolean("NoSpawns", false);
      SHOW_QUEST_LOAD_IN_LOGS = config.getBoolean("ShowQuestLoadInLogs", false);
      SHOW_SCRIPT_LOAD_IN_LOGS = config.getBoolean("ShowScriptLoadInLogs", false);
      DEBUG_CLIENT_PACKETS = config.getBoolean("DebugClientPackets", false);
      DEBUG_EX_CLIENT_PACKETS = config.getBoolean("DebugExClientPackets", false);
      DEBUG_SERVER_PACKETS = config.getBoolean("DebugServerPackets", false);
      DEBUG_UNKNOWN_PACKETS = config.getBoolean("DebugUnknownPackets", true);
      String[] packets = config.getString("ExcludedPacketList", "").trim().split(",");
      EXCLUDED_DEBUG_PACKETS = new HashSet<>(packets.length);

      for (String packet : packets) {
         EXCLUDED_DEBUG_PACKETS.add(packet.trim());
      }
   }
}
