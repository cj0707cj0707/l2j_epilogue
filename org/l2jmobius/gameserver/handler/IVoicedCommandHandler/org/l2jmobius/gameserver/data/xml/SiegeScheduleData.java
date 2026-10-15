package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.mechanics.siege.SiegeScheduleDate;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SiegeScheduleData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(SiegeScheduleData.class.getName());
   private final Map<Integer, SiegeScheduleDate> _scheduleData = new HashMap<>();

   protected SiegeScheduleData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this.parseDatapackFile("config/SiegeSchedule.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._scheduleData.size() + " siege schedulers.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
               String var5 = cd.getNodeName();
               switch (var5) {
                  case "schedule":
                     StatSet set = new StatSet();
                     NamedNodeMap attrs = cd.getAttributes();

                     for (int i = 0; i < attrs.getLength(); i++) {
                        Node node = attrs.item(i);
                        String key = node.getNodeName();
                        String val = node.getNodeValue();
                        if ("day".equals(key) && !StringUtil.isNumeric(val)) {
                           val = Integer.toString(this.getValueForField(val));
                        }

                        set.set(key, val);
                     }

                     this._scheduleData.put(set.getInt("castleId"), new SiegeScheduleDate(set));
               }
            }
         }
      }
   }

   private int getValueForField(String field) {
      try {
         return Calendar.class.getField(field).getInt(Calendar.class.getName());
      } catch (Exception var3) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Could not get value for field " + field + ". " + var3.getMessage());
         return -1;
      }
   }

   public SiegeScheduleDate getScheduleDateForCastleId(int castleId) {
      return this._scheduleData.get(castleId);
   }

   public static SiegeScheduleData getInstance() {
      return SiegeScheduleData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SiegeScheduleData INSTANCE = new SiegeScheduleData();
   }
}
