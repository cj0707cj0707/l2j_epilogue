package org.l2jmobius.gameserver.config.custom;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;

public class OfflineTradeConfig {
   private static final String OFFLINE_TRADE_CONFIG_FILE = "./config/Custom/OfflineTrade.ini";
   public static boolean OFFLINE_TRADE_ENABLE;
   public static boolean OFFLINE_CRAFT_ENABLE;
   public static boolean OFFLINE_MODE_IN_PEACE_ZONE;
   public static boolean OFFLINE_MODE_NO_DAMAGE;
   public static boolean OFFLINE_SET_NAME_COLOR;
   public static int OFFLINE_NAME_COLOR;
   public static boolean OFFLINE_FAME;
   public static boolean RESTORE_OFFLINERS;
   public static int OFFLINE_MAX_DAYS;
   public static boolean OFFLINE_DISCONNECT_FINISHED;
   public static boolean OFFLINE_DISCONNECT_SAME_ACCOUNT;
   public static boolean STORE_OFFLINE_TRADE_IN_REALTIME;
   public static boolean ENABLE_OFFLINE_COMMAND;
   public static List<AbnormalVisualEffect> OFFLINE_ABNORMAL_EFFECTS = new ArrayList<>();

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/OfflineTrade.ini");
      OFFLINE_TRADE_ENABLE = config.getBoolean("OfflineTradeEnable", false);
      OFFLINE_CRAFT_ENABLE = config.getBoolean("OfflineCraftEnable", false);
      OFFLINE_MODE_IN_PEACE_ZONE = config.getBoolean("OfflineModeInPeaceZone", false);
      OFFLINE_MODE_NO_DAMAGE = config.getBoolean("OfflineModeNoDamage", false);
      OFFLINE_SET_NAME_COLOR = config.getBoolean("OfflineSetNameColor", false);
      OFFLINE_NAME_COLOR = Integer.decode("0x" + config.getString("OfflineNameColor", "808080"));
      OFFLINE_FAME = config.getBoolean("OfflineFame", true);
      RESTORE_OFFLINERS = config.getBoolean("RestoreOffliners", false);
      OFFLINE_MAX_DAYS = config.getInt("OfflineMaxDays", 10);
      OFFLINE_DISCONNECT_FINISHED = config.getBoolean("OfflineDisconnectFinished", true);
      OFFLINE_DISCONNECT_SAME_ACCOUNT = config.getBoolean("OfflineDisconnectSameAccount", false);
      STORE_OFFLINE_TRADE_IN_REALTIME = config.getBoolean("StoreOfflineTradeInRealtime", true);
      ENABLE_OFFLINE_COMMAND = config.getBoolean("EnableOfflineCommand", true);
      OFFLINE_ABNORMAL_EFFECTS.clear();
      String offlineAbnormalEffects = config.getString("OfflineAbnormalEffect", "").trim();
      if (!offlineAbnormalEffects.isEmpty()) {
         for (String ave : offlineAbnormalEffects.split(",")) {
            OFFLINE_ABNORMAL_EFFECTS.add(Enum.valueOf(AbnormalVisualEffect.class, ave.trim()));
         }
      }
   }
}
