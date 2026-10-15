package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.mechanics.fishing.Fish;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class FishData implements IXmlReader {
   private final Map<Integer, Fish> _fishNormal = new HashMap<>();
   private final Map<Integer, Fish> _fishEasy = new HashMap<>();
   private final Map<Integer, Fish> _fishHard = new HashMap<>();

   protected FishData() {
      this.load();
   }

   @Override
   public void load() {
      this._fishEasy.clear();
      this._fishNormal.clear();
      this._fishHard.clear();
      this.parseDatapackFile("data/stats/fishing/fishes.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + (this._fishEasy.size() + this._fishNormal.size() + this._fishHard.size()) + " fishes.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("fish".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  StatSet set = new StatSet();

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  Fish fish = new Fish(set);
                  switch (fish.getFishGrade()) {
                     case 0:
                        this._fishEasy.put(fish.getFishId(), fish);
                        break;
                     case 1:
                        this._fishNormal.put(fish.getFishId(), fish);
                        break;
                     case 2:
                        this._fishHard.put(fish.getFishId(), fish);
                  }
               }
            }
         }
      }
   }

   public List<Fish> getFish(int level, int group, int grade) {
      List<Fish> result = new ArrayList<>();
      Map<Integer, Fish> fish = null;
      switch (grade) {
         case 0:
            fish = this._fishEasy;
            break;
         case 1:
            fish = this._fishNormal;
            break;
         case 2:
            fish = this._fishHard;
            break;
         default:
            LOGGER.warning(this.getClass().getSimpleName() + ": Unmanaged fish grade!");
            return result;
      }

      for (Fish f : fish.values()) {
         if (f.getFishLevel() == level && f.getFishGroup() == group) {
            result.add(f);
         }
      }

      if (result.isEmpty()) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Cannot find any fish for level: " + level + " group: " + group + " and grade: " + grade + "!");
      }

      return result;
   }

   public static FishData getInstance() {
      return FishData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final FishData INSTANCE = new FishData();
   }
}
