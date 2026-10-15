package org.l2jmobius.gameserver.config.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.config.GeneralConfig;

public class MultilingualSupportConfig {
   private static final Logger LOGGER = Logger.getLogger(MultilingualSupportConfig.class.getName());
   private static final String MULTILANGUAL_SUPPORT_CONFIG_FILE = "./config/Custom/MultilingualSupport.ini";
   public static String MULTILANG_DEFAULT;
   public static boolean MULTILANG_ENABLE;
   public static List<String> MULTILANG_ALLOWED = new ArrayList<>();
   public static boolean MULTILANG_VOICED_ALLOW;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/MultilingualSupport.ini");
      MULTILANG_DEFAULT = config.getString("MultiLangDefault", "en").toLowerCase();
      MULTILANG_ENABLE = config.getBoolean("MultiLangEnable", false);
      if (MULTILANG_ENABLE) {
         GeneralConfig.CHECK_HTML_ENCODING = false;
      }

      String[] allowed = config.getString("MultiLangAllowed", MULTILANG_DEFAULT).split(";");
      MULTILANG_ALLOWED = new ArrayList<>(allowed.length);

      for (String lang : allowed) {
         MULTILANG_ALLOWED.add(lang.toLowerCase());
      }

      if (!MULTILANG_ALLOWED.contains(MULTILANG_DEFAULT)) {
         LOGGER.warning("MultiLang[MultilingualSupportConfig.load()]: default language: " + MULTILANG_DEFAULT + " is not in allowed list!");
      }

      MULTILANG_VOICED_ALLOW = config.getBoolean("MultiLangVoiceCommand", true);
   }
}
