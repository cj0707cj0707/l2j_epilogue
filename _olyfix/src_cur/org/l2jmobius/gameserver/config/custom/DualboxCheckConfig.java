package org.l2jmobius.gameserver.config.custom;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;

public class DualboxCheckConfig {
   private static final Logger LOGGER = Logger.getLogger(DualboxCheckConfig.class.getName());
   private static final String DUALBOX_CHECK_CONFIG_FILE = "./config/Custom/DualboxCheck.ini";
   public static int DUALBOX_CHECK_MAX_PLAYERS_PER_IP;
   public static int DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP;
   public static int DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP;
   public static int DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP;
   public static int DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP;
   public static boolean DUALBOX_COUNT_OFFLINE_TRADERS;
   public static Map<Integer, Integer> DUALBOX_CHECK_WHITELIST;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/DualboxCheck.ini");
      DUALBOX_CHECK_MAX_PLAYERS_PER_IP = config.getInt("DualboxCheckMaxPlayersPerIP", 0);
      DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP = config.getInt("DualboxCheckMaxOlympiadParticipantsPerIP", 0);
      DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP = config.getInt("DualboxCheckMaxL2EventParticipantsPerIP", 0);
      DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP = config.getInt("DualboxCheckMaxOfflinePlayPerIP", 0);
      DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP = config.getInt("DualboxCheckMaxOfflinePlayPremiumPerIP", 0);
      DUALBOX_COUNT_OFFLINE_TRADERS = config.getBoolean("DualboxCountOfflineTraders", false);
      String[] dualboxCheckWhiteList = config.getString("DualboxCheckWhitelist", "127.0.0.1,0").split(";");
      DUALBOX_CHECK_WHITELIST = new HashMap<>(dualboxCheckWhiteList.length);

      for (String entry : dualboxCheckWhiteList) {
         String[] entrySplit = entry.split(",");
         if (entrySplit.length != 2) {
            LOGGER.warning(StringUtil.concat("DualboxCheck[DualboxCheckConfig.load()]: invalid config property -> DualboxCheckWhitelist \"", entry, "\""));
         } else {
            try {
               int num = Integer.parseInt(entrySplit[1]);
               num = num == 0 ? -1 : num;
               DUALBOX_CHECK_WHITELIST.put(InetAddress.getByName(entrySplit[0]).hashCode(), num);
            } catch (UnknownHostException var8) {
               LOGGER.warning(StringUtil.concat("DualboxCheck[DualboxCheckConfig.load()]: invalid address -> DualboxCheckWhitelist \"", entrySplit[0], "\""));
            } catch (NumberFormatException var9) {
               LOGGER.warning(StringUtil.concat("DualboxCheck[DualboxCheckConfig.load()]: invalid number -> DualboxCheckWhitelist \"", entrySplit[1], "\""));
            }
         }
      }
   }
}
