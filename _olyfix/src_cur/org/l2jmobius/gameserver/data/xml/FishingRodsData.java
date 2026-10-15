package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.mechanics.fishing.FishingRod;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class FishingRodsData implements IXmlReader {
   private final Map<Integer, FishingRod> _fishingRods = new HashMap<>();

   protected FishingRodsData() {
      this.load();
   }

   @Override
   public void load() {
      this._fishingRods.clear();
      this.parseDatapackFile("data/stats/fishing/fishingRods.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._fishingRods.size() + " fishing rods.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("fishingRod".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  StatSet set = new StatSet();

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  FishingRod fishingRod = new FishingRod(set);
                  this._fishingRods.put(fishingRod.getFishingRodItemId(), fishingRod);
               }
            }
         }
      }
   }

   public FishingRod getFishingRod(int itemId) {
      return this._fishingRods.get(itemId);
   }

   public static FishingRodsData getInstance() {
      return FishingRodsData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final FishingRodsData INSTANCE = new FishingRodsData();
   }
}
