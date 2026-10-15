package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DynamicExpRateData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(DynamicExpRateData.class.getName());
   private static float[] _expRates = new float[PlayerConfig.PLAYER_MAXIMUM_LEVEL + 1];
   private static float[] _spRates = new float[PlayerConfig.PLAYER_MAXIMUM_LEVEL + 1];
   private static boolean _enabled = false;

   protected DynamicExpRateData() {
      this.load();
   }

   @Override
   public void load() {
      this.parseDatapackFile("config/DynamicExpRates.xml");
   }

   @Override
   public void parseDocument(Document document, File file) {
      int count = 0;
      Arrays.fill(_expRates, 1.0F);
      Arrays.fill(_spRates, 1.0F);

      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equals(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("dynamic".equals(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  int level = this.parseInteger(attrs, "level");
                  float exp = this.parseFloat(attrs, "exp");
                  float sp = this.parseFloat(attrs, "sp");
                  if (exp != 1.0F || sp != 1.0F) {
                     _expRates[level] = exp;
                     _spRates[level] = sp;
                     count++;
                  }
               }
            }
         }
      }

      _enabled = count > 0;
      if (_enabled) {
         LOGGER.info(this.getClass().getSimpleName() + ": Loaded dynamic rates for " + count + " levels.");
      }
   }

   public float getDynamicExpRate(int level) {
      return _expRates[level];
   }

   public float getDynamicSpRate(int level) {
      return _spRates[level];
   }

   public boolean isEnabled() {
      return _enabled;
   }

   public static DynamicExpRateData getInstance() {
      return DynamicExpRateData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final DynamicExpRateData INSTANCE = new DynamicExpRateData();
   }
}
