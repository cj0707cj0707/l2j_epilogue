package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;

public class SendMessageLocalisationData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(SendMessageLocalisationData.class.getName());
   private static final String SPLIT_STRING = "XXX";
   private static final Map<String, Map<String[], String[]>> SEND_MESSAGE_LOCALISATIONS = new ConcurrentHashMap<>();
   private static String _lang;

   protected SendMessageLocalisationData() {
      this.load();
   }

   @Override
   public void load() {
      SEND_MESSAGE_LOCALISATIONS.clear();
      if (MultilingualSupportConfig.MULTILANG_ENABLE) {
         for (String lang : MultilingualSupportConfig.MULTILANG_ALLOWED) {
            File file = new File("data/lang/" + lang + "/SendMessageLocalisation.xml");
            if (file.isFile()) {
               SEND_MESSAGE_LOCALISATIONS.put(lang, new ConcurrentHashMap<>());
               _lang = lang;
               this.parseDatapackFile("data/lang/" + lang + "/SendMessageLocalisation.xml");
               int count = SEND_MESSAGE_LOCALISATIONS.get(lang).values().size();
               if (count == 0) {
                  SEND_MESSAGE_LOCALISATIONS.remove(lang);
               } else {
                  LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded localisations for [" + lang + "].");
               }
            }
         }
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(document, "list", listNode -> this.forEach(listNode, "localisation", localisationNode -> {
         StatSet set = new StatSet(this.parseAttributes(localisationNode));
         SEND_MESSAGE_LOCALISATIONS.get(_lang).put(set.getString("message").split("XXX"), set.getString("translation").split("XXX"));
      }));
   }

   public static String getLocalisation(Player player, String message) {
      if (MultilingualSupportConfig.MULTILANG_ENABLE && player != null) {
         Map<String[], String[]> localisations = SEND_MESSAGE_LOCALISATIONS.get(player.getLang());
         if (localisations != null) {
            String localisation = message;

            for (Entry<String[], String[]> entry : localisations.entrySet()) {
               String[] searchMessage = entry.getKey();
               String[] replacementMessage = entry.getValue();
               if (searchMessage.length == 1) {
                  if (searchMessage[0].equals(localisation)) {
                     return replacementMessage[0];
                  }
               } else {
                  boolean found = true;

                  for (String part : searchMessage) {
                     if (!localisation.contains(part)) {
                        found = false;
                        break;
                     }
                  }

                  if (found) {
                     for (int i = 0; i < searchMessage.length; i++) {
                        localisation = localisation.replace(searchMessage[i], replacementMessage[i]);
                     }
                     break;
                  }
               }
            }

            return localisation;
         }
      }

      return message;
   }

   public static SendMessageLocalisationData getInstance() {
      return SendMessageLocalisationData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SendMessageLocalisationData INSTANCE = new SendMessageLocalisationData();
   }
}
