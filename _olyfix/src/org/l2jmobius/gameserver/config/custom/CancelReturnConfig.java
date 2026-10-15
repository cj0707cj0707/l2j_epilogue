package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class CancelReturnConfig {
   private static final String CANCEL_RETURN_CONFIG_FILE = "./config/Custom/CancelReturn.ini";
   public static boolean CANCEL_RETURN_ON;
   public static boolean CANCEL_RETURN_MOB;
   public static boolean CANCEL_RETURN_PLAYER;
   public static boolean CANCEL_RETURN_PLAYER_OLYS;
   public static int TIME_TO_RETURN;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/CancelReturn.ini");
      CANCEL_RETURN_ON = config.getBoolean("CancelReturn", false);
      CANCEL_RETURN_MOB = config.getBoolean("ReturnMonster", true);
      CANCEL_RETURN_PLAYER = config.getBoolean("ReturnPlayer", true);
      CANCEL_RETURN_PLAYER_OLYS = config.getBoolean("ReturnPlayerOlys", false);
      TIME_TO_RETURN = config.getInt("TimeToReturn", 10) * 1000;
   }
}
