package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class PvpTitleColorConfig {
   private static final String PVP_TITLE_CONFIG_FILE = "./config/Custom/PvpTitleColor.ini";
   public static boolean PVP_COLOR_SYSTEM_ENABLED;
   public static int PVP_AMOUNT1;
   public static int PVP_AMOUNT2;
   public static int PVP_AMOUNT3;
   public static int PVP_AMOUNT4;
   public static int PVP_AMOUNT5;
   public static int NAME_COLOR_FOR_PVP_AMOUNT1;
   public static int NAME_COLOR_FOR_PVP_AMOUNT2;
   public static int NAME_COLOR_FOR_PVP_AMOUNT3;
   public static int NAME_COLOR_FOR_PVP_AMOUNT4;
   public static int NAME_COLOR_FOR_PVP_AMOUNT5;
   public static String TITLE_FOR_PVP_AMOUNT1;
   public static String TITLE_FOR_PVP_AMOUNT2;
   public static String TITLE_FOR_PVP_AMOUNT3;
   public static String TITLE_FOR_PVP_AMOUNT4;
   public static String TITLE_FOR_PVP_AMOUNT5;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/PvpTitleColor.ini");
      PVP_COLOR_SYSTEM_ENABLED = config.getBoolean("EnablePvPColorSystem", false);
      PVP_AMOUNT1 = config.getInt("PvpAmount1", 500);
      PVP_AMOUNT2 = config.getInt("PvpAmount2", 1000);
      PVP_AMOUNT3 = config.getInt("PvpAmount3", 1500);
      PVP_AMOUNT4 = config.getInt("PvpAmount4", 2500);
      PVP_AMOUNT5 = config.getInt("PvpAmount5", 5000);
      NAME_COLOR_FOR_PVP_AMOUNT1 = Integer.decode("0x" + config.getString("ColorForAmount1", "00FF00"));
      NAME_COLOR_FOR_PVP_AMOUNT2 = Integer.decode("0x" + config.getString("ColorForAmount2", "00FF00"));
      NAME_COLOR_FOR_PVP_AMOUNT3 = Integer.decode("0x" + config.getString("ColorForAmount3", "00FF00"));
      NAME_COLOR_FOR_PVP_AMOUNT4 = Integer.decode("0x" + config.getString("ColorForAmount4", "00FF00"));
      NAME_COLOR_FOR_PVP_AMOUNT5 = Integer.decode("0x" + config.getString("ColorForAmount5", "00FF00"));
      TITLE_FOR_PVP_AMOUNT1 = config.getString("PvPTitleForAmount1", "Title");
      TITLE_FOR_PVP_AMOUNT2 = config.getString("PvPTitleForAmount2", "Title");
      TITLE_FOR_PVP_AMOUNT3 = config.getString("PvPTitleForAmount3", "Title");
      TITLE_FOR_PVP_AMOUNT4 = config.getString("PvPTitleForAmount4", "Title");
      TITLE_FOR_PVP_AMOUNT5 = config.getString("PvPTitleForAmount5", "Title");
   }
}
