package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class CaptchaConfig {
   private static final String CAPTCHA_CONFIG_FILE = "./config/Custom/Captcha.ini";
   public static boolean ENABLE_CAPTCHA;
   public static int KILL_COUNTER;
   public static int KILL_COUNTER_RANDOMIZATION;
   public static boolean KILL_COUNTER_RESET;
   public static int KILL_COUNTER_RESET_TIME;
   public static int VALIDATION_TIME;
   public static int CAPTCHA_ATTEMPTS;
   public static int PUNISHMENT;
   public static int JAIL_TIME;
   public static boolean DOUBLE_JAIL_TIME;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/Captcha.ini");
      ENABLE_CAPTCHA = config.getBoolean("EnableCaptcha", false);
      KILL_COUNTER = config.getInt("KillCounter", 100);
      KILL_COUNTER_RANDOMIZATION = config.getInt("KillCounterRandomization", 50);
      KILL_COUNTER_RESET = config.getBoolean("KillCounterReset", false);
      KILL_COUNTER_RESET_TIME = config.getInt("KillCounterResetTime", 20) * 60000;
      VALIDATION_TIME = config.getInt("ValidationTime", 60);
      CAPTCHA_ATTEMPTS = config.getInt("CaptchaAttempts", 2);
      PUNISHMENT = config.getInt("Punishment", 0);
      JAIL_TIME = config.getInt("JailTime", 2);
      DOUBLE_JAIL_TIME = config.getBoolean("DoubleJailTime", false);
   }
}
