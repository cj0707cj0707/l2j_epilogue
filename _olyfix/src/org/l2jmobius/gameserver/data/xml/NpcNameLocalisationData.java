package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;

public class NpcNameLocalisationData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(NpcNameLocalisationData.class.getName());
   private static final Map<String, Map<Integer, String[]>> NPC_NAME_LOCALISATIONS = new ConcurrentHashMap<>();
   private static String _lang;

   protected NpcNameLocalisationData() {
      this.load();
   }

   @Override
   public void load() {
      NPC_NAME_LOCALISATIONS.clear();
      if (MultilingualSupportConfig.MULTILANG_ENABLE) {
         for (String lang : MultilingualSupportConfig.MULTILANG_ALLOWED) {
            File file = new File("data/lang/" + lang + "/NpcNameLocalisation.xml");
            if (file.isFile()) {
               NPC_NAME_LOCALISATIONS.put(lang, new ConcurrentHashMap<>());
               _lang = lang;
               this.parseDatapackFile("data/lang/" + lang + "/NpcNameLocalisation.xml");
               int count = NPC_NAME_LOCALISATIONS.get(lang).values().size();
               if (count == 0) {
                  NPC_NAME_LOCALISATIONS.remove(lang);
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
         NPC_NAME_LOCALISATIONS.get(_lang).put(set.getInt("id"), new String[]{set.getString("name"), set.getString("title")});
      }));
   }

   public String[] getLocalisation(String lang, int id) {
      Map<Integer, String[]> localisations = NPC_NAME_LOCALISATIONS.get(lang);
      return localisations != null ? localisations.get(id) : null;
   }

   public boolean hasLocalisation(int id) {
      for (Map<Integer, String[]> data : NPC_NAME_LOCALISATIONS.values()) {
         if (data.containsKey(id)) {
            return true;
         }
      }

      return false;
   }

   public static NpcNameLocalisationData getInstance() {
      return NpcNameLocalisationData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final NpcNameLocalisationData INSTANCE = new NpcNameLocalisationData();
   }
}
