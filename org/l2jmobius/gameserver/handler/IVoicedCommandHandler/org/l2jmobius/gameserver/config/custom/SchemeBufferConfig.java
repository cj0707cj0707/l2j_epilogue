package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

public class SchemeBufferConfig {
   private static final String SCHEME_BUFFER_CONFIG_FILE = "./config/Custom/SchemeBuffer.ini";
   public static int BUFFER_MAX_SCHEMES;
   public static int BUFFER_ITEM_ID;
   public static int BUFFER_STATIC_BUFF_COST;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/SchemeBuffer.ini");
      BUFFER_MAX_SCHEMES = config.getInt("BufferMaxSchemesPerChar", 4);
      BUFFER_ITEM_ID = config.getInt("BufferItemId", 57);
      BUFFER_STATIC_BUFF_COST = config.getInt("BufferStaticCostPerBuff", -1);
   }
}
