package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.mechanics.fishing.FishingMonster;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class FishingMonstersData implements IXmlReader {
   private final Map<Integer, FishingMonster> _fishingMonstersData = new HashMap<>();

   protected FishingMonstersData() {
      this.load();
   }

   @Override
   public void load() {
      this._fishingMonstersData.clear();
      this.parseDatapackFile("data/stats/fishing/fishingMonsters.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._fishingMonstersData.size() + " fishing monsters.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("fishingMonster".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  StatSet set = new StatSet();

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  FishingMonster fishingMonster = new FishingMonster(set);
                  this._fishingMonstersData.put(fishingMonster.getFishingMonsterId(), fishingMonster);
               }
            }
         }
      }
   }

   public FishingMonster getFishingMonster(int level) {
      for (FishingMonster fishingMonster : this._fishingMonstersData.values()) {
         if (level >= fishingMonster.getUserMinLevel() && level <= fishingMonster.getUserMaxLevel()) {
            return fishingMonster;
         }
      }

      return null;
   }

   public FishingMonster getFishingMonsterById(int id) {
      return this._fishingMonstersData.get(id);
   }

   public static FishingMonstersData getInstance() {
      return FishingMonstersData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final FishingMonstersData INSTANCE = new FishingMonstersData();
   }
}
