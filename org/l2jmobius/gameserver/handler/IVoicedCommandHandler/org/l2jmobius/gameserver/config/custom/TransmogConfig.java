package org.l2jmobius.gameserver.config.custom;

import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.commons.util.ConfigReader;

public class TransmogConfig {
   private static final String TRANSMOG_CONFIG_FILE = "./config/Custom/Transmog.ini";
   public static boolean ENABLE_TRANSMOG;
   public static boolean TRANSMOG_SHARE_ACCOUNT;
   public static int TRANSMOG_APPLY_COST;
   public static int TRANSMOG_REMOVE_COST;
   public static Set<Integer> TRANSMOG_BANNED_ITEM_IDS = new HashSet<>();

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Custom/Transmog.ini");
      ENABLE_TRANSMOG = config.getBoolean("TransmogEnabled", false);
      TRANSMOG_SHARE_ACCOUNT = config.getBoolean("TransmogShareAccount", false);
      TRANSMOG_APPLY_COST = config.getInt("TransmogApplyCost", 0);
      TRANSMOG_REMOVE_COST = config.getInt("TransmogRemoveCost", 0);
      TRANSMOG_BANNED_ITEM_IDS.clear();
      String transmogBannedItemIds = config.getString("TransmogBannedItemIds", "");
      if (!transmogBannedItemIds.isEmpty()) {
         for (String s : transmogBannedItemIds.split(",")) {
            TRANSMOG_BANNED_ITEM_IDS.add(Integer.parseInt(s.trim()));
         }
      }
   }
}
