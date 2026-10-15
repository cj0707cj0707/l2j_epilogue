package org.l2jmobius.gameserver.config.custom;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;

public class OfflinePlayConfig {
   private static final String OFFLINE_PLAY_CONFIG_FILE = "./config/Custom/OfflinePlay.ini";
   public static boolean ENABLE_OFFLINE_PLAY_COMMAND;
   public static boolean RESTORE_AUTO_PLAY_OFFLINERS;
   public static boolean OFFLINE_PLAY_PREMIUM;
   public static boolean OFFLINE_PLAY_LOGOUT_ON_DEATH;
   public static boolean OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT;
   public static String OFFLINE_PLAY_LOGIN_MESSAGE;
   public static boolean OFFLINE_PLAY_SET_NAME_COLOR;
   public static int OFFLINE_PLAY_NAME_COLOR;
   public static List<AbnormalVisualEffect> OFFLINE_PLAY_ABNORMAL_EFFECTS = new ArrayList<>();

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/OfflinePlay.ini");
      ENABLE_OFFLINE_PLAY_COMMAND = config.getBoolean("EnableOfflinePlayCommand", false);
      RESTORE_AUTO_PLAY_OFFLINERS = config.getBoolean("RestoreAutoPlayOffliners", true);
      OFFLINE_PLAY_PREMIUM = config.getBoolean("OfflinePlayPremium", false);
      OFFLINE_PLAY_LOGOUT_ON_DEATH = config.getBoolean("OfflinePlayLogoutOnDeath", true);
      OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT = config.getBoolean("OfflinePlayDisconnectSameAccount", false);
      OFFLINE_PLAY_LOGIN_MESSAGE = config.getString("OfflinePlayLoginMessage", "");
      OFFLINE_PLAY_SET_NAME_COLOR = config.getBoolean("OfflinePlaySetNameColor", false);
      OFFLINE_PLAY_NAME_COLOR = Integer.decode("0x" + config.getString("OfflinePlayNameColor", "808080"));
      OFFLINE_PLAY_ABNORMAL_EFFECTS.clear();
      String offlinePlayAbnormalEffects = config.getString("OfflinePlayAbnormalEffect", "").trim();
      if (!offlinePlayAbnormalEffects.isEmpty()) {
         for (String ave : offlinePlayAbnormalEffects.split(",")) {
            OFFLINE_PLAY_ABNORMAL_EFFECTS.add(Enum.valueOf(AbnormalVisualEffect.class, ave.trim()));
         }
      }
   }
}
