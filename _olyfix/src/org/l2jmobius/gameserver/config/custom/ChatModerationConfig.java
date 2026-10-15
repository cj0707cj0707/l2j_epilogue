package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class ChatModerationConfig {
   private static final String CHAT_MODERATION_CONFIG_FILE = "./config/Custom/ChatModeration.ini";
   public static boolean CHAT_ADMIN;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/ChatModeration.ini");
      CHAT_ADMIN = config.getBoolean("ChatAdmin", true);
   }
}
